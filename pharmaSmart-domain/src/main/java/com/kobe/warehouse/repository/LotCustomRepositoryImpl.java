package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Lot_;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLine_;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import java.time.LocalDate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
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
             cb.coalesce(cb.sumAsLong(cb.prod(root.get(Lot_.quantity), orderLineJoin.get(FournisseurProduit_.prixAchat))), 0L),
                cb.coalesce(cb.sumAsLong(cb.prod(root.get(Lot_.quantity), orderLineJoin.get(FournisseurProduit_.prixUni))), 0L),
                cb.coalesce(cb.sum(root.get(Lot_.quantity)), 0),
                cb.coalesce(cb.count(root), 0L),
                // Lots dont la péremption tombe dans les 30 prochains jours, dans le MÊME
                // périmètre filtré que les autres agrégats. La fenêtre est fixée à 30 jours
                // et non au seuil d'alerte configurable : la carte du bandeau annonce « 30j »
                // et le filtre qu'elle déclenche porte le même horizon.
                cb.coalesce(
                    cb.sum(
                        cb.<Long>selectCase()
                            .when(
                                cb.between(
                                    root.get(Lot_.expiryDate),
                                    cb.literal(LocalDate.now()),
                                    cb.literal(LocalDate.now().plusDays(30))
                                ),
                                1L
                            )
                            .otherwise(0L)
                    ),
                    0L
                ),
                // Complété par le service : les bons de retour ne se comptent pas depuis
                // cette racine.
                cb.literal(0L)
            )
        );

        query.where( specification.toPredicate(root, query, cb));

        TypedQuery<LotPerimeValeurSum> typedQuery = entityManager.createQuery(query);
        return typedQuery.getSingleResult();
    }
}
