package com.minicommerce.orders.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerce.orders.events.EventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
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
    private final MeterRegistry meterRegistry;
    private final AtomicLong pendingGauge = new AtomicLong(0);
    private final AtomicLong failedGauge = new AtomicLong(0);
    private final AtomicLong oldestAgeGauge = new AtomicLong(0);

    public OutboxRelay(OutboxEventRepository repository, OutboxProperties properties, EventPublisher publisher, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.properties = properties;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        meterRegistry.gauge("orders.outbox.pending", pendingGauge);
        meterRegistry.gauge("orders.outbox.failed", failedGauge);
        meterRegistry.gauge("orders.outbox.backlog.oldest_age_seconds", oldestAgeGauge);
    }

    @Scheduled(fixedDelayString = "${outbox.relay-interval-ms:1000}")
    @Transactional
    public void dispatchDueEvents() {
        Timer.Sample sample = Timer.start(meterRegistry);
        var due = repository.findBatchDue(OffsetDateTime.now(), properties.getBatchSize());
        if (!due.isEmpty()) {
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
                    meterRegistry.counter("orders.outbox.publish.attempt", "status", "success").increment();
                } catch (Exception e) {
                    int attempt = row.getAttempts() + 1;
                    row.setAttempts(attempt);
                    boolean overMax = attempt >= properties.getMaxAttempts();
                    row.setStatus(overMax ? OutboxStatus.FAILED : OutboxStatus.RETRY);
                    long backoff = (long) (properties.getInitialBackoffMs() * Math.pow(properties.getBackoffMultiplier(), Math.max(0, attempt - 1)));
                    row.setNextAttemptAt(OffsetDateTime.now().plusNanos(backoff * 1_000_000));
                    row.setLastError(e.getMessage());
                    meterRegistry.counter("orders.outbox.publish.attempt", "status", "failure").increment();
                    log.warn("Outbox publish failure topic={} key={} attempt={} status={}: {}", row.getTopic(), row.getKey(), attempt, row.getStatus(), e.getMessage());
                }
            }
        }
        sample.stop(meterRegistry.timer("orders.outbox.relay.batch.duration"));
        refreshGauges();
    }

    private void refreshGauges() {
        long pending = repository.countByStatus(OutboxStatus.NEW) + repository.countByStatus(OutboxStatus.RETRY);
        long failed = repository.countByStatus(OutboxStatus.FAILED);
        var oldest = repository.findOldestCreatedAtForStatuses(List.of(OutboxStatus.NEW, OutboxStatus.RETRY));
        long oldestAgeSeconds = 0;
        if (oldest != null) {
            oldestAgeSeconds = java.time.Duration.between(oldest, OffsetDateTime.now()).getSeconds();
        }
        pendingGauge.set(pending);
        failedGauge.set(failed);
        oldestAgeGauge.set(oldestAgeSeconds);
    }
}
