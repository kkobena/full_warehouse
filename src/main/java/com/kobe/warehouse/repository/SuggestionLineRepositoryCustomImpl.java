package com.kobe.warehouse.repository;

import com.kobe.warehouse.service.dto.EtatProduit;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.enumeration.Mois;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
@Transactional(readOnly = true)
public class SuggestionLineRepositoryCustomImpl implements SuggestionLineRepositoryCustom {
    // ─── column indices (must match selectClause order) ─────────────
    private static final int COL_SL_ID = 0;
    private static final int COL_SL_QUANTITY = 1;
    private static final int COL_SL_CREATED_AT = 2;
    private static final int COL_SL_UPDATED_AT = 3;
    private static final int COL_PRODUIT_LIBELLE = 4;
    private static final int COL_FP_CODE_CIP = 5;
    private static final int COL_FP_CODE_EAN = 6;
    private static final int COL_PRODUIT_ID = 7;
    private static final int COL_FP_ID = 8;
    private static final int COL_CURRENT_STOCK = 9;
    private static final int COL_PRIX_ACHAT = 10;
    private static final int COL_PRIX_UNI = 11;
    private static final int COL_SUGGESTION_COUNT = 12;
    private static final int COL_COMMANDE_COUNT = 13;
    private static final int COL_ENTREE = 14;
    private static final int COL_NIVEAU_URGENCE = 15;
    private static final int COL_JOURS_RESTANTS = 16;
    private static final int COL_SOURCE_CALCUL = 17;
    private final EntityManager em;

    public SuggestionLineRepositoryCustomImpl(EntityManager em) {
        this.em = em;
    }



    @Override
    public Page<SuggestionLineDTO> fetchSuggestionLinesWithConsommation(
        Integer suggestionId, String search, String niveauUrgence, Integer storageId, LocalDate dateRetention, int nthMois, Pageable pageable
    ) {
        return doFetch(suggestionId, search, niveauUrgence, storageId, dateRetention, pageable, nthMois);
    }

    // ─── orchestration ────────────────────────────────────────────────

    private Page<SuggestionLineDTO> doFetch(
        Integer suggestionId, String search, String niveauUrgence, Integer storageId,
        LocalDate dateRetention, Pageable pageable,
        int nthMois
    ) {
        boolean hasSearch = StringUtils.hasLength(search);
        boolean hasUrgence = StringUtils.hasLength(niveauUrgence) && !"TOUS".equals(niveauUrgence);
        boolean unpaged = pageable.isUnpaged();
        String baseFrom = baseFromClause(nthMois);
        String searchClause = hasSearch ? searchClause() : "";
        String urgenceClause = hasUrgence ? urgenceFilterClause() : "";

        // COUNT inutile quand on récupère tout
        long total = unpaged
            ? -1
            : count(baseFrom + searchClause + urgenceClause, storageId, suggestionId, hasSearch, search, hasUrgence, niveauUrgence);

        if (!unpaged && total == 0) {
            return Page.empty(pageable);
        }

        String dataSql = selectClause() + baseFrom + searchClause + urgenceClause
            + (unpaged ? orderClause() : orderAndPagination());

        List<Object[]> rows = executeData(
            dataSql, storageId, suggestionId, dateRetention, pageable, hasSearch, search, hasUrgence, niveauUrgence
        );

        if (unpaged && rows.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Integer, Map<Mois, Integer>> consoParProduit = Collections.emptyMap();
        if (!rows.isEmpty()) {
            List<Integer> produitIds = rows.stream()
                .map(row -> ((Number) row[COL_PRODUIT_ID]).intValue())
                .distinct()
                .toList();
            consoParProduit = fetchConsommationMensuelle(produitIds, nthMois);
        }

        Map<Integer, Map<Mois, Integer>> finalConsoParProduit = consoParProduit;
        List<SuggestionLineDTO> content = rows.stream()
            .map(row -> {
                Integer produitId = ((Number) row[COL_PRODUIT_ID]).intValue();
                return toDto(row, finalConsoParProduit.get(produitId));
            })
            .toList();

        long resolvedTotal = unpaged ? content.size() : total;
        return new PageImpl<>(content, pageable, resolvedTotal);
    }

    // ─── consommation mensuelle ───────────────────────────────────────

    /**
     * Charge la consommation mensuelle détaillée pour chaque produit.
     * Retourne par exemple pour nthMois=3 en mars 2026 :
     * { 42 → { DECEMBRE → 8, JANVIER → 6, FEVRIER → 10 } }
     */
    @SuppressWarnings("unchecked")
    private Map<Integer, Map<Mois, Integer>> fetchConsommationMensuelle(List<Integer> produitIds, int nthMois) {
        String sql = """
            SELECT
                mv.produit_id,
                mv.mois,
                COALESCE(mv.qte_vendue, 0) AS qte_vendue
            FROM mv_monthly_top_products mv
            WHERE mv.produit_id IN (:produitIds)
              AND mv.mois::date >= (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '%d months')::date
              AND mv.mois::date < DATE_TRUNC('month', CURRENT_DATE)::date
            ORDER BY mv.produit_id, mv.mois ASC
        """.formatted(nthMois);

        List<Object[]> rows = em.createNativeQuery(sql)
            .setParameter("produitIds", produitIds)
            .getResultList();

        Map<Integer, Map<Mois, Integer>> result = new HashMap<>();
        for (Object[] row : rows) {
            Integer produitId = ((Number) row[0]).intValue();
            LocalDate dateMois = LocalDate.parse(row[1].toString().substring(0, 10));
            Mois mois = Mois.fromMonth(dateMois.getMonth());
            int qteVendue = ((Number) row[2]).intValue();
            result.computeIfAbsent(produitId, _ -> new LinkedHashMap<>()).put(mois, qteVendue);
        }
        return result;
    }

    // ─── SQL fragments ────────────────────────────────────────────────

    private static String baseFromClause(int nthMois) {
        return """
            FROM suggestion_line sl
            JOIN fournisseur_produit fp ON fp.id = sl.fournisseur_produit_id
            JOIN produit p ON p.id = fp.produit_id
            LEFT JOIN stock_produit sp ON sp.produit_id = p.id AND sp.storage_id = :storageId
            LEFT JOIN v_semois_suggestion sm ON sm.produit_id = p.id
            LEFT JOIN LATERAL (
                SELECT ROUND(AVG(mv.qte_vendue))::integer AS vmm_p2
                FROM mv_monthly_top_products mv
                WHERE mv.produit_id = p.id
                  AND mv.mois::date >= (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '%d months')::date
                  AND mv.mois::date <  DATE_TRUNC('month', CURRENT_DATE)::date
            ) vmm_q ON true
            WHERE p.status = 'ENABLE'
              AND sl.suggestion_id = :suggestionId
        """.formatted(nthMois);
    }

    private static String searchClause() {
        return """
            AND (
                UPPER(fp.code_cip) LIKE :search
                OR UPPER(p.libelle) LIKE :search
                OR UPPER(fp.code_ean) LIKE :search
                OR UPPER(p.code_ean_laboratoire) LIKE :search
            )
        """;
    }

    private static String selectClause() {
        return """
            SELECT
                sl.id                                    AS sl_id,
                sl.quantity                              AS sl_quantity,
                sl.created_at                            AS sl_created_at,
                sl.updated_at                            AS sl_updated_at,
                p.libelle                                AS produit_libelle,
                fp.code_cip                              AS fp_code_cip,
                fp.code_ean                              AS fp_code_ean,
                p.id                                     AS produit_id,
                fp.id                                    AS fournisseur_produit_id,
                COALESCE(sp.qty_stock + sp.qty_ug, 0)   AS current_stock,
                fp.prix_achat                            AS prix_achat,
                fp.prix_uni                              AS prix_uni,
                (SELECT COUNT(sl2.id)
                   FROM suggestion_line sl2
                   JOIN fournisseur_produit fp2 ON fp2.id = sl2.fournisseur_produit_id
                  WHERE fp2.produit_id = p.id)           AS suggestion_count,
                (SELECT COUNT(ol.id)
                   FROM order_line ol
                   JOIN fournisseur_produit fp3 ON fp3.id = ol.fournisseur_produit_id
                   JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
                  WHERE fp3.produit_id = p.id
                    AND c.order_status = 'REQUESTED'
                    AND c.order_date > :dateRetention)   AS commande_count,
                EXISTS(
                    SELECT 1
                      FROM order_line ol2
                      JOIN fournisseur_produit fp4 ON fp4.id = ol2.fournisseur_produit_id
                      JOIN commande c2 ON c2.id = ol2.commande_id AND c2.order_date = ol2.commande_order_date
                     WHERE fp4.produit_id = p.id
                       AND c2.order_status = 'RECEIVED'
                       AND c2.order_date > :dateRetention
                )                                        AS entree,
                CASE
                    WHEN sm.produit_id IS NOT NULL
                         AND (COALESCE(sm.stock_objectif, 0) - COALESCE(sp.qty_stock + sp.qty_ug, 0)) > 0
                         AND COALESCE(sp.qty_stock + sp.qty_ug, 0) < COALESCE(sm.marge_securite, 0)
                        THEN 'URGENT'
                    WHEN sm.produit_id IS NOT NULL
                         AND (COALESCE(sm.stock_objectif, 0) - COALESCE(sp.qty_stock + sp.qty_ug, 0)) > 0
                        THEN 'NORMAL'
                    WHEN sm.produit_id IS NULL
                         AND COALESCE(vmm_q.vmm_p2, 0) > 0
                         AND COALESCE(sp.qty_stock + sp.qty_ug, 0) = 0
                        THEN 'URGENT'
                    WHEN sm.produit_id IS NULL
                         AND COALESCE(vmm_q.vmm_p2, 0) > 0
                         AND COALESCE(sp.qty_stock + sp.qty_ug, 0) < vmm_q.vmm_p2
                        THEN 'NORMAL'
                    ELSE 'OK'
                END                                      AS niveau_urgence,
                CASE
                    WHEN COALESCE(sm.vmm, 0) > 0
                        THEN ROUND(COALESCE(sp.qty_stock + sp.qty_ug, 0)::numeric / sm.vmm * 30)::integer
                    WHEN COALESCE(vmm_q.vmm_p2, 0) > 0
                        THEN ROUND(COALESCE(sp.qty_stock + sp.qty_ug, 0)::numeric / vmm_q.vmm_p2 * 30)::integer
                    ELSE NULL
                END                                      AS jours_restants,
                CASE
                    WHEN sm.produit_id IS NOT NULL THEN 'SEMOIS'
                    WHEN COALESCE(vmm_q.vmm_p2, 0) > 0 THEN 'P2'
                    ELSE 'CLASSIQUE'
                END                                      AS source_calcul
        """;
    }

    private static String urgenceFilterClause() {
        return """
            AND CASE
                    WHEN sm.produit_id IS NOT NULL
                         AND (COALESCE(sm.stock_objectif, 0) - COALESCE(sp.qty_stock + sp.qty_ug, 0)) > 0
                         AND COALESCE(sp.qty_stock + sp.qty_ug, 0) < COALESCE(sm.marge_securite, 0)
                        THEN 'URGENT'
                    WHEN sm.produit_id IS NOT NULL
                         AND (COALESCE(sm.stock_objectif, 0) - COALESCE(sp.qty_stock + sp.qty_ug, 0)) > 0
                        THEN 'NORMAL'
                    WHEN sm.produit_id IS NULL
                         AND COALESCE(vmm_q.vmm_p2, 0) > 0
                         AND COALESCE(sp.qty_stock + sp.qty_ug, 0) = 0
                        THEN 'URGENT'
                    WHEN sm.produit_id IS NULL
                         AND COALESCE(vmm_q.vmm_p2, 0) > 0
                         AND COALESCE(sp.qty_stock + sp.qty_ug, 0) < vmm_q.vmm_p2
                        THEN 'NORMAL'
                    ELSE 'OK'
                END = :niveauUrgence
        """;
    }

    private static String orderClause() {
        return """
            ORDER BY p.libelle ASC
        """;
    }

    private static String orderAndPagination() {
        return """
            ORDER BY p.libelle ASC
            LIMIT :limit OFFSET :offset
        """;
    }



    // ─── query execution ──────────────────────────────────────────────

    private long count(String whereClause, Integer storageId, Integer suggestionId, boolean hasSearch, String search, boolean hasUrgence, String niveauUrgence) {
        Query query = em.createNativeQuery("SELECT COUNT(*) " + whereClause)
            .setParameter("storageId", storageId)
            .setParameter("suggestionId", suggestionId);
        if (hasSearch) {
            query.setParameter("search", search.toUpperCase() + "%");
        }
        if (hasUrgence) {
            query.setParameter("niveauUrgence", niveauUrgence);
        }
        return ((Number) query.getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> executeData(
        String sql, Integer storageId, Integer suggestionId,
        LocalDate dateRetention, Pageable pageable, boolean hasSearch, String search, boolean hasUrgence, String niveauUrgence
    ) {
        Query query = em.createNativeQuery(sql)
            .setParameter("storageId", storageId)
            .setParameter("suggestionId", suggestionId)
            .setParameter("dateRetention", dateRetention);
        if (!pageable.isUnpaged()) {
            query.setParameter("limit", pageable.getPageSize())
                 .setParameter("offset", pageable.getOffset());
        }
        if (hasSearch) {
            query.setParameter("search", search.toUpperCase() + "%");
        }
        if (hasUrgence) {
            query.setParameter("niveauUrgence", niveauUrgence);
        }
        return query.getResultList();
    }

    // ─── mapping ──────────────────────────────────────────────────────

    private static SuggestionLineDTO toDto(Object[] row, Map<Mois, Integer> consommation) {
        int currentStock = intValue(row[COL_CURRENT_STOCK]);
        int suggestionCount = intValue(row[COL_SUGGESTION_COUNT]);
        int commandeCount = intValue(row[COL_COMMANDE_COUNT]);
        boolean entree = Boolean.TRUE.equals(row[COL_ENTREE]);

        EtatProduit etatProduit = new EtatProduit(
            currentStock > 0,
            currentStock == 0,
            currentStock < 0,
            suggestionCount > 0,
            commandeCount > 0,
            entree,
            suggestionCount > 1,
            commandeCount > 1
        );

        String niveauUrgence = row[COL_NIVEAU_URGENCE] instanceof String s ? s : "OK";
        Integer joursRestants = row[COL_JOURS_RESTANTS] instanceof Number n ? n.intValue() : null;
        String sourceCalcul = row[COL_SOURCE_CALCUL] instanceof String s ? s : "CLASSIQUE";

        return new SuggestionLineDTO(
            intValue(row[COL_SL_ID]),
            intValue(row[COL_SL_QUANTITY]),
            toLocalDateTime(row[COL_SL_CREATED_AT]),
            toLocalDateTime(row[COL_SL_UPDATED_AT]),
            (String) row[COL_PRODUIT_LIBELLE],
            (String) row[COL_FP_CODE_CIP],
            (String) row[COL_FP_CODE_EAN],
            intValue(row[COL_PRODUIT_ID]),
            intValue(row[COL_FP_ID]),
            currentStock,
            etatProduit,
            intValue(row[COL_PRIX_ACHAT]),
            intValue(row[COL_PRIX_UNI]),
            consommation,
            niveauUrgence,
            joursRestants,
            sourceCalcul
        );
    }

    private static int intValue(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        return null;
    }
}

