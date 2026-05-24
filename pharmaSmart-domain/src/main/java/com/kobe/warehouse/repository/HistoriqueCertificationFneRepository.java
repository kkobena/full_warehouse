package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.HistoriqueCertificationFne;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoriqueCertificationFneRepository extends JpaRepository<HistoriqueCertificationFne, Long> {
    Page<HistoriqueCertificationFne> findByPlanificationIdOrderByExecutionDebutDesc(
        Integer planificationId, Pageable pageable);
}
