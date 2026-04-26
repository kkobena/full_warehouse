package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.SuggestionLine_;
import com.kobe.warehouse.domain.Suggestion_;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.service.dto.FournisseurSuggestionSummaryDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class SuggestionCustomRepositoryImpl implements SuggestionCustomRepository {
    private static final String PAR_FOURNISSEUR_BASE_SQL = """
        SELECT
            s.id,
            f.id,
            f.libelle,
            s.statut,
            COUNT(sl.id)                                    AS nb_produits,
            COALESCE(SUM(sl.quantity * fp.prix_achat), 0)  AS montant_estime,
            COALESCE(SUM(sl.quantity * fp.prix_uni),   0)  AS montant_estime_vente,
            CASE
                WHEN COUNT(sl.id) = 0 THEN 'STANDARD'
                WHEN COUNT(sc.id) = 0 THEN 'STANDARD'
                WHEN COUNT(sc.id) = COUNT(sl.id) THEN 'SEMOIS'
                ELSE 'MIXTE'
            END                                             AS source,
            s.updated_at
        FROM suggestion s
        JOIN fournisseur f ON f.id = s.fournisseur_id
        LEFT JOIN suggestion_line sl ON sl.suggestion_id = s.id
        LEFT JOIN fournisseur_produit fp ON fp.id = sl.fournisseur_produit_id
        LEFT JOIN semois_configuration sc ON sc.produit_id = fp.produit_id
        """;

    private static final String PAR_FOURNISSEUR_SEARCH_EXISTS = """
        EXISTS (
            SELECT 1 FROM suggestion_line sl2
            JOIN fournisseur_produit fp2 ON fp2.id = sl2.fournisseur_produit_id
            JOIN produit p2 ON p2.id = fp2.produit_id
            WHERE sl2.suggestion_id = s.id
              AND (
                  UPPER(fp2.code_cip) LIKE :pattern
                  OR UPPER(fp2.code_ean) LIKE :pattern
                  OR UPPER(p2.libelle) LIKE :pattern
                  OR UPPER(p2.code_ean_labo) LIKE :pattern
              )
        )
        """;

    private static final String PAR_FOURNISSEUR_GROUP_ORDER = """
        GROUP BY s.id, f.id, f.libelle, s.statut, s.updated_at
        ORDER BY f.libelle ASC
        """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<SuggestionProjection> getAllSuggestion(Specification<Suggestion> specification, Pageable pageable, String searchTerm) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SuggestionProjection> query = cb.createQuery(SuggestionProjection.class);
        Root<Suggestion> root = query.from(Suggestion.class);
        Join<Suggestion, Fournisseur> fournisseurJoin = root.join(Suggestion_.fournisseur);

        // Sous-requête corrélée : nombre de lignes
        Subquery<Long> countSub = query.subquery(Long.class);
        Root<SuggestionLine> slCount = countSub.from(SuggestionLine.class);
        countSub.select(cb.count(slCount))
            .where(cb.equal(slCount.get(SuggestionLine_.suggestion), root));

        // Sous-requête corrélée : montant estimé achat = SUM(quantité * prixAchat)
        Subquery<Long> montantAchatSub = query.subquery(Long.class);
        Root<SuggestionLine> slMontant = montantAchatSub.from(SuggestionLine.class);
        Join<SuggestionLine, FournisseurProduit> fpMontant = slMontant.join(SuggestionLine_.fournisseurProduit);
        montantAchatSub.select(
            cb.coalesce(
                cb.sum(cb.prod(
                    cb.toLong(slMontant.get(SuggestionLine_.quantity)),
                    cb.toLong(fpMontant.get(FournisseurProduit_.prixAchat))
                )),
                0L
            )
        ).where(cb.equal(slMontant.get(SuggestionLine_.suggestion), root));

        // Sous-requête corrélée : montant estimé vente = SUM(quantité * prixUni)
        Subquery<Long> montantVenteSub = query.subquery(Long.class);
        Root<SuggestionLine> slVente = montantVenteSub.from(SuggestionLine.class);
        Join<SuggestionLine, FournisseurProduit> fpVente = slVente.join(SuggestionLine_.fournisseurProduit);
        montantVenteSub.select(
            cb.coalesce(
                cb.sum(cb.prod(
                    cb.toLong(slVente.get(SuggestionLine_.quantity)),
                    cb.toLong(fpVente.get(FournisseurProduit_.prixUni))
                )),
                0L
            )
        ).where(cb.equal(slVente.get(SuggestionLine_.suggestion), root));

        query.select(
            cb.construct(
                SuggestionProjection.class,
                root.get(Suggestion_.id),
                root.get(Suggestion_.suggessionReference),
                root.get(Suggestion_.createdAt),
                root.get(Suggestion_.updatedAt),
                root.get(Suggestion_.typeSuggession),
                root.get(Suggestion_.statut),
                fournisseurJoin.get(Fournisseur_.id),
                fournisseurJoin.get(Fournisseur_.libelle),
                countSub,
                montantAchatSub,
                montantVenteSub
            )
        ).orderBy(cb.desc(root.get(Suggestion_.updatedAt)));

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(specification.toPredicate(root, query, cb));
        if (StringUtils.hasLength(searchTerm)) {
            predicates.add(buildSearchPredicate(cb, query, root, searchTerm));
        }
        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<SuggestionProjection> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<SuggestionProjection> results = typedQuery.getResultList();

        return new PageImpl<>(results, pageable, count(specification, searchTerm));
    }

    private <T> Predicate buildSearchPredicate(CriteriaBuilder cb, CriteriaQuery<T> query, Root<Suggestion> root, String searchTerm) {
        String pattern = "%" + searchTerm.toUpperCase() + "%";
        Subquery<Integer> existsSub = query.subquery(Integer.class);
        Root<SuggestionLine> slRoot = existsSub.from(SuggestionLine.class);
        Join<SuggestionLine, FournisseurProduit> fpJoin = slRoot.join(SuggestionLine_.fournisseurProduit);
        Join<FournisseurProduit, Produit> produitJoin = fpJoin.join(FournisseurProduit_.produit);
        existsSub.select(slRoot.get(SuggestionLine_.id))
            .where(cb.and(
                cb.equal(slRoot.get(SuggestionLine_.suggestion), root),
                cb.or(
                    cb.like(cb.upper(fpJoin.get(FournisseurProduit_.codeCip)), pattern),
                    cb.like(cb.upper(fpJoin.get(FournisseurProduit_.codeEan)), pattern),
                    cb.like(cb.upper(produitJoin.get(Produit_.libelle)), pattern),
                    cb.like(cb.upper(produitJoin.get(Produit_.codeEanLaboratoire)), pattern)
                )
            ));
        return cb.exists(existsSub);
    }


    @Override
    public List<FournisseurSuggestionSummaryDTO> getSuggestionsParFournisseur(Set<StatutSuggession> statut, Set<Integer> fournisseurIds, String searchTerm) {
        boolean hasStatut = statut != null && !statut.isEmpty();
        boolean hasFournisseurs = fournisseurIds != null && !fournisseurIds.isEmpty();
        boolean hasSearch = StringUtils.hasLength(searchTerm);

        StringBuilder sql = new StringBuilder(PAR_FOURNISSEUR_BASE_SQL);
        List<String> conditions = new ArrayList<>();
        if (hasStatut) {
            conditions.add("s.statut IN (:statut)");
        }
        if (hasFournisseurs) {
            conditions.add("s.fournisseur_id IN (:fournisseurIds)");
        }
        if (hasSearch) {
            conditions.add(PAR_FOURNISSEUR_SEARCH_EXISTS);
        }
        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append("\n");
        }
        sql.append(PAR_FOURNISSEUR_GROUP_ORDER);

        @SuppressWarnings("unchecked")
        var query = entityManager.createNativeQuery(sql.toString());
        if (hasStatut) {
            query.setParameter("statut", statut.stream().map(Enum::name).collect(Collectors.toSet()));
        }
        if (hasFournisseurs) {
            query.setParameter("fournisseurIds", fournisseurIds);
        }
        if (hasSearch) {
            query.setParameter("pattern", "%" + searchTerm.toUpperCase() + "%");
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::toFournisseurSuggestionSummaryDTO).toList();
    }

    private FournisseurSuggestionSummaryDTO toFournisseurSuggestionSummaryDTO(Object[] row) {
        return new FournisseurSuggestionSummaryDTO(
            toInt(row[0]),             // suggestionId
            toInt(row[1]),             // fournisseurId
            (String) row[2],           // libelle
            (String) row[3],           // statut
            toLong(row[4]).intValue(), // nbProduits
            0,                         // nbUrgents (calculé côté frontend)
            toLong(row[5]),            // montantEstime (achat)
            toLong(row[6]),            // montantEstimeVente
            (String) row[7],           // source
            toLocalDateTime(row[8])    // updatedAt
        );
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

    private Long count(Specification<Suggestion> specification, String searchTerm) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Suggestion> root = countQuery.from(Suggestion.class);
        countQuery.select(cb.count(root));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(specification.toPredicate(root, countQuery, cb));
        if (StringUtils.hasLength(searchTerm)) {
            predicates.add(buildSearchPredicate(cb, countQuery, root, searchTerm));
        }
        countQuery.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
