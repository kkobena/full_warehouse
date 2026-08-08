package com.kobe.warehouse.service.dto.builder;

import com.kobe.warehouse.service.dto.StoreInventoryLineExport;
import com.kobe.warehouse.service.dto.enumeration.StoreInventoryLineEnum;
import com.kobe.warehouse.service.dto.filter.StoreInventoryExportRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryByGroupRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.math.BigInteger;
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
 * <p>Les constantes SQL publiques restantes servent aux requêtes qui n'ont pas de variante
 * paramétrable — insertion ABC, agrégats de valorisation, export. Toute requête de liste ou de
 * comptage passe par {@link LineQueryBuilder} ou {@link LotQueryBuilder}.
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
            JOIN v_abc_pareto_analysis abc ON abc.produit_id = p.id
            WHERE p.status = 'ENABLE'
              AND (:classePareto IS NULL OR abc.classe_pareto = :classePareto)
            """;

    // ── SUMMARY / EXPORT SQL (utilisés par InventaireServiceImpl legacy) ─────

    /**
     * Les colonnes agrégées sont des {@code integer} : {@code SUM(integer)} renvoie un
     * {@code bigint} (donc un {@code Long}) alors que le record attend des {@code BigDecimal},
     * et {@code integer * integer} déborde silencieusement au-delà de 2^31 avant même
     * l'agrégation. Le {@code ::numeric} sur le premier opérande règle les deux : le produit
     * puis la somme restent en {@code numeric}.
     */
    public static final String SUMMARY_SQL =
        """
            SELECT COALESCE(SUM(i.quantity_on_hand::numeric * i.inventory_value_cost), 0) AS costValueAfter,
                   COALESCE(SUM(i.quantity_on_hand::numeric * i.last_unit_price),      0) AS amountValueAfter,
                   COALESCE(SUM(i.quantity_init::numeric   * i.inventory_value_cost),  0) AS costValueBegin,
                   COALESCE(SUM(i.quantity_init::numeric   * i.last_unit_price),       0) AS amountValueBegin,
                   COALESCE(SUM(i.gap::numeric             * i.inventory_value_cost),  0) AS gapCost,
                   COALESCE(SUM(i.gap::numeric             * i.last_unit_price),       0) AS gapAmount
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
                   COALESCE(SUM(i.quantity_init::numeric   * i.inventory_value_cost), 0) AS costBefore,
                   COALESCE(SUM(i.quantity_on_hand::numeric * i.inventory_value_cost), 0) AS costAfter,
                   COALESCE(SUM(i.quantity_init::numeric   * i.last_unit_price),  0) AS amountBefore,
                   COALESCE(SUM(i.quantity_on_hand::numeric * i.last_unit_price), 0) AS amountAfter,
                   COALESCE(SUM(i.gap::numeric             * i.inventory_value_cost), 0) AS gapCost,
                   COALESCE(SUM(i.gap::numeric             * i.last_unit_price),  0) AS gapAmount
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
                   COALESCE(SUM(i.quantity_init::numeric   * i.inventory_value_cost), 0) AS costBefore,
                   COALESCE(SUM(i.quantity_on_hand::numeric * i.inventory_value_cost), 0) AS costAfter,
                   COALESCE(SUM(i.quantity_init::numeric   * i.last_unit_price),  0) AS amountBefore,
                   COALESCE(SUM(i.quantity_on_hand::numeric * i.last_unit_price), 0) AS amountAfter,
                   COALESCE(SUM(i.gap::numeric             * i.inventory_value_cost), 0) AS gapCost,
                   COALESCE(SUM(i.gap::numeric             * i.last_unit_price),  0) AS gapAmount
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
                   COALESCE(SUM(i.quantity_init::numeric   * i.inventory_value_cost), 0) AS costBefore,
                   COALESCE(SUM(i.quantity_on_hand::numeric * i.inventory_value_cost), 0) AS costAfter,
                   COALESCE(SUM(i.quantity_init::numeric   * i.last_unit_price),  0) AS amountBefore,
                   COALESCE(SUM(i.quantity_on_hand::numeric * i.last_unit_price), 0) AS amountAfter,
                   COALESCE(SUM(i.gap::numeric             * i.inventory_value_cost), 0) AS gapCost,
                   COALESCE(SUM(i.gap::numeric             * i.last_unit_price),  0) AS gapAmount
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
     *
     * <p>Piloté par {@code store_inventory_line} et non par {@code inventory_lot}, pour la même
     * raison que la grille de saisie (voir {@link LotQueryBuilder}) : les produits du périmètre
     * dépourvus de lot doivent figurer à l'export, sinon le document ne rend pas compte de tout
     * ce qui a été inventorié. Ils y apparaissent en une ligne sans numéro de lot, valorisée au
     * niveau de la ligne produit.
     */
    public static final String LOT_EXPORT_SQL =
        """
            SELECT fp.code_cip,
                   p.libelle                        AS produit_libelle,
                   p.id                             AS produit_id,
                   il.id                            AS inventory_lot_id,
                   l.num_lot,
                   l.expiry_date,
                   COALESCE(il.quantity_init, sil.quantity_init)       AS quantity_init,
                   COALESCE(il.quantity_on_hand, sil.quantity_on_hand) AS quantity_on_hand,
                   COALESCE(il.gap, sil.gap)                           AS gap,
                   COALESCE(sil.last_unit_price, fp.prix_uni)   AS last_unit_price,
                   COALESCE(sil.inventory_value_cost, fp.prix_achat) AS prix_achat
            FROM store_inventory_line sil
            JOIN produit p               ON p.id        = sil.produit_id
            JOIN fournisseur_produit fp  ON fp.id       = p.fournisseur_produit_principal_id
            LEFT JOIN inventory_lot il   ON il.store_inventory_line_id = sil.id
            LEFT JOIN lot l              ON l.id        = il.lot_id
            WHERE sil.store_inventory_id = ?1
            ORDER BY fp.code_cip, p.libelle, l.num_lot, il.id
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
            toBigDecimal(t, "costValueBegin"),
            toBigDecimal(t, "costValueAfter"),
            toBigDecimal(t, "amountValueBegin"),
            toBigDecimal(t, "amountValueAfter"),
            toBigDecimal(t, "gapCost"),
            toBigDecimal(t, "gapAmount")
        );
    }

    public static StoreInventorySummaryByGroupRecord buildGroupRow(Tuple t) {
        return new StoreInventorySummaryByGroupRecord(
            t.get("groupKey", String.class),
            t.get("groupLabel", String.class),
            ((Number) t.get("lineCount")).longValue(),
            toBigDecimal(t, "costBefore"),
            toBigDecimal(t, "costAfter"),
            toBigDecimal(t, "amountBefore"),
            toBigDecimal(t, "amountAfter"),
            toBigDecimal(t, "gapCost"),
            toBigDecimal(t, "gapAmount")
        );
    }

    /**
     * Lit une colonne agrégée sans présumer du type JDBC renvoyé.
     *
     * <p>{@code Tuple#get(String, Class)} applique un {@code Class#cast} : demander
     * {@code BigDecimal.class} sur un {@code SUM} de colonnes {@code integer} — que PostgreSQL
     * renvoie en {@code bigint} — lève {@code ClassCastException}. Le type exact dépend du
     * dialecte et du cast SQL, on normalise donc ici plutôt que de s'y fier.
     */
    private static BigDecimal toBigDecimal(Tuple tuple, String alias) {
        return switch (tuple.get(alias)) {
            case null -> BigDecimal.ZERO;
            case BigDecimal value -> value;
            case BigInteger value -> new BigDecimal(value);
            case Double value -> BigDecimal.valueOf(value);
            case Float value -> BigDecimal.valueOf(value);
            case Number value -> BigDecimal.valueOf(value.longValue());
            case Object value -> throw new IllegalArgumentException(
                "Colonne agrégée [%s] non numérique : %s".formatted(alias, value.getClass().getName())
            );
        };
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
        String updated = LotQueryBuilder.EFFECTIVE_UPDATED;
        String onHand = LotQueryBuilder.EFFECTIVE_QUANTITY_ON_HAND;
        String init = LotQueryBuilder.EFFECTIVE_QUANTITY_INIT;
        switch (lineEnum) {
            case NOT_UPDATED -> sql.append(" AND ").append(updated).append(" IS false");
            case UPDATED -> sql.append(" AND ").append(updated);
            case GAP -> sql.append(" AND ").append(updated)
                .append(" AND ").append(onHand).append(" <> ").append(init);
            case GAP_NEGATIF -> sql.append(" AND ").append(updated)
                .append(" AND ").append(onHand).append(" < ").append(init);
            case GAP_POSITIF -> sql.append(" AND ").append(updated)
                .append(" AND ").append(onHand).append(" >= ").append(init);
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
     *   <li>{@link #withAbcPareto(boolean)} — LEFT JOIN sur {@code v_abc_pareto_analysis}</li>
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
         * Active le LEFT JOIN sur {@code v_abc_pareto_analysis}. À utiliser uniquement pour les
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
                       sp.seuil_mini,
                       a.version,
                       CASE WHEN cb.id IS NULL THEN NULL
                            ELSE CONCAT(LEFT(cb.first_name, 1), '. ', cb.last_name)
                       END                 AS counted_by""");

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
                sql.append(" LEFT JOIN v_abc_pareto_analysis abc ON abc.produit_id = p.id");
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
                LEFT JOIN stock_produit sp  ON sp.produit_id = p.id AND sp.storage_id = a.storage_id
                LEFT JOIN app_user cb       ON cb.id = a.counted_by_id""");
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
     * des lots (un lot par ligne).
     *
     * <p>La requête est pilotée par {@code store_inventory_line}, pas par {@code inventory_lot} :
     * à la création d'un inventaire, un {@code inventory_lot} n'est inséré que pour les lots dont
     * la quantité est strictement positive. Un INNER JOIN rendait donc invisibles — et de ce fait
     * incomptables — les produits sans lot ou dont tous les lots sont à zéro, alors qu'ils sont
     * bien dans le périmètre et seront clôturés. Ils apparaissent désormais en une ligne unique,
     * sans numéro de lot, qui se compte au niveau de la ligne produit.
     *
     * <p>Join optionnel :
     * <ul>
     *   <li>{@link #withAbcPareto(boolean)} — LEFT JOIN sur {@code v_abc_pareto_analysis}</li>
     * </ul>
     */
    public static final class LotQueryBuilder {

        /**
         * Colonnes lues au niveau du lot quand il y en a un, au niveau de la ligne produit sinon.
         * Partagées avec {@link #appendLotFilter} : un filtre qui interrogerait {@code il}
         * directement écarterait toutes les lignes sans lot.
         */
        static final String EFFECTIVE_UPDATED = "COALESCE(il.updated, sil.updated)";
        static final String EFFECTIVE_QUANTITY_ON_HAND =
            "COALESCE(il.quantity_on_hand, sil.quantity_on_hand)";
        static final String EFFECTIVE_QUANTITY_INIT =
            "COALESCE(il.quantity_init, sil.quantity_init)";

        private final StoreInventoryLineFilterRecord filter;
        private boolean includeAbcPareto = false;

        private LotQueryBuilder(StoreInventoryLineFilterRecord filter) {
            this.filter = filter;
        }

        /**
         * Active le LEFT JOIN sur {@code v_abc_pareto_analysis}.
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
            // il.id départage les lots d'un même produit : sans lui, deux lignes de même
            // num_lot pourraient changer de page d'un appel à l'autre
            sql.append(" ORDER BY fp.code_cip, p.libelle, l.num_lot, il.id");
            return sql.toString();
        }

        /**
         * Requête de comptage — sans le join ABC.
         */
        public String buildCount() {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*)");
            appendFromForCount(sql);
            appendWhere(sql);
            return sql.toString();
        }

        // ── SELECT ────────────────────────────────────────────────────────────

        /**
         * {@code il.id} nul identifie une ligne sans lot : c'est à ce signal que les clients
         * routent la saisie vers l'API ligne produit plutôt que vers l'API lot.
         */
        private void appendSelect(StringBuilder sql) {
            sql.append("""
                SELECT il.id,
                       sil.id AS store_inventory_line_id,
                       p.id AS produit_id,
                       fp.code_cip,
                       p.libelle,
                       l.num_lot,
                       l.expiry_date,
                """);
            sql.append(EFFECTIVE_QUANTITY_ON_HAND).append(" AS quantity_on_hand,");
            sql.append(EFFECTIVE_QUANTITY_INIT).append(" AS quantity_init,");
            sql.append("COALESCE(il.gap, sil.gap) AS gap,");
            sql.append(EFFECTIVE_UPDATED).append(" AS updated,");
            // Verrou optimiste de la ligne produit : seules les lignes sans lot s'écrivent
            // par l'API ligne, qui exige la version lue pour arbitrer un comptage concurrent
            sql.append("sil.version AS version");

            sql.append(includeAbcPareto
                ? ",abc.classe_pareto"
                : ",CAST(NULL AS VARCHAR) AS classe_pareto");
        }

        // ── FROM / JOIN ───────────────────────────────────────────────────────

        private void appendFrom(StringBuilder sql) {
            appendCoreJoins(sql);
            if (includeAbcPareto) {
                sql.append(" LEFT JOIN v_abc_pareto_analysis abc ON abc.produit_id = p.id");
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
                  FROM store_inventory_line sil
                JOIN produit p               ON sil.produit_id = p.id
                JOIN fournisseur_produit fp  ON p.fournisseur_produit_principal_id = fp.id
                LEFT JOIN inventory_lot il   ON il.store_inventory_line_id = sil.id
                LEFT JOIN lot l              ON l.id = il.lot_id""");
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
