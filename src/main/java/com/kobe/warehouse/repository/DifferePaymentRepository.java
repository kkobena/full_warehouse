package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DifferePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the DifferePayment entity.
 */
@Repository
public interface DifferePaymentRepository extends JpaRepository<DifferePayment, Long> {}
