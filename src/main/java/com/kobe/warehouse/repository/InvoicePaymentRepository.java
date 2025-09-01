package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FactureTiersPayant_;
import com.kobe.warehouse.domain.GroupeTiersPayant_;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePayment_;
import com.kobe.warehouse.domain.PaymentId;
import com.kobe.warehouse.domain.TiersPayant_;
import com.kobe.warehouse.service.dto.projection.ReglementTiersPayants;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, PaymentId>, JpaSpecificationExecutor<InvoicePayment> {
    List<InvoicePayment> findInvoicePaymentByParentId(long parentId);

    Optional<InvoicePayment> findInvoicePaymentById(Long paymentId);

    default Specification<InvoicePayment> specialisationQueryString(String queryValue) {
        return (root, _, cb) ->
            cb.or(
                cb.like(
                    cb.upper(root.get(InvoicePayment_.factureTiersPayant).get(FactureTiersPayant_.tiersPayant).get(TiersPayant_.name)),
                    queryValue
                ),
                cb.like(
                    cb.upper(
                        root.get(InvoicePayment_.factureTiersPayant).get(FactureTiersPayant_.groupeTiersPayant).get(GroupeTiersPayant_.name)
                    ),
                    queryValue
                )
            );
    }

    default Specification<InvoicePayment> filterByOrganismeId(long id) {
        return (root, _, cb) ->
            cb.equal(
                root.get(InvoicePayment_.factureTiersPayant).get(FactureTiersPayant_.groupeTiersPayant).get(GroupeTiersPayant_.id),
                id
            );
    }

    default Specification<InvoicePayment> filterByTiersPayantId(long id) {
        return (root, _, cb) ->
            cb.equal(root.get(InvoicePayment_.factureTiersPayant).get(FactureTiersPayant_.tiersPayant).get(TiersPayant_.id), id);
    }

    default Specification<InvoicePayment> periodeCriteria(LocalDate startDate, LocalDate endDate) {
        return (root, _, cb) -> cb.between(root.get(InvoicePayment_.transactionDate), startDate, endDate);
    }

    default Specification<InvoicePayment> invoicesTypePredicats(boolean grouped) {
        return (root, _, cb) -> {
            if (grouped) {
                return cb.isNotNull(root.get(InvoicePayment_.factureTiersPayant).get(FactureTiersPayant_.groupeTiersPayant));
            } else {
                return cb.isNotNull(root.get(InvoicePayment_.factureTiersPayant).get(FactureTiersPayant_.tiersPayant));
            }
        };
    }

    @Query(
        value = "SELECT tp.full_name AS libelle,tp.categorie AS type,f.num_facture,it.montantReglement,it.montantFacture FROM  payment_transaction p JOIN facture_tiers_payant f ON p.facture_tiers_payant_id = f.id JOIN tiers_payant tp on f.tiers_payant_id = tp.id JOIN (SELECT s.facture_tiers_payant_id, SUM(s.montant_regle) AS montantReglement,SUM(s.montant) AS montantFacture FROM third_party_sale_line s" +
        " GROUP BY s.facture_tiers_payant_id) AS it ON it.facture_tiers_payant_id=f.id WHERE p.transaction_date  BETWEEN :fromDate AND :toDate AND (tp.name like :search or tp.full_name like :search ) GROUP BY tp.id",
        countQuery = "SELECT COUNT(p.id) FROM  payment_transaction p JOIN facture_tiers_payant f ON p.facture_tiers_payant_id = f.id JOIN tiers_payant tp on f.tiers_payant_id = tp.id WHERE p.transaction_date  BETWEEN :fromDate AND :toDate AND (tp.name like :search or tp.full_name like :search ) GROUP BY tp.id",
        nativeQuery = true
    )
    Page<ReglementTiersPayants> findReglementTierspayant(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("search") String search,
        Pageable pageable
    );
}
