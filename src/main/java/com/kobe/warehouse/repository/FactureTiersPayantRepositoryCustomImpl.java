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
import com.kobe.warehouse.service.dto.enumeration.TypeFacture;
import com.kobe.warehouse.service.facturation.dto.FacturationKpiRow;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
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
                cb.sum(details.get(ThirdPartySaleLine_.montant)),
                root.get(FactureTiersPayant_.fneResponse),
                tiersPayantJoin.get(TiersPayant_.delaiReglement)
            )
        );

        query.groupBy(
            root.get(FactureTiersPayant_.fneResponse),
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
            tiersPayantJoin.get(TiersPayant_.delaiReglement)
        );

        TypedQuery<FactureDto> typedQuery = em.createQuery(query);


        List<FactureDto> result;
        if (Objects.nonNull(pageable) && pageable.isPaged()){
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
            result = typedQuery.getResultList();
            return new PageImpl<>(result, pageable, countInvoices(specification));
        }else{
            result = typedQuery.getResultList();

        }
        return new PageImpl<>(result, Pageable.unpaged(), result.size());




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
                cb.sum(sale.get(Sales_.discountAmount)),
                groupeTp.get(GroupeTiersPayant_.delaiReglement)
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
            groupeTp.get(GroupeTiersPayant_.name),
            groupeTp.get(GroupeTiersPayant_.delaiReglement)
        );

        TypedQuery<FactureDto> typedQuery = em.createQuery(query);
        List<FactureDto> result;
        if (Objects.nonNull(pageable) && pageable.isPaged()){
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
           result = typedQuery.getResultList();
            return new PageImpl<>(result, pageable, countGroupedInvoices(specification));
        }else{
            result = typedQuery.getResultList();

        }
        return new PageImpl<>(result, Pageable.unpaged(), result.size());


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

    @Override
    public Optional<FacturationKpiRow> getKpiData(
        LocalDate fromDate,
        LocalDate toDate,
        Integer organismeId,
        Integer groupeId,
        TypeFacture typeFacture,
        int delaiReglementDefaut
    ) {
        StringBuilder sql = buildSqlQuery(organismeId, groupeId, typeFacture);

        var query = em.createNativeQuery(sql.toString());
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        query.setParameter("delaiDefaut", delaiReglementDefaut);

        switch (typeFacture) {
            case INDIVIDUAL -> {
                if (organismeId != null) query.setParameter("organismeId", organismeId);
            }
            case GROUPED -> {
                if (groupeId != null) query.setParameter("groupeId", groupeId);
            }
            case ALL -> {
                if (organismeId != null) query.setParameter("organismeId", organismeId);
                if (groupeId != null)    query.setParameter("groupeId", groupeId);
            }
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(FacturationKpiRow.from(rows.getFirst()));
    }

    /**
     * Construit dynamiquement la requête SQL native pour les KPI.
     *
     * <p>La jointure ET la colonne {@code delai_reglement} varient selon {@link TypeFacture} :</p>
     * <ul>
     *   <li>{@code INDIVIDUAL} — {@code JOIN tiers_payant} ; {@code delai_reglement} issu de {@code tp}.</li>
     *   <li>{@code GROUPED}    — {@code JOIN groupe_tiers_payant} ; {@code delai_reglement} issu de {@code gtp}.</li>
     *   <li>{@code ALL}        — {@code LEFT JOIN} des deux tables ;
     *       {@code COALESCE(tp.delai_reglement, gtp.delai_reglement, :delaiDefaut)}.</li>
     * </ul>
     */
    private static @NonNull StringBuilder buildSqlQuery(Integer organismeId, Integer groupeId, TypeFacture typeFacture) {
        // ── En-tête SELECT commun ──────────────────────────────────────────
        String selectHead =
            """
            SELECT
                COALESCE(SUM(CAST(f.montant_net AS bigint)), 0),
                COALESCE(SUM(f.montant_regle), 0),
                COUNT(f.id),
                SUM(CASE WHEN f.statut <> 'PAID' THEN 1 ELSE 0 END),
            """;

        StringBuilder sql = new StringBuilder(selectHead);

        // ── Colonne retard + JOIN selon le type ───────────────────────────
        switch (typeFacture) {
            case INDIVIDUAL -> {
                sql.append(
                    """
                        SUM(CASE WHEN f.statut <> 'PAID'
                                  AND f.invoice_date + make_interval(days => COALESCE(tp.delai_reglement, :delaiDefaut)) < CURRENT_DATE
                             THEN 1 ELSE 0 END)
                    FROM facture_tiers_payant f
                    JOIN tiers_payant tp ON tp.id = f.tiers_payant_id
                    WHERE f.invoice_date BETWEEN :fromDate AND :toDate
                      AND f.groupe_tiers_payant_id IS NULL
                      AND f.groupe_facture_tiers_payant_id IS NULL
                    """);
                if (organismeId != null) {
                    sql.append("  AND f.tiers_payant_id = :organismeId");
                }
            }
            case GROUPED -> {
                sql.append(
                    """
                        SUM(CASE WHEN f.statut <> 'PAID'
                                  AND f.invoice_date + make_interval(days => COALESCE(gtp.delai_reglement, :delaiDefaut)) < CURRENT_DATE
                             THEN 1 ELSE 0 END)
                    FROM facture_tiers_payant f
                    JOIN groupe_tiers_payant gtp ON gtp.id = f.groupe_tiers_payant_id
                    WHERE f.invoice_date BETWEEN :fromDate AND :toDate
                    AND f.groupe_facture_tiers_payant_id IS NULL

                    """);
                if (groupeId != null) {
                    sql.append("  AND f.groupe_tiers_payant_id = :groupeId ");
                }
            }
            case ALL -> {
                sql.append(
                    """
                        SUM(CASE WHEN f.statut <> 'PAID'
                                  AND f.invoice_date + make_interval(days => COALESCE(tp.delai_reglement, gtp.delai_reglement, :delaiDefaut)) < CURRENT_DATE
                             THEN 1 ELSE 0 END)
                    FROM facture_tiers_payant f
                    LEFT JOIN tiers_payant tp ON tp.id = f.tiers_payant_id
                    LEFT JOIN groupe_tiers_payant gtp ON gtp.id = f.groupe_tiers_payant_id
                    WHERE f.invoice_date BETWEEN :fromDate AND :toDate
                      AND f.groupe_facture_tiers_payant_id IS NULL
                    """);

            }
        }
        if (organismeId != null) {
            sql.append("  AND f.tiers_payant_id = :organismeId ");
        }
        if (groupeId != null) {
            sql.append("  AND f.groupe_tiers_payant_id = :groupeId ");
        }
        return sql;
    }
}
