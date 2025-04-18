package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.service.dto.HistoriqueProduitVente;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelle;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteMensuelleSummary;
import com.kobe.warehouse.service.dto.HistoriqueProduitVenteSummary;
import com.kobe.warehouse.service.dto.projection.LastDateProjection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the SalesLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SalesLineRepository extends JpaRepository<SalesLine, Long> {
    List<SalesLine> findBySalesIdOrderByProduitLibelle(Long salesId);

    Optional<SalesLine> findBySalesIdAndProduitId(Long salesId, Long produitId);

    Optional<List<SalesLine>> findAllBySalesId(Long salesId);

    List<SalesLine> findAllByQuantityAvoirGreaterThan(Integer zero);

    @Query(
        value = "SELECT MAX(s.updated_at) AS updatedAt FROM sales_line o JOIN sales s ON o.sales_id = s.id WHERE o.produit_id =:produitId AND s.statut=:statut",
        nativeQuery = true
    )
    LastDateProjection findLastUpdatedAtByProduitIdAndSalesStatut(@Param("produitId") Long produitId, @Param("statut") String statut);

    @Query(
        value = "SELECT s.updated_at AS mvtDate,s.number_transaction AS reference,o.quantity_requested AS quantite," +
        "o.regular_unit_price AS prixUnitaire,o.ht_amount AS montantHt,o.net_amount AS montantNet,o.sales_amount AS montantTtc," +
        "o.discount_amount AS montantRemise,o.tax_amount AS montantTva,u.first_name AS firstName,u.last_name AS lastName    FROM sales_line o JOIN sales s ON o.sales_id = s.id JOIN user u ON s.caisse_id=u.id WHERE o.produit_id =:produitId AND s.statut IN(:statuts) AND DATE(s.updated_at) BETWEEN :startDate AND :endDate ORDER BY s.updated_at DESC",
        nativeQuery = true
    )
    Page<HistoriqueProduitVente> getHistoriqueVente(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") Set<String> statuts,
        Pageable pageable
    );

    @Query(
        value = "SELECT YEAR(s.updated_at) AS annee,SUM(o.quantity_requested) AS quantite,MONTH(s.updated_at) AS mois    FROM sales_line o JOIN sales s ON o.sales_id = s.id  WHERE o.produit_id =:produitId AND s.statut IN(:statuts) AND DATE(s.updated_at) BETWEEN :startDate AND :endDate GROUP BY YEAR(s.updated_at),MONTH(s.updated_at) ORDER BY YEAR(s.updated_at) DESC",
        nativeQuery = true
    )
    List<HistoriqueProduitVenteMensuelle> getHistoriqueVenteMensuelle(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") Set<String> statuts
    );
    @Query(
        value = "SELECT SUM(o.quantity_requested) AS quantite, SUM(o.sales_amount) AS montantTtc, SUM(o.ht_amount) AS montantHt, SUM(o.discount_amount) AS montantRemise, SUM(o.tax_amount) AS montantTva,SUM(o.net_amount) AS montantNet FROM sales_line o JOIN sales s ON o.sales_id = s.id WHERE o.produit_id =:produitId AND s.statut IN(:statuts) AND DATE(s.updated_at) BETWEEN :startDate AND :endDate",
        nativeQuery = true
    )
    HistoriqueProduitVenteSummary getHistoriqueVenteSummary(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") Set<String> statuts
    );
    @Query(value = "SELECT SUM(o.quantity_requested) AS quantite  FROM sales_line o JOIN sales s ON o.sales_id = s.id  WHERE o.produit_id =:produitId AND s.statut IN(:statuts) AND DATE(s.updated_at) BETWEEN :startDate AND :endDate",nativeQuery = true)
    HistoriqueProduitVenteMensuelleSummary getHistoriqueVenteMensuelleSummary(
        @Param("produitId") long produitId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuts") Set<String> statuts
    );
}
