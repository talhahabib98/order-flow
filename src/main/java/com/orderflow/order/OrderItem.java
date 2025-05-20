package com.orderflow.order;

import java.math.BigDecimal;

import jakarta.persistence.Embeddable;

@Embeddable
public record OrderItem(String sku, int quantity, BigDecimal unitPrice) {
}
