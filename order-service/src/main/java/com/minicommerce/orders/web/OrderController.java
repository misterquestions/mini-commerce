package com.minicommerce.orders.web;

import com.minicommerce.orders.domain.Order;
import com.minicommerce.orders.domain.OrderStatus;
import com.minicommerce.orders.service.OrderService;
import com.minicommerce.orders.util.OrderMapper;
import com.minicommerce.orders.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody @Valid CreateOrderRequest orderRequest) {
        Order created = orderService.create(orderRequest);
        OrderResponse body = OrderMapper.toResponse(created);
        return ResponseEntity
                .created(URI.create("/api/v1/orders/" + created.getId()))
                .body(body);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return OrderMapper.toResponse(orderService.get(id));
    }

    @GetMapping
    public PageResponse<OrderResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        OrderStatus orderStatus = null;

        if (status != null) {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        }

        Page<Order> orders = orderService.list(orderStatus, PageRequest.of(page, size));
        return new PageResponse<>(
                orders.map(OrderMapper::toResponse).getContent(),
                orders.getNumber(), orders.getSize(), orders.getTotalElements(), orders.getTotalPages()
        );
    }

    @PatchMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) {
        return OrderMapper.toResponse(orderService.cancel(id));
    }
}
