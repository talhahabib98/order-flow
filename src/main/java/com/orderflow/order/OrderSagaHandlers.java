package com.orderflow.order;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.orderflow.shared.OrderEvents.CompensationRequested;
import com.orderflow.shared.OrderEvents.InventoryRejected;
import com.orderflow.shared.OrderEvents.InventoryReserved;
import com.orderflow.shared.OrderEvents.OrderCancelled;
import com.orderflow.shared.OrderEvents.OrderConfirmed;
import com.orderflow.shared.OrderEvents.OrderShipped;
import com.orderflow.shared.OrderEvents.PaymentCompleted;
import com.orderflow.shared.OrderEvents.PaymentFailed;
import com.orderflow.shared.OrderEvents.PaymentRequested;
import com.orderflow.shared.SagaListener;

/** Drives the order's status in response to what inventory, payment and shipping report. */
@Component
class OrderSagaHandlers {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaHandlers.class);

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    OrderSagaHandlers(OrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    @SagaListener
    void on(InventoryReserved e) {
        Order order = load(e);
        if (order.getStatus() == OrderStatus.PENDING) {
            order.moveTo(OrderStatus.STOCK_RESERVED, null);
            events.publishEvent(new PaymentRequested(order.getId(), Instant.now(), order.getTotal()));
        } else {
            lateEvent(order, e);
        }
    }

    @SagaListener
    void on(InventoryRejected e) {
        Order order = load(e);
        if (order.getStatus() == OrderStatus.PENDING) {
            order.moveTo(OrderStatus.REJECTED, e.reason());
        } else {
            lateEvent(order, e);
        }
    }

    @SagaListener
    void on(PaymentCompleted e) {
        Order order = load(e);
        if (order.getStatus() == OrderStatus.STOCK_RESERVED) {
            order.moveTo(OrderStatus.CONFIRMED, null);
            events.publishEvent(new OrderConfirmed(order.getId(), Instant.now()));
        } else {
            lateEvent(order, e);
        }
    }

    @SagaListener
    void on(PaymentFailed e) {
        Order order = load(e);
        if (order.getStatus() == OrderStatus.STOCK_RESERVED) {
            cancel(order, e.reason());
        } else {
            lateEvent(order, e);
        }
    }

    @SagaListener
    void on(OrderShipped e) {
        Order order = load(e);
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            order.markShipped(e.trackingNumber());
        } else {
            lateEvent(order, e);
        }
    }

    void cancel(Order order, String reason) {
        order.moveTo(OrderStatus.CANCELLED, reason);
        events.publishEvent(new OrderCancelled(order.getId(), Instant.now(), reason));
        events.publishEvent(new CompensationRequested(order.getId(), Instant.now()));
    }

    /**
     * A step finished for an order that has moved on (typically cancelled while the step was in flight).
     * If the order is cancelled the step may have left stock or money behind, so ask for compensation again.
     */
    private void lateEvent(Order order, Object event) {
        log.warn("Ignoring {} for order {} in status {}", event.getClass().getSimpleName(), order.getId(), order.getStatus());
        if (order.getStatus() == OrderStatus.CANCELLED) {
            events.publishEvent(new CompensationRequested(order.getId(), Instant.now()));
        }
    }

    private Order load(com.orderflow.shared.OrderEvents.OrderEvent e) {
        return orders.findById(e.orderId()).orElseThrow(() -> new OrderNotFoundException(e.orderId()));
    }
}
