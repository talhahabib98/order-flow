package com.orderflow.inventory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByOrderId(UUID orderId);

    List<Reservation> findByOrderIdAndStatus(UUID orderId, Reservation.Status status);
}
