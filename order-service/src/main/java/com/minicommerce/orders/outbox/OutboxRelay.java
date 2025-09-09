package com.minicommerce.orders.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerce.orders.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private final OutboxEventRepository repository;
    private final OutboxProperties properties;
    private final EventPublisher publisher;
    private final ObjectMapper objectMapper;

    public OutboxRelay(OutboxEventRepository repository, OutboxProperties properties, EventPublisher publisher, ObjectMapper objectMapper) {
        this.repository = repository;
        this.properties = properties;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.relay-interval-ms:1000}")
    @Transactional
    public void dispatchDueEvents() {
        var due = repository.findBatchDue(OffsetDateTime.now(), properties.getBatchSize());
        if (due.isEmpty()) {
            return;
        }
        for (var row : due) {
            try {
                Object payloadToSend;
                try {
                    payloadToSend = objectMapper.readValue(row.getPayload(), JsonNode.class);
                } catch (Exception parseEx) {
                    log.warn("Failed to parse stored outbox JSON, sending raw string topic={} key={}", row.getTopic(), row.getKey());
                    payloadToSend = row.getPayload();
                }
                publisher.publish(row.getTopic(), row.getKey(), payloadToSend);
                row.setStatus(OutboxStatus.SENT);
            } catch (Exception e) {
                int attempt = row.getAttempts() + 1;
                row.setAttempts(attempt);
                boolean overMax = attempt >= properties.getMaxAttempts();
                row.setStatus(overMax ? OutboxStatus.FAILED : OutboxStatus.RETRY);
                long backoff = (long) (properties.getInitialBackoffMs() * Math.pow(properties.getBackoffMultiplier(), Math.max(0, attempt - 1)));
                row.setNextAttemptAt(OffsetDateTime.now().plusNanos(backoff * 1_000_000));
                row.setLastError(e.getMessage());
                log.warn("Outbox publish failure topic={} key={} attempt={} status={}: {}", row.getTopic(), row.getKey(), attempt, row.getStatus(), e.getMessage());
            }
        }
    }
}

