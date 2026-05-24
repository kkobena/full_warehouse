package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.MargeDTO;
import com.kobe.warehouse.service.dto.report.MargeSummaryDTO;
import com.kobe.warehouse.service.report.pdf.ProfitabilityPdfReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation du rapport de marges, sans classification BCG.
 * <p>
 * Repose sur la vue matérialisée dédiée {@code mv_marge_produit} qui :
 * <ul>
 *   <li>expose {@code famille_produit_id} → filtrage direct sans sous-requête</li>
 *   <li>type les agrégats monétaires en {@code BIGINT} → pas de cast côté Java</li>
 *   <li>n'inclut pas {@code bcg_category}</li>
 *   <li>stock déjà restreint au magasin principal (id = 1)</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class MargeReportServiceImpl implements MargeReportService {

    private final ProfitabilityPdfReportService profitabilityPdfReportService;
    private static final String SELECT_COLS =
        "SELECT produit_id, libelle, code_cip, categorie, nb_ventes, qte_vendue, " +
            "       ca_total, cout_achat_total, marge_brute, taux_marge_pct, " +
            "       prix_vente_moyen, prix_achat_moyen, stock_quantity, " +
            "       prix_achat_unitaire, prix_vente_unitaire, taux_rotation_annuel " +
            "FROM mv_marge_produit ";

    private static final String COUNT_COLS = "SELECT COUNT(*) FROM mv_marge_produit ";

    private final EntityManager entityManager;

    public MargeReportServiceImpl(ProfitabilityPdfReportService profitabilityPdfReportService, EntityManager entityManager) {
        this.profitabilityPdfReportService = profitabilityPdfReportService;
        this.entityManager = entityManager;
    }

    // -------------------------------------------------------------------------
    // Interface publique
    // -------------------------------------------------------------------------

    @Override
    @Cacheable(value = "margeReport",
        key = "'marges:fam=' + #familleProduitId + ':q=' + #search + ':p=' + #pageable.pageNumber")
    public Page<MargeDTO> getMarges(Integer familleProduitId, String search, Pageable pageable) {
        boolean hasFamille = familleProduitId != null;
        boolean hasSearch = StringUtils.hasText(search);

        String where = buildWhere(hasFamille, hasSearch);

        long total = count(COUNT_COLS + where, familleProduitId, search, hasFamille, hasSearch, null);
        if (total == 0) return Page.empty(pageable);

        Query q = entityManager.createNativeQuery(
            SELECT_COLS + where + buildSort(pageable) + "LIMIT :limit OFFSET :offset");
        bindParams(q, familleProduitId, search, hasFamille, hasSearch);
        q.setParameter("limit", pageable.getPageSize());
        q.setParameter("offset", pageable.getOffset());

        return new PageImpl<>(mapRows(q), pageable, total);
    }

    @Override
    @Cacheable(value = "margeReport",
        key = "'faibleMarge:seuil=' + #seuilPct + ':p=' + #pageable.pageNumber")
    public Page<MargeDTO> getProduitsMargeInsuffisante(int seuilPct, Pageable pageable) {
        String where = "WHERE taux_marge_pct < :seuil ";

        long total = count(COUNT_COLS + where, null, null, false, false, seuilPct);
        if (total == 0) return Page.empty(pageable);

        Query q = entityManager.createNativeQuery(
            SELECT_COLS + where + "ORDER BY taux_marge_pct ASC LIMIT :limit OFFSET :offset");
        q.setParameter("seuil", seuilPct);
        q.setParameter("limit", pageable.getPageSize());
        q.setParameter("offset", pageable.getOffset());

        return new PageImpl<>(mapRows(q), pageable, total);
    }

    @Override
    @Cacheable(value = "margeReport",
        key = "'top:limit=' + #limit + ':p=' + #pageable.pageNumber")
    public Page<MargeDTO> getTopProduitsParMarge(int limit, Pageable pageable) {
        // Fenêtre des top N, paginée à l'intérieur
        String inner = "SELECT * FROM mv_marge_produit ORDER BY marge_brute DESC LIMIT :limit";
        String countSql = "SELECT COUNT(*) FROM (" + inner + ") sub";
        String dataSql =
            "SELECT produit_id, libelle, code_cip, categorie, nb_ventes, qte_vendue, " +
                "       ca_total, cout_achat_total, marge_brute, taux_marge_pct, " +
                "       prix_vente_moyen, prix_achat_moyen, stock_quantity, " +
                "       prix_achat_unitaire, prix_vente_unitaire, taux_rotation_annuel " +
                "FROM (" + inner + ") sub LIMIT :pageSize OFFSET :offset";

        Query cq = entityManager.createNativeQuery(countSql);
        cq.setParameter("limit", limit);
        long total = ((Number) cq.getSingleResult()).longValue();

        Query dq = entityManager.createNativeQuery(dataSql);
        dq.setParameter("limit", limit);
        dq.setParameter("pageSize", pageable.getPageSize());
        dq.setParameter("offset", pageable.getOffset());

        return new PageImpl<>(mapRows(dq), pageable, total);
    }

    @Override
    @Cacheable(value = "margeReport",
        key = "'summary:fam=' + #familleProduitId + ':bas=' + #seuilBasPct + ':haut=' + #seuilHautPct")
    public MargeSummaryDTO getMargeSummary(Integer familleProduitId, int seuilBasPct, int seuilHautPct) {
        boolean hasFamille = familleProduitId != null;
        // famille_produit_id est directement disponible dans la vue → pas de sous-requête
        String familleClause = hasFamille ? "AND famille_produit_id = :familleId " : "";

        String sql =
            "SELECT " +
                "    COUNT(*)                                                                  AS total_produits, " +
                "    COALESCE(SUM(ca_total), 0)                                               AS ca_total_global, " +
                "    COALESCE(SUM(cout_achat_total), 0)                                       AS cout_achat_global, " +
                "    COALESCE(SUM(marge_brute), 0)                                            AS marge_brute_globale, " +
                "    CASE WHEN SUM(ca_total) > 0 " +
                "         THEN ROUND(SUM(marge_brute)::numeric / SUM(ca_total)::numeric * 100, 2) " +
                "         ELSE 0 END                                                          AS taux_marge_moyen, " +
                "    COUNT(*)        FILTER (WHERE taux_marge_pct <  :seuilBas)              AS nb_marge_insuffisante, " +
                "    COALESCE(SUM(ca_total) FILTER (WHERE taux_marge_pct <  :seuilBas),  0)  AS ca_marge_insuffisante, " +
                "    COUNT(*)        FILTER (WHERE taux_marge_pct >= :seuilHaut)             AS nb_bonne_marge, " +
                "    COALESCE(SUM(ca_total) FILTER (WHERE taux_marge_pct >= :seuilHaut), 0)  AS ca_bonne_marge " +
                "FROM mv_marge_produit " +
                "WHERE 1=1 " + familleClause;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("seuilBas", seuilBasPct);
        q.setParameter("seuilHaut", seuilHautPct);
        if (hasFamille) q.setParameter("familleId", familleProduitId);

        Object[] r = (Object[]) q.getSingleResult();
        return new MargeSummaryDTO(
            ((Number) r[0]).intValue(),
            ((Number) r[1]).longValue(),
            ((Number) r[2]).longValue(),
            ((Number) r[3]).longValue(),
            r[4] != null ? new BigDecimal(r[4].toString()) : BigDecimal.ZERO,
            ((Number) r[5]).intValue(),
            ((Number) r[6]).longValue(),
            ((Number) r[7]).intValue(),
            ((Number) r[8]).longValue()
        );
    }

    @Override
    public byte[] export(Integer familleProduitId, String search) {
        //  getMarges(Integer familleProduitId, String search, Pageable pageable)
        //TODO: implémenter l'export avec les vrais paramètres, et pas une export générique sans filtre
        return profitabilityPdfReportService.export(null, List.of());
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    private String buildWhere(boolean hasFamille, boolean hasSearch) {
        if (!hasFamille && !hasSearch) return "";
        List<String> conditions = new ArrayList<>();
        if (hasFamille) conditions.add("famille_produit_id = :familleId");
        if (hasSearch) conditions.add("(LOWER(libelle) LIKE LOWER(:search) OR LOWER(code_cip) LIKE LOWER(:search))");
        return "WHERE " + String.join(" AND ", conditions) + " ";
    }

    private String buildSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            StringBuilder sb = new StringBuilder("ORDER BY ");
            pageable.getSort().forEach(o ->
                sb.append(sanitizeColumn(o.getProperty()))
                    .append(" ").append(o.getDirection().name()).append(", "));
            return sb.substring(0, sb.length() - 2) + " ";
        }
        return "ORDER BY marge_brute DESC ";
    }

    /**
     * Whitelist des colonnes triables — protège contre l'injection SQL.
     */
    private String sanitizeColumn(String col) {
        return switch (col) {
            case "libelle" -> "libelle";
            case "tauxMargePct", "taux_marge_pct" -> "taux_marge_pct";
            case "caTotal", "ca_total" -> "ca_total";
            case "qteVendue", "qte_vendue" -> "qte_vendue";
            case "nbVentes", "nb_ventes" -> "nb_ventes";
            case "stockQuantity", "stock_quantity" -> "stock_quantity";
            case "tauxRotationAnnuel", "taux_rotation_annuel" -> "taux_rotation_annuel";
            default -> "marge_brute";
        };
    }

    private long count(String sql, Integer familleId, String search,
                       boolean hasFamille, boolean hasSearch, Integer seuil) {
        Query q = entityManager.createNativeQuery(sql);
        bindParams(q, familleId, search, hasFamille, hasSearch);
        if (seuil != null) q.setParameter("seuil", seuil);
        return ((Number) q.getSingleResult()).longValue();
    }

    private void bindParams(Query q, Integer familleId, String search,
                            boolean hasFamille, boolean hasSearch) {
        if (hasFamille) q.setParameter("familleId", familleId);
        if (hasSearch) q.setParameter("search", "%" + search + "%");
    }

    @SuppressWarnings("unchecked")
    private List<MargeDTO> mapRows(Query q) {
        return ((List<Object[]>) q.getResultList()).stream().map(this::mapRow).toList();
    }

    private MargeDTO mapRow(Object[] r) {
        return new MargeDTO(
            r[0] != null ? ((Number) r[0]).intValue() : null,
            (String) r[1],
            (String) r[2],
            (String) r[3],
            r[4] != null ? ((Number) r[4]).intValue() : 0,
            r[5] != null ? ((Number) r[5]).intValue() : 0,
            r[6] != null ? ((Number) r[6]).longValue() : 0L,
            r[7] != null ? ((Number) r[7]).longValue() : 0L,
            r[8] != null ? ((Number) r[8]).longValue() : 0L,
            r[9] != null ? new BigDecimal(r[9].toString()) : BigDecimal.ZERO,
            r[10] != null ? ((Number) r[10]).intValue() : 0,
            r[11] != null ? ((Number) r[11]).intValue() : 0,
            r[12] != null ? ((Number) r[12]).intValue() : 0,
            r[13] != null ? ((Number) r[13]).intValue() : 0,
            r[14] != null ? ((Number) r[14]).intValue() : 0,
            r[15] != null ? new BigDecimal(r[15].toString()) : BigDecimal.ZERO
        );
    }
}

