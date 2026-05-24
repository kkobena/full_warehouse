package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.HistoriquePlanification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoriquePlanificationRepository extends JpaRepository<HistoriquePlanification, Long> {
    Page<HistoriquePlanification> findByPlanificationId(Integer planificationId, Pageable pageable);
}
