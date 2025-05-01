package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Customer_;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.service.reglement.differe.dto.Differe;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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
public class CustomSalesRepositoryImpl implements CustomSalesRepository {

    private final EntityManager entityManager;

    public CustomSalesRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<DiffereItem> getDiffereItems(Specification<Sales> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DiffereItem> query = cb.createQuery(DiffereItem.class);
        Root<Sales> root = query.from(Sales.class);

        query
            .select(
                cb.construct(
                    DiffereItem.class,
                    root.get(Sales_.customer).get(Customer_.firstName),
                    root.get(Sales_.customer).get(Customer_.lastName),
                    root.get(Sales_.numberTransaction),
                    root.get(Sales_.salesAmount),
                    root.get(Sales_.payrollAmount),
                    root.get(Sales_.restToPay),
                    root.get(Sales_.updatedAt),
                    root.get(Sales_.id),
                    root.get(Sales_.customer).get(Customer_.id)
                )
            )
            .orderBy(cb.desc(root.get(Sales_.effectiveUpdateDate)));

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<DiffereItem> typedQuery = entityManager.createQuery(query);
        if (pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        List<DiffereItem> results = typedQuery.getResultList();
        if (pageable.isUnpaged()) {
            return new PageImpl<>(results);
        }

        return new PageImpl<>(results, pageable, count(specification));
    }

    @Override
    public Page<Differe> getDiffere(Specification<Sales> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Differe> query = cb.createQuery(Differe.class);
        Root<Sales> root = query.from(Sales.class);

        query
            .select(
                cb.construct(
                    Differe.class,
                    root.get(Sales_.customer).get(Customer_.id),
                    root.get(Sales_.customer).get(Customer_.firstName),
                    root.get(Sales_.customer).get(Customer_.lastName),
                    cb.sumAsLong(root.get(Sales_.salesAmount)),
                    cb.sumAsLong(root.get(Sales_.payrollAmount)),
                    cb.sumAsLong(root.get(Sales_.restToPay))
                )
            )
            .groupBy(root.get(Sales_.customer).get(Customer_.id))
            .orderBy(
                cb.desc(root.get(Sales_.customer).get(Customer_.firstName)),
                cb.desc(root.get(Sales_.customer).get(Customer_.lastName))
            );

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<Differe> typedQuery = entityManager.createQuery(query);
        if (pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        List<Differe> results = typedQuery.getResultList();
        if (pageable.isUnpaged()) {
            return new PageImpl<>(results);
        }

        return new PageImpl<>(results, pageable, countDifferes(specification));
    }

    private Long countDifferes(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Sales> root = countQuery.from(Sales.class);
        countQuery.select(cb.countDistinct(root.get(Sales_.customer)));
        Predicate predicate = specification.toPredicate(root, countQuery, cb);
        countQuery.where(predicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private Long count(Specification<Sales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Sales> root = countQuery.from(Sales.class);
        countQuery.select(cb.count(root));
        Predicate predicate = specification.toPredicate(root, countQuery, cb);
        countQuery.where(predicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
