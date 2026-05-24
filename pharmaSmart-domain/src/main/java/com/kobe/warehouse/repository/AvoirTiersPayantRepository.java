package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AvoirTiersPayant;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AvoirTiersPayantRepository extends JpaRepository<AvoirTiersPayant, Long> {

    @Query("""
        SELECT a FROM AvoirTiersPayant a
        WHERE a.factureTiersPayant.tiersPayant.id = :tiersPayantId
          AND a.avoirDate BETWEEN :start AND :end
          AND a.statut IN :statuts
          AND (:numAvoir IS NULL OR LOWER(a.numAvoir) LIKE :numAvoir)
        """)
    Page<AvoirTiersPayant> searchByTiersPayant(
        @Param("tiersPayantId") Integer tiersPayantId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end,
        @Param("statuts") List<AvoirStatut> statuts,
        @Param("numAvoir") String numAvoir,
        Pageable pageable
    );

    @Query("""
        SELECT a FROM AvoirTiersPayant a
        WHERE a.avoirDate BETWEEN :start AND :end
          AND a.statut IN :statuts
          AND (:numAvoir IS NULL OR LOWER(a.numAvoir) LIKE :numAvoir)
        """)
    Page<AvoirTiersPayant> searchAll(
        @Param("start") LocalDate start,
        @Param("end") LocalDate end,
        @Param("statuts") List<AvoirStatut> statuts,
        @Param("numAvoir") String numAvoir,
        Pageable pageable
    );

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(a.numAvoir, 4) AS int)), 0) FROM AvoirTiersPayant a")
    int findMaxNumeroAvoir();
}
