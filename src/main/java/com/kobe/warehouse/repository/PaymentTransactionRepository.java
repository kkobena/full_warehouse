package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PaymentTransaction;
import com.kobe.warehouse.service.dto.projection.MouvementCaisse;
import com.kobe.warehouse.service.dto.projection.MouvementCaisseGroupByMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the PaymentTransaction entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    void deleteByOrganismeId(Long organismeId);

    @Query(
        value = "SELECT  SUM(p.amount) AS montant,p.typeFinancialTransaction AS type  FROM PaymentTransaction p WHERE   FUNCTION('DATE',p.createdAt) BETWEEN :fromDate AND :toDate GROUP BY p.typeFinancialTransaction"
    )
    List<MouvementCaisse> findMouvementsCaisse(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query(
        value = "SELECT  SUM(p.amount) AS montant,p.typeFinancialTransaction AS type,p.paymentMode.code AS modePaimentCode,p.paymentMode.libelle AS modePaimentLibelle   FROM PaymentTransaction p WHERE   FUNCTION('DATE',p.createdAt) BETWEEN :fromDate AND :toDate GROUP BY p.typeFinancialTransaction,p.paymentMode.code"
    )
    List<MouvementCaisseGroupByMode> findMouvementsCaisseGroupBYModeReglement(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );
}
