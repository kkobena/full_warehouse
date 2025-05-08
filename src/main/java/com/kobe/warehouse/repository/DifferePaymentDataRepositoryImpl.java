package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.*;

import com.kobe.warehouse.service.reglement.differe.dto.CustomerReglementDiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DifferePaymentSummary;
import com.kobe.warehouse.service.reglement.differe.dto.DifferePaymentSummaryDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DifferePaymentDataRepositoryImpl implements DifferePaymentDataRepository {
    private final EntityManager entityManager;

    public DifferePaymentDataRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<CustomerReglementDiffereDTO> getDifferePayments(Specification<DifferePayment> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CustomerReglementDiffereDTO> query = cb.createQuery(CustomerReglementDiffereDTO.class);
        Root<DifferePayment> root = query.from(DifferePayment.class);

        query
            .select(
                cb.construct(
                    CustomerReglementDiffereDTO.class,
                    root.get(DifferePayment_.differeCustomer).get(Customer_.id),
                    root.get(DifferePayment_.differeCustomer).get(Customer_.firstName),
                    root.get(DifferePayment_.differeCustomer).get(Customer_.lastName),
                    cb.sumAsLong(root.get(DifferePayment_.paidAmount))
                )
            )
            .groupBy(root.get(DifferePayment_.differeCustomer).get(Customer_.id));

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<CustomerReglementDiffereDTO> typedQuery = entityManager.createQuery(query);
        if (pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        List<CustomerReglementDiffereDTO> results = typedQuery.getResultList();
        if (pageable.isUnpaged()) {
            return new PageImpl<>(results);
        }

        return new PageImpl<>(results, pageable, count(specification));
    }

    @Override
    public DifferePaymentSummary getDiffereSummary(Specification<DifferePayment> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DifferePaymentSummary> query = cb.createQuery(DifferePaymentSummary.class);
        Root<DifferePayment> root = query.from(DifferePayment.class);

        query
            .select(
                cb.construct(
                    DifferePaymentSummary.class,
                    cb.sumAsLong(root.get(DifferePayment_.paidAmount))
                )
            ).orderBy(cb.desc(root.get(DifferePayment_.createdAt)));

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<DifferePaymentSummary> typedQuery = entityManager.createQuery(query);


        return typedQuery.getSingleResult();
    }

    @Override
    public List<ReglementDiffereDTO> getDifferePaymentsByCustomerId(Specification<DifferePayment> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReglementDiffereDTO> query = cb.createQuery(ReglementDiffereDTO.class);
        Root<DifferePayment> root = query.from(DifferePayment.class);

        query
            .select(
                cb.construct(
                    ReglementDiffereDTO.class,
                    root.get(DifferePayment_.id),
                    root.get(DifferePayment_.cashRegister).get(CashRegister_.user).get(User_.firstName),
                    root.get(DifferePayment_.cashRegister).get(CashRegister_.user).get(User_.lastName),
                    root.get(DifferePayment_.createdAt),
                    root.get(DifferePayment_.expectedAmount),
                    root.get(DifferePayment_.montantVerse),
                    root.get(DifferePayment_.paidAmount),
                    root.get(DifferePayment_.paymentMode).get(PaymentMode_.code),
                    root.get(DifferePayment_.paymentMode).get(PaymentMode_.libelle)
                )
            ).orderBy(cb.asc(root.get(DifferePayment_.createdAt)));

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<ReglementDiffereDTO> typedQuery = entityManager.createQuery(query);


        return typedQuery.getResultList();
    }

    private Long count(Specification<DifferePayment> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<DifferePayment> root = countQuery.from(DifferePayment.class);
        countQuery.select(cb.count(root));
        Predicate predicate = specification.toPredicate(root, countQuery, cb);
        countQuery.where(predicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
