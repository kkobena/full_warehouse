package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.StockProduit_;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurTotal;
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
public class ProduitCustomRepositoryImpl implements ProduitCustomRepository {

    private final EntityManager entityManager;

    public ProduitCustomRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public LotPerimeValeurTotal fetchPerimeSum(Specification<Produit> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LotPerimeValeurTotal> query = cb.createQuery(LotPerimeValeurTotal.class);
        Root<Produit> root = query.from(Produit.class);
        Join<Produit, StockProduit> stockJoin = root.join(Produit_.stockProduits);

        Join<Produit, FournisseurProduit> fournisseurJoin = root.join(Produit_.fournisseurProduitPrincipal);
        query.select(
            cb.construct(
                LotPerimeValeurTotal.class,
                cb.sumAsLong(cb.prod(stockJoin.get(StockProduit_.qtyStock), fournisseurJoin.get(FournisseurProduit_.prixAchat))),
                cb.sumAsLong(cb.prod(stockJoin.get(StockProduit_.qtyStock), fournisseurJoin.get(FournisseurProduit_.prixUni))),
                cb.sum(stockJoin.get(StockProduit_.qtyStock)),
                cb.countDistinct(root)
            )
        );

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<LotPerimeValeurTotal> typedQuery = entityManager.createQuery(query);
        return typedQuery.getSingleResult();
    }
}
