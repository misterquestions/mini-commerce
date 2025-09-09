package com.minicommerce.orders.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerce.orders.events.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists serialized event envelopes into the outbox table inside the same DB transaction
 * as the aggregate state change.
 */
@Service
public class OutboxService {
    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void persistEvent(String topic,
                              String messageKey,
                              String aggregateType,
                              String aggregateId,
                              EventEnvelope<?> envelope) {
        try {
            var serialized = objectMapper.writeValueAsString(envelope);
            OutboxEvent row = new OutboxEvent();
            row.setAggregateId(aggregateId);
            row.setAggregateType(aggregateType);
            row.setTopic(topic);
            row.setKey(messageKey);
            row.setPayload(serialized);
            row.setStatus(OutboxStatus.NEW);
            row.setAttempts(0);
            outboxRepository.save(row);
        } catch (Exception e) {
            log.error("Failed to serialize envelope for topic={} key={} : {}", topic, messageKey, e.getMessage(), e);
            throw new IllegalStateException("Failed to persist outbox event", e);
        }
    }
}

