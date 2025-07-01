package com.orderflow.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.orderflow.shared.SimulatedLatency;

/** Approves every charge up to a configured limit and declines anything above it. */
@Component
class SimulatedPaymentGateway implements PaymentGateway {

    private final BigDecimal maxAmount;
    private final SimulatedLatency latency;

    SimulatedPaymentGateway(@Value("${orderflow.payment.max-amount:10000}") BigDecimal maxAmount, SimulatedLatency latency) {
        this.maxAmount = maxAmount;
        this.latency = latency;
    }

    @Override
    public ChargeResult charge(UUID orderId, BigDecimal amount) {
        latency.pause();
        if (amount.compareTo(maxAmount) > 0) {
            return ChargeResult.declined("Card declined: amount exceeds limit of " + maxAmount);
        }
        return ChargeResult.approved("txn-" + UUID.randomUUID());
    }

    @Override
    public void refund(String transactionId) {
        latency.pause();
    }
}
