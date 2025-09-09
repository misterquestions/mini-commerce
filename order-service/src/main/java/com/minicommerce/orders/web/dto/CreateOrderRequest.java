package com.minicommerce.orders.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID customerId,
        @NotBlank @Size(min=3, max=3) String currency,
        @NotEmpty List<OrderItemRequest> items
) { }

