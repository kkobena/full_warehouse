-- ============================================================================
-- 24_mouvements.sql — Journal des mouvements de stock (inventory_transaction)
--
-- Le journal est l'unique source du stock à une date T (fn_stock_quantites_at_time, V2.1.33) : valorisation
-- passée, bilan de période, historique produit, « Suivi article ». L'application y trace TOUS ses mouvements ;
-- la démonstration doit faire de même, sans ajustement synthétique pour retomber sur le stock.
--
-- Il passe donc EN DERNIER (après 16b, 18, 22, 23), et dérive chaque mouvement de la pièce qui l'a causé, avec
-- les conventions de l'application (InventoryTransactionBuilder, procédure de clôture d'inventaire) :
--
--   ENTREE_STOCK        ligne de commande reçue (+ reçu + UG)          ; ligne de vente dépôt, côté dépôt (+)
--   SALE                ligne de vente (- servi)
--   RETOUR_DEPOT        retour du dépôt, côté officine (+) — le côté dépôt n'est pas tracé, comme dans l'application
--   RETRAIT_PERIME      produit périmé détruit (-)
--   RETOUR_FOURNISSEUR  ligne de retour fournisseur (-)
--   AJUSTEMENT_IN/OUT   ligne d'un bon d'ajustement CLÔTURÉ (quantité signée)
--   MOUVEMENT_STOCK_*   répartition rayon / réserve (sortie signée -, entrée +)
--   DECONDTION_IN/OUT   déconditionnement (+ unités / - boîte)
--   RETOUR_CLIENT       retour client remis en stock (+) ; DESTRUCTION s'il ne l'est pas (stock inchangé)
--   INVENTAIRE          ligne d'inventaire clôturé (écart signé)
--
-- POURQUOI DEUX INVENTAIRES SONT CRÉÉS ICI
--
-- 07_stock.sql fixe le stock courant indépendamment de trois ans d'achats et de ventes. Rejoué tel quel,
-- l'historique ferait passer le stock sous zéro pour des centaines de produits. Le journal est donc reconstitué,
-- par ligne de stock (produit × stockage), entre deux inventaires — ce que ferait une officine réelle :
--
--   * un INVENTAIRE D'OUVERTURE, la veille du premier mouvement : le stock de reprise, juste ce qu'il faut pour
--     que le stock ne soit jamais négatif et, autant que possible, retombe sur le stock courant ;
--   * un INVENTAIRE ANNUEL, compté hier à 21 h 30 : il porte les écarts que l'ouverture ne peut absorber (stock
--     constaté inférieur au théorique). Ses lignes sont qualifiées (casse, vol…) comme celles de 11. Une ligne
--     dont le stock courant ne couvre pas une réception du jour est comptée juste après celle-ci.
--
-- Après l'inventaire annuel, le stock se lit à rebours depuis le stock courant : le dernier mouvement de chaque
-- ligne de stock retombe sur son stock réel, par construction.
--
-- Les pièces sources reçoivent le stock avant / après du journal (ligne de vente, ligne de commande, ajustement,
-- retour fournisseur, répartition, déconditionnement, destruction, inventaire de 11) : ce que l'écran d'une pièce
-- affiche concorde avec l'historique du produit.
--
-- Photos : une à chaque clôture d'inventaire (INVENTAIRE_CLOTURE) et une de fin de jeu (BATCH_QUOTIDIEN), ancres
-- de la valorisation à date. created_at et les photos sont lus en UTC (règle de V2.1.33).
--
-- Rejouable seul : le journal, les photos et les deux inventaires qu'il crée sont reconstruits.
-- ============================================================================

\i _header.sql

\echo '>> 24_mouvements : journal des mouvements de stock'

-- ---------------------------------------------------------------------------
-- 0. Ce que ce script reconstruit
-- ---------------------------------------------------------------------------
TRUNCATE TABLE inventory_transaction;
TRUNCATE TABLE stock_produit_snapshot;

DELETE FROM inventory_gap_analysis g
 USING store_inventory_line l, store_inventory i
 WHERE g.store_inventory_line_id = l.id AND l.store_inventory_id = i.id
   AND (i.description LIKE 'Inventaire d''ouverture%' OR i.description LIKE 'Inventaire annuel%');
DELETE FROM store_inventory_line l
 USING store_inventory i
 WHERE l.store_inventory_id = i.id
   AND (i.description LIKE 'Inventaire d''ouverture%' OR i.description LIKE 'Inventaire annuel%');
DELETE FROM store_inventory
 WHERE description LIKE 'Inventaire d''ouverture%' OR description LIKE 'Inventaire annuel%';

-- Repères : rayon et réserve de l'officine, compte qui signe les inventaires, heure de l'inventaire annuel.
CREATE TEMP TABLE tmp_jr_ctx AS
SELECT
    (SELECT id FROM storage WHERE storage_type = 'PRINCIPAL' AND magasin_id = 1 LIMIT 1) AS rayon_id,
    (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1)                                AS admin_id,
    (CURRENT_DATE - 1) + TIME '21:30:00'                                                     AS t_annuel;

-- ---------------------------------------------------------------------------
-- 1. Les mouvements, dérivés de leurs pièces
--
-- delta : effet sur le stock de la ligne (produit, stockage) ; quantite : la valeur que l'application inscrit
-- dans `quantity` (positive pour une vente, signée pour un ajustement, une répartition sortante, un inventaire).
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_jr_mvt (
    produit_id     int       NOT NULL,
    storage_id     int       NOT NULL,
    moment         timestamp NOT NULL,
    mouvement_type text      NOT NULL,
    delta          int       NOT NULL,
    quantite       int       NOT NULL,
    entity_id      bigint    NOT NULL,
    source         text      NOT NULL,
    user_id        int,
    cost_amount    int       NOT NULL,
    unit_price     int       NOT NULL,
    ordre          int       NOT NULL
);

-- Réceptions de commande : le reçu et les unités gratuites entrent ensemble (final = init + reçu + UG).
INSERT INTO tmp_jr_mvt
SELECT fp.produit_id, x.rayon_id, c.receipt_date + TIME '10:00:00', 'ENTREE_STOCK',
       ol.quantity_received + COALESCE(ol.free_qty, 0), ol.quantity_received,
       ol.id, 'order_line', c.user_id, COALESCE(ol.order_cost_amount, 0), COALESCE(ol.order_unit_price, 0), 1
  FROM order_line ol
  JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
  JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
 CROSS JOIN tmp_jr_ctx x
 WHERE c.order_status IN ('RECEIVED', 'CLOSED')
   AND ol.quantity_received > 0;

-- Ventes : la quantité servie sort du rayon.
INSERT INTO tmp_jr_mvt
SELECT sl.produit_id, x.rayon_id, sl.created_at, 'SALE',
       -sl.quantity_sold, sl.quantity_sold,
       sl.id, 'sales_line', s.caissier_id, COALESCE(sl.cost_amount, 0), COALESCE(sl.regular_unit_price, 0), 2
  FROM sales_line sl
  JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
 CROSS JOIN tmp_jr_ctx x
 WHERE sl.quantity_sold > 0;

-- Ventes dépôt : ce que l'officine cède entre au dépôt (saveVenteDepotExtensionInventoryTransactions).
INSERT INTO tmp_jr_mvt
SELECT sl.produit_id, st.id, sl.created_at, 'ENTREE_STOCK',
       sl.quantity_sold, sl.quantity_sold,
       sl.id, 'vente_depot', s.user_id, COALESCE(sl.cost_amount, 0), COALESCE(sl.regular_unit_price, 0), 1
  FROM sales_line sl
  JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date AND s.dtype = 'VenteDepot'
  JOIN magasin m ON m.type_magasin = 'DEPOT'
  JOIN storage st ON st.magasin_id = m.id AND st.storage_type = 'PRINCIPAL'
 WHERE sl.quantity_sold > 0;

-- Retours du dépôt : réintégrés au rayon de l'officine.
INSERT INTO tmp_jr_mvt
SELECT i.produit_id, x.rayon_id, rd.date_mtv, 'RETOUR_DEPOT',
       i.qty_mvt, i.qty_mvt,
       i.id, 'retour_depot_item', rd.user_id, COALESCE(p.cost_amount, 0), i.regular_unit_price, 3
  FROM retour_depot_item i
  JOIN retour_depot rd ON rd.id = i.retour_depot_id
  JOIN produit p ON p.id = i.produit_id
 CROSS JOIN tmp_jr_ctx x;

-- Retraits de périmés : datés à la destruction, qui est le vrai mouvement.
INSERT INTO tmp_jr_mvt
SELECT fp.produit_id, st.id, d.datedestuction + TIME '17:00:00', 'RETRAIT_PERIME',
       -d.quantity, d.quantity,
       d.id, 'products_to_destroy', d.user_id, d.prixachat, d.prixunit, 4
  FROM products_to_destroy d
  JOIN fournisseur_produit fp ON fp.id = d.fournisseur_produit_id
  JOIN storage st ON st.magasin_id = d.magasin_id AND st.storage_type = 'PRINCIPAL'
 WHERE d.destroyed;

-- Retours fournisseur : la marchandise sort dès la création du bon (RetourBonServiceImpl), sauf hors stock.
INSERT INTO tmp_jr_mvt
SELECT fp.produit_id, x.rayon_id, i.date_mtv, 'RETOUR_FOURNISSEUR',
       -i.qty_mvt, i.qty_mvt,
       i.id, 'retour_bon_item', rb.user_id, COALESCE(i.prix_achat, 0), COALESCE(fp.prix_uni, 0), 4
  FROM retour_bon_item i
  JOIN retour_bon rb ON rb.id = i.retour_bon_id AND NOT COALESCE(rb.hors_stock, false)
  JOIN order_line ol ON ol.id = i.orderline_id AND ol.commande_order_date = i.orderline_order_date
  JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
 CROSS JOIN tmp_jr_ctx x;

-- Ajustements : seuls les bons clôturés ont modifié le stock.
INSERT INTO tmp_jr_mvt
SELECT sp.produit_id, sp.storage_id, a.date_mtv, a.type_ajust,
       a.qty_mvt, a.qty_mvt,
       a.id, 'ajustement', aj.user_id, COALESCE(p.cost_amount, 0), COALESCE(p.regular_unit_price, 0), 5
  FROM ajustement a
  JOIN ajust aj ON aj.id = a.ajust_id AND aj.statut = 'CLOSED'
  JOIN stock_produit sp ON sp.id = a.stock_produit_id
  JOIN produit p ON p.id = sp.produit_id;

-- Répartitions rayon / réserve : une sortie sur la source, une entrée sur la destination.
INSERT INTO tmp_jr_mvt
SELECT src.produit_id, src.storage_id, r.created_at, 'MOUVEMENT_STOCK_OUT',
       -r.qty_mvt, -r.qty_mvt,
       r.id, 'repartition_source', r.user_id, COALESCE(p.cost_amount, 0), COALESCE(p.regular_unit_price, 0), 6
  FROM repartition_stock_produit r
  JOIN stock_produit src ON src.id = r.stock_produit_source_id
  JOIN produit p ON p.id = src.produit_id;

INSERT INTO tmp_jr_mvt
SELECT dst.produit_id, dst.storage_id, r.created_at, 'MOUVEMENT_STOCK_IN',
       r.qty_mvt, r.qty_mvt,
       r.id, 'repartition_destination', r.user_id, COALESCE(p.cost_amount, 0), COALESCE(p.regular_unit_price, 0), 7
  FROM repartition_stock_produit r
  JOIN stock_produit dst ON dst.id = r.stock_produit_destination_id
  JOIN produit p ON p.id = dst.produit_id;

-- Déconditionnements : la boîte sort, les unités entrent.
INSERT INTO tmp_jr_mvt
SELECT d.produit_id, x.rayon_id, d.date_mtv, d.type_deconditionnement,
       CASE WHEN d.type_deconditionnement = 'DECONDTION_IN' THEN d.qty_mvt ELSE -d.qty_mvt END, d.qty_mvt,
       d.id, 'decondition', d.user_id, COALESCE(p.cost_amount, 0), COALESCE(p.regular_unit_price, 0), 8
  FROM decondition d
  JOIN produit p ON p.id = d.produit_id
 CROSS JOIN tmp_jr_ctx x;

-- Retours clients : remis en stock si le produit est conforme et non thermosensible, sinon DESTRUCTION
-- (retour accepté, stock inchangé) — la règle de RetourClientServiceImpl.
INSERT INTO tmp_jr_mvt
SELECT l.produit_id, x.rayon_id, COALESCE(rc.validated_at, rc.created_at),
       CASE WHEN r.restockable THEN 'RETOUR_CLIENT' ELSE 'DESTRUCTION' END,
       CASE WHEN r.restockable THEN l.quantite ELSE 0 END,
       CASE WHEN r.restockable THEN l.quantite ELSE -l.quantite END,
       l.id, 'retour_client_line', rc.created_by_id, COALESCE(sl.cost_amount, p.cost_amount, 0), l.prix_unitaire, 9
  FROM retour_client_line l
  JOIN retour_client rc ON rc.id = l.retour_client_id
  JOIN produit p ON p.id = l.produit_id
  LEFT JOIN sales_line sl ON sl.id = l.original_sales_line_id AND sl.sale_date = l.original_sales_line_date
 CROSS JOIN tmp_jr_ctx x
 CROSS JOIN LATERAL (
     SELECT l.emballage_intact AND l.num_lot_lisible AND l.date_peremption_valide
            AND NOT COALESCE(p.thermosensible, false) AS restockable
 ) r;

-- Inventaires clôturés de 11 : l'écart compté. Le stock théorique de la ligne sera relu dans le journal (§4).
INSERT INTO tmp_jr_mvt
SELECT l.produit_id, l.storage_id, l.updated_at, 'INVENTAIRE',
       COALESCE(l.quantity_on_hand, 0) - COALESCE(l.quantity_init, 0),
       COALESCE(l.quantity_on_hand, 0) - COALESCE(l.quantity_init, 0),
       l.id, 'store_inventory_line', COALESCE(l.counted_by_id, i.user_id),
       COALESCE(l.inventory_value_cost, 0), COALESCE(l.last_unit_price, 0), 10
  FROM store_inventory_line l
  JOIN store_inventory i ON i.id = l.store_inventory_id AND i.statut = 'CLOSED'
 WHERE l.updated;

UPDATE tmp_jr_mvt SET user_id = (SELECT admin_id FROM tmp_jr_ctx) WHERE user_id IS NULL;
CREATE INDEX ON tmp_jr_mvt (produit_id, storage_id, moment);

-- L'inventaire annuel se place dans un creux : aucun mouvement dans la minute qui suit sa clôture.
DO $$
DECLARE v_n int;
BEGIN
    SELECT count(*) INTO v_n FROM tmp_jr_mvt m, tmp_jr_ctx x
     WHERE m.moment >= x.t_annuel AND m.moment < x.t_annuel + INTERVAL '1 minute';
    IF v_n > 0 THEN
        RAISE EXCEPTION '% mouvement(s) pendant la clôture de l''inventaire annuel (veille, 21 h 30)', v_n;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 2. Reconstitution par ligne de stock (produit, stockage)
--
-- Avant le comptage annuel : on part du stock d'ouverture S0. Après : on remonte depuis le stock courant.
-- S0 est le stock qui ferait retomber l'ouverture sur le constat annuel, relevé si besoin pour que le stock ne
-- soit jamais négatif ; ce relèvement devient l'écart de l'inventaire annuel (constaté < théorique).
--
-- Chaque ligne de stock est comptée hier à 21 h 30, SAUF si le stock courant ne couvre pas ce qui est entré
-- depuis (une réception du jour plus forte que le stock posé par 07) : elle est alors comptée juste après son
-- dernier mouvement, et l'inventaire se clôture après ce dernier comptage.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_jr_tcle AS
WITH cles AS (
    SELECT produit_id, storage_id FROM tmp_jr_mvt
    UNION
    SELECT produit_id, storage_id FROM stock_produit WHERE qty_stock <> 0
),
apres AS (
    SELECT m.produit_id, m.storage_id, m.delta,
           sum(m.delta) OVER (PARTITION BY m.produit_id, m.storage_id ORDER BY m.moment, m.ordre, m.entity_id
                              ROWS UNBOUNDED PRECEDING) AS cumul
      FROM tmp_jr_mvt m, tmp_jr_ctx x
     WHERE m.moment > x.t_annuel
),
bilan AS (
    SELECT produit_id, storage_id, sum(delta) AS d_apres, min(cumul) AS min_apres FROM apres GROUP BY 1, 2
)
SELECT c.produit_id, c.storage_id,
       CASE WHEN COALESCE(sp.qty_stock, 0) - COALESCE(b.d_apres, 0) < 0
              OR COALESCE(sp.qty_stock, 0) - COALESCE(b.d_apres, 0) + LEAST(COALESCE(b.min_apres, 0), 0) < 0
            THEN (SELECT max(m.moment) FROM tmp_jr_mvt m WHERE m.produit_id = c.produit_id AND m.storage_id = c.storage_id)
                 + INTERVAL '1 minute'
            ELSE x.t_annuel
       END AS t_cle
  FROM cles c
 CROSS JOIN tmp_jr_ctx x
  LEFT JOIN stock_produit sp ON sp.produit_id = c.produit_id AND sp.storage_id = c.storage_id
  LEFT JOIN bilan b ON b.produit_id = c.produit_id AND b.storage_id = c.storage_id;
CREATE UNIQUE INDEX ON tmp_jr_tcle (produit_id, storage_id);

CREATE TEMP TABLE tmp_jr_seq AS
SELECT m.*,
       m.moment > t.t_cle AS apres,
       sum(m.delta) OVER (PARTITION BY m.produit_id, m.storage_id, (m.moment > t.t_cle)
                          ORDER BY m.moment, m.ordre, m.entity_id
                          ROWS UNBOUNDED PRECEDING) AS cumul
  FROM tmp_jr_mvt m
  JOIN tmp_jr_tcle t ON t.produit_id = m.produit_id AND t.storage_id = m.storage_id;

CREATE TEMP TABLE tmp_jr_cle AS
WITH cles AS (
    SELECT produit_id, storage_id FROM tmp_jr_mvt
    UNION
    SELECT produit_id, storage_id FROM stock_produit WHERE qty_stock <> 0
),
bornes AS (
    SELECT produit_id, storage_id,
           COALESCE(sum(delta) FILTER (WHERE NOT apres), 0) AS d_avant,
           COALESCE(min(cumul) FILTER (WHERE NOT apres), 0) AS min_avant,
           COALESCE(sum(delta) FILTER (WHERE apres), 0)     AS d_apres,
           COALESCE(min(cumul) FILTER (WHERE apres), 0)     AS min_apres,
           bool_or(NOT apres)                               AS avec_avant
      FROM tmp_jr_seq
     GROUP BY produit_id, storage_id
)
SELECT c.produit_id, c.storage_id, st.magasin_id, t.t_cle,
       COALESCE(sp.qty_stock, 0)                          AS courant,
       COALESCE(b.d_avant, 0)                             AS d_avant,
       COALESCE(b.min_avant, 0)                           AS min_avant,
       COALESCE(b.d_apres, 0)                             AS d_apres,
       COALESCE(b.min_apres, 0)                           AS min_apres,
       COALESCE(sp.qty_stock, 0) - COALESCE(b.d_apres, 0) AS constate
  FROM cles c
  JOIN storage st ON st.id = c.storage_id
  JOIN tmp_jr_tcle t ON t.produit_id = c.produit_id AND t.storage_id = c.storage_id
  LEFT JOIN stock_produit sp ON sp.produit_id = c.produit_id AND sp.storage_id = c.storage_id
  LEFT JOIN bornes b ON b.produit_id = c.produit_id AND b.storage_id = c.storage_id;

ALTER TABLE tmp_jr_cle ADD COLUMN ouverture int, ADD COLUMN theorique int, ADD COLUMN ecart int;
UPDATE tmp_jr_cle
   SET ouverture = GREATEST(-LEAST(min_avant, 0), constate - d_avant, 0);
UPDATE tmp_jr_cle
   SET theorique = ouverture + d_avant,
       ecart     = constate - (ouverture + d_avant);
CREATE UNIQUE INDEX ON tmp_jr_cle (produit_id, storage_id);

-- Après l'inventaire annuel, le stock remonté depuis le stock courant ne doit jamais être négatif.
DO $$
DECLARE v_n int;
BEGIN
    SELECT count(*) INTO v_n FROM tmp_jr_cle WHERE constate < 0 OR constate + LEAST(min_apres, 0) < 0;
    IF v_n > 0 THEN
        RAISE EXCEPTION '% ligne(s) de stock négative(s) après l''inventaire annuel : le stock courant ne couvre pas les mouvements du jour', v_n;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 3. Les deux inventaires : ouverture et annuel, un par stockage concerné
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_jr_inv (
    id          bigint PRIMARY KEY,
    nature      text   NOT NULL,
    storage_id  int    NOT NULL,
    ouvert_le   timestamp NOT NULL,
    clos_le     timestamp NOT NULL
);

INSERT INTO tmp_jr_inv
SELECT (SELECT COALESCE(max(id), 0) FROM store_inventory) + row_number() OVER (ORDER BY n.nature DESC, n.storage_id),
       n.nature, n.storage_id,
       CASE WHEN n.nature = 'OUVERTURE' THEN d.jour + TIME '07:30:00' ELSE (CURRENT_DATE - 1) + TIME '07:30:00' END,
       CASE WHEN n.nature = 'OUVERTURE' THEN d.jour + TIME '18:00:00' ELSE n.dernier_comptage + INTERVAL '30 seconds' END
  FROM (
      SELECT 'OUVERTURE' AS nature, storage_id, NULL::timestamp AS dernier_comptage FROM tmp_jr_cle WHERE ouverture > 0 GROUP BY storage_id
      UNION ALL
      SELECT 'ANNUEL', storage_id, max(t_cle) FROM tmp_jr_cle GROUP BY storage_id
  ) n
 CROSS JOIN (SELECT (min(moment)::date - 1) AS jour FROM tmp_jr_mvt) d
 CROSS JOIN tmp_jr_ctx x;

INSERT INTO store_inventory (
    id, category, created_at, updated_at, description,
    inventory_amount_begin, inventory_amount_after, inventory_value_cost_begin, inventory_value_cost_after,
    inventory_category, inventory_type, statut, storage_id, user_id, version
)
SELECT i.id, 'STOCK_TOTAL', i.ouvert_le, i.clos_le,
       CASE WHEN i.nature = 'OUVERTURE'
            THEN 'Inventaire d''ouverture — reprise du stock (' || st.name || ')'
            ELSE 'Inventaire annuel — ' || st.name || ' — clôturé' END,
       0, 0, 0, 0,
       'STORAGE', 'MANUEL', 'CLOSED', i.storage_id, x.admin_id, 0
  FROM tmp_jr_inv i
  JOIN storage st ON st.id = i.storage_id
 CROSS JOIN tmp_jr_ctx x;

-- Ouverture : de rien au stock de reprise. Annuel : chaque ligne de stock du stockage est comptée.
INSERT INTO store_inventory_line (
    store_inventory_id, produit_id, storage_id, quantity_init, quantity_on_hand, quantity_sold,
    gap, inventory_value_cost, last_unit_price, updated, updated_at, counted_by_id, version
)
SELECT i.id, c.produit_id, c.storage_id,
       CASE WHEN i.nature = 'OUVERTURE' THEN 0 ELSE c.theorique END,
       CASE WHEN i.nature = 'OUVERTURE' THEN c.ouverture ELSE c.constate END,
       0,
       CASE WHEN i.nature = 'OUVERTURE' THEN c.ouverture ELSE c.ecart END,
       COALESCE(p.cost_amount, 0), COALESCE(p.regular_unit_price, 0),
       true, CASE WHEN i.nature = 'OUVERTURE' THEN i.clos_le - INTERVAL '15 minutes' ELSE c.t_cle END, x.admin_id, 0
  FROM tmp_jr_inv i
  JOIN tmp_jr_cle c ON c.storage_id = i.storage_id AND (i.nature = 'ANNUEL' OR c.ouverture > 0)
  JOIN produit p ON p.id = c.produit_id
 CROSS JOIN tmp_jr_ctx x;

-- Les écarts de l'inventaire annuel sont qualifiés, une cause par ligne (même rotation que 11).
INSERT INTO inventory_gap_analysis (store_inventory_line_id, cause, quantity, commentaire, created_at)
SELECT l.id,
       (ARRAY['CASSE', 'VOL', 'ERREUR_SAISIE', 'ERREUR_RECEPTION', 'PEREMPTION'])[1 + l.rang % 5],
       abs(l.gap),
       (ARRAY['Casse constatée au comptage annuel.',
              'Écart non expliqué par les mouvements — démarque inconnue.',
              'Sorties saisies à tort sur un autre produit.',
              'Livraisons incomplètes non signalées au fournisseur.',
              'Retiré pour péremption sans passer par la sortie de stock.'])[1 + l.rang % 5],
       l.updated_at + INTERVAL '10 minutes'
  FROM (SELECT x.id, x.gap, x.updated_at, row_number() OVER (ORDER BY x.id) AS rang
          FROM store_inventory_line x
          JOIN tmp_jr_inv i ON i.id = x.store_inventory_id AND i.nature = 'ANNUEL'
         WHERE x.gap <> 0) l;

-- Leurs mouvements : avant = théorique, après = compté (procédure de clôture).
INSERT INTO tmp_jr_mvt
SELECT l.produit_id, l.storage_id, l.updated_at, 'INVENTAIRE',
       l.quantity_on_hand - l.quantity_init, l.quantity_on_hand - l.quantity_init,
       l.id, CASE WHEN i.nature = 'OUVERTURE' THEN 'ouverture' ELSE 'annuel' END,
       l.counted_by_id, l.inventory_value_cost, l.last_unit_price, 0
  FROM store_inventory_line l
  JOIN tmp_jr_inv i ON i.id = l.store_inventory_id;

-- ---------------------------------------------------------------------------
-- 4. Le journal : stock avant / après de chaque mouvement
--
-- Avant l'inventaire annuel : ouverture + cumul. L'inventaire annuel : théorique → constaté. Après : constaté +
-- cumul, qui retombe sur le stock courant. L'ouverture elle-même part de zéro.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_jr_journal AS
SELECT m.*,
       CASE
         WHEN m.source = 'ouverture' THEN 0
         WHEN m.source = 'annuel'    THEN c.theorique
         WHEN s.apres                THEN c.constate + s.cumul - m.delta
         ELSE c.ouverture + s.cumul - m.delta
       END AS avant
  FROM tmp_jr_mvt m
  JOIN tmp_jr_cle c ON c.produit_id = m.produit_id AND c.storage_id = m.storage_id
  LEFT JOIN tmp_jr_seq s ON s.source = m.source AND s.entity_id = m.entity_id
                        AND s.produit_id = m.produit_id AND s.storage_id = m.storage_id
                        AND s.mouvement_type = m.mouvement_type;

ALTER TABLE tmp_jr_journal ADD COLUMN apres int;
UPDATE tmp_jr_journal SET apres = avant + delta;

-- L'inventaire de 11 reçoit le stock théorique du journal ; son écart compté est conservé.
UPDATE store_inventory_line l
   SET quantity_init = j.avant, quantity_on_hand = j.apres, gap = j.delta
  FROM tmp_jr_journal j
 WHERE j.source = 'store_inventory_line' AND j.entity_id = l.id;

-- Valeurs théoriques et comptées de chaque inventaire clôturé, recalculées sur les lignes (comme 11 §5).
UPDATE store_inventory si
   SET inventory_value_cost_begin = v.cout_theorique, inventory_amount_begin = v.vente_theorique,
       inventory_value_cost_after = v.cout_compte,    inventory_amount_after = v.vente_compte,
       gap_cost = v.cout_compte - v.cout_theorique,   gap_amount = v.vente_compte - v.vente_theorique
  FROM (SELECT l.store_inventory_id,
               sum(l.quantity_init * l.inventory_value_cost)::bigint    AS cout_theorique,
               sum(l.quantity_init * l.last_unit_price)::bigint         AS vente_theorique,
               sum(l.quantity_on_hand * l.inventory_value_cost)::bigint AS cout_compte,
               sum(l.quantity_on_hand * l.last_unit_price)::bigint      AS vente_compte
          FROM store_inventory_line l GROUP BY l.store_inventory_id) v
 WHERE si.id = v.store_inventory_id AND si.statut = 'CLOSED';

-- Clé d'unicité du journal (entity_id, produit_id, mouvement_type, date) : une réception et une vente dépôt
-- partagent le type ENTREE_STOCK, leurs identifiants viennent de séquences différentes.
DO $$
DECLARE v_n int;
BEGIN
    SELECT count(*) INTO v_n FROM (
        SELECT 1 FROM tmp_jr_journal GROUP BY entity_id, produit_id, mouvement_type, moment::date HAVING count(*) > 1
    ) d;
    IF v_n > 0 THEN RAISE EXCEPTION '% mouvement(s) en double sur la clé d''unicité du journal', v_n; END IF;
END $$;

INSERT INTO inventory_transaction (
    id, transaction_date, mouvement_type, quantity, quantity_befor, quantity_after,
    cost_amount, regular_unit_price, entity_id, produit_id, user_id, magasin_id, storage_id, created_at
)
SELECT nextval('id_mvt_produit_seq'), j.moment::date, j.mouvement_type, j.quantite, j.avant, j.apres,
       j.cost_amount, j.unit_price, j.entity_id, j.produit_id, j.user_id, st.magasin_id, j.storage_id, j.moment
  FROM tmp_jr_journal j
  JOIN storage st ON st.id = j.storage_id
 ORDER BY j.moment, j.ordre, j.entity_id;

-- ---------------------------------------------------------------------------
-- 5. Les pièces reçoivent le stock avant / après du journal
-- ---------------------------------------------------------------------------
UPDATE sales_line sl SET init_stock = j.avant, after_stock = j.apres
  FROM tmp_jr_journal j
 WHERE j.source = 'sales_line' AND j.entity_id = sl.id AND sl.produit_id = j.produit_id;

UPDATE order_line ol SET init_stock = j.avant, final_stock = j.apres
  FROM tmp_jr_journal j
 WHERE j.source = 'order_line' AND j.entity_id = ol.id;

UPDATE ajustement a SET stock_before = j.avant, stock_after = j.apres
  FROM tmp_jr_journal j
 WHERE j.source = 'ajustement' AND j.entity_id = a.id;

UPDATE retour_bon_item i SET init_stock = j.avant, after_stock = j.apres
  FROM tmp_jr_journal j
 WHERE j.source = 'retour_bon_item' AND j.entity_id = i.id;

UPDATE repartition_stock_produit r SET source_init_stock = j.avant, source_final_stock = j.apres
  FROM tmp_jr_journal j
 WHERE j.source = 'repartition_source' AND j.entity_id = r.id;

UPDATE repartition_stock_produit r SET dest_init_stock = j.avant, dest_final_stock = j.apres
  FROM tmp_jr_journal j
 WHERE j.source = 'repartition_destination' AND j.entity_id = r.id;

UPDATE decondition d SET stock_before = j.avant, stock_after = j.apres
  FROM tmp_jr_journal j
 WHERE j.source = 'decondition' AND j.entity_id = d.id;

UPDATE products_to_destroy d SET stock_initial = j.avant
  FROM tmp_jr_journal j
 WHERE j.source = 'products_to_destroy' AND j.entity_id = d.id;

-- ---------------------------------------------------------------------------
-- 6. Photos : à chaque clôture d'inventaire, et en fin de jeu
--
-- La quantité photographiée est celle du journal à l'instant de la photo : par construction, une photo et le
-- journal ne se contredisent jamais (v_stock_ecart_journal).
-- ---------------------------------------------------------------------------
INSERT INTO stock_produit_snapshot (produit_id, storage_id, snapshot_date, qty_stock, qty_ug, source_type, source_inventory_id, created_at)
SELECT l.produit_id, l.storage_id, i.updated_at AT TIME ZONE 'UTC',
       COALESCE(dernier.apres, 0), 0, 'INVENTAIRE_CLOTURE', i.id, i.updated_at AT TIME ZONE 'UTC'
  FROM store_inventory i
  JOIN store_inventory_line l ON l.store_inventory_id = i.id
  LEFT JOIN LATERAL (
      SELECT j.apres FROM tmp_jr_journal j
       WHERE j.produit_id = l.produit_id AND j.storage_id = l.storage_id AND j.moment <= i.updated_at
       ORDER BY j.moment DESC, j.ordre DESC, j.entity_id DESC LIMIT 1
  ) dernier ON true
 WHERE i.statut = 'CLOSED';

INSERT INTO stock_produit_snapshot (produit_id, storage_id, snapshot_date, qty_stock, qty_ug, source_type, created_at)
SELECT sp.produit_id, sp.storage_id, f.instant, sp.qty_stock, COALESCE(sp.qty_ug, 0), 'BATCH_QUOTIDIEN', f.instant
  FROM stock_produit sp
 CROSS JOIN (SELECT GREATEST(NOW(), (SELECT max(moment) FROM tmp_jr_mvt) AT TIME ZONE 'UTC' + INTERVAL '1 minute') AS instant) f;

DROP TABLE tmp_jr_journal;
DROP TABLE tmp_jr_seq;
DROP TABLE tmp_jr_inv;
DROP TABLE tmp_jr_mvt;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_mvt int; v_neg int; v_dernier int; v_ecart_journal int; v_sans int; v_ecarts_annuel int; v_types text;
BEGIN
    SELECT count(*) INTO v_mvt FROM inventory_transaction;
    IF v_mvt < 5000 THEN RAISE EXCEPTION 'Mouvements : % (attendu >= 5000)', v_mvt; END IF;

    SELECT count(*) INTO v_neg FROM inventory_transaction WHERE quantity_befor < 0 OR quantity_after < 0;
    IF v_neg > 0 THEN RAISE EXCEPTION '% mouvement(s) à stock négatif', v_neg; END IF;

    -- Le dernier mouvement de chaque ligne de stock retombe sur son stock réel.
    SELECT count(*) INTO v_dernier
      FROM (SELECT DISTINCT ON (it.produit_id, it.storage_id) it.produit_id, it.storage_id, it.quantity_after
              FROM inventory_transaction it
             ORDER BY it.produit_id, it.storage_id, it.created_at DESC, it.id DESC) d
      LEFT JOIN stock_produit sp ON sp.produit_id = d.produit_id AND sp.storage_id = d.storage_id
     WHERE COALESCE(sp.qty_stock, 0) <> d.quantity_after;
    IF v_dernier > 0 THEN RAISE EXCEPTION '% ligne(s) de stock dont le dernier mouvement contredit le stock', v_dernier; END IF;

    SELECT count(*) INTO v_ecart_journal FROM v_stock_ecart_journal WHERE ecart <> 0;
    IF v_ecart_journal > 0 THEN RAISE EXCEPTION 'v_stock_ecart_journal : % écart(s)', v_ecart_journal; END IF;

    -- Plus d'ajustement synthétique : tout ajustement vient d'une ligne de bon.
    SELECT count(*) INTO v_sans FROM inventory_transaction it
     WHERE it.mouvement_type IN ('AJUSTEMENT_IN', 'AJUSTEMENT_OUT')
       AND NOT EXISTS (SELECT 1 FROM ajustement a WHERE a.id = it.entity_id);
    IF v_sans > 0 THEN RAISE EXCEPTION '% ajustement(s) sans bon', v_sans; END IF;

    SELECT count(*) INTO v_ecarts_annuel FROM store_inventory_line l
      JOIN store_inventory i ON i.id = l.store_inventory_id
     WHERE i.description LIKE 'Inventaire annuel%' AND l.gap <> 0;
    SELECT string_agg(mouvement_type || ' ' || n, ', ' ORDER BY mouvement_type) INTO v_types
      FROM (SELECT mouvement_type, count(*) AS n FROM inventory_transaction GROUP BY 1) t;

    RAISE NOTICE '% mouvements (%), % écart(s) à l''inventaire annuel.', v_mvt, v_types, v_ecarts_annuel;
END $$;

DROP TABLE tmp_jr_cle;
DROP TABLE tmp_jr_tcle;
DROP TABLE tmp_jr_ctx;

\echo '<< 24_mouvements : terminé'
