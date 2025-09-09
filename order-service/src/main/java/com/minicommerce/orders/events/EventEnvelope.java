package com.minicommerce.orders.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String type,
        String version,
        OffsetDateTime occurredAt,
        String aggregateType,
        String aggregateId,
        String traceId,
        T data
) {}

