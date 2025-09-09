package com.minicommerce.orders.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        String sku,
        String name,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) { }
