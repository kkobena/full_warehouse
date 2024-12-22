package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the PaymentTransaction entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    void deleteByOrganismeId(Long organismeId);
}
