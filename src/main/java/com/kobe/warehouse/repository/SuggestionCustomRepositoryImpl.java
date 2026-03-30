package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.Suggestion_;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.service.dto.FournisseurSuggestionSummaryDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class SuggestionCustomRepositoryImpl implements SuggestionCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<SuggestionProjection> getAllSuggestion(Specification<Suggestion> specification, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SuggestionProjection> query = cb.createQuery(SuggestionProjection.class);

        Root<Suggestion> root = query.from(Suggestion.class);
        query
            .select(
                cb.construct(
                    SuggestionProjection.class,
                    root.get(Suggestion_.id),
                    root.get(Suggestion_.suggessionReference),
                    root.get(Suggestion_.createdAt),
                    root.get(Suggestion_.updatedAt),
                    root.get(Suggestion_.typeSuggession),
                    root.get(Suggestion_.statut),
                    root.get(Suggestion_.fournisseur).get(Fournisseur_.id),
                    root.get(Suggestion_.fournisseur).get(Fournisseur_.libelle)
                )
            )
            .orderBy(cb.desc(root.get(Suggestion_.updatedAt)));

        // Appliquer la specification pour ajouter des critères à la requête
        Predicate predicate = specification.toPredicate(root, query, cb);
        query.where(predicate);

        TypedQuery<SuggestionProjection> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<SuggestionProjection> results = typedQuery.getResultList();

        return new PageImpl<>(results, pageable, count(specification));
    }

    private static final String PAR_FOURNISSEUR_SQL = """
        SELECT
            s.id                                          AS suggestion_id,
            f.id                                          AS fournisseur_id,
            f.libelle                                     AS libelle,
            s.statut,
            COUNT(sl.id)                                  AS nb_produits,
            COALESCE(SUM(sl.quantity * fp.prix_achat), 0) AS montant_estime,
            CASE
                WHEN COUNT(sl.id) = 0 THEN 'STANDARD'
                WHEN COUNT(sc.id) = 0 THEN 'STANDARD'
                WHEN COUNT(sc.id) = COUNT(sl.id) THEN 'SEMOIS'
                ELSE 'MIXTE'
            END                                           AS source,
            s.updated_at                                  AS updated_at
        FROM suggestion s
        JOIN fournisseur f ON f.id = s.fournisseur_id
        LEFT JOIN suggestion_line sl ON sl.suggestion_id = s.id
        LEFT JOIN fournisseur_produit fp ON fp.id = sl.fournisseur_produit_id
        LEFT JOIN semois_configuration sc ON sc.produit_id = fp.produit_id
        WHERE s.updated_at >= NOW() - make_interval(days => :retentionDays)
        GROUP BY s.id, f.id, f.libelle, s.statut, s.updated_at
        ORDER BY f.libelle ASC
        """;

    @Override
    public List<FournisseurSuggestionSummaryDTO> getParFournisseur(int retentionDays) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager
            .createNativeQuery(PAR_FOURNISSEUR_SQL)
            .setParameter("retentionDays", retentionDays)
            .getResultList();

        return rows.stream().map(row -> new FournisseurSuggestionSummaryDTO(
            toInt(row[0]),            // suggestionId
            toInt(row[1]),            // fournisseurId
            (String) row[2],          // libelle
            (String) row[3],          // statut
            toLong(row[4]).intValue(), // nbProduits
            0,                        // nbUrgents (calculé côté frontend après chargement des lignes)
            toLong(row[5]),           // montantEstime
            (String) row[6],          // source
            toLocalDateTime(row[7])   // updatedAt
        )).toList();
    }

    private static final String PAR_FOURNISSEUR_SQL_WITH_STATUT = """
        SELECT
            s.id                                          AS suggestion_id,
            f.id                                          AS fournisseur_id,
            f.libelle                                     AS libelle,
            s.statut,
            COUNT(sl.id)                                  AS nb_produits,
            COALESCE(SUM(sl.quantity * fp.prix_achat), 0) AS montant_estime,
            CASE
                WHEN COUNT(sl.id) = 0 THEN 'STANDARD'
                WHEN COUNT(sc.id) = 0 THEN 'STANDARD'
                WHEN COUNT(sc.id) = COUNT(sl.id) THEN 'SEMOIS'
                ELSE 'MIXTE'
            END                                           AS source,
            s.updated_at                                  AS updated_at
        FROM suggestion s
        JOIN fournisseur f ON f.id = s.fournisseur_id
        LEFT JOIN suggestion_line sl ON sl.suggestion_id = s.id
        LEFT JOIN fournisseur_produit fp ON fp.id = sl.fournisseur_produit_id
        LEFT JOIN semois_configuration sc ON sc.produit_id = fp.produit_id
        WHERE s.updated_at >= NOW() - make_interval(days => :retentionDays)
          AND s.statut = :statut
        GROUP BY s.id, f.id, f.libelle, s.statut, s.updated_at
        ORDER BY f.libelle ASC
        """;

    @Override
    public List<FournisseurSuggestionSummaryDTO> getParFournisseur(int retentionDays, StatutSuggession statut) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager
            .createNativeQuery(PAR_FOURNISSEUR_SQL_WITH_STATUT)
            .setParameter("retentionDays", retentionDays)
            .setParameter("statut", statut.name())
            .getResultList();

        return rows.stream().map(row -> new FournisseurSuggestionSummaryDTO(
            toInt(row[0]),
            toInt(row[1]),
            (String) row[2],
            (String) row[3],
            toLong(row[4]).intValue(),
            0,
            toLong(row[5]),
            (String) row[6],
            toLocalDateTime(row[7])
        )).toList();
    }

    private static LocalDateTime toLocalDateTime(Object val) {
        if (val instanceof Timestamp ts) return ts.toLocalDateTime();
        if (val instanceof LocalDateTime ldt) return ldt;
        return null;
    }

    private static Integer toInt(Object val) {
        if (val instanceof Number n) return n.intValue();
        return null;
    }

    private static Long toLong(Object val) {
        if (val instanceof BigDecimal bd) return bd.longValue();
        if (val instanceof BigInteger bi) return bi.longValue();
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }

    private Long count(Specification<Suggestion> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Suggestion> root = countQuery.from(Suggestion.class);
        countQuery.select(cb.count(root));
        Predicate predicate = specification.toPredicate(root, countQuery, cb);
        countQuery.where(predicate);
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
