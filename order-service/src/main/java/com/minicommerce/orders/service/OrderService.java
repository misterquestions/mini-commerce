package com.minicommerce.orders.service;

import com.minicommerce.orders.domain.*;
import com.minicommerce.orders.events.EventPublisher;
import com.minicommerce.orders.events.OrderCancelledEvent;
import com.minicommerce.orders.events.OrderCreatedEvent;
import com.minicommerce.orders.events.Topics;
import com.minicommerce.orders.repository.CustomerRepository;
import com.minicommerce.orders.repository.OrderRepository;
import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final EventPublisher events;
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public OrderService(OrderRepository orders, CustomerRepository customers, EventPublisher events) {
        this.orders = orders;
        this.customers = customers;
        this.events = events;
    }

    @Transactional
    public Order create(CreateOrderRequest orderRequest) {
        if (!customers.existsById(orderRequest.customerId())) {
            // auto-create minimal customer for demo purposes
            Customer c = new Customer();
            c.setId(orderRequest.customerId());
            c.setEmail(orderRequest.customerId() + "@demo.local");
            c.setName("Demo Customer");
            customers.save(c);
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
        Order saved = orders.save(order);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                var items = saved.getItems().stream()
                        .map(i -> new OrderCreatedEvent.Item(i.getSku(), i.getName(), i.getQuantity(), i.getUnitPrice()))
                        .toList();
                try {
                    events.publish(Topics.ORDER_CREATED, saved.getId().toString(),
                            new OrderCreatedEvent(
                                    "order.created",
                                    "v1",
                                    saved.getId(),
                                    saved.getCustomerId(),
                                    saved.getCurrency(),
                                    saved.getTotal(),
                                    saved.getCreatedAt(),
                                    items
                            ));
                } catch (RuntimeException e) {
                    log.error("Failed to publish ORDER_CREATED event for order {}: {}", saved.getId(), e.getMessage(), e);
                }
            }
        });

        return saved;
    }

    public Order get(UUID id) {
        return orders.findById(id).orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public Page<Order> list(OrderStatus status, Pageable pageable) {
        if (status == null) return orders.findAll(pageable);
        return orders.findByStatus(status, pageable);
    }

    @Transactional
    public Order cancel(UUID id) {
        Order o = get(id);
        if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.FULFILLED || o.getStatus() == OrderStatus.REFUNDED) {
            throw new IllegalStateException("Cannot cancel order in status: " + o.getStatus());
        }
        o.setStatus(OrderStatus.CANCELLED);
        Order saved = orders.save(o);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    events.publish(Topics.ORDER_CANCELLED, saved.getId().toString(),
                            new OrderCancelledEvent(
                                    "order.cancelled",
                                    "v1",
                                    saved.getId(),
                                    OffsetDateTime.now(),
                                    null
                            ));
                } catch (RuntimeException e) {
                    log.error("Failed to publish ORDER_CANCELLED event for order {}: {}", saved.getId(), e.getMessage(), e);
                }
            }
        });

        return saved;
    }
}
