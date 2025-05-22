package com.orderflow.order;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.shared.OrderEvents.OrderEvent;

/**
 * Synchronous listener: the log row is written in the same transaction as the state change
 * that produced the event, so the timeline can never disagree with the order.
 */
@Component
class OrderEventRecorder {

    private final OrderEventLogRepository log;
    private final ObjectMapper mapper;

    OrderEventRecorder(OrderEventLogRepository log, ObjectMapper mapper) {
        this.log = log;
        this.mapper = mapper;
    }

    @EventListener
    void record(OrderEvent event) {
        try {
            log.save(new OrderEventLog(event.orderId(), event.type(), mapper.writeValueAsString(event), event.occurredAt()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + event.type(), e);
        }
    }
}
