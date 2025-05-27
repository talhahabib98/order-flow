package com.orderflow.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CreateOrderRequest(
            @NotBlank String customerId,
            @NotEmpty @Valid List<ItemRequest> items) {
    }

    public record ItemRequest(
            @NotBlank String sku,
            @Min(1) int quantity,
            @DecimalMin("0.01") BigDecimal unitPrice) {
    }

    public record OrderResponse(UUID id, String customerId, OrderStatus status, String statusReason,
            BigDecimal total, List<OrderItem> items, String trackingNumber, Instant createdAt, Instant updatedAt) {

        static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getCustomerId(), o.getStatus(), o.getStatusReason(), o.getTotal(),
                    o.getItems(), o.getTrackingNumber(), o.getCreatedAt(), o.getUpdatedAt());
        }
    }

    public record TimelineEntry(String type, Instant occurredAt, JsonNode payload) {
    }
}
