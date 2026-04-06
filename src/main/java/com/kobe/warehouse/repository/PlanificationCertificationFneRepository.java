package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PlanificationCertificationFne;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanificationCertificationFneRepository
    extends JpaRepository<PlanificationCertificationFne, Integer> {

    /** Planification active dont prochaineExecution est échue (ou null → jamais exécutée). */
    List<PlanificationCertificationFne> findByActifTrueAndProchaineExecutionBefore(LocalDateTime now);

    /** Planifications actives sans prochaineExecution définie (first activation). */
    List<PlanificationCertificationFne> findByActifTrueAndProchaineExecutionIsNull();

    Optional<PlanificationCertificationFne> findFirstByActifTrue();
}
