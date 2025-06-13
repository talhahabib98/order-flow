package com.orderflow.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.OrderDtos.CreateOrderRequest;
import com.orderflow.order.OrderDtos.TimelineEntry;
import com.orderflow.shared.OrderEvents.Line;
import com.orderflow.shared.OrderEvents.OrderCreated;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final OrderEventLogRepository eventLog;
    private final OrderSagaHandlers saga;
    private final ApplicationEventPublisher events;
    private final ObjectMapper mapper;

    OrderService(OrderRepository orders, OrderEventLogRepository eventLog, OrderSagaHandlers saga,
            ApplicationEventPublisher events, ObjectMapper mapper) {
        this.orders = orders;
        this.eventLog = eventLog;
        this.saga = saga;
        this.events = events;
        this.mapper = mapper;
    }

    @Transactional
    public Order create(CreateOrderRequest request) {
        List<OrderItem> items = request.items().stream()
                .map(i -> new OrderItem(i.sku(), i.quantity(), i.unitPrice()))
                .toList();
        Order order = orders.save(new Order(request.customerId(), items));
        List<Line> lines = items.stream().map(i -> new Line(i.sku(), i.quantity())).toList();
        events.publishEvent(new OrderCreated(order.getId(), Instant.now(), lines, order.getTotal()));
        return order;
    }

    @Transactional(readOnly = true)
    public Order get(UUID id) {
        return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public Order cancel(UUID id) {
        Order order = get(id);
        saga.cancel(order, "Cancelled by customer");
        return order;
    }

    @Transactional(readOnly = true)
    public List<TimelineEntry> timeline(UUID id) {
        get(id);
        return eventLog.findByOrderIdOrderByIdAsc(id).stream()
                .map(e -> new TimelineEntry(e.getType(), e.getOccurredAt(), parse(e.getPayload())))
                .toList();
    }

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
