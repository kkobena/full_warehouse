package com.kobe.warehouse.service.dto.builder;

import com.kobe.warehouse.service.dto.StoreInventoryLineExport;
import com.kobe.warehouse.service.dto.enumeration.StoreInventoryLineEnum;
import com.kobe.warehouse.service.dto.filter.StoreInventoryExportRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryByGroupRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.util.StringUtils;

/**
 * Fabrique de requêtes SQL pour les lignes et lots d'inventaire.
 *
 * <p><b>Utilisation recommandée (nouveaux services) :</b>
 * <pre>{@code
 * // Page de lignes — joins optionnels selon le contexte
 * String sql = StoreInventoryLineFilterBuilder
 *     .lineQuery(filter)
 *     .withLotCount(gestionLot)
 *     .withAbcPareto(category == InventoryCategory.ABC)
 *     .buildPage();
 *
 * // Count — aucun join optionnel nécessaire
 * String count = StoreInventoryLineFilterBuilder.lineQuery(filter).buildCount();
 *
 * // Page de lots
 * String lotSql = StoreInventoryLineFilterBuilder
 *     .lotQuery(filter)
 *     .withAbcPareto(true)
 *     .buildPage();
 * }</pre>
 *
 * <p>Les constantes SQL marquées {@code @Deprecated} restent pour compatibilité avec
 * {@code InventaireService} (interface legacy) et {@code InventaireServiceImpl}. Elles seront
 * supprimées lors de la refonte de la couche legacy.
 */
public class StoreInventoryLineFilterBuilder {

    // ── INSERT SQL (utilisés par InventaireCreationServiceImpl) ──────────────

    /**
     * ABC : filtre par classe Pareto ('A', 'B', 'C') — null = toutes les classes.
     */
    public static final String SQL_INSERT_ABC =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id, storage_id)
            SELECT DISTINCT p.id, NOW(), false, :inventoryId, :storageId
            FROM produit p
            JOIN mv_abc_pareto_analysis abc ON abc.produit_id = p.id
            WHERE p.status = 'ENABLE'
              AND (:classePareto IS NULL OR abc.classe_pareto = :classePareto)
            """;

    // ── SUMMARY / EXPORT SQL (utilisés par InventaireServiceImpl legacy) ─────

    public static final String SUMMARY_SQL =
        """
            SELECT SUM(i.quantity_on_hand * i.inventory_value_cost) AS costValueAfter,
                   SUM(i.quantity_on_hand * i.last_unit_price)      AS amountValueAfter,
                   SUM(i.quantity_init   * i.inventory_value_cost)  AS costValueBegin,
                   SUM(i.quantity_init   * i.last_unit_price)       AS amountValueBegin,
                   SUM(i.gap             * i.inventory_value_cost)  AS gapCost,
                   SUM(i.gap             * i.last_unit_price)       AS gapAmount
            FROM store_inventory_line i
            WHERE i.store_inventory_id = ?1
            """;

    public static final String EXPORT_QUERY =
        """
            SELECT r.id AS rayon_id, s.id AS storage_id,
                   fm.code AS famillyCode, fm.libelle AS famillyLibelle, fm.id AS famillyId,
                   a.gap, r.code AS code_rayon,
                   a.inventory_value_cost, a.quantity_init, a.quantity_on_hand, a.last_unit_price,
                   p.libelle AS produit_libelle, p.code_ean_labo,
                   r.libelle AS rayon_libelle, s.name AS storage_name,
                   fp.code_cip AS produit_code_cip, fp.prix_uni, fp.prix_achat
            FROM store_inventory_line a
            JOIN produit p             ON p.id  = a.produit_id
            JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
            JOIN famille_produit fm    ON fm.id = p.famille_id
            LEFT JOIN rayon_produit rp ON p.id        = rp.produit_id
            LEFT JOIN rayon r          ON rp.rayon_id = r.id
            LEFT JOIN storage s        ON r.storage_id = s.id
            WHERE a.store_inventory_id = ?1 %s
            ORDER BY {order_by} fp.code_cip
            """;

    public static final String EXPORT_RAYON_CLOSE_QUERY = " AND r.id = %d ";
    public static final String EXPORT_STORAGE_CLOSE_QUERY = " AND s.id = %d ";

    // ── Valorisation ventilée (Phase 5.4) ────────────────────────────────────

    /**
     * Valorisation ventilée par emplacement (storage).
     */
    public static final String VALUATION_BY_STORAGE_SQL =
        """
            SELECT COALESCE(s.id::text, 'SANS_EMPLACEMENT')             AS groupKey,
                   COALESCE(s.name, 'Sans emplacement')                 AS groupLabel,
                   COUNT(*)                                              AS lineCount,
                   COALESCE(SUM(i.quantity_init   * i.inventory_value_cost), 0) AS costBefore,
                   COALESCE(SUM(i.quantity_on_hand * i.inventory_value_cost), 0) AS costAfter,
                   COALESCE(SUM(i.quantity_init   * i.last_unit_price),  0) AS amountBefore,
                   COALESCE(SUM(i.quantity_on_hand * i.last_unit_price), 0) AS amountAfter,
                   COALESCE(SUM(i.gap             * i.inventory_value_cost), 0) AS gapCost,
                   COALESCE(SUM(i.gap             * i.last_unit_price),  0) AS gapAmount
            FROM store_inventory_line i
            LEFT JOIN storage s ON s.id = i.storage_id
            WHERE i.store_inventory_id = ?1
            GROUP BY s.id, s.name
            ORDER BY groupLabel
            """;

    /**
     * Valorisation ventilée par famille de produit.
     */
    public static final String VALUATION_BY_FAMILLE_SQL =
        """
            SELECT fm.id::text                                           AS groupKey,
                   COALESCE(fm.libelle, 'Sans famille')                  AS groupLabel,
                   COUNT(*)                                              AS lineCount,
                   COALESCE(SUM(i.quantity_init   * i.inventory_value_cost), 0) AS costBefore,
                   COALESCE(SUM(i.quantity_on_hand * i.inventory_value_cost), 0) AS costAfter,
                   COALESCE(SUM(i.quantity_init   * i.last_unit_price),  0) AS amountBefore,
                   COALESCE(SUM(i.quantity_on_hand * i.last_unit_price), 0) AS amountAfter,
                   COALESCE(SUM(i.gap             * i.inventory_value_cost), 0) AS gapCost,
                   COALESCE(SUM(i.gap             * i.last_unit_price),  0) AS gapAmount
            FROM store_inventory_line i
            JOIN produit p       ON p.id    = i.produit_id
            JOIN famille_produit fm ON fm.id = p.famille_id
            WHERE i.store_inventory_id = ?1
            GROUP BY fm.id, fm.libelle
            ORDER BY groupLabel
            """;

    /**
     * Valorisation ventilée par rayon (via rayon_produit).
     */
    public static final String VALUATION_BY_RAYON_SQL =
        """
            SELECT COALESCE(r.id::text, 'SANS_RAYON')                   AS groupKey,
                   COALESCE(r.libelle, 'Sans rayon')                     AS groupLabel,
                   COUNT(*)                                              AS lineCount,
                   COALESCE(SUM(i.quantity_init   * i.inventory_value_cost), 0) AS costBefore,
                   COALESCE(SUM(i.quantity_on_hand * i.inventory_value_cost), 0) AS costAfter,
                   COALESCE(SUM(i.quantity_init   * i.last_unit_price),  0) AS amountBefore,
                   COALESCE(SUM(i.quantity_on_hand * i.last_unit_price), 0) AS amountAfter,
                   COALESCE(SUM(i.gap             * i.inventory_value_cost), 0) AS gapCost,
                   COALESCE(SUM(i.gap             * i.last_unit_price),  0) AS gapAmount
            FROM store_inventory_line i
            JOIN produit p              ON p.id        = i.produit_id
            LEFT JOIN rayon_produit rp  ON rp.produit_id = p.id
            LEFT JOIN rayon r           ON r.id          = rp.rayon_id
            WHERE i.store_inventory_id = ?1
            GROUP BY r.id, r.libelle
            ORDER BY groupLabel
            """;

    /**
     * Export PDF en mode gestion de lot : une ligne par lot, groupé par produit.
     */
    public static final String LOT_EXPORT_SQL =
        """
            SELECT fp.code_cip,
                   p.libelle                        AS produit_libelle,
                   l.num_lot,
                   l.expiry_date,
                   il.quantity_init,
                   il.quantity_on_hand,
                   il.gap,
                   COALESCE(sil.last_unit_price, fp.prix_uni)   AS last_unit_price,
                   COALESCE(sil.inventory_value_cost, fp.prix_achat) AS prix_achat
            FROM inventory_lot il
            JOIN store_inventory_line sil ON sil.id     = il.store_inventory_line_id
            JOIN produit p               ON p.id        = sil.produit_id
            JOIN fournisseur_produit fp  ON fp.id       = p.fournisseur_produit_principal_id
            JOIN lot l                   ON l.id        = il.lot_id
            WHERE sil.store_inventory_id = ?1
            ORDER BY fp.code_cip, p.libelle, l.num_lot
            """;

    // ── Constantes legacy (InventaireService interface + InventaireServiceImpl) ─
    // @Deprecated : à supprimer lors de la refonte des services legacy.

    /**
     * @deprecated Utiliser {@link #lineQuery(StoreInventoryLineFilterRecord)}
     */
    @Deprecated
    public static final String BASE_QUERY =
        """
            SELECT p.id AS produitId, p.code_ean_labo,
                   p.libelle, fp.code_cip,
                   a.quantity_on_hand, a.gap, a.updated_at, a.id AS id,
                   fp.prix_achat, fp.prix_uni, a.updated,
                   a.storage_id, sp.seuil_mini,
                   COALESCE(ilc.lot_count, 0) AS lot_count,
                   abc.classe_pareto
            FROM produit p
            JOIN (SELECT fp.id, fp.code_cip, fp.produit_id, fp.prix_achat, fp.prix_uni
                  FROM fournisseur_produit fp) AS fp
              ON p.fournisseur_produit_principal_id = fp.id
            JOIN store_inventory_line a ON p.id = a.produit_id
            LEFT JOIN stock_produit sp ON sp.produit_id = p.id AND sp.storage_id = a.storage_id
            LEFT JOIN (SELECT il.store_inventory_line_id, COUNT(*) AS lot_count
                       FROM inventory_lot il
                       GROUP BY il.store_inventory_line_id) ilc
              ON ilc.store_inventory_line_id = a.id
            LEFT JOIN mv_abc_pareto_analysis abc ON abc.produit_id = p.id
            {join_statement} WHERE a.store_inventory_id = ?1 {join_statement_where} %s
            ORDER BY fp.code_cip, p.libelle
            """;

    /**
     * @deprecated Utiliser {@link #lineQuery(StoreInventoryLineFilterRecord)}
     */
    @Deprecated
    public static final String COUNT =
        """
            SELECT COUNT(p.id)
            FROM produit p
            JOIN (SELECT fp.id, fp.code_cip, fp.produit_id, fp.prix_achat, fp.prix_uni
                  FROM fournisseur_produit fp) AS fp
              ON p.fournisseur_produit_principal_id = fp.id
            JOIN store_inventory_line a ON p.id = a.produit_id
            {join_statement} WHERE a.store_inventory_id = ?1 {join_statement_where} %s
            """;

    /**
     * @deprecated Utiliser {@link LineQueryBuilder}
     */
    @Deprecated
    public static final String RAYON_STATEMENT = " JOIN rayon_produit rp ON p.id = rp.produit_id ";
    /**
     * @deprecated Utiliser {@link LineQueryBuilder}
     */
    @Deprecated
    public static final String RAYON_STATEMENT_WHERE = " AND rp.rayon_id = %d ";
    /**
     * @deprecated Utiliser {@link LineQueryBuilder}
     */
    @Deprecated
    public static final String STOCKAGE_STATEMENT_WHERE = " AND rp.rayon_id IN (SELECT ry.id FROM rayon ry WHERE ry.storage_id = %d) ";
    /**
     * @deprecated Utiliser {@link LineQueryBuilder}
     */
    @Deprecated
    public static final String LIKE_STATEMENT_WHERE = " AND (p.libelle LIKE '%s' OR fp.code_cip LIKE '%s' OR p.code_ean_labo LIKE '%s') ";

    /**
     * @deprecated Utiliser {@link #lotQuery(StoreInventoryLineFilterRecord)}
     */
    @Deprecated
    public static final String LOT_FLAT_BASE_QUERY =
        """
            SELECT il.id, il.store_inventory_line_id,
                   p.id AS produit_id, fp.code_cip, p.libelle,
                   l.num_lot, l.expiry_date,
                   il.quantity_on_hand, il.quantity_init, il.gap, il.updated,
                   abc.classe_pareto
            FROM inventory_lot il
            JOIN store_inventory_line sil ON il.store_inventory_line_id = sil.id
            JOIN produit p               ON sil.produit_id = p.id
            JOIN fournisseur_produit fp  ON p.fournisseur_produit_principal_id = fp.id
            JOIN lot l                   ON il.lot_id = l.id
            LEFT JOIN mv_abc_pareto_analysis abc ON abc.produit_id = p.id
            {join_statement} WHERE sil.store_inventory_id = ?1 {join_statement_where} %s
            ORDER BY fp.code_cip, p.libelle, l.num_lot
            """;

    /**
     * @deprecated Utiliser {@link #lotQuery(StoreInventoryLineFilterRecord)}
     */
    @Deprecated
    public static final String LOT_FLAT_COUNT =
        """
            SELECT COUNT(il.id)
            FROM inventory_lot il
            JOIN store_inventory_line sil ON il.store_inventory_line_id = sil.id
            JOIN produit p               ON sil.produit_id = p.id
            JOIN fournisseur_produit fp  ON p.fournisseur_produit_principal_id = fp.id
            JOIN lot l                   ON il.lot_id = l.id
            {join_statement} WHERE sil.store_inventory_id = ?1 {join_statement_where} %s
            """;

    /**
     * @deprecated Utiliser {@link LotQueryBuilder}
     */
    @Deprecated
    public static final String LOT_LIKE_STATEMENT_WHERE =
        " AND (p.libelle LIKE '%s' OR fp.code_cip LIKE '%s' OR l.num_lot LIKE '%s') ";

    /**
     * @deprecated Utiliser {@link LotQueryBuilder}
     */
    @Deprecated
    public static final String SQL_ALL_INSERT_ALL =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id)
            SELECT p.id, NOW(), false, %d
            FROM produit p
            WHERE p.status = 'ENABLE' {famille_close}
            """;

    /**
     * @deprecated Utiliser {@link LotQueryBuilder}
     */
    @Deprecated
    public static final String SQL_ALL_INSERT =
        """
            INSERT INTO store_inventory_line (produit_id, updated_at, updated, store_inventory_id)
            SELECT p.id, NOW(), false, %d
            FROM produit p
            JOIN rayon_produit rp ON p.id  = rp.produit_id
            JOIN rayon r          ON r.id  = rp.rayon_id
            JOIN storage s        ON s.id  = r.storage_id
            WHERE p.status = 'ENABLE'
            """;

    // ── Export / filter static helpers ───────────────────────────────────────

    /**
     * Construit la requête SQL d'export de l'inventaire avec filtres optionnels (rayon, storage,
     * filtre ligne) et tri selon le groupBy.
     */
    public static String buildExportQuery(StoreInventoryExportRecord record) {
        StoreInventoryLineFilterRecord f = record.filterRecord();
        String whereClose = "";
        if (Objects.nonNull(f.rayonId())) {
            whereClose = whereClose.concat(String.format(EXPORT_RAYON_CLOSE_QUERY, f.rayonId()));
        }
        if (Objects.nonNull(f.storageId())) {
            whereClose = whereClose.concat(
                String.format(EXPORT_STORAGE_CLOSE_QUERY, f.storageId()));
        }
        String query = String.format(EXPORT_QUERY,
            whereClose.concat(buildLineFilter(f.selectedFilter())));
        return switch (record.exportGroupBy()) {
            case RAYON -> query.replace("{order_by}", "storage_name, rayon_libelle,");
            case FAMILLY -> query.replace("{order_by}", "storage_name, fm.code,fm.libelle,");
            case STORAGE -> query.replace("{order_by}", "storage_name,");
            case NONE -> query.replace("{order_by}", "");
        };
    }

    /**
     * Retourne la clause SQL correspondant au filtre de ligne d'inventaire. Retourne une chaîne
     * vide si {@code null} ou {@code NONE}.
     */
    public static String buildLineFilter(StoreInventoryLineEnum lineEnum) {
        if (lineEnum == null || lineEnum == StoreInventoryLineEnum.NONE) {
            return "";
        }
        return switch (lineEnum) {
            case NOT_UPDATED -> " AND a.updated IS false ";
            case UPDATED -> " AND a.updated ";
            case GAP -> " AND a.updated  AND  a.quantity_on_hand <> a.quantity_init ";
            case GAP_NEGATIF -> " AND a.updated  AND  a.quantity_on_hand < a.quantity_init ";
            case GAP_POSITIF -> " AND a.updated  AND  a.quantity_on_hand >= a.quantity_init ";
            default -> "";
        };
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    /**
     * Builder pour les requêtes sur {@code store_inventory_line}.
     */
    public static LineQueryBuilder lineQuery(StoreInventoryLineFilterRecord filter) {
        return new LineQueryBuilder(filter);
    }

    /**
     * Builder pour les requêtes à plat sur {@code inventory_lot}.
     */
    public static LotQueryBuilder lotQuery(StoreInventoryLineFilterRecord filter) {
        return new LotQueryBuilder(filter);
    }

    // ── Mappers statiques ─────────────────────────────────────────────────────

    public static StoreInventoryLineExport buildStoreInventoryLineExportRecord(Tuple t) {
        return new StoreInventoryLineExport(
            t.get("gap", Integer.class),
            t.get("inventory_value_cost", Integer.class),
            t.get("quantity_init", Integer.class),
            t.get("quantity_on_hand", Integer.class),
            t.get("produit_code_cip", String.class),
            t.get("code_ean_labo", String.class),
            t.get("produit_libelle", String.class),
            t.get("rayon_libelle", String.class),
            t.get("storage_name", String.class),
            t.get("prix_uni", Integer.class),
            t.get("prix_achat", Integer.class),
            t.get("last_unit_price", Integer.class),
            t.get("rayon_id", Integer.class),
            t.get("storage_id", Integer.class),
            t.get("code_rayon", String.class),
            t.get("famillyCode", String.class),
            t.get("famillyLibelle", String.class),
            t.get("famillyId", Integer.class)
        );
    }

    public static StoreInventorySummaryRecord buildSammary(Tuple t) {
        return new StoreInventorySummaryRecord(
            t.get("costValueBegin", BigDecimal.class),
            t.get("costValueAfter", BigDecimal.class),
            t.get("amountValueBegin", BigDecimal.class),
            t.get("amountValueAfter", BigDecimal.class),
            t.get("gapCost", BigDecimal.class),
            t.get("gapAmount", BigDecimal.class)
        );
    }

    public static StoreInventorySummaryByGroupRecord buildGroupRow(Tuple t) {
        return new StoreInventorySummaryByGroupRecord(
            t.get("groupKey", String.class),
            t.get("groupLabel", String.class),
            ((Number) t.get("lineCount")).longValue(),
            ((BigDecimal) t.get("costBefore")),
            ((BigDecimal) t.get("costAfter")),
            ((BigDecimal) t.get("amountBefore")),
            ((BigDecimal) t.get("amountAfter")),
            ((BigDecimal) t.get("gapCost")),
            ((BigDecimal) t.get("gapAmount"))
        );
    }

    // ── LineQueryBuilder ──────────────────────────────────────────────────────

    private static void appendSearch(
        StringBuilder sql, String search, String col1, String col2, String col3
    ) {
        if (!StringUtils.hasLength(search)) {
            return;
        }
        String term = search + "%";
        sql.append(String.format(
            " AND (%s LIKE '%s' OR %s LIKE '%s' OR %s LIKE '%s')",
            col1, term, col2, term, col3, term));
    }

    // ── LotQueryBuilder ───────────────────────────────────────────────────────

    private static void appendLineFilter(StringBuilder sql, StoreInventoryLineEnum lineEnum) {
        if (lineEnum == null || lineEnum == StoreInventoryLineEnum.NONE) {
            return;
        }
        switch (lineEnum) {
            case NOT_UPDATED -> sql.append(" AND a.updated IS false");
            case UPDATED -> sql.append(" AND a.updated");
            case GAP -> sql.append(" AND a.updated AND a.quantity_on_hand <> a.quantity_init");
            case GAP_NEGATIF ->
                sql.append(" AND a.updated AND a.quantity_on_hand < a.quantity_init");
            case GAP_POSITIF ->
                sql.append(" AND a.updated AND a.quantity_on_hand >= a.quantity_init");
            default -> { /* NONE déjà géré */ }
        }
    }

    // ── Helpers partagés (privés) ─────────────────────────────────────────────

    private static void appendLotFilter(StringBuilder sql, StoreInventoryLineEnum lineEnum) {
        if (lineEnum == null || lineEnum == StoreInventoryLineEnum.NONE) {
            return;
        }
        switch (lineEnum) {
            case NOT_UPDATED -> sql.append(" AND il.updated IS false");
            case UPDATED -> sql.append(" AND il.updated");
            case GAP -> sql.append(" AND il.updated AND il.quantity_on_hand <> il.quantity_init");
            case GAP_NEGATIF ->
                sql.append(" AND il.updated AND il.quantity_on_hand < il.quantity_init");
            case GAP_POSITIF ->
                sql.append(" AND il.updated AND il.quantity_on_hand >= il.quantity_init");
            default -> { /* NONE déjà géré */ }
        }
    }

    /**
     * Construit dynamiquement les requêtes SQL de pagination et de comptage sur
     * {@code store_inventory_line}.
     *
     * <p>Les joins optionnels ne sont ajoutés que si explicitement activés :
     * <ul>
     *   <li>{@link #withLotCount(boolean)} — sous-requête d'agrégation sur {@code inventory_lot}</li>
     *   <li>{@link #withAbcPareto(boolean)} — LEFT JOIN sur {@code mv_abc_pareto_analysis}</li>
     * </ul>
     *
     * <p>Les colonnes correspondantes sont toujours présentes dans le SELECT
     * (valeur neutre {@code 0} / {@code NULL} quand le join est désactivé),
     * ce qui garantit la compatibilité avec le mapping {@code toRecord()} sans modification.
     */
    public static final class LineQueryBuilder {

        private final StoreInventoryLineFilterRecord filter;
        private boolean includeLotCount = false;
        private boolean includeAbcPareto = false;

        private LineQueryBuilder(StoreInventoryLineFilterRecord filter) {
            this.filter = filter;
        }

        /**
         * Active la sous-requête {@code COUNT(*)} sur {@code inventory_lot}. À utiliser uniquement
         * quand {@code GESTION_LOT_INVENTAIRE = true}.
         */
        public LineQueryBuilder withLotCount(boolean include) {
            this.includeLotCount = include;
            return this;
        }

        /**
         * Active le LEFT JOIN sur {@code mv_abc_pareto_analysis}. À utiliser uniquement pour les
         * inventaires de type {@code ABC}, ou quand l'affichage du badge Pareto est souhaité.
         */
        public LineQueryBuilder withAbcPareto(boolean include) {
            this.includeAbcPareto = include;
            return this;
        }

        /**
         * Requête de pagination — inclut ORDER BY.
         */
        public String buildPage() {
            StringBuilder sql = new StringBuilder();
            appendSelect(sql);
            appendFrom(sql);
            appendWhere(sql);
            sql.append(" ORDER BY fp.code_cip, p.libelle,p.id");
            return sql.toString();
        }

        /**
         * Requête de comptage — aucun join optionnel (lot_count / abc) n'est nécessaire pour
         * compter ; les joins de scope (rayon/storage) sont conservés.
         */
        public String buildCount() {
            StringBuilder sql = new StringBuilder("SELECT COUNT(p.id)");
            appendFromForCount(sql);
            appendWhere(sql);
            return sql.toString();
        }

        // ── SELECT ────────────────────────────────────────────────────────────

        private void appendSelect(StringBuilder sql) {
            sql.append("""
                SELECT p.id                AS produitId,
                       p.code_ean_labo,
                       p.libelle,
                       fp.code_cip,
                       a.quantity_on_hand,
                       a.gap,
                       a.updated_at,
                       a.id               AS id,
                       fp.prix_achat,
                       fp.prix_uni,
                       a.updated,
                       a.storage_id,
                       sp.seuil_mini""");

            sql.append(includeLotCount
                ? ", COALESCE(ilc.lot_count, 0) AS lot_count"
                : ", 0 AS lot_count");

            sql.append(includeAbcPareto
                ? ",abc.classe_pareto"
                : ",CAST(NULL AS VARCHAR)      AS classe_pareto");
        }

        // ── FROM / JOIN ───────────────────────────────────────────────────────

        private void appendFrom(StringBuilder sql) {
            appendCoreJoins(sql);

            if (includeLotCount) {
                sql.append("""
                       LEFT JOIN (
                        SELECT il.store_inventory_line_id, COUNT(*) AS lot_count
                        FROM   inventory_lot il
                        GROUP BY il.store_inventory_line_id
                    ) ilc ON ilc.store_inventory_line_id = a.id""");
            }
            if (includeAbcPareto) {
                sql.append(" LEFT JOIN mv_abc_pareto_analysis abc ON abc.produit_id = p.id");
            }
            appendScopeJoin(sql);
        }

        /**
         * FROM allégé pour le COUNT : pas de seuil_mini ni de joins optionnels.
         */
        private void appendFromForCount(StringBuilder sql) {
            sql.append("""
                \nFROM produit p
                JOIN (SELECT fp.id, fp.code_cip, fp.produit_id
                      FROM fournisseur_produit fp) AS fp
                  ON p.fournisseur_produit_principal_id = fp.id
                JOIN store_inventory_line a ON p.id = a.produit_id""");
            appendScopeJoin(sql);
        }

        private void appendCoreJoins(StringBuilder sql) {
            sql.append("""
                \nFROM produit p
                JOIN (SELECT fp.id, fp.code_cip, fp.produit_id, fp.prix_achat, fp.prix_uni
                      FROM fournisseur_produit fp) AS fp
                  ON p.fournisseur_produit_principal_id = fp.id
                JOIN store_inventory_line a ON p.id = a.produit_id
                LEFT JOIN stock_produit sp  ON sp.produit_id = p.id AND sp.storage_id = a.storage_id""");
        }

        private void appendScopeJoin(StringBuilder sql) {
            if (filter.rayonId() != null || filter.storageId() != null) {
                sql.append("\nJOIN rayon_produit rp ON p.id = rp.produit_id");
            }
        }

        // ── WHERE ─────────────────────────────────────────────────────────────

        private void appendWhere(StringBuilder sql) {
            sql.append(" WHERE a.store_inventory_id = ?1");
            if (filter.rayonId() != null) {
                sql.append(String.format(" AND rp.rayon_id = %d", filter.rayonId()));
            } else if (filter.storageId() != null) {
                sql.append(String.format(
                    " AND rp.rayon_id IN (SELECT ry.id FROM rayon ry WHERE ry.storage_id = %d)",
                    filter.storageId()));
            }
            appendSearch(sql, filter.search(), "p.libelle", "fp.code_cip", "p.code_ean_labo");
            appendLineFilter(sql, filter.selectedFilter());
        }
    }

    /**
     * Construit dynamiquement les requêtes SQL de pagination et de comptage sur la vue plate
     * {@code inventory_lot} (un lot par ligne).
     *
     * <p>Join optionnel :
     * <ul>
     *   <li>{@link #withAbcPareto(boolean)} — LEFT JOIN sur {@code mv_abc_pareto_analysis}</li>
     * </ul>
     */
    public static final class LotQueryBuilder {

        private final StoreInventoryLineFilterRecord filter;
        private boolean includeAbcPareto = false;

        private LotQueryBuilder(StoreInventoryLineFilterRecord filter) {
            this.filter = filter;
        }

        /**
         * Active le LEFT JOIN sur {@code mv_abc_pareto_analysis}.
         */
        public LotQueryBuilder withAbcPareto(boolean include) {
            this.includeAbcPareto = include;
            return this;
        }

        /**
         * Requête de pagination — inclut ORDER BY.
         */
        public String buildPage() {
            StringBuilder sql = new StringBuilder();
            appendSelect(sql);
            appendFrom(sql);
            appendWhere(sql);
            sql.append(" ORDER BY fp.code_cip, p.libelle, l.num_lot");
            return sql.toString();
        }

        /**
         * Requête de comptage — sans le join ABC.
         */
        public String buildCount() {
            StringBuilder sql = new StringBuilder("SELECT COUNT(il.id)");
            appendFromForCount(sql);
            appendWhere(sql);
            return sql.toString();
        }

        // ── SELECT ────────────────────────────────────────────────────────────

        private void appendSelect(StringBuilder sql) {
            sql.append("""
                SELECT il.id,
                       il.store_inventory_line_id,
                       p.id AS produit_id,
                       fp.code_cip,
                       p.libelle,
                       l.num_lot,
                       l.expiry_date,
                       il.quantity_on_hand,
                       il.quantity_init,
                       il.gap,
                       il.updated""");

            sql.append(includeAbcPareto
                ? ",abc.classe_pareto"
                : ",CAST(NULL AS VARCHAR) AS classe_pareto");
        }

        // ── FROM / JOIN ───────────────────────────────────────────────────────

        private void appendFrom(StringBuilder sql) {
            appendCoreJoins(sql);
            if (includeAbcPareto) {
                sql.append(" LEFT JOIN mv_abc_pareto_analysis abc ON abc.produit_id = p.id");
            }
            appendScopeJoin(sql);
        }

        /**
         * FROM allégé pour le COUNT : pas de join ABC.
         */
        private void appendFromForCount(StringBuilder sql) {
            appendCoreJoins(sql);
            appendScopeJoin(sql);
        }

        private void appendCoreJoins(StringBuilder sql) {
            sql.append("""
                  FROM inventory_lot il
                JOIN store_inventory_line sil ON il.store_inventory_line_id = sil.id
                JOIN produit p               ON sil.produit_id = p.id
                JOIN fournisseur_produit fp  ON p.fournisseur_produit_principal_id = fp.id
                JOIN lot l                   ON il.lot_id = l.id""");
        }

        private void appendScopeJoin(StringBuilder sql) {
            if (filter.rayonId() != null || filter.storageId() != null) {
                sql.append(" JOIN rayon_produit rp ON p.id = rp.produit_id");
            }
        }

        // ── WHERE ─────────────────────────────────────────────────────────────

        private void appendWhere(StringBuilder sql) {
            sql.append(" WHERE sil.store_inventory_id = ?1");
            if (filter.rayonId() != null) {
                sql.append(String.format(" AND rp.rayon_id = %d", filter.rayonId()));
            } else if (filter.storageId() != null) {
                sql.append(String.format(
                    " AND rp.rayon_id IN (SELECT ry.id FROM rayon ry WHERE ry.storage_id = %d)",
                    filter.storageId()));
            }
            appendSearch(sql, filter.search(), "p.libelle", "fp.code_cip", "l.num_lot");
            appendLotFilter(sql, filter.selectedFilter());
        }
    }
}
