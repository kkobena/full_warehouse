package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AvoirTiersPayant;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AvoirTiersPayantRepository extends JpaRepository<AvoirTiersPayant, Long> {
    Page<AvoirTiersPayant> findByTiersPayantIdAndAvoirDateBetweenAndStatutIn(
        Integer tiersPayantId,
        LocalDate start,
        LocalDate end,
        List<AvoirStatut> statuts,
        Pageable pageable
    );

    Page<AvoirTiersPayant> findByAvoirDateBetweenAndStatutIn(
        LocalDate start,
        LocalDate end,
        List<AvoirStatut> statuts,
        Pageable pageable
    );

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(a.numAvoir, 4) AS int)), 0) FROM AvoirTiersPayant a")
    int findMaxNumeroAvoir();
}
