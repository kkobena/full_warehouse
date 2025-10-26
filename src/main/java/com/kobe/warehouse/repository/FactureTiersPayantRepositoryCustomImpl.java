package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant_;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySaleLine_;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.TiersPayant_;
import com.kobe.warehouse.service.facturation.dto.FactureDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class FactureTiersPayantRepositoryCustomImpl implements FactureTiersPayantRepositoryCustom {

    private final EntityManager em;

    public FactureTiersPayantRepositoryCustomImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<FactureDto> fetchInvoices(Specification<FactureTiersPayant> specification, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<FactureDto> query = cb.createQuery(FactureDto.class);
        Root<FactureTiersPayant> root = query.from(FactureTiersPayant.class);
        Predicate predicate = specification.toPredicate(root, query, cb);

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<FactureTiersPayant> subRoot = subquery.from(FactureTiersPayant.class);
        subquery.select(cb.literal(1L)).where(cb.equal(subRoot.get(FactureTiersPayant_.groupeFactureTiersPayant), root));
        Predicate noGroupExists = cb.not(cb.exists(subquery));
        if (predicate != null) {
            query.where(cb.and(predicate, noGroupExists));
        } else {
            query.where(noGroupExists);
        }

        Join<FactureTiersPayant, ThirdPartySaleLine> details = root.join(FactureTiersPayant_.facturesDetails);
        Join<ThirdPartySaleLine, ThirdPartySales> sales = details.join(ThirdPartySaleLine_.sale);
        Join<FactureTiersPayant, FactureTiersPayant> groupe = root.join(FactureTiersPayant_.groupeFactureTiersPayant, JoinType.LEFT);
        Join<FactureTiersPayant, TiersPayant> tiersPayantJoin = root.join(FactureTiersPayant_.tiersPayant);

        query.select(
            cb.construct(
                FactureDto.class,
                root.get(FactureTiersPayant_.invoiceDate),
                root.get(FactureTiersPayant_.created),
                root.get(FactureTiersPayant_.id),
                groupe.get(FactureTiersPayant_.id),
                root.get(FactureTiersPayant_.montantRegle),
                root.get(FactureTiersPayant_.remiseForfetaire),
                root.get(FactureTiersPayant_.debutPeriode),
                root.get(FactureTiersPayant_.statut),
                root.get(FactureTiersPayant_.finPeriode),
                root.get(FactureTiersPayant_.factureProvisoire),
                root.get(FactureTiersPayant_.numFacture),
                groupe.get(FactureTiersPayant_.numFacture),
                tiersPayantJoin.get(TiersPayant_.fullName),
                cb.sum(sales.get(Sales_.salesAmount)),
                cb.sum(sales.get(Sales_.discountAmount)),
                cb.sum(details.get(ThirdPartySaleLine_.montantRegle)),
                cb.count(details),
                cb.sum(details.get(ThirdPartySaleLine_.montant))
            )
        );

        query.groupBy(
            root.get(FactureTiersPayant_.invoiceDate),
            root.get(FactureTiersPayant_.created),
            root.get(FactureTiersPayant_.id),
            groupe.get(FactureTiersPayant_.id),
            root.get(FactureTiersPayant_.montantRegle),
            root.get(FactureTiersPayant_.remiseForfetaire),
            root.get(FactureTiersPayant_.debutPeriode),
            root.get(FactureTiersPayant_.statut),
            root.get(FactureTiersPayant_.finPeriode),
            root.get(FactureTiersPayant_.factureProvisoire),
            root.get(FactureTiersPayant_.numFacture),
            groupe.get(FactureTiersPayant_.numFacture),
            tiersPayantJoin.get(TiersPayant_.fullName)
        );

        TypedQuery<FactureDto> typedQuery = em.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<FactureDto> result = typedQuery.getResultList();
        return new PageImpl<>(result, pageable, countInvoices(specification));
    }

    @Override
    public Page<FactureDto> fetchGroupedInvoices(Specification<FactureTiersPayant> specification, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<FactureDto> query = cb.createQuery(FactureDto.class);
        Root<FactureTiersPayant> root = query.from(FactureTiersPayant.class);
        Join<FactureTiersPayant, FactureTiersPayant> details = root.join(FactureTiersPayant_.factureTiersPayants);
        Join<FactureTiersPayant, GroupeTiersPayant> groupeTp = root.join(FactureTiersPayant_.groupeTiersPayant, JoinType.LEFT);
        Join<FactureTiersPayant, ThirdPartySaleLine> detailsVenete = details.join(FactureTiersPayant_.facturesDetails);
        Join<ThirdPartySaleLine, ThirdPartySales> sale = detailsVenete.join(ThirdPartySaleLine_.sale);
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }

        query.select(
            cb.construct(
                FactureDto.class,
                root.get(FactureTiersPayant_.invoiceDate),
                cb.sum(details.get(FactureTiersPayant_.montantRegle)),
                root.get(FactureTiersPayant_.created),
                root.get(FactureTiersPayant_.id),
                root.get(FactureTiersPayant_.debutPeriode),
                root.get(FactureTiersPayant_.statut),
                root.get(FactureTiersPayant_.finPeriode),
                root.get(FactureTiersPayant_.factureProvisoire),
                root.get(FactureTiersPayant_.numFacture),
                groupeTp.get(GroupeTiersPayant_.name),
                cb.sum(sale.get(Sales_.salesAmount)),
                cb.count(details),
                cb.sum(detailsVenete.get(ThirdPartySaleLine_.montant)),
                cb.sum(sale.get(Sales_.discountAmount))
            )
        );
        query.groupBy(
            root.get(FactureTiersPayant_.invoiceDate),
            root.get(FactureTiersPayant_.created),
            root.get(FactureTiersPayant_.id),
            root.get(FactureTiersPayant_.debutPeriode),
            root.get(FactureTiersPayant_.statut),
            root.get(FactureTiersPayant_.finPeriode),
            root.get(FactureTiersPayant_.factureProvisoire),
            root.get(FactureTiersPayant_.numFacture),
            groupeTp.get(GroupeTiersPayant_.name)
        );

        TypedQuery<FactureDto> typedQuery = em.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<FactureDto> result = typedQuery.getResultList();
        return new PageImpl<>(result, pageable, countGroupedInvoices(specification));
    }

    private long countInvoices(Specification<FactureTiersPayant> specification) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<FactureTiersPayant> root = query.from(FactureTiersPayant.class);
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        query.select(cb.count(root));
        TypedQuery<Long> typedQuery = em.createQuery(query);
        return typedQuery.getSingleResult();
    }

    private long countGroupedInvoices(Specification<FactureTiersPayant> specification) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<FactureTiersPayant> root = query.from(FactureTiersPayant.class);
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        query.select(cb.count(root));
        TypedQuery<Long> typedQuery = em.createQuery(query);
        return typedQuery.getSingleResult();
    }
}
