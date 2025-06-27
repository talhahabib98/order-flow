package com.orderflow.payment;

import java.math.BigDecimal;
import java.util.UUID;

interface PaymentGateway {

    ChargeResult charge(UUID orderId, BigDecimal amount);

    void refund(String transactionId);

    record ChargeResult(boolean approved, String transactionId, String declineReason) {

        static ChargeResult approved(String transactionId) {
            return new ChargeResult(true, transactionId, null);
        }

        static ChargeResult declined(String reason) {
            return new ChargeResult(false, null, reason);
        }
    }
}
