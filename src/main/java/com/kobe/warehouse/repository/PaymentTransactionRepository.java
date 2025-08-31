package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CashRegister_;
import com.kobe.warehouse.domain.PaymentMode_;
import com.kobe.warehouse.domain.PaymentTransaction;
import com.kobe.warehouse.domain.PaymentTransaction_;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.dto.projection.MouvementCaisse;
import com.kobe.warehouse.service.dto.projection.MouvementCaisseGroupByMode;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

/**
 * Spring Data repository for the PaymentTransaction entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentTransactionRepository
    extends JpaRepository<PaymentTransaction, Long>, JpaSpecificationExecutor<PaymentTransaction>, PaymentTransactionCustomRepository {
    @Query(
        value = "SELECT  SUM(p.paidAmount) AS montant,p.typeFinancialTransaction AS type  FROM PaymentTransaction p WHERE   FUNCTION('DATE',p.createdAt) BETWEEN :fromDate AND :toDate GROUP BY p.typeFinancialTransaction"
    )
    List<MouvementCaisse> findMouvementsCaisse(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query(
        value = "SELECT  SUM(p.paidAmount) AS montant,p.typeFinancialTransaction AS type,p.paymentMode.code AS modePaimentCode,p.paymentMode.libelle AS modePaimentLibelle   FROM PaymentTransaction p WHERE   FUNCTION('DATE',p.createdAt) BETWEEN :fromDate AND :toDate GROUP BY p.typeFinancialTransaction,p.paymentMode.code"
    )
    List<MouvementCaisseGroupByMode> findMouvementsCaisseGroupBYModeReglement(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    default Specification<PaymentTransaction> filterByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(PaymentTransaction_.cashRegister).get(CashRegister_.user).get(AppUser_.id), userId);
    }

    default Specification<PaymentTransaction> filterByPeriode(LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, _, cb) -> cb.between(root.get(PaymentTransaction_.createdAt), fromDate, toDate);
    }

    default Specification<PaymentTransaction> filterByTypeFinancialTransaction(
        EnumSet<TypeFinancialTransaction> typeFinancialTransactions
    ) {
        if (CollectionUtils.isEmpty(typeFinancialTransactions)) {
            return null;
        }
        return (root, _, cb) -> {
            In<TypeFinancialTransaction> typeFinancialTransactionIn = cb.in(root.get(PaymentTransaction_.typeFinancialTransaction));
            typeFinancialTransactions.forEach(typeFinancialTransactionIn::value);
            return typeFinancialTransactionIn;
        };
    }

    default Specification<PaymentTransaction> filterByPaymentMode(Set<String> paymentModes) {
        if (CollectionUtils.isEmpty(paymentModes)) {
            return null;
        }
        return (root, _, cb) -> {
            In<String> paymentModeIn = cb.in(root.get(PaymentTransaction_.paymentMode).get(PaymentMode_.code));
            paymentModes.forEach(paymentModeIn::value);
            return paymentModeIn;
        };
    }

    default Specification<PaymentTransaction> filterByCategorieChiffreAffaire(EnumSet<CategorieChiffreAffaire> categorieChiffreAffaires) {
        if (CollectionUtils.isEmpty(categorieChiffreAffaires)) {
            return null;
        }
        return (root, _, cb) -> {
            In<CategorieChiffreAffaire> categorieChiffreAffaireIn = cb.in(root.get(PaymentTransaction_.categorieChiffreAffaire));
            categorieChiffreAffaires.forEach(categorieChiffreAffaireIn::value);
            return categorieChiffreAffaireIn;
        };
    }

    default Specification<PaymentTransaction> filterByCaissierId(Set<Long> caissierIds) {
        if (caissierIds == null || caissierIds.isEmpty()) {
            return null; // No filter applied
        }
        return (root, query, cb) -> root.get(PaymentTransaction_.cashRegister).get(CashRegister_.user).get(AppUser_.id).in(caissierIds);
    }


}
