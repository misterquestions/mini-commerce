package com.minicommerce.orders.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @NotNull @Positive Integer quantity,
        @NotNull @DecimalMin(value="0.0", inclusive=true) BigDecimal unitPrice
) { }
