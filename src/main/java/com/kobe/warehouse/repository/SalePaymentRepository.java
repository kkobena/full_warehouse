package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.CashRegister_;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.SalePayment_;
import com.kobe.warehouse.domain.User_;
import com.kobe.warehouse.service.dto.projection.Recette;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Payment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SalePaymentRepository
    extends JpaRepository<SalePayment, Long>, JpaSpecificationExecutor<SalePayment>, SalePaymentCustomRepository {
    List<SalePayment> findAllBySaleId(Long id);

    Optional<List<SalePayment>> findBySaleId(Long id);

    @Query(
        value = "SELECT  SUM(p.reel_amount) AS montantReel,SUM(p.paid_amount) AS montantPaye,pm.libelle AS modePaimentLibelle,pm.code AS modePaimentCode FROM payment_transaction p JOIN payment_mode pm ON p.payment_mode_code = pm.code " +
        "    JOIN sales s on p.sale_id = s.id WHERE s.ca IN ('CA') AND s.statut IN('CANCELED', 'CLOSED','REMOVE') AND DATE(s.updated_at) BETWEEN :fromDate AND :toDate GROUP BY pm.code",
        nativeQuery = true
    )
    List<Recette> findRecettes(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    default Specification<SalePayment> filterByCaissierId(Set<Long> caissierIds) {
        if (caissierIds == null || caissierIds.isEmpty()) {
            return null; // No filter applied
        }
        return (root, query, cb) -> root.get(SalePayment_.cashRegister).get(CashRegister_.user).get(User_.id).in(caissierIds);
    }

    default Specification<SalePayment> filterByPeriode(LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, _, cb) -> cb.between(root.get(SalePayment_.createdAt), fromDate, toDate);
    }
}
