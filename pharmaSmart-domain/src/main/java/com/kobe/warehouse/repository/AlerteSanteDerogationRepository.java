package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AlerteSanteDerogation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlerteSanteDerogationRepository extends JpaRepository<AlerteSanteDerogation, Integer> {
    List<AlerteSanteDerogation> findAllByCustomerIdOrderByCreatedAtDesc(Integer customerId);
}
