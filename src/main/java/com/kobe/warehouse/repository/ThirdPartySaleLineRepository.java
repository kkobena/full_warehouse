package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ThirdPartySaleLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThirdPartySaleLineRepository extends JpaRepository<ThirdPartySaleLine, Long> {
    long countByClientTiersPayantId(Long clientTiersPayantId);
}
