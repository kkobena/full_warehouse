package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FactureTiersPayantRepository extends JpaRepository<FactureTiersPayant, FactureItemId> {
    List<FactureTiersPayant> findByStatutIn(List<InvoiceStatut> statuts);

    List<FactureTiersPayant> findByGroupeTiersPayantIdAndStatutIn(Integer groupeTiersPayantId, List<InvoiceStatut> statuts);

    @Query(
        "SELECT f FROM FactureTiersPayant f " +
        "WHERE f.statut IN :statuts " +
        "AND f.invoiceDate BETWEEN :startDate AND :endDate"
    )
    List<FactureTiersPayant> findByStatutInAndInvoiceDateBetween(
        @Param("statuts") List<InvoiceStatut> statuts,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query(
        "SELECT f FROM FactureTiersPayant f " +
        "WHERE f.groupeTiersPayant.id = :groupeTiersPayantId " +
        "AND f.statut IN :statuts " +
        "AND f.invoiceDate BETWEEN :startDate AND :endDate"
    )
    List<FactureTiersPayant> findByGroupeTiersPayantIdAndStatutInAndInvoiceDateBetween(
        @Param("groupeTiersPayantId") Integer groupeTiersPayantId,
        @Param("statuts") List<InvoiceStatut> statuts,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
