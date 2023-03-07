package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.LotSold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LotSoldRepository extends JpaRepository<LotSold, Long> {
}
