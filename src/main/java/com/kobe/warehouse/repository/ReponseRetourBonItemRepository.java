package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ReponseRetourBonItem;
import com.kobe.warehouse.service.dto.projection.ReponseRetourBonItemProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReponseRetourBonItemRepository extends JpaRepository<ReponseRetourBonItem, Integer> {
    @Query("""
        SELECT
            SUM(rbi.qtyMvt) AS acceptedQty,
            rrb.retourBon.commande.fournisseur.id  AS fournisseurId,
            SUM(rbi.prixAchat * rbi.qtyMvt) AS valeurAchat,
            DATE(rrb.dateMtv) AS dateMtv
        FROM ReponseRetourBonItem rbi
        JOIN rbi.reponseRetourBon rrb
        WHERE rrb.dateMtv BETWEEN :startDate AND :endDate
        GROUP BY fournisseurId, dateMtv
    """)
    List<ReponseRetourBonItemProjection> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);



    @Query("""
                SELECT
                    SUM(rbi.qtyMvt) AS acceptedQty,
                    rrb.retourBon.commande.fournisseur.id  AS fournisseurId,
                    SUM(rbi.prixAchat * rbi.qtyMvt) AS valeurAchat,
                  DATE(DATE_TRUNC('MONTH', rrb.dateMtv))  AS dateMtv


                FROM ReponseRetourBonItem rbi
                JOIN rbi.reponseRetourBon rrb
                WHERE rrb.dateMtv BETWEEN :startDate AND :endDate
                GROUP BY fournisseurId, dateMtv
        """)
    List<ReponseRetourBonItemProjection> findByDateRangeGroupByMonth(LocalDateTime startDate, LocalDateTime endDate);

}
