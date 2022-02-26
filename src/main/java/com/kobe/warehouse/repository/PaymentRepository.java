package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Payment;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data  repository for the Payment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment>findAllBySalesId(Long id);
   Optional<List<Payment>>findBySalesId(Long id);
}
