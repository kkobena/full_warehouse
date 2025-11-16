package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Lot_;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLine_;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class LotCustomRepositoryImpl implements LotCustomRepository {

    private final EntityManager entityManager;

    public LotCustomRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public LotPerimeValeurSum fetchPerimeSum(Specification<Lot> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LotPerimeValeurSum> query = cb.createQuery(LotPerimeValeurSum.class);
        Root<Lot> root = query.from(Lot.class);
        Join<Lot, OrderLine> fournisseurProduitJoin = root.join(Lot_.orderLine);
        Join<OrderLine, FournisseurProduit> orderLineJoin = fournisseurProduitJoin.join(OrderLine_.fournisseurProduit);
        query.select(
            cb.construct(
                LotPerimeValeurSum.class,
                cb.sumAsLong(cb.prod(root.get(Lot_.quantity), orderLineJoin.get(FournisseurProduit_.prixAchat))),
                cb.sumAsLong(cb.prod(root.get(Lot_.quantity), orderLineJoin.get(FournisseurProduit_.prixUni))),
                cb.sum(root.get(Lot_.quantity)),
                cb.count(root)
            )
        );

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<LotPerimeValeurSum> typedQuery = entityManager.createQuery(query);
        return typedQuery.getSingleResult();
    }
}
