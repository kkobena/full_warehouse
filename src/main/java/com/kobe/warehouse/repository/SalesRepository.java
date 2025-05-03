package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Customer_;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.dto.projection.ChiffreAffaire;
import com.kobe.warehouse.service.financiel_transaction.dto.SaleInfo;
import com.kobe.warehouse.service.reglement.differe.dto.ClientDiffere;
import jakarta.persistence.criteria.CriteriaBuilder.In;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Spring Data repository for the Sales entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SalesRepository extends JpaSpecificationExecutor<Sales>, JpaRepository<Sales, Long>, CustomSalesRepository {
    @Query("select sale from Sales sale left join fetch sale.salesLines where sale.id =:id")
    Optional<Sales> findOneWithEagerSalesLines(@Param("id") Long id);

    @Query(
        value = "SELECT SUM(s.cost_amount) montantAchat, SUM(s.sales_amount) AS montantTtc,SUM(s.tax_amount) AS montantTva,SUM(s.ht_amount) AS montantHt,SUM(s.discount_amount) AS montantRemise,SUM(s.net_amount) AS montantNet,SUM(s.part_tiers_payant) AS MontantTp,SUM(s.rest_to_pay) AS montantDiffere FROM sales s  WHERE s.ca IN ('CA') AND s.statut IN('CANCELED', 'CLOSED','REMOVE') AND DATE(s.updated_at) BETWEEN :fromDate AND :toDate",
        nativeQuery = true
    )
    ChiffreAffaire getChiffreAffaire(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query(
        "SELECT o.numberTransaction AS reference,c.firstName AS customerFirstName,c.lastName AS customerLastName   FROM Sales o LEFT JOIN o.customer c  WHERE o.id =:id"
    )
    SaleInfo findSaleInfoById(@Param("id") Long id);

    @Query(
        "SELECT c.lastName AS lastName,c.firstName AS firsName,c.id AS id   FROM Sales o  JOIN o.customer c  WHERE o.differe AND o.statut='CLOSED' AND o.canceled =FALSE  GROUP BY c.id ORDER BY c.firstName,c.lastName"
    )
    Page<ClientDiffere> getClientDiffere(Pageable pageable);
    @Query(
        "SELECT SUM(o.restToPay)   FROM Sales o  JOIN o.customer c  WHERE o.differe AND o.statut='CLOSED' AND o.canceled =FALSE  AND c.id =:customerId"
    )
    BigDecimal getDiffereSoldeByCustomerId(Long customerId);

    default Specification<Sales> filterByCustomerId(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(Sales_.customer).get(Customer_.id), customerId);
    }

    default Specification<Sales> filterByPeriode(LocalDate fromDate, LocalDate toDate) {
        return (root, _, cb) ->
            cb.between(cb.function("DATE", LocalDate.class, root.get(Sales_.updatedAt)), cb.literal(fromDate), cb.literal(toDate));
    }

    default Specification<Sales> filterByPaymentStatus(Set<PaymentStatus> paymentStatuses) {
        if (CollectionUtils.isEmpty(paymentStatuses)) {
            return null;
        }
        return (root, _, cb) -> {
            In<PaymentStatus> salesIn = cb.in(root.get(Sales_.paymentStatus));
            paymentStatuses.forEach(salesIn::value);
            return salesIn;
        };
    }

    default Specification<Sales> filterNumberTransaction(String numberTransaction) {
        if (!StringUtils.hasLength(numberTransaction)) {
            return null;
        }
        return (root, query, cb) -> cb.like(root.get(Sales_.numberTransaction), numberTransaction + "%");
    }
}
