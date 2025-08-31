package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.ClientTiersPayant_;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant_;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine_;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.TiersPayant_;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import com.kobe.warehouse.service.facturation.dto.TiersPayantDossierFactureDto;
import com.kobe.warehouse.service.facturation.specification.EditionDataSpecification;
import com.kobe.warehouse.service.tiers_payant.TiersPayantAchat;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
            .groupBy(root.get(ThirdPartySaleLine_.clientTiersPayant).get(ClientTiersPayant_.tiersPayant).get(TiersPayant_.id), root.get(ThirdPartySaleLine_.clientTiersPayant).get(ClientTiersPayant_.tiersPayant).get(TiersPayant_.name))
            .orderBy(cb.desc(sumExpr));

        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);
        TypedQuery<TiersPayantAchat> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        return typedQuery.getResultList();
    }

    @Override
    public Page<TiersPayantDossierFactureDto> fetch(Specification<ThirdPartySaleLine> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TiersPayantDossierFactureDto> query = cb.createQuery(TiersPayantDossierFactureDto.class);
        Root<ThirdPartySaleLine> root = query.from(ThirdPartySaleLine.class);
      //  Specification<ThirdPartySaleLine> specification = EditionDataSpecification.aThirdPartySaleLine(editionSearchParams);
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }

            Join<ThirdPartySaleLine, ClientTiersPayant> clientTiersPayantJoin = root.join(ThirdPartySaleLine_.clientTiersPayant, JoinType.INNER);
            Join<ClientTiersPayant, TiersPayant> tiersPayantJoin = clientTiersPayantJoin.join(ClientTiersPayant_.tiersPayant, JoinType.INNER);
            query.groupBy(tiersPayantJoin.get(TiersPayant_.id), tiersPayantJoin.get(TiersPayant_.fullName));
            query.select(
                cb.construct(
                    TiersPayantDossierFactureDto.class,
                    tiersPayantJoin.get(TiersPayant_.id),
                    tiersPayantJoin.get(TiersPayant_.name),
                    cb.sum(root.get(ThirdPartySaleLine_.montant)),
                    cb.count(root.get(ThirdPartySaleLine_.id))
                )
            );


        TypedQuery<TiersPayantDossierFactureDto> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<TiersPayantDossierFactureDto> result = typedQuery.getResultList();
        return new PageImpl<>(result, pageable, count(specification));
    }

    private long count(Specification<ThirdPartySaleLine> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ThirdPartySaleLine> root = query.from(ThirdPartySaleLine.class);
      //  Specification<ThirdPartySaleLine> specification = EditionDataSpecification.aThirdPartySaleLine(editionSearchParams);
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
            Join<ThirdPartySaleLine, ClientTiersPayant> clientTiersPayantJoin = root.join(ThirdPartySaleLine_.clientTiersPayant, JoinType.INNER);
            Join<ClientTiersPayant, TiersPayant> tiersPayantJoin = clientTiersPayantJoin.join(ClientTiersPayant_.tiersPayant, JoinType.INNER);
            query.select(cb.countDistinct(tiersPayantJoin.get(TiersPayant_.id)));

        TypedQuery<Long> typedQuery = entityManager.createQuery(query);
        return typedQuery.getSingleResult();
    }
    private long countGroup(Specification<ThirdPartySaleLine> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ThirdPartySaleLine> root = query.from(ThirdPartySaleLine.class);
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
            Join<ThirdPartySaleLine, ClientTiersPayant> clientTiersPayantJoin = root.join(ThirdPartySaleLine_.clientTiersPayant, JoinType.INNER);
            Join<ClientTiersPayant, TiersPayant> tiersPayantJoin = clientTiersPayantJoin.join(ClientTiersPayant_.tiersPayant, JoinType.INNER);
            Join<TiersPayant, GroupeTiersPayant> groupeTiersPayantJoin = tiersPayantJoin.join(TiersPayant_.groupeTiersPayant, JoinType.INNER);
            query.select(cb.countDistinct(groupeTiersPayantJoin.get(GroupeTiersPayant_.id)));

        TypedQuery<Long> typedQuery = entityManager.createQuery(query);
        return typedQuery.getSingleResult();
    }
    @Override
    public Page<TiersPayantDossierFactureDto> fetchGroup(Specification<ThirdPartySaleLine> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TiersPayantDossierFactureDto> query = cb.createQuery(TiersPayantDossierFactureDto.class);
        Root<ThirdPartySaleLine> root = query.from(ThirdPartySaleLine.class);
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
            Join<ThirdPartySaleLine, ClientTiersPayant> clientTiersPayantJoin = root.join(ThirdPartySaleLine_.clientTiersPayant, JoinType.INNER);
            Join<ClientTiersPayant, TiersPayant> tiersPayantJoin = clientTiersPayantJoin.join(ClientTiersPayant_.tiersPayant, JoinType.INNER);
            Join<TiersPayant, GroupeTiersPayant> groupeTiersPayantJoin = tiersPayantJoin.join(TiersPayant_.groupeTiersPayant, JoinType.INNER);
            query.groupBy(groupeTiersPayantJoin.get(GroupeTiersPayant_.id), groupeTiersPayantJoin.get(GroupeTiersPayant_.name));
            query.select(
                cb.construct(
                    TiersPayantDossierFactureDto.class,
                    groupeTiersPayantJoin.get(GroupeTiersPayant_.id),
                    groupeTiersPayantJoin.get(GroupeTiersPayant_.name),
                    cb.sum(root.get(ThirdPartySaleLine_.montant)),
                    cb.count(root.get(ThirdPartySaleLine_.id))
                )
            );


        TypedQuery<TiersPayantDossierFactureDto> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<TiersPayantDossierFactureDto> result = typedQuery.getResultList();
        return new PageImpl<>(result, pageable, countGroup(specification));
    }

}
