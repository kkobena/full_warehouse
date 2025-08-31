package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.SalesLine_;
import com.kobe.warehouse.service.dto.OrderBy;
import com.kobe.warehouse.service.dto.records.ProductStatRecord;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class SalesLineRepositoryCustomImpl implements SalesLineRepositoryCustom {

    private final EntityManager em;

    public SalesLineRepositoryCustomImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<ProductStatRecord> fetchProductStat(Specification<SalesLine> spec, Long fournisseurId, OrderBy order, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<ProductStatRecord> cq = cb.createQuery(ProductStatRecord.class);
        Root<SalesLine> root = cq.from(SalesLine.class);
        Join<SalesLine, Produit> produitJoin = root.join(SalesLine_.produit, JoinType.INNER);
        cq.groupBy(produitJoin.get(Produit_.id), produitJoin.get(Produit_.fournisseurProduitPrincipal).get(FournisseurProduit_.codeCip));

       // TODO avoir comment metre le fourniseur principal en  jsonb dans produit
        Predicate predicates=spec.toPredicate(root, cq, cb);
        cq.where(predicates);

        cq.select(
            cb.construct(
                ProductStatRecord.class,
                produitJoin.get(Produit_.id),
                cb.count(produitJoin.get(Produit_.id)),
                //  cb.sum(root.get(SalesLine_.htAmount)),
                produitJoin.get(Produit_.fournisseurProduitPrincipal).get(FournisseurProduit_.codeCip),
                produitJoin.get(Produit_.codeEan),
                produitJoin.get(Produit_.libelle),
                cb.sum(root.get(SalesLine_.quantitySold)),
                cb.sum(root.get(SalesLine_.costAmount)),
                cb.sum(root.get(SalesLine_.salesAmount)),
                cb.sum(root.get(SalesLine_.discountAmount)),
                cb.sum(root.get(SalesLine_.amountToBeTakenIntoAccount)),
                cb.ceiling(cb.sum(cb.quot(root.get(SalesLine_.salesAmount), cb.sum(1, cb.quot(root.get(SalesLine_.taxValue), 100.0d))))),
                cb.diff(cb.sum(root.get(SalesLine_.salesAmount)), cb.sum(root.get(SalesLine_.discountAmount)))
            )
        );

        if (Objects.nonNull(order) && order == OrderBy.AMOUNT) {
            cq.orderBy(cb.desc(cb.sum(root.get(SalesLine_.salesAmount)))).groupBy(produitJoin.get(Produit_.id));
        } else {
            cq.orderBy(cb.desc(cb.sum(root.get(SalesLine_.quantitySold)))).groupBy(produitJoin.get(Produit_.id));
        }

        TypedQuery<ProductStatRecord> q = em.createQuery(cq);
        if (pageable.isPaged()) {
            q.setFirstResult((int) pageable.getOffset());
            q.setMaxResults(pageable.getPageSize());
        }
        return new PageImpl<>(q.getResultList(), pageable, count(predicates));
    }

    private long count(Predicate predicates) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<SalesLine> root = countQuery.from(SalesLine.class);
        countQuery.select(cb.countDistinct(root.get(SalesLine_.produit)));
        countQuery.where(predicates);
        return em.createQuery(countQuery).getSingleResult();
    }
}
