package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.ThirdPartySales_;
import com.kobe.warehouse.domain.AppUser_;
import com.kobe.warehouse.service.tiketz.dto.TicketZCreditProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ThirdPartySaleCustomRepositoryImpl implements ThirdPartySaleCustomRepository {

    private final EntityManager entityManager;

    public ThirdPartySaleCustomRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<TicketZCreditProjection> getTicketZCreditProjection(Specification<ThirdPartySales> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TicketZCreditProjection> query = cb.createQuery(TicketZCreditProjection.class);
        Root<ThirdPartySales> root = query.from(ThirdPartySales.class);
        query
            .select(
                cb.construct(
                    TicketZCreditProjection.class,
                    root.get(ThirdPartySales_.caissier).get(AppUser_.id),
                    root.get(ThirdPartySales_.caissier).get(AppUser_.firstName),
                    root.get(ThirdPartySales_.caissier).get(AppUser_.lastName),
                    cb.sumAsLong(root.get(ThirdPartySales_.partTiersPayant))
                )
            )
            .groupBy( root.get(ThirdPartySales_.caissier).get(AppUser_.id),
                root.get(ThirdPartySales_.caissier).get(AppUser_.firstName),
                root.get(ThirdPartySales_.caissier).get(AppUser_.lastName));

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<TicketZCreditProjection> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }
}
