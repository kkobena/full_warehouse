package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AvoirFournisseurLine;
import com.kobe.warehouse.service.dto.projection.ReponseRetourBonItemProjection;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AvoirFournisseurLineRepository extends JpaRepository<AvoirFournisseurLine, Integer> {

    @Query("""
        SELECT
            SUM(l.qtyMvt)                              AS acceptedQty,
            l.avoirFournisseur.fournisseur.id           AS fournisseurId,
            SUM(l.prixAchat * l.qtyMvt)                AS valeurAchat,
            DATE(af.dateMtv)                            AS dateMtv
        FROM AvoirFournisseurLine l
        JOIN l.avoirFournisseur af
        WHERE af.dateMtv BETWEEN :startDate AND :endDate
        GROUP BY fournisseurId, dateMtv
        """)
    List<ReponseRetourBonItemProjection> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    @Query("""
        SELECT
            SUM(l.qtyMvt)                                       AS acceptedQty,
            l.avoirFournisseur.fournisseur.id                    AS fournisseurId,
            SUM(l.prixAchat * l.qtyMvt)                         AS valeurAchat,
            DATE(DATE_TRUNC('MONTH', af.dateMtv))                AS dateMtv
        FROM AvoirFournisseurLine l
        JOIN l.avoirFournisseur af
        WHERE af.dateMtv BETWEEN :startDate AND :endDate
        GROUP BY fournisseurId, dateMtv
        """)
    List<ReponseRetourBonItemProjection> findByDateRangeGroupByMonth(LocalDateTime startDate, LocalDateTime endDate);
}
