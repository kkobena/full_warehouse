package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PlanificationFacturation;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanificationFacturationRepository extends JpaRepository<PlanificationFacturation, Integer> {
    List<PlanificationFacturation> findByActifTrueAndProchaineExecutionBefore(LocalDateTime now);
}
