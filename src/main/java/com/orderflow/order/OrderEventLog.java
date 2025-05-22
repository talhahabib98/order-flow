package com.orderflow.order;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Append-only audit trail of every event published for an order. */
@Entity
@Table(name = "order_event_log", indexes = @Index(columnList = "orderId"))
public class OrderEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID orderId;

    private String type;

    @Column(length = 4000)
    private String payload;

    private Instant occurredAt;

    protected OrderEventLog() {
    }

    public OrderEventLog(UUID orderId, String type, String payload, Instant occurredAt) {
        this.orderId = orderId;
        this.type = type;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
