package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant_;
import com.kobe.warehouse.domain.GroupeTiersPayant_;
import com.kobe.warehouse.domain.Sales_;
import com.kobe.warehouse.domain.ThirdPartySaleLine_;
import com.kobe.warehouse.domain.TiersPayant_;
import com.kobe.warehouse.service.facturation.dto.FactureDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        if (predicate != null) {
            query.where(predicate);
        }

        Subquery<Long> subquery = query.subquery(Long.class);
        Root<FactureTiersPayant> subRoot = subquery.from(FactureTiersPayant.class);
        subquery.select(subRoot.get(FactureTiersPayant_.groupeFactureTiersPayant).get(FactureTiersPayant_.id));
        subquery.where(subRoot.get(FactureTiersPayant_.groupeFactureTiersPayant).isNotNull());

        query.where(cb.not(root.get(FactureTiersPayant_.id).in(subquery)));

        query.select(
            cb.construct(
                FactureDto.class,
                root.get(FactureTiersPayant_.created),
                root.get(FactureTiersPayant_.id),
                root.get(FactureTiersPayant_.groupeFactureTiersPayant).get(FactureTiersPayant_.id),
                root.get(FactureTiersPayant_.montantRegle),
                root.get(FactureTiersPayant_.remiseForfetaire),
                root.get(FactureTiersPayant_.debutPeriode),
                root.get(FactureTiersPayant_.statut),
                root.get(FactureTiersPayant_.finPeriode),
                root.get(FactureTiersPayant_.factureProvisoire),
                root.get(FactureTiersPayant_.numFacture),
                root.get(FactureTiersPayant_.groupeFactureTiersPayant).get(FactureTiersPayant_.numFacture),
                root.get(FactureTiersPayant_.tiersPayant).get(TiersPayant_.fullName),
                cb.sum(root.join(FactureTiersPayant_.facturesDetails).join(ThirdPartySaleLine_.sale).get(Sales_.salesAmount)),
                cb.sum(root.join(FactureTiersPayant_.facturesDetails).join(ThirdPartySaleLine_.sale).get(Sales_.discountAmount)),
                cb.sum(root.join(FactureTiersPayant_.facturesDetails).get(ThirdPartySaleLine_.montantRegle)),
                cb.count(root.join(FactureTiersPayant_.facturesDetails)),
                cb.sum(root.join(FactureTiersPayant_.facturesDetails).get(ThirdPartySaleLine_.montant))
            )
        );
        query.groupBy(root.get(FactureTiersPayant_.id));

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
        //   Specification<FactureTiersPayant> specification = InvoiceSpecification.aGroupedFacture(invoiceSearchParams);
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }

        query.select(
            cb.construct(
                FactureDto.class,
                cb.sum(root.join(FactureTiersPayant_.factureTiersPayants).get(FactureTiersPayant_.montantRegle)),
                root.get(FactureTiersPayant_.created),
                root.get(FactureTiersPayant_.id),
                root.get(FactureTiersPayant_.debutPeriode),
                root.get(FactureTiersPayant_.statut),
                root.get(FactureTiersPayant_.finPeriode),
                root.get(FactureTiersPayant_.factureProvisoire),
                root.get(FactureTiersPayant_.numFacture),
                root.get(FactureTiersPayant_.groupeTiersPayant).get(GroupeTiersPayant_.name),
                cb.sum(root.join(FactureTiersPayant_.factureTiersPayants).join(FactureTiersPayant_.facturesDetails).get(ThirdPartySaleLine_.montant)),
                cb.count(root.join(FactureTiersPayant_.factureTiersPayants)),
                cb.sum(
                    root
                        .join(FactureTiersPayant_.factureTiersPayants)
                        .join(FactureTiersPayant_.facturesDetails)
                        .join(ThirdPartySaleLine_.sale)
                        .get(Sales_.salesAmount)
                ),
                cb.sum(
                    root
                        .join(FactureTiersPayant_.factureTiersPayants)
                        .join(FactureTiersPayant_.facturesDetails)
                        .join(ThirdPartySaleLine_.sale)
                        .get(Sales_.discountAmount)
                )
            )
        );
        query.groupBy(root.get(FactureTiersPayant_.id));

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
        // Specification<FactureTiersPayant> specification = InvoiceSpecification.aFacture(invoiceSearchParams);
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
