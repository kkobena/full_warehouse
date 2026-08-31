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

-- La table est ENTIÈREMENT dérivée des commandes, ventes, retours et
-- destructions : on la reconstruit plutôt que d'y ajouter. Sans cela, rejouer
-- ce seul script après correction butterait sur la contrainte d'unicité
-- (entity_id, produit_id, mouvement_type) — ou pire, doublerait l'historique.
TRUNCATE TABLE inventory_transaction;

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
--
-- Daté à la DESTRUCTION, pas à la péremption : un produit périmé reste en rayon
-- jusqu'à son retrait effectif, et c'est ce retrait qui constitue le mouvement
-- de stock. 12_destruction.sql garantit datedestuction >= dateperemption, donc
-- dater à la péremption anticiperait la sortie de plusieurs semaines — et
-- placerait created_at avant transaction_date, ce qui ne se défend pas.
--
-- Noter la colonne « datedestuction », sans le « r » : la faute est dans le
-- schéma, donc dans l'entité.
INSERT INTO tmp_mvt
SELECT
    d.datedestuction,
    d.datedestuction + TIME '17:00:00',
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
-- les sorties retranchent.
--
-- Le point de départ ne peut PAS être simplement « stock courant moins effet
-- net de l'historique ». 07_stock.sql fixe les stocks indépendamment des
-- réceptions et des ventes : pour 388 produits sur 494, ce calcul fait plonger
-- le solde intermédiaire jusqu'à -108. Écrêter à zéro à l'insertion, comme le
-- faisait la version précédente, brisait l'arithmétique du mouvement —
-- après <> avant ± quantité sur 3476 lignes.
--
-- Le départ est donc relevé au minimum nécessaire pour que le solde reste
-- positif d'un bout à l'autre. Contrepartie : pour ces produits, l'historique
-- ne retombe plus sur le stock compté. L'écart est soldé en 5 par un mouvement
-- d'ajustement — ce que fait une officine réelle quand l'inventaire diverge.
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
bornes AS (
    SELECT produit_id,
           sum(delta)      AS delta_total,
           min(cumul_apres) AS cumul_min
      FROM cumul GROUP BY produit_id
)
SELECT
    c.*,
    b.delta_total,
    COALESCE(sp.qty_stock, 0) AS stock_courant,
    GREATEST(
        -- ce qu'il faudrait pour retomber sur le stock courant…
        COALESCE(sp.qty_stock, 0) - b.delta_total,
        -- …et ce qu'il faut au minimum pour ne jamais passer sous zéro.
        -LEAST(b.cumul_min, 0)
    ) AS stock_depart,
    c.cumul_apres - c.delta AS cumul_avant
FROM cumul c
JOIN bornes b ON b.produit_id = c.produit_id
LEFT JOIN LATERAL (
    SELECT x.qty_stock FROM stock_produit x
      JOIN storage st ON st.id = x.storage_id
     WHERE x.produit_id = c.produit_id
       AND st.storage_type = 'PRINCIPAL' AND st.magasin_id = 1
     LIMIT 1
) sp ON true;

-- ---------------------------------------------------------------------------
-- 4. Insertion
--
-- Plus aucun écrêtage : le stock de départ garantit déjà la positivité, et
-- l'invariant après = avant ± quantité tient donc exactement.
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
    (m.stock_depart + m.cumul_avant)::int,
    (m.stock_depart + m.cumul_apres)::int,
    m.cost_amount, m.regular_unit_price,
    m.entity_id, m.produit_id, m.user_id, 1, s.id,
    m.moment
FROM tmp_mvt_pos m
CROSS JOIN LATERAL (
    SELECT st.id FROM storage st
     WHERE st.storage_type = 'PRINCIPAL' AND st.magasin_id = 1 LIMIT 1
) s;

-- ---------------------------------------------------------------------------
-- 5. Ajustement d'inventaire de clôture
--
-- Pour les produits dont l'historique ne retombe pas sur le stock compté, une
-- ligne d'ajustement solde l'écart à la date du jour. Sans elle, l'écran
-- « Suivi article » afficherait un dernier mouvement en contradiction avec la
-- fiche produit — l'incohérence la plus visible qui soit dans une démo.
--
-- entity_id est NOT NULL et désigne l'objet à l'origine du mouvement. Aucun
-- enregistrement d'ajustement n'existe ici : on y met le produit lui-même,
-- faute de mieux, plutôt que d'inventer une table.
-- ---------------------------------------------------------------------------
INSERT INTO inventory_transaction (
    id, transaction_date, mouvement_type,
    quantity, quantity_befor, quantity_after,
    cost_amount, regular_unit_price,
    entity_id, produit_id, user_id, magasin_id, storage_id,
    created_at
)
SELECT
    nextval('id_mvt_produit_seq'), CURRENT_DATE,
    CASE WHEN e.ecart > 0 THEN 'AJUSTEMENT_IN' ELSE 'AJUSTEMENT_OUT' END,
    abs(e.ecart), e.stock_final, e.stock_courant,
    p.cost_amount, p.regular_unit_price,
    e.produit_id::bigint, e.produit_id, u.id, 1, s.id,
    CURRENT_DATE + TIME '08:00:00'
FROM (
    SELECT DISTINCT
        produit_id,
        stock_courant,
        (stock_depart + delta_total)::int          AS stock_final,
        stock_courant - (stock_depart + delta_total)::int AS ecart
      FROM tmp_mvt_pos
) e
JOIN produit p ON p.id = e.produit_id
CROSS JOIN LATERAL (
    SELECT st.id FROM storage st
     WHERE st.storage_type = 'PRINCIPAL' AND st.magasin_id = 1 LIMIT 1
) s
CROSS JOIN LATERAL (SELECT id FROM app_user ORDER BY id LIMIT 1) u
WHERE e.ecart <> 0;

DROP TABLE tmp_mvt_pos;
DROP TABLE tmp_mvt;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_mvt int; v_types int; v_neg int; v_ecart int; v_ventes int;
    v_ajust int; v_desaccord int;
BEGIN
    SELECT count(*) INTO v_mvt FROM inventory_transaction;
    SELECT count(DISTINCT mouvement_type) INTO v_types FROM inventory_transaction;

    SELECT count(*) INTO v_neg FROM inventory_transaction
     WHERE quantity <= 0 OR quantity_befor < 0 OR quantity_after < 0;

    -- Le mouvement doit se refermer : après = avant ± quantité.
    SELECT count(*) INTO v_ecart FROM inventory_transaction
     WHERE (mouvement_type IN ('ENTREE_STOCK', 'RETOUR_DEPOT', 'AJUSTEMENT_IN')
            AND quantity_after <> quantity_befor + quantity)
        OR (mouvement_type IN ('SALE', 'RETRAIT_PERIME', 'AJUSTEMENT_OUT')
            AND quantity_after <> quantity_befor - quantity);

    -- Toute ligne de vente doit avoir laissé une trace.
    SELECT count(*) INTO v_ventes FROM sales_line sl
     WHERE sl.quantity_sold > 0
       AND NOT EXISTS (SELECT 1 FROM inventory_transaction it
                        WHERE it.mouvement_type = 'SALE' AND it.entity_id = sl.id);

    SELECT count(*) INTO v_ajust FROM inventory_transaction
     WHERE mouvement_type IN ('AJUSTEMENT_IN', 'AJUSTEMENT_OUT');

    -- Le dernier mouvement de chaque produit doit retomber sur son stock réel :
    -- c'est ce que l'écran « Suivi article » met sous les yeux de l'utilisateur.
    SELECT count(*) INTO v_desaccord
      FROM (
        SELECT DISTINCT ON (it.produit_id) it.produit_id, it.quantity_after
          FROM inventory_transaction it
         ORDER BY it.produit_id, it.transaction_date DESC, it.id DESC
      ) dernier
      JOIN stock_produit sp ON sp.produit_id = dernier.produit_id
      JOIN storage st ON st.id = sp.storage_id
     WHERE st.storage_type = 'PRINCIPAL' AND st.magasin_id = 1
       AND sp.qty_stock <> dernier.quantity_after;

    IF v_mvt < 5000 THEN RAISE EXCEPTION 'Mouvements : % (attendu >= 5000)', v_mvt; END IF;
    IF v_types < 3 THEN RAISE EXCEPTION 'Types de mouvement : % (attendu >= 3)', v_types; END IF;
    IF v_neg > 0 THEN RAISE EXCEPTION '% mouvement(s) à quantité invalide', v_neg; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% mouvement(s) qui ne se referme(nt) pas', v_ecart; END IF;
    IF v_ventes > 0 THEN RAISE EXCEPTION '% ligne(s) de vente sans mouvement', v_ventes; END IF;
    IF v_desaccord > 0 THEN
        RAISE EXCEPTION '% produit(s) dont le dernier mouvement contredit le stock', v_desaccord;
    END IF;

    RAISE NOTICE '% mouvements, % types distincts, dont % ajustement(s) de clôture.',
                 v_mvt, v_types, v_ajust;
END $$;

\echo '<< 16_mouvements : terminé'
