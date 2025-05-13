package com.orderflow.shared;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Event contracts shared by all modules. Modules talk to each other only through these. */
public final class OrderEvents {

    private OrderEvents() {
    }

    public interface OrderEvent {
        UUID orderId();

        Instant occurredAt();

        default String type() {
            return getClass().getSimpleName();
        }
    }

    public record Line(String sku, int quantity) {
    }

    public record OrderCreated(UUID orderId, Instant occurredAt, List<Line> lines, BigDecimal total) implements OrderEvent {
    }

    public record InventoryReserved(UUID orderId, Instant occurredAt) implements OrderEvent {
    }

    public record InventoryRejected(UUID orderId, Instant occurredAt, String reason) implements OrderEvent {
    }

    public record InventoryReleased(UUID orderId, Instant occurredAt) implements OrderEvent {
    }

    public record PaymentRequested(UUID orderId, Instant occurredAt, BigDecimal amount) implements OrderEvent {
    }

    public record PaymentCompleted(UUID orderId, Instant occurredAt, String transactionId) implements OrderEvent {
    }

    public record PaymentFailed(UUID orderId, Instant occurredAt, String reason) implements OrderEvent {
    }

    public record PaymentRefunded(UUID orderId, Instant occurredAt, String transactionId) implements OrderEvent {
    }

    public record OrderConfirmed(UUID orderId, Instant occurredAt) implements OrderEvent {
    }

    public record OrderShipped(UUID orderId, Instant occurredAt, String trackingNumber) implements OrderEvent {
    }

    public record OrderCancelled(UUID orderId, Instant occurredAt, String reason) implements OrderEvent {
    }

    /** Asks inventory to release stock and payment to refund; both handlers are idempotent. */
    public record CompensationRequested(UUID orderId, Instant occurredAt) implements OrderEvent {
    }
}
