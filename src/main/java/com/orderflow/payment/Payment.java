package com.orderflow.payment;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

    enum Status { CHARGED, REFUNDED }

    @Id
    private UUID orderId;

    private BigDecimal amount;

    private String transactionId;

    @Enumerated(EnumType.STRING)
    private Status status = Status.CHARGED;

    protected Payment() {
    }

    Payment(UUID orderId, BigDecimal amount, String transactionId) {
        this.orderId = orderId;
        this.amount = amount;
        this.transactionId = transactionId;
    }

    boolean isCharged() {
        return status == Status.CHARGED;
    }

    String getTransactionId() {
        return transactionId;
    }

    void refund() {
        this.status = Status.REFUNDED;
    }
}
