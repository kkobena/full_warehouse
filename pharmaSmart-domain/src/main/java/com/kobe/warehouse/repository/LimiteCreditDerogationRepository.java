package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.LimiteCreditDerogation;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LimiteCreditDerogationRepository extends JpaRepository<LimiteCreditDerogation, Integer> {
    boolean existsBySaleIdAndSaleDate(Long saleId, LocalDate saleDate);
}
