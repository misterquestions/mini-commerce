package com.minicommerce.orders.events;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        String type,
        String version,
        UUID orderId,
        UUID customerId,
        String currency,
        BigDecimal total,
        OffsetDateTime createdAt,
        List<Item> items
) {
    public static record Item(String sku, String name, int quantity, BigDecimal unitPrice) {}
}
