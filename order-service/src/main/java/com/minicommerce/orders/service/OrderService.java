package com.minicommerce.orders.service;

import com.minicommerce.orders.domain.*;
import com.minicommerce.orders.repository.CustomerRepository;
import com.minicommerce.orders.repository.OrderRepository;
import com.minicommerce.orders.web.dto.CreateOrderRequest;
import com.minicommerce.orders.web.dto.OrderItemRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final CustomerRepository customers;

    public OrderService(OrderRepository orders, CustomerRepository customers) {
        this.orders = orders;
        this.customers = customers;
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
        return orders.save(order);
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
        return orders.save(o);
    }
}
