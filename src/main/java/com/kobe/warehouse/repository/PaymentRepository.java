package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for the Payment entity. */
@SuppressWarnings("unused")
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
  List<Payment> findAllBySalesId(Long id);

  Optional<List<Payment>> findBySalesId(Long id);
}
