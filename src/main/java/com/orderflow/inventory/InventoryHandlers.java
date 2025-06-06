package com.orderflow.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.orderflow.shared.OrderEvents.CompensationRequested;
import com.orderflow.shared.OrderEvents.InventoryRejected;
import com.orderflow.shared.OrderEvents.InventoryReleased;
import com.orderflow.shared.OrderEvents.InventoryReserved;
import com.orderflow.shared.OrderEvents.Line;
import com.orderflow.shared.OrderEvents.OrderCreated;
import com.orderflow.shared.SagaListener;

@Component
class InventoryHandlers {

    private final ProductRepository products;
    private final ReservationRepository reservations;
    private final ApplicationEventPublisher events;

    InventoryHandlers(ProductRepository products, ReservationRepository reservations, ApplicationEventPublisher events) {
        this.products = products;
        this.reservations = reservations;
        this.events = events;
    }

    /** Reserves every line or none of them. */
    @SagaListener
    void on(OrderCreated e) {
        UUID orderId = e.orderId();
        if (reservations.existsByOrderId(orderId)) {
            return; // duplicate delivery
        }
        Map<String, Integer> wanted = e.lines().stream()
                .collect(Collectors.toMap(Line::sku, Line::quantity, Integer::sum, TreeMap::new));
        Map<String, Product> found = products.findAllForUpdate(wanted.keySet()).stream()
                .collect(Collectors.toMap(Product::getSku, p -> p));

        for (var line : wanted.entrySet()) {
            Product product = found.get(line.getKey());
            if (product == null) {
                reject(orderId, "Unknown SKU " + line.getKey());
                return;
            }
            if (!product.canFulfil(line.getValue())) {
                reject(orderId, "Insufficient stock for " + line.getKey());
                return;
            }
        }
        wanted.forEach((sku, qty) -> {
            found.get(sku).take(qty);
            reservations.save(new Reservation(orderId, sku, qty));
        });
        events.publishEvent(new InventoryReserved(orderId, Instant.now()));
    }

    /** Puts reserved stock back. Safe to receive more than once. */
    @SagaListener
    void on(CompensationRequested e) {
        List<Reservation> held = reservations.findByOrderIdAndStatus(e.orderId(), Reservation.Status.RESERVED);
        if (held.isEmpty()) {
            return;
        }
        Map<String, Product> locked = products.findAllForUpdate(held.stream().map(Reservation::getSku).toList()).stream()
                .collect(Collectors.toMap(Product::getSku, p -> p));
        for (Reservation r : held) {
            locked.get(r.getSku()).giveBack(r.getQuantity());
            r.release();
        }
        events.publishEvent(new InventoryReleased(e.orderId(), Instant.now()));
    }

    private void reject(UUID orderId, String reason) {
        events.publishEvent(new InventoryRejected(orderId, Instant.now(), reason));
    }
}
