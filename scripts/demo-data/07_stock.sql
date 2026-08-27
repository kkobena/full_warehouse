-- ============================================================================
-- 07_stock.sql — Réceptions de lots, emplacements et stock produit
--
-- À la finalisation d'une réception, l'application fait TROIS choses ensemble
-- (StockEntryServiceImpl.mergeLots) — elles doivent donc être présentes ou
-- absentes ensemble :
--     lot.statut : IN_PROGRESS → AVAILABLE
--   + création de la ligne lot_reception
--   + crédit de lot_stock_location sur le stockage PRINCIPAL
--
-- Les lots entrent TOUJOURS par le stockage principal ; la réserve se remplit
-- ensuite par transfert (transferFefo), jamais directement.
--
-- Comptabilité à deux niveaux (§3.7) :
--     lot.current_quantity = Σ lot_stock_location.qty   (tous stockages)
-- et les lignes tombées à zéro sont SUPPRIMÉES, jamais conservées.
-- ============================================================================

\i _header.sql

\echo '>> 07_stock : réceptions, emplacements, stock produit'

-- ---------------------------------------------------------------------------
-- 1. lot_reception — trace de chaque réception
--
-- Uniquement pour les lots rattachés à une ligne de commande : les lots
-- historiques (order_line_id NULL) n'ont pas de réception tracée, et
-- lot_reception.order_line_id est NOT NULL.
--
-- Noter prix_achat AVEC underscore ici — LotReception déclare @Column, à la
-- différence de Lot (voir l'en-tête de 06_lots.sql).
-- ---------------------------------------------------------------------------
INSERT INTO lot_reception (
    lot_id, order_line_id, commande_order_date,
    quantity_received, free_qty, prix_achat, receipt_date, created_at
)
SELECT
    l.id, l.order_line_id, l.commande_order_date,
    l.quantity, 0, l.prixachat, c.receipt_date,
    c.receipt_date + TIME '10:00:00'
FROM lot l
JOIN order_line ol ON ol.id = l.order_line_id AND ol.order_date = l.commande_order_date
JOIN commande c    ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
WHERE l.order_line_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM lot_reception lr
       WHERE lr.lot_id = l.id
         AND lr.order_line_id = l.order_line_id
         AND lr.commande_order_date = l.commande_order_date
  );

-- ---------------------------------------------------------------------------
-- 2. lot_stock_location — tout entre par le stockage PRINCIPAL
-- ---------------------------------------------------------------------------
INSERT INTO lot_stock_location (lot_id, storage_id, qty, updated_at)
SELECT l.id, s.id, l.current_quantity, COALESCE(l.updated, NOW())
FROM lot l
CROSS JOIN LATERAL (
    SELECT st.id FROM storage st
     WHERE st.storage_type = 'PRINCIPAL' AND st.magasin_id = 1
     LIMIT 1
) s
WHERE l.current_quantity > 0
ON CONFLICT (lot_id, storage_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. Transfert d'une part vers la réserve
--
-- Reproduit transferFefo : la quantité est prélevée sur l'emplacement principal
-- et créditée en réserve. Le total du lot ne change pas — seule sa répartition.
--
-- Un produit sur cinq, et seulement les lots sains : on ne met pas en réserve
-- de la marchandise périmée.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_transfert AS
SELECT
    lsl.id      AS lsl_id,
    lsl.lot_id,
    -- Un tiers part en réserve, en gardant au moins une unité en rayon.
    LEAST(lsl.qty - 1, GREATEST(1, lsl.qty / 3)) AS qty_transferee
FROM lot_stock_location lsl
JOIN lot l   ON l.id = lsl.lot_id
JOIN produit p ON p.id = l.produit_id
JOIN storage st ON st.id = lsl.storage_id AND st.storage_type = 'PRINCIPAL'
WHERE l.statut = 'AVAILABLE'
  AND lsl.qty >= 6
  AND p.id % 5 = 0;

-- Crédit de la réserve
INSERT INTO lot_stock_location (lot_id, storage_id, qty, updated_at)
SELECT t.lot_id, s.id, t.qty_transferee, NOW()
FROM tmp_transfert t
CROSS JOIN LATERAL (
    SELECT st.id FROM storage st
     WHERE st.storage_type = 'SAFETY_STOCK' AND st.magasin_id = 1
     LIMIT 1
) s
WHERE t.qty_transferee > 0
ON CONFLICT (lot_id, storage_id) DO UPDATE SET qty = lot_stock_location.qty + EXCLUDED.qty;

-- Débit du rayon
UPDATE lot_stock_location lsl
   SET qty = lsl.qty - t.qty_transferee,
       updated_at = NOW()
  FROM tmp_transfert t
 WHERE lsl.id = t.lsl_id
   AND t.qty_transferee > 0;

DROP TABLE tmp_transfert;

-- Les lignes épuisées sont supprimées, jamais laissées à zéro : c'est le
-- comportement de LotStockLocationServiceImpl.debit.
DELETE FROM lot_stock_location WHERE qty <= 0;

-- ---------------------------------------------------------------------------
-- 4. stock_produit
--
-- Deux origines selon que le produit est suivi par lot :
--   * suivi par lot  → somme de TOUS ses emplacements ;
--   * hors lot       → quantités reçues, sans traçabilité fine.
--
-- Les lots périmés sont COMPTÉS dans le stock. C'est le comportement réel de
-- l'application : rien ne décrémente stock_produit quand un lot périme — la
-- marchandise reste physiquement en rayon jusqu'à sa destruction ou un
-- ajustement. C'est précisément ce stock immobilisé que les rapports de
-- péremption servent à mettre en évidence.
--
-- Conséquence pour l'étape 5 : la sortie FEFO ne consomme que les lots
-- AVAILABLE, donc une part du stock reste invendable. C'est la situation d'une
-- officine qui n'a pas encore traité ses périmés, pas une incohérence.
--
-- version = 0 : la colonne porte le verrou optimiste (@Version) et est NOT NULL.
-- ---------------------------------------------------------------------------
INSERT INTO stock_produit (
    produit_id, storage_id, qty_stock, qty_virtual, qty_ug,
    seuil_mini, stock_maxi, stock_reassort,
    version, last_modified_by, created_at, updated_at
)
-- 4a. Produits suivis par lot
SELECT
    l.produit_id,
    lsl.storage_id,
    sum(lsl.qty)::int,
    sum(lsl.qty)::int,
    0,
    GREATEST(5, (sum(lsl.qty) / 6)::int),
    GREATEST(20, (sum(lsl.qty) * 2)::int),
    0,
    0, 'system', NOW() - INTERVAL '180 days', NOW()
FROM lot_stock_location lsl
JOIN lot l ON l.id = lsl.lot_id
GROUP BY l.produit_id, lsl.storage_id

UNION ALL

-- 4b. Produits hors gestion de lot : le reçu fait foi.
SELECT
    p.id,
    s.id,
    sum(ol.quantity_received)::int,
    sum(ol.quantity_received)::int,
    0,
    GREATEST(5, (sum(ol.quantity_received) / 6)::int),
    GREATEST(20, (sum(ol.quantity_received) * 2)::int),
    0,
    0, 'system', NOW() - INTERVAL '180 days', NOW()
FROM order_line ol
JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
JOIN produit p ON p.id = fp.produit_id
CROSS JOIN LATERAL (
    SELECT st.id FROM storage st
     WHERE st.storage_type = 'PRINCIPAL' AND st.magasin_id = 1
     LIMIT 1
) s
WHERE NOT p.gestion_lot
  AND c.order_status IN ('RECEIVED', 'CLOSED')
  AND ol.quantity_received > 0
GROUP BY p.id, s.id

ON CONFLICT (storage_id, produit_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. Ligne de stock à zéro pour les produits jamais commandés
--
-- Un produit sans ligne de stock n'apparaît nulle part dans les écrans
-- d'inventaire ni dans les suggestions de réapprovisionnement. Une entrée à
-- zéro le rend visible et « en rupture », ce qui est l'état réel.
-- ---------------------------------------------------------------------------
INSERT INTO stock_produit (
    produit_id, storage_id, qty_stock, qty_virtual, qty_ug,
    seuil_mini, stock_maxi, stock_reassort,
    version, last_modified_by, created_at, updated_at
)
SELECT p.id, s.id, 0, 0, 0, 5, 20, 0, 0, 'system',
       NOW() - INTERVAL '180 days', NOW()
FROM produit p
CROSS JOIN LATERAL (
    SELECT st.id FROM storage st
     WHERE st.storage_type = 'PRINCIPAL' AND st.magasin_id = 1
     LIMIT 1
) s
WHERE NOT EXISTS (
    SELECT 1 FROM stock_produit sp
     WHERE sp.produit_id = p.id AND sp.storage_id = s.id
)
ON CONFLICT (storage_id, produit_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_reception int;
    v_lsl       int;
    v_stock     int;
    v_reserve   int;
    v_ecart_lot int;
    v_ecart_sp  int;
    v_zero      int;
BEGIN
    SELECT count(*) INTO v_reception FROM lot_reception;
    SELECT count(*) INTO v_lsl       FROM lot_stock_location;
    SELECT count(*) INTO v_stock     FROM stock_produit;

    SELECT count(*) INTO v_reserve
      FROM stock_produit sp
      JOIN storage st ON st.id = sp.storage_id
     WHERE st.storage_type = 'SAFETY_STOCK' AND sp.qty_stock > 0;

    -- Comptabilité à deux niveaux : le lot égale la somme de ses emplacements.
    SELECT count(*) INTO v_ecart_lot FROM (
        SELECT l.id FROM lot l
          LEFT JOIN lot_stock_location lsl ON lsl.lot_id = l.id
         GROUP BY l.id, l.current_quantity
        HAVING l.current_quantity <> COALESCE(sum(lsl.qty), 0)
    ) x;

    -- Stock physique = somme de TOUS les emplacements du produit.
    SELECT count(*) INTO v_ecart_sp FROM stock_produit sp
      JOIN produit p ON p.id = sp.produit_id AND p.gestion_lot
     WHERE sp.qty_stock <> COALESCE((
         SELECT sum(lsl.qty) FROM lot_stock_location lsl
           JOIN lot l ON l.id = lsl.lot_id
          WHERE l.produit_id = sp.produit_id
            AND lsl.storage_id = sp.storage_id), 0);

    SELECT count(*) INTO v_zero FROM lot_stock_location WHERE qty <= 0;

    IF v_reception < 500 THEN RAISE EXCEPTION 'lot_reception : % (attendu >= 500)', v_reception; END IF;
    IF v_lsl < 500 THEN RAISE EXCEPTION 'lot_stock_location : % (attendu >= 500)', v_lsl; END IF;
    IF v_stock < 600 THEN RAISE EXCEPTION 'stock_produit : % (attendu >= 600)', v_stock; END IF;
    IF v_reserve < 20 THEN RAISE EXCEPTION 'Produits en réserve : % (attendu >= 20)', v_reserve; END IF;
    IF v_ecart_lot > 0 THEN RAISE EXCEPTION '% lot(s) dont le restant contredit ses emplacements', v_ecart_lot; END IF;
    IF v_ecart_sp > 0 THEN RAISE EXCEPTION '% ligne(s) de stock incohérente(s) avec les lots', v_ecart_sp; END IF;
    IF v_zero > 0 THEN RAISE EXCEPTION '% emplacement(s) à zéro non supprimé(s)', v_zero; END IF;

    RAISE NOTICE '% réceptions, % emplacements, % lignes de stock (% en réserve).',
        v_reception, v_lsl, v_stock, v_reserve;
END $$;

\echo '<< 07_stock : terminé'
