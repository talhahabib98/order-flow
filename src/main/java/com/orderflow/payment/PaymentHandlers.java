package com.orderflow.payment;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.orderflow.payment.PaymentGateway.ChargeResult;
import com.orderflow.shared.OrderEvents.CompensationRequested;
import com.orderflow.shared.OrderEvents.PaymentCompleted;
import com.orderflow.shared.OrderEvents.PaymentFailed;
import com.orderflow.shared.OrderEvents.PaymentRefunded;
import com.orderflow.shared.OrderEvents.PaymentRequested;
import com.orderflow.shared.SagaListener;

@Component
class PaymentHandlers {

    private final PaymentGateway gateway;
    private final PaymentRepository payments;
    private final ApplicationEventPublisher events;

    PaymentHandlers(PaymentGateway gateway, PaymentRepository payments, ApplicationEventPublisher events) {
        this.gateway = gateway;
        this.payments = payments;
        this.events = events;
    }

    @SagaListener
    void on(PaymentRequested e) {
        if (payments.existsById(e.orderId())) {
            return; // duplicate delivery, never charge twice
        }
        ChargeResult result = gateway.charge(e.orderId(), e.amount());
        if (result.approved()) {
            payments.save(new Payment(e.orderId(), e.amount(), result.transactionId()));
            events.publishEvent(new PaymentCompleted(e.orderId(), Instant.now(), result.transactionId()));
        } else {
            events.publishEvent(new PaymentFailed(e.orderId(), Instant.now(), result.declineReason()));
        }
    }

    /** Refunds a charged payment. Safe to receive more than once. */
    @SagaListener
    void on(CompensationRequested e) {
        payments.findById(e.orderId()).filter(Payment::isCharged).ifPresent(payment -> {
            gateway.refund(payment.getTransactionId());
            payment.refund();
            events.publishEvent(new PaymentRefunded(e.orderId(), Instant.now(), payment.getTransactionId()));
        });
    }
}
