package com.techlab.ecommerce.inventory.repository;

import com.techlab.ecommerce.inventory.entity.StockReservation;
import com.techlab.ecommerce.inventory.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);

    long countByOrderId(Long orderId);
}
