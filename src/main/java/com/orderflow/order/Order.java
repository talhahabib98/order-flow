package com.orderflow.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    private String customerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items")
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String statusReason;

    private String trackingNumber;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private long version;

    protected Order() {
    }

    public Order(String customerId, List<OrderItem> items) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.items = new ArrayList<>(items);
        this.total = items.stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void moveTo(OrderStatus target, String reason) {
        if (!status.canMoveTo(target)) {
            throw new InvalidOrderStateException(id, status, target);
        }
        this.status = target;
        this.statusReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markShipped(String trackingNumber) {
        moveTo(OrderStatus.SHIPPED, null);
        this.trackingNumber = trackingNumber;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
