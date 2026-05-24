package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashRegister_;
import com.kobe.warehouse.domain.Commande_;
import com.kobe.warehouse.domain.FactureTiersPayant_;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.InvoicePayment_;
import com.kobe.warehouse.domain.PaymentFournisseur;
import com.kobe.warehouse.domain.PaymentFournisseur_;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.PaymentMode_;
import com.kobe.warehouse.domain.PaymentTransaction;
import com.kobe.warehouse.domain.PaymentTransaction_;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.SalePayment_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseProjection;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseSumProjection;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.tiketz.dto.TicketZProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class PaymentTransactionCustomRepositoryImpl implements PaymentTransactionCustomRepository {

    private final EntityManager entityManager;

    public PaymentTransactionCustomRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<MvtCaisseProjection> fetchAll(Specification<PaymentTransaction> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MvtCaisseProjection> query = cb.createQuery(MvtCaisseProjection.class);

        Root<PaymentTransaction> root = query.from(PaymentTransaction.class);
        Join<PaymentTransaction, PaymentMode> paymentModeJoin = root.join(PaymentTransaction_.paymentMode, JoinType.INNER);
        Join<PaymentTransaction, CashRegister> cashRegisterJoin = root.join(PaymentTransaction_.cashRegister, JoinType.INNER);
        Join<CashRegister, AppUser> appUserJoin = cashRegisterJoin.join(CashRegister_.user, JoinType.INNER);
        Path<SalePayment> salePaymentPath = cb.treat(root, SalePayment.class);
        Path<InvoicePayment> invoicePaymentPath = cb.treat(root, InvoicePayment.class);

        Path<PaymentFournisseur> paymentFournisseurPath = cb.treat(root, PaymentFournisseur.class);
        query
            .select(
                cb.construct(
                    MvtCaisseProjection.class,
                    root.get(PaymentTransaction_.id),
                    salePaymentPath.get(SalePayment_.sale).get(Sales_.id),
                    root.get(PaymentTransaction_.cashRegister).get(CashRegister_.id),
                    invoicePaymentPath.get(InvoicePayment_.factureTiersPayant).get(FactureTiersPayant_.id),
                    root.get(PaymentTransaction_.credit),
                    root.get(PaymentTransaction_.type),
                    root.get(PaymentTransaction_.createdAt),
                    root.get(PaymentTransaction_.paidAmount),
                    root.get(PaymentTransaction_.typeFinancialTransaction),
                    paymentModeJoin.get(PaymentMode_.code),
                    paymentModeJoin.get(PaymentMode_.libelle),
                    root.get(PaymentTransaction_.categorieChiffreAffaire),
                    root.get(PaymentTransaction_.transactionDate),
                    paymentFournisseurPath.get(PaymentFournisseur_.commande).get(Commande_.id),
                    appUserJoin.get(AppUser_.firstName),
                    appUserJoin.get(AppUser_.lastName),
                    root.get(PaymentTransaction_.reelAmount),
                    salePaymentPath.get(SalePayment_.sale).get(Sales_.saleDate),
                    root.get(PaymentTransaction_.transactionNumber)
                )
            )
            .orderBy(cb.desc(root.get(PaymentTransaction_.createdAt)));

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<MvtCaisseProjection> typedQuery = entityManager.createQuery(query);
        if (pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        List<MvtCaisseProjection> results = typedQuery.getResultList();
        if (pageable.isUnpaged()) {
            return new PageImpl<>(results);
        }

        return new PageImpl<>(results, pageable, count(specification));
    }

    private Long count(Specification<PaymentTransaction> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<PaymentTransaction> root = countQuery.from(PaymentTransaction.class);
        countQuery.select(cb.count(root));
        Predicate predicate = specification.toPredicate(root, countQuery, cb);
        countQuery.where(predicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    @Override
    public List<MvtCaisseSumProjection> fetchAllSum(Specification<PaymentTransaction> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MvtCaisseSumProjection> query = cb.createQuery(MvtCaisseSumProjection.class);
        Root<PaymentTransaction> root = query.from(PaymentTransaction.class);
        Join<PaymentTransaction, PaymentMode> paymentModeJoin = root.join(PaymentTransaction_.paymentMode, JoinType.INNER);
        query
            .select(
                cb.construct(
                    MvtCaisseSumProjection.class,
                    cb.sumAsLong(root.get(PaymentTransaction_.paidAmount)),
                    root.get(PaymentTransaction_.typeFinancialTransaction),
                    paymentModeJoin.get(PaymentMode_.code),
                    paymentModeJoin.get(PaymentMode_.libelle)
                )
            )
            .groupBy(
                root.get(PaymentTransaction_.typeFinancialTransaction),
                paymentModeJoin.get(PaymentMode_.code),
                paymentModeJoin.get(PaymentMode_.libelle)
            );

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<MvtCaisseSumProjection> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    @Override
    public List<TicketZProjection> fetchAllMvts(Specification<PaymentTransaction> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TicketZProjection> query = cb.createQuery(TicketZProjection.class);
        Root<PaymentTransaction> root = query.from(PaymentTransaction.class);
        Join<PaymentTransaction, PaymentMode> paymentModeJoin = root.join(PaymentTransaction_.paymentMode, JoinType.INNER);
        Join<PaymentTransaction, CashRegister> cashRegisterJoin = root.join(PaymentTransaction_.cashRegister, JoinType.INNER);
        Join<CashRegister, AppUser> appUserJoin = cashRegisterJoin.join(CashRegister_.user, JoinType.INNER);
        query
            .select(
                cb.construct(
                    TicketZProjection.class,
                    paymentModeJoin.get(PaymentMode_.code),
                    paymentModeJoin.get(PaymentMode_.libelle),
                    appUserJoin.get(AppUser_.id),
                    appUserJoin.get(AppUser_.firstName),
                    appUserJoin.get(AppUser_.lastName),
                    cb.sumAsLong(root.get(PaymentTransaction_.paidAmount)),
                    cb.sumAsLong(root.get(PaymentTransaction_.reelAmount)),
                    root.get(PaymentTransaction_.credit)
                )
            )
            .groupBy(
                paymentModeJoin.get(PaymentMode_.code),
                paymentModeJoin.get(PaymentMode_.libelle),
                appUserJoin.get(AppUser_.id),
                appUserJoin.get(AppUser_.firstName),
                appUserJoin.get(AppUser_.lastName),
                root.get(PaymentTransaction_.credit)
            );

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<TicketZProjection> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    @Override
    public List<BalanceCaisseDTO> fetchPaymentTransactionsForBalanceCaisse(MvtParam mvtParam) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BalanceCaisseDTO> cq = cb.createQuery(BalanceCaisseDTO.class);
        Root<PaymentTransaction> root = cq.from(PaymentTransaction.class);
        Join<PaymentTransaction, PaymentMode> paymentMode = root.join(PaymentTransaction_.paymentMode, JoinType.INNER);
        cq.select(createBalanceCaisseDTOResult(cb, root, paymentMode));
        cq.where(
            cb.and(
                cb.between(root.get(PaymentTransaction_.transactionDate), mvtParam.getFromDate(), mvtParam.getToDate()),
                root.get(PaymentTransaction_.categorieChiffreAffaire).in(mvtParam.getCategorieChiffreAffaires()),
                root.get(PaymentTransaction_.type).notEqualTo(SalePayment.class.getSimpleName())
            )
        );
        cq.groupBy(
            paymentMode.get(PaymentMode_.code),
            paymentMode.get(PaymentMode_.libelle),
            root.get(PaymentTransaction_.typeFinancialTransaction)
        );

        return entityManager.createQuery(cq).getResultList();
    }

    private CompoundSelection<BalanceCaisseDTO> createBalanceCaisseDTOResult(
        CriteriaBuilder cb,
        Root<PaymentTransaction> root,
        Join<PaymentTransaction, PaymentMode> paymentMode
    ) {
        return cb.construct(
            BalanceCaisseDTO.class,
            cb.sumAsLong(root.get(PaymentTransaction_.paidAmount)),
            cb.sumAsLong(root.get(PaymentTransaction_.reelAmount)),
            paymentMode.get(PaymentMode_.code),
            paymentMode.get(PaymentMode_.libelle),
            root.get(PaymentTransaction_.typeFinancialTransaction)
        );
    }
}
