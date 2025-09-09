package com.minicommerce.orders.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        OffsetDateTime cancelledAt,
        String reason
) {}
