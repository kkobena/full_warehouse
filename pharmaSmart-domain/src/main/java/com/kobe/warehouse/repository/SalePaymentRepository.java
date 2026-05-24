package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.CashRegister_;
import com.kobe.warehouse.domain.PaymentId;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.SalePayment_;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.dto.projection.Recette;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Spring Data JPA repository for the Payment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SalePaymentRepository
    extends JpaRepository<SalePayment, PaymentId>, JpaSpecificationExecutor<SalePayment>, SalePaymentCustomRepository {
    List<SalePayment> findAllBySale(Sales sale);

    @Query("SELECT o FROM SalePayment o WHERE o.sale.id=:saleId AND o.sale.saleDate=:saleDate")
    List<SalePayment> findAllBySaleIdAndSaleSaleDate(Long saleId, LocalDate saleDate);

    @Query(
        value = "SELECT  SUM(p.reel_amount) AS realAmount,SUM(p.paid_amount) AS paidAmount,pm.libelle AS libelle,pm.code AS code FROM payment_transaction p JOIN payment_mode pm ON p.payment_mode_code = pm.code " +
            "    JOIN sales s on p.sale_id = s.id WHERE s.ca IN ('CA') AND s.statut IN('CANCELED', 'CLOSED') AND s.sale_date BETWEEN :fromDate AND :toDate GROUP BY pm.code",
        nativeQuery = true
    )
    List<Recette> findRecettes(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    default Specification<SalePayment> filterByCaissierId(Set<Integer> caissierIds) {
        if (caissierIds == null || caissierIds.isEmpty()) {
            return null; // No filter applied
        }
        return (root, query, cb) -> root.get(SalePayment_.cashRegister).get(CashRegister_.user).get(AppUser_.id).in(caissierIds);
    }

    default Specification<SalePayment> filterByPeriode(LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, _, cb) -> cb.between(root.get(SalePayment_.createdAt), fromDate, toDate);
    }

    default Specification<SalePayment> between(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> cb.between(root.get(SalePayment_.sale).get(Sales_.saleDate), fromDate, toDate);
    }

    default Specification<SalePayment> paymentBetween(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> cb.between(root.get(SalePayment_.transactionDate), fromDate, toDate);
    }

    default Specification<SalePayment> hasStatut(EnumSet<SalesStatut> statut) {
        return (root, query, cb) -> root.get(SalePayment_.sale).get(Sales_.statut).in(statut);
    }

    default Specification<SalePayment> notImported() {
        return (root, query, cb) -> cb.equal(root.get(SalePayment_.sale).get(Sales_.imported), false);
    }

    default Specification<SalePayment> hasType(TypeVente typeVente) {
        return (root, query, cb) -> cb.equal(root.get(SalePayment_.sale).get(Sales_.type), typeVente.name());
    }

    default Specification<SalePayment> hasCaissier(AppUser caissier) {
        return (root, query, cb) -> cb.equal(root.get(SalePayment_.sale).get(Sales_.caissier), caissier);
    }

    default Specification<SalePayment> hasVendeur(AppUser vendeur) {
        return (root, query, cb) -> cb.equal(root.get(SalePayment_.sale).get(Sales_.seller), vendeur);
    }

    default Specification<SalePayment> isDiffere() {
        return (root, query, cb) -> cb.isNull(root.get(SalePayment_.sale).get(Sales_.differe));
    }

    default Specification<SalePayment> hasCategorieCa(EnumSet<CategorieChiffreAffaire> categorieChiffreAffaires) {
        return (root, query, cb) -> root.get(SalePayment_.sale).get(Sales_.categorieChiffreAffaire).in(categorieChiffreAffaires);
    }
}
