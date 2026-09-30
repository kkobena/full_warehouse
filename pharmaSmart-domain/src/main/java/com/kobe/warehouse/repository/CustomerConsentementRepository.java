package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CustomerConsentement;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerConsentementRepository extends JpaRepository<CustomerConsentement, Integer> {
    @EntityGraph(attributePaths = "user")
    List<CustomerConsentement> findAllByCustomerIdOrderByCreatedAtDesc(Integer customerId);
}
