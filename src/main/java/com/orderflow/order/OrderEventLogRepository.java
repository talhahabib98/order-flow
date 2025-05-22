package com.orderflow.order;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventLogRepository extends JpaRepository<OrderEventLog, Long> {

    List<OrderEventLog> findByOrderIdOrderByIdAsc(UUID orderId);
}
