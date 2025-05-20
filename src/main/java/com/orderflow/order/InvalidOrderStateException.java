package com.orderflow.order;

import java.util.UUID;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(UUID id, OrderStatus from, OrderStatus to) {
        super("Order " + id + " cannot move from " + from + " to " + to);
    }
}
