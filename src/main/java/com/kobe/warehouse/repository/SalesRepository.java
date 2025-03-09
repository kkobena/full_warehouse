package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.service.dto.projection.ChiffreAffaire;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the Sales entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SalesRepository extends JpaRepository<Sales, Long> {
    @Query("select sale from Sales sale left join fetch sale.salesLines where sale.id =:id")
    Optional<Sales> findOneWithEagerSalesLines(@Param("id") Long id);

    @Query(
        value = "SELECT SUM(s.cost_amount) montantAchat, SUM(s.sales_amount) AS montantTtc,SUM(s.tax_amount) AS montantTva,SUM(s.ht_amount) AS montantHt,SUM(s.discount_amount) AS montantRemise,SUM(s.net_amount) AS montantNet,SUM(s.part_tiers_payant) AS MontantTp,SUM(s.rest_to_pay) AS montantDiffere FROM sales s  WHERE s.ca IN ('CA') AND s.statut IN('CANCELED', 'CLOSED','REMOVE') AND DATE(s.updated_at) BETWEEN :fromDate AND :toDate",
        nativeQuery = true
    )
    ChiffreAffaire getChiffreAffaire(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
