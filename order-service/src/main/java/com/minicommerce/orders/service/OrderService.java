package com.minicommerce.orders.service;

import com.minicommerce.orders.domain.*;
import com.minicommerce.orders.events.EventEnvelope;
import com.minicommerce.orders.events.OrderCancelledEvent;
import com.minicommerce.orders.events.OrderCreatedEvent;
import com.minicommerce.orders.events.Topics;
import com.minicommerce.orders.outbox.OutboxService;
import com.minicommerce.orders.repository.CustomerRepository;
import com.minicommerce.orders.repository.OrderRepository;
import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OutboxService outboxService;
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository, OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public Order create(CreateOrderRequest orderRequest) {
        if (!customerRepository.existsById(orderRequest.customerId())) {
            // auto-create minimal customer for demo purposes
            Customer c = new Customer();
            c.setId(orderRequest.customerId());
            c.setEmail(orderRequest.customerId() + "@demo.local");
            c.setName("Demo Customer");
            customerRepository.save(c);
        }

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId(orderRequest.customerId());
        order.setCurrency(orderRequest.currency());
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : orderRequest.items()) {
            OrderItem item = new OrderItem();
            item.setId(UUID.randomUUID());
            item.setSku(itemRequest.sku());
            item.setName(itemRequest.name());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice());
            order.addItem(item);
            total = total.add(itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }
        order.setTotal(total);
        Order saved = orderRepository.save(order);

        var items = saved.getItems().stream()
                .map(i -> new OrderCreatedEvent.Item(i.getSku(), i.getName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        var payload = new OrderCreatedEvent(
                saved.getId(),
                saved.getCustomerId(),
                saved.getCurrency(),
                saved.getTotal(),
                saved.getCreatedAt(),
                items
        );
        var envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "order.created",
                "v1",
                OffsetDateTime.now(),
                "order",
                saved.getId().toString(),
                UUID.randomUUID().toString(),
                payload
        );
        outboxService.persistEvent(Topics.ORDER_CREATED, saved.getId().toString(), "order", saved.getId().toString(), envelope);

        return saved;
    }

    public Order get(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public Page<Order> list(OrderStatus status, Pageable pageable) {
        if (status == null) return orderRepository.findAll(pageable);
        return orderRepository.findByStatus(status, pageable);
    }

    @Transactional
    public Order cancel(UUID id) {
        Order existing = get(id);
        if (existing.getStatus() == OrderStatus.CANCELLED || existing.getStatus() == OrderStatus.FULFILLED || existing.getStatus() == OrderStatus.REFUNDED) {
            throw new IllegalStateException("Cannot cancel order in status: " + existing.getStatus());
        }
        existing.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(existing);

        var payload = new OrderCancelledEvent(
                saved.getId(),
                OffsetDateTime.now(),
                null
        );
        var envelope = new EventEnvelope<>(
                UUID.randomUUID(),
                "order.cancelled",
                "v1",
                OffsetDateTime.now(),
                "order",
                saved.getId().toString(),
                UUID.randomUUID().toString(),
                payload
        );
        outboxService.persistEvent(Topics.ORDER_CANCELLED, saved.getId().toString(), "order", saved.getId().toString(), envelope);

        return saved;
    }
}
