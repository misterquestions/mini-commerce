package com.minicommerce.orders.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    // getters/setters
    public UUID getId(){ return id; }
    public void setId(UUID id){ this.id = id; }
    public Order getOrder(){ return order; }
    public void setOrder(Order order){ this.order = order; }
    public String getSku(){ return sku; }
    public void setSku(String sku){ this.sku = sku; }
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public Integer getQuantity(){ return quantity; }
    public void setQuantity(Integer quantity){ this.quantity = quantity; }
    public BigDecimal getUnitPrice(){ return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice){ this.unitPrice = unitPrice; }
}
