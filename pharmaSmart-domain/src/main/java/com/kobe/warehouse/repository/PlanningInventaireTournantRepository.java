package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PlanningInventaireTournant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanningInventaireTournantRepository extends
    JpaRepository<PlanningInventaireTournant, Integer> {

    /**
     * Tous les plannings actifs dont l'échéance est dépassée ou aujourd'hui.
     */
    @Query("SELECT p FROM PlanningInventaireTournant p WHERE p.actif = true AND p.prochaineExecution <= :today")
    List<PlanningInventaireTournant> findEchus(@Param("today") LocalDate today);

    /**
     * Tous les plannings d'un storage, triés par libellé.
     */
    List<PlanningInventaireTournant> findAllByStorageIdOrderByLibelle(Integer storageId);

    /**
     * Tous les plannings, triés par prochaine exécution.
     */
    List<PlanningInventaireTournant> findAllByOrderByProchaineExecutionAsc();

    /**
     * Plannings actifs seulement.
     */
    List<PlanningInventaireTournant> findAllByActifTrueOrderByProchaineExecutionAsc();
}
