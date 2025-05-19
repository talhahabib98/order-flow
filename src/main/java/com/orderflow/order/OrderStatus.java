package com.orderflow.order;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PENDING, STOCK_RESERVED, CONFIRMED, SHIPPED, REJECTED, CANCELLED;

    public Set<OrderStatus> next() {
        return switch (this) {
            case PENDING -> EnumSet.of(STOCK_RESERVED, REJECTED, CANCELLED);
            case STOCK_RESERVED -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED, REJECTED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canMoveTo(OrderStatus target) {
        return next().contains(target);
    }
}
