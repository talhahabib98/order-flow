package com.orderflow.shipping;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.orderflow.shared.OrderEvents.OrderConfirmed;
import com.orderflow.shared.OrderEvents.OrderShipped;
import com.orderflow.shared.SagaListener;
import com.orderflow.shared.SimulatedLatency;

/** Simulated fulfilment: "ships" every confirmed order and hands back a tracking number. */
@Component
class ShippingHandlers {

    private final ApplicationEventPublisher events;
    private final SimulatedLatency latency;

    ShippingHandlers(ApplicationEventPublisher events, SimulatedLatency latency) {
        this.events = events;
        this.latency = latency;
    }

    @SagaListener
    void on(OrderConfirmed e) {
        latency.pause();
        String tracking = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        events.publishEvent(new OrderShipped(e.orderId(), Instant.now(), tracking));
    }
}
