package com.minicommerce.orders.outbox;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class OutboxHealthIndicator implements HealthIndicator {
    private final OutboxEventRepository repository;

    public OutboxHealthIndicator(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public Health health() {
        long pending = repository.countByStatus(OutboxStatus.NEW) + repository.countByStatus(OutboxStatus.RETRY);
        long failed = repository.countByStatus(OutboxStatus.FAILED);
        OffsetDateTime oldest = repository.findOldestCreatedAtForStatuses(List.of(OutboxStatus.NEW, OutboxStatus.RETRY));
        Long oldestAgeSeconds = oldest == null ? 0L : Duration.between(oldest, OffsetDateTime.now()).getSeconds();
        Health.Builder builder = failed > 0 ? Health.status("DEGRADED") : Health.up();
        return builder.withDetail("outbox.pending", pending)
                .withDetail("outbox.failed", failed)
                .withDetail("outbox.oldestAgeSeconds", oldestAgeSeconds)
                .build();
    }
}

