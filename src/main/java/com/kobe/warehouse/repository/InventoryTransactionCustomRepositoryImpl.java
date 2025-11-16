package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.InventoryTransaction_;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingSum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryTransactionCustomRepositoryImpl implements InventoryTransactionCustomRepository {

    private final EntityManager entityManager;

    public InventoryTransactionCustomRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<ProduitAuditingState> fetchProduitDailyTransaction(Specification<InventoryTransaction> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProduitAuditingState> query = cb.createQuery(ProduitAuditingState.class);
        Root<InventoryTransaction> root = query.from(InventoryTransaction.class);
        Expression<LocalDate> dateExpr = root.get(InventoryTransaction_.transactionDate);

        // Subquery to find the minimum createdAt (first transaction) per day
        var subqueryMin = query.subquery(LocalDateTime.class);
        var rootMin = subqueryMin.from(InventoryTransaction.class);
        subqueryMin
            .select(cb.least(rootMin.get(InventoryTransaction_.createdAt)))
            .where(cb.equal(rootMin.get(InventoryTransaction_.transactionDate), dateExpr));

        // Subquery to find the maximum createdAt (last transaction) per day
        var subqueryMax = query.subquery(LocalDateTime.class);
        var rootMax = subqueryMax.from(InventoryTransaction.class);
        subqueryMax
            .select(cb.greatest(rootMax.get(InventoryTransaction_.createdAt)))
            .where(cb.equal(rootMax.get(InventoryTransaction_.transactionDate), dateExpr));

        // Use CASE expressions to extract start and end quantities based on transaction timing
        Expression<Integer> startQuantity = cb
            .<Integer>selectCase()
            .when(cb.equal(root.get(InventoryTransaction_.createdAt), subqueryMin), root.get(InventoryTransaction_.quantityBefor))
            .otherwise((Integer) null);

        Expression<Integer> endQuantity = cb
            .<Integer>selectCase()
            .when(cb.equal(root.get(InventoryTransaction_.createdAt), subqueryMax), root.get(InventoryTransaction_.quantityAfter))
            .otherwise((Integer) null);

        query
            .select(
                cb.construct(
                    ProduitAuditingState.class,
                    root.get(InventoryTransaction_.mouvementType),
                    dateExpr,
                    cb.max(startQuantity), // Start quantity from first transaction of the day
                    cb.sum(root.get(InventoryTransaction_.quantity)), // Sum of quantities by movement type
                    cb.max(endQuantity) // End quantity from last transaction of the day
                )
            )
            .groupBy(root.get(InventoryTransaction_.mouvementType), root.get(InventoryTransaction_.transactionDate))
            .orderBy(cb.asc(root.get(InventoryTransaction_.transactionDate)));

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<ProduitAuditingState> typedQuery = entityManager.createQuery(query);

        if (pageable.isUnpaged()) {
            List<ProduitAuditingState> results = typedQuery.getResultList();
            return new PageImpl<>(results, pageable, results.size());
        }
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<ProduitAuditingState> results = typedQuery.getResultList();
        return new PageImpl<>(results, pageable, count(specification));
    }

    @Override
    public List<ProduitAuditingSum> fetchProduitDailyTransactionSum(Specification<InventoryTransaction> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProduitAuditingSum> query = cb.createQuery(ProduitAuditingSum.class);
        Root<InventoryTransaction> root = query.from(InventoryTransaction.class);
        query
            .select(
                cb.construct(
                    ProduitAuditingSum.class,
                    root.get(InventoryTransaction_.mouvementType),
                    cb.sum(root.get(InventoryTransaction_.quantity))
                )
            )
            .groupBy(root.get(InventoryTransaction_.mouvementType));
        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<ProduitAuditingSum> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    private Long count(Specification<InventoryTransaction> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<InventoryTransaction> root = countQuery.from(InventoryTransaction.class);

        // Count distinct combinations of (movementType, transactionDate)
        countQuery.select(
            cb.countDistinct(
                cb.concat(
                    cb.concat(root.get(InventoryTransaction_.mouvementType).as(String.class), "-"),
                    root.get(InventoryTransaction_.transactionDate).as(String.class)
                )
            )
        );

        Predicate predicate = specification.toPredicate(root, countQuery, cb);
        if (predicate != null) {
            countQuery.where(predicate);
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
