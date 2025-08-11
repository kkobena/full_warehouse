package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ClientTiersPayant_;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine_;
import com.kobe.warehouse.domain.TiersPayant_;
import com.kobe.warehouse.service.tiers_payant.TiersPayantAchat;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ThirdPartySaleLineCustomRepositoryImpl implements ThirdPartySaleLineCustomRepository {

    private final EntityManager entityManager;

    public ThirdPartySaleLineCustomRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<TiersPayantAchat> fetchAchatTiersPayant(Specification<ThirdPartySaleLine> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TiersPayantAchat> query = cb.createQuery(TiersPayantAchat.class);
        Root<ThirdPartySaleLine> root = query.from(ThirdPartySaleLine.class);
        Expression<Long> sumExpr = cb.sumAsLong(root.get(ThirdPartySaleLine_.montant));
        query
            .select(
                cb.construct(
                    TiersPayantAchat.class,
                    root.get(ThirdPartySaleLine_.clientTiersPayant).get(ClientTiersPayant_.tiersPayant).get(TiersPayant_.name),
                    cb.sumAsLong(root.get(ThirdPartySaleLine_.montant))
                )
            )
            .groupBy(root.get(ThirdPartySaleLine_.clientTiersPayant).get(ClientTiersPayant_.tiersPayant).get(TiersPayant_.id))
            .orderBy(cb.desc(sumExpr));

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);
        TypedQuery<TiersPayantAchat> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        return typedQuery.getResultList();
    }
}
