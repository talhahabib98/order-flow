package com.orderflow.payment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
