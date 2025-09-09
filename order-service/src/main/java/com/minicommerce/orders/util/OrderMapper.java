package com.minicommerce.orders.util;

import com.minicommerce.orders.domain.Order;
import com.minicommerce.orders.domain.OrderItem;
import com.minicommerce.orders.web.dto.OrderItemResponse;
import com.minicommerce.orders.web.dto.OrderResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public final class OrderMapper {
    private OrderMapper() {}

    public static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderMapper::toItem)
                .collect(Collectors.toList());
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus().name().toLowerCase(),
                order.getCurrency(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }

    private static OrderItemResponse toItem(OrderItem it) {
        BigDecimal line = it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
        return new OrderItemResponse(it.getId(), it.getSku(), it.getName(), it.getQuantity(), it.getUnitPrice(), line);
    }
}
