-- ============================================================================
-- 16_mouvements.sql — Historique des mouvements produit
--
-- Table absente du plan initial, repérée en écrivant l'étape 6 : sans elle,
-- les écrans « Suivi article » et « Historique produit » restent VIDES alors
-- que le stock et les ventes sont parfaitement cohérents. C'est le genre de
-- trou qui ne se voit qu'en ouvrant l'application.
--
-- InventoryTransaction a une PK COMPOSITE (id, transaction_date), avec des
-- identifiants assignés à la main depuis id_mvt_produit_seq.
--
-- entity_id est NOT NULL : il pointe l'objet à l'origine du mouvement — la
-- ligne de vente pour une SALE, la ligne de commande pour une ENTREE_STOCK.
--
-- quantity_befor / quantity_after encadrent le mouvement. Ils sont reconstitués
-- à rebours depuis le stock final : c'est la seule façon d'obtenir un
-- historique qui se recoupe avec l'état courant.
--
-- Noter « quantity_befor », sans le « e » final : la faute de frappe est dans
-- l'entité et donc dans la colonne.
-- ============================================================================

\i _header.sql

\echo '>> 16_mouvements : historique des mouvements produit'

-- ---------------------------------------------------------------------------
-- 1. Entrées en stock — réceptions de commande
--
-- Chronologiquement les premières : elles constituent le stock que les ventes
-- viendront ensuite consommer.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_mvt AS
SELECT
    c.receipt_date                      AS mvt_date,
    c.receipt_date + TIME '10:00:00'    AS moment,
    'ENTREE_STOCK'                      AS mouvement_type,
    p.id                                AS produit_id,
    ol.quantity_received                AS quantity,
    ol.order_cost_amount                AS cost_amount,
    ol.order_unit_price                 AS regular_unit_price,
    ol.id::bigint                       AS entity_id,
    cmd_user.id                         AS user_id,
    1                                   AS ordre_type       -- entrées d'abord
FROM order_line ol
JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
JOIN produit p ON p.id = fp.produit_id
JOIN app_user cmd_user ON cmd_user.id = c.user_id
WHERE c.order_status IN ('RECEIVED', 'CLOSED')
  AND ol.quantity_received > 0;

-- ---------------------------------------------------------------------------
-- 2. Sorties — ventes et transferts dépôt
--
-- Les ventes dépôt portent RETOUR_DEPOT côté officine ? Non : c'est une SORTIE
-- de l'officine vers le dépôt, tracée comme une vente. Le retour effectif du
-- dépôt vers l'officine, lui, porte RETOUR_DEPOT.
-- ---------------------------------------------------------------------------
INSERT INTO tmp_mvt
SELECT
    sl.sale_date,
    sl.created_at,
    'SALE',
    sl.produit_id,
    sl.quantity_sold,
    sl.cost_amount,
    sl.regular_unit_price,
    sl.id,
    s.caissier_id,
    2
FROM sales_line sl
JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
WHERE sl.quantity_sold > 0;

-- Retours de dépôt : réintégration à l'officine.
INSERT INTO tmp_mvt
SELECT
    rd.date_mtv::date,
    rd.date_mtv,
    'RETOUR_DEPOT',
    i.produit_id,
    i.qty_mvt,
    p.cost_amount,
    i.regular_unit_price,
    i.id::bigint,
    rd.user_id,
    3
FROM retour_depot_item i
JOIN retour_depot rd ON rd.id = i.retour_depot_id
JOIN produit p ON p.id = i.produit_id;

-- Retraits de périmés.
INSERT INTO tmp_mvt
SELECT
    d.dateperemption,
    d.created,
    'RETRAIT_PERIME',
    fp.produit_id,
    d.quantity,
    d.prixachat,
    d.prixunit,
    d.id::bigint,
    d.user_id,
    4
FROM products_to_destroy d
JOIN fournisseur_produit fp ON fp.id = d.fournisseur_produit_id
WHERE d.destroyed;

-- ---------------------------------------------------------------------------
-- 3. Reconstitution du stock avant / après
--
-- Le solde est parcouru chronologiquement par produit : les entrées ajoutent,
-- les sorties retranchent. Le point de départ est calculé à rebours pour que
-- le dernier mouvement retombe exactement sur le stock courant.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_mvt_pos AS
WITH signe AS (
    SELECT m.*,
           CASE WHEN m.mouvement_type IN ('ENTREE_STOCK', 'RETOUR_DEPOT')
                THEN m.quantity ELSE -m.quantity END AS delta
      FROM tmp_mvt m
),
cumul AS (
    SELECT s.*,
           sum(s.delta) OVER (PARTITION BY s.produit_id
                              ORDER BY s.mvt_date, s.ordre_type, s.entity_id
                              ROWS UNBOUNDED PRECEDING) AS cumul_apres
      FROM signe s
),
total AS (
    SELECT produit_id, sum(delta) AS delta_total FROM signe GROUP BY produit_id
)
SELECT
    c.*,
    -- Stock de départ : l'état courant moins l'effet net de tout l'historique.
    COALESCE(sp.qty_stock, 0) - t.delta_total AS stock_depart,
    c.cumul_apres - c.delta                   AS cumul_avant
FROM cumul c
JOIN total t ON t.produit_id = c.produit_id
LEFT JOIN LATERAL (
    SELECT x.qty_stock FROM stock_produit x
      JOIN storage st ON st.id = x.storage_id
     WHERE x.produit_id = c.produit_id
       AND st.storage_type = 'PRINCIPAL' AND st.magasin_id = 1
     LIMIT 1
) sp ON true;

-- ---------------------------------------------------------------------------
-- 4. Insertion
-- ---------------------------------------------------------------------------
INSERT INTO inventory_transaction (
    id, transaction_date, mouvement_type,
    quantity, quantity_befor, quantity_after,
    cost_amount, regular_unit_price,
    entity_id, produit_id, user_id, magasin_id, storage_id,
    created_at
)
SELECT
    nextval('id_mvt_produit_seq'), m.mvt_date, m.mouvement_type,
    m.quantity,
    GREATEST(0, (m.stock_depart + m.cumul_avant)::int),
    GREATEST(0, (m.stock_depart + m.cumul_apres)::int),
    m.cost_amount, m.regular_unit_price,
    m.entity_id, m.produit_id, m.user_id, 1, s.id,
    m.moment
FROM tmp_mvt_pos m
CROSS JOIN LATERAL (
    SELECT st.id FROM storage st
     WHERE st.storage_type = 'PRINCIPAL' AND st.magasin_id = 1 LIMIT 1
) s;

DROP TABLE tmp_mvt_pos;
DROP TABLE tmp_mvt;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_mvt int; v_types int; v_neg int; v_ecart int; v_ventes int;
BEGIN
    SELECT count(*) INTO v_mvt FROM inventory_transaction;
    SELECT count(DISTINCT mouvement_type) INTO v_types FROM inventory_transaction;

    SELECT count(*) INTO v_neg FROM inventory_transaction
     WHERE quantity <= 0 OR quantity_befor < 0 OR quantity_after < 0;

    -- Le mouvement doit se refermer : après = avant ± quantité.
    SELECT count(*) INTO v_ecart FROM inventory_transaction
     WHERE (mouvement_type IN ('ENTREE_STOCK', 'RETOUR_DEPOT')
            AND quantity_after <> quantity_befor + quantity)
        OR (mouvement_type IN ('SALE', 'RETRAIT_PERIME')
            AND quantity_after <> quantity_befor - quantity);

    -- Toute ligne de vente doit avoir laissé une trace.
    SELECT count(*) INTO v_ventes FROM sales_line sl
     WHERE sl.quantity_sold > 0
       AND NOT EXISTS (SELECT 1 FROM inventory_transaction it
                        WHERE it.mouvement_type = 'SALE' AND it.entity_id = sl.id);

    IF v_mvt < 5000 THEN RAISE EXCEPTION 'Mouvements : % (attendu >= 5000)', v_mvt; END IF;
    IF v_types < 3 THEN RAISE EXCEPTION 'Types de mouvement : % (attendu >= 3)', v_types; END IF;
    IF v_neg > 0 THEN RAISE EXCEPTION '% mouvement(s) à quantité invalide', v_neg; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% mouvement(s) qui ne se referme(nt) pas', v_ecart; END IF;
    IF v_ventes > 0 THEN RAISE EXCEPTION '% ligne(s) de vente sans mouvement', v_ventes; END IF;

    RAISE NOTICE '% mouvements, % types distincts.', v_mvt, v_types;
END $$;

\echo '<< 16_mouvements : terminé'
