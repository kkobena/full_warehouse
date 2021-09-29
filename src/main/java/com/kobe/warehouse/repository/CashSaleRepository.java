package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashSaleRepository extends JpaRepository<CashSale, Long> {
}
