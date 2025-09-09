package com.minicommerce.orders.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item){
        item.setOrder(this);
        this.items.add(item);
    }

    // getters/setters
    public UUID getId(){ return id; }
    public void setId(UUID id){ this.id = id; }
    public UUID getCustomerId(){ return customerId; }
    public void setCustomerId(UUID customerId){ this.customerId = customerId; }
    public OrderStatus getStatus(){ return status; }
    public void setStatus(OrderStatus status){ this.status = status; }
    public String getCurrency(){ return currency; }
    public void setCurrency(String currency){ this.currency = currency; }
    public BigDecimal getTotal(){ return total; }
    public void setTotal(BigDecimal total){ this.total = total; }
    public OffsetDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt){ this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt(){ return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt){ this.updatedAt = updatedAt; }
    public List<OrderItem> getItems(){ return items; }
    public void setItems(List<OrderItem> items){ this.items = items; }
}
