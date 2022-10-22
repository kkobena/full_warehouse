package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PaymentFournisseur;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the PaymentFournisseur entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentFournisseurRepository extends JpaRepository<PaymentFournisseur, Long> {
}
