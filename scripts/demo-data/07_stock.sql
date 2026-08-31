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
--
-- S'y ajoute un CAS NOMMÉ : l'ARNICA MONTANA 9CH, quel que soit son identifiant.
-- « Un produit sur cinq » dépend de l'ordre d'insertion, donc du jour de
-- chargement : aucun parcours ne peut citer un produit dont la réserve n'est
-- pas garantie, et REF-48 doit pouvoir montrer les deux stocks côte à côte sur
-- une fiche toujours la même.
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
  -- Le produit nommé ne doit PAS être un de ceux que les parcours de vente mettent en
  -- rupture : une réserve change ce que l'écran propose alors — un transfert plutôt qu'un
  -- forçage de stock (VTE-41).
  AND (p.id % 5 = 0 OR p.libelle LIKE 'ARNICA MONTANA 9CH%');

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
-- 6. Des produits SOUS LEUR SEUIL, sans être en rupture
--
-- Le rapport « Alertes de Stock » distingue trois cas : la RUPTURE (stock nul),
-- le STOCK BAS (stock inférieur au seuil mini) et la PÉREMPTION. Les deux
-- premiers et le troisième étaient largement pourvus ; le stock bas, lui, ne
-- comptait aucun cas — le seuil mini du catalogue vaut 5 ou 10, et le stock de
-- démonstration se compte en dizaines ou en centaines. Le troisième compteur
-- restait donc à zéro, et l'écran ne pouvait pas montrer ce qu'il sait faire.
--
-- On relève le SEUIL plutôt que d'abaisser le stock : la comptabilité à deux
-- niveaux impose que `stock_produit.qty_stock` égale la somme des emplacements
-- du produit, et y toucher mettrait en défaut les contrôles ci-dessous. Un
-- pharmacien qui pose un seuil élevé sur un produit dont il ne veut jamais
-- manquer — un antibiotique de garde, une insuline — est par ailleurs le cas
-- le plus ordinaire qui soit.
-- ---------------------------------------------------------------------------
UPDATE produit p
   SET qty_seuil_mini = sp.qty_stock + 5,
       qty_appro      = sp.qty_stock + 20
  FROM (
      SELECT sp.produit_id, sp.qty_stock,
             row_number() OVER (ORDER BY sp.produit_id) AS rang
        FROM stock_produit sp
        JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'PRINCIPAL'
        JOIN produit pr ON pr.id = sp.produit_id AND pr.status = 'ENABLE'
       WHERE sp.qty_stock BETWEEN 5 AND 60
  ) sp
 WHERE sp.produit_id = p.id
   AND sp.rang <= 15;

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
    v_bas       int;
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

    -- Le troisième cas des alertes de stock : sous le seuil, mais pas à zéro.
    SELECT count(*) INTO v_bas
      FROM stock_produit sp
      JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'PRINCIPAL'
      JOIN produit p ON p.id = sp.produit_id AND p.status = 'ENABLE'
     WHERE sp.qty_stock > 0 AND sp.qty_stock < p.qty_seuil_mini;

    IF v_reception < 500 THEN RAISE EXCEPTION 'lot_reception : % (attendu >= 500)', v_reception; END IF;
    IF v_lsl < 500 THEN RAISE EXCEPTION 'lot_stock_location : % (attendu >= 500)', v_lsl; END IF;
    IF v_stock < 600 THEN RAISE EXCEPTION 'stock_produit : % (attendu >= 600)', v_stock; END IF;
    IF v_reserve < 20 THEN RAISE EXCEPTION 'Produits en réserve : % (attendu >= 20)', v_reserve; END IF;
    IF v_ecart_lot > 0 THEN RAISE EXCEPTION '% lot(s) dont le restant contredit ses emplacements', v_ecart_lot; END IF;
    IF v_ecart_sp > 0 THEN RAISE EXCEPTION '% ligne(s) de stock incohérente(s) avec les lots', v_ecart_sp; END IF;
    IF v_zero > 0 THEN RAISE EXCEPTION '% emplacement(s) à zéro non supprimé(s)', v_zero; END IF;
    IF v_bas < 10 THEN
        RAISE EXCEPTION 'Produits sous leur seuil : % (attendu >= 10)', v_bas;
    END IF;

    RAISE NOTICE '% réceptions, % emplacements, % lignes de stock (% en réserve, % sous seuil).',
        v_reception, v_lsl, v_stock, v_reserve, v_bas;
END $$;

\echo '<< 07_stock : terminé'

-- ---------------------------------------------------------------------------
-- Une ligne de réserve pour les produits des bons en cours
--
-- La répartition rayon → réserve suppose une ligne de stock DES DEUX CÔTÉS :
-- sans ligne de réserve, `getPutawayPreview` écarte le produit, même largement
-- au-dessus de son maximum. Or seuls les produits transférés plus haut en ont
-- une. On en pose donc une, à zéro, pour les produits des bons en cours de
-- réception : une réserve vide est un état parfaitement ordinaire, et c'est
-- précisément celui qu'une livraison excédentaire vient remplir.
-- ---------------------------------------------------------------------------
INSERT INTO stock_produit (
    produit_id, storage_id, qty_stock, qty_virtual, qty_ug,
    seuil_mini, stock_maxi, stock_reassort,
    version, last_modified_by, created_at, updated_at
)
SELECT DISTINCT fp.produit_id, s.id, 0, 0, 0, 0, 0, 0,
       0, 'system', NOW() - INTERVAL '180 days', NOW()
  FROM order_line ol
  JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
  JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
 CROSS JOIN LATERAL (
    SELECT st.id FROM storage st
     WHERE st.storage_type = 'SAFETY_STOCK' AND st.magasin_id = 1
     LIMIT 1
 ) s
 WHERE c.order_status = 'RECEIVED'
ON CONFLICT (storage_id, produit_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Des rayons au-dessus de leur maximum
--
-- Le rangement proposé à la finalisation d'une réception (ACH-46, ACH-75) ne
-- concerne QUE les produits dont le stock rayon dépasse le maximum fixé pour ce
-- rayon : `getPutawayPreview` ne rend rien d'autre. Or le maximum posé plus haut
-- vaut le double du stock — aucun produit ne le dépasse jamais, et l'écran de
-- rangement ne s'ouvre pas.
--
-- Un maximum de rayon n'est pas une prévision : c'est la place disponible sur
-- l'étagère. Il est donc normal qu'une livraison la dépasse, et c'est
-- exactement ce moment-là que la répartition rayon → réserve sert à traiter. On
-- abaisse ce maximum sur un produit sur neuf, parmi ceux qui existent DES DEUX
-- côtés (rayon et réserve) — sans quoi il n'y a nulle part où transférer.
-- ---------------------------------------------------------------------------
UPDATE stock_produit sp
   SET stock_maxi = GREATEST(1, (sp.qty_stock * 2) / 3)
  FROM storage st
 WHERE st.id = sp.storage_id
   AND st.storage_type = 'PRINCIPAL'
   AND sp.qty_stock > 3
   -- Les produits des bons EN COURS DE RÉCEPTION : ce sont eux qu'on finalisera, et donc
   -- eux dont le rangement doit se poser. Un débordement sur un produit absent des bons
   -- ouverts n'ouvrirait jamais l'écran.
   AND EXISTS (
       SELECT 1
         FROM order_line ol
         JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
         JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
        WHERE fp.produit_id = sp.produit_id
          AND c.order_status = 'RECEIVED'
   )
   AND EXISTS (
       SELECT 1 FROM stock_produit r
         JOIN storage sr ON sr.id = r.storage_id AND sr.storage_type = 'SAFETY_STOCK'
        WHERE r.produit_id = sp.produit_id
   );

DO $$
DECLARE v_nb int;
BEGIN
    SELECT count(*) INTO v_nb
      FROM stock_produit sp
      JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'PRINCIPAL'
     WHERE sp.stock_maxi > 0 AND sp.qty_stock > sp.stock_maxi;
    IF v_nb = 0 THEN
        RAISE EXCEPTION 'Aucun rayon au-dessus de son maximum : le rangement ne se démontre pas';
    END IF;
    RAISE NOTICE 'Rayons au-dessus de leur maximum : % produit(s).', v_nb;
END $$;

-- ---------------------------------------------------------------------------
-- Produits VEDETTES du manuel : un stock qui tient toute une campagne
--
-- Les parcours illustrés s'appuient toujours sur les mêmes articles —
-- DOLIPRANE 500MG revient dans trente-huit d'entre eux, PARACETAMOL 1G dans
-- neuf. Or les ventes de demonstration puisent dans le meme stock, et rien ne
-- garantissait qu'il en reste : au premier chargement, PARACETAMOL 1G se
-- retrouvait a zero, et toute la serie des ventes echouait sur « Stock
-- insuffisant » — un echec qui accuse l'application alors qu'il ne dit rien
-- d'autre que l'epuisement du jeu de donnees.
--
-- On leur assure donc une reserve confortable sur le stockage PRINCIPAL, seul
-- lieu ou l'ecran de vente sert. 600 unites couvrent une campagne entiere,
-- rejouee plusieurs fois, sans jamais rendre le chiffre invraisemblable pour
-- une officine.
-- ---------------------------------------------------------------------------
-- Le complement se pose SUR UN LOT, et non sur le seul compteur de stock : la
-- quantite d'un produit suivi par lot doit toujours egaler la somme de ses
-- emplacements (controle « Stock = tous les emplacements du meme stockage »).
-- Crediter stock_produit sans crediter lot_stock_location fabriquerait
-- exactement l'incoherence que le jeu de donnees est cense ne jamais montrer.
-- Un produit vedette peut n'avoir AUCUN lot en rayon — il n'est jamais entre en
-- commande sur ce jeu de donnees. On lui en pose un, date au large, avant de
-- crediter : sans lot, il n'y a rien a gonfler et la garantie tombe.
INSERT INTO lot (
    num_lot, produit_id, quantity, current_quantity, quantity_received_ug,
    prixachat, prixunit, expiry_date, manufacturing_date, statut,
    created_date, updated
)
SELECT 'LVEDETTE' || lpad(p.id::text, 4, '0'),
       p.id, 0, 0, 0,
       p.cost_amount, p.regular_unit_price,
       CURRENT_DATE + INTERVAL '30 months',
       CURRENT_DATE - INTERVAL '6 months',
       'AVAILABLE', now(), now()
  FROM produit p
 WHERE p.libelle LIKE ANY (ARRAY['DOLIPRANE 500MG%', 'DOLIPRANE 1G%',
                                 'DOLIPRANE 250MG%', 'PARACETAMOL 1G%'])
   AND NOT EXISTS (
       SELECT 1 FROM lot l
         JOIN lot_stock_location lsl ON lsl.lot_id = l.id
         JOIN storage st ON st.id = lsl.storage_id AND st.storage_type = 'PRINCIPAL'
        WHERE l.produit_id = p.id
   )
ON CONFLICT (num_lot, produit_id) DO NOTHING;

INSERT INTO lot_stock_location (lot_id, storage_id, qty, updated_at)
SELECT l.id, st.id, 0, now()
  FROM lot l
  JOIN produit p ON p.id = l.produit_id
  CROSS JOIN LATERAL (
      SELECT s.id FROM storage s
       WHERE s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1 LIMIT 1
  ) st
 WHERE l.num_lot LIKE 'LVEDETTE%'
   AND NOT EXISTS (
       SELECT 1 FROM lot_stock_location x
        WHERE x.lot_id = l.id AND x.storage_id = st.id
   );

-- Et le compteur de stock lui-meme, s'il manque.
INSERT INTO stock_produit (produit_id, storage_id, qty_stock, qty_virtual, qty_ug,
                           stock_maxi, updated_at)
SELECT p.id, st.id, 0, 0, 0, 0, now()
  FROM produit p
  CROSS JOIN LATERAL (
      SELECT s.id FROM storage s
       WHERE s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1 LIMIT 1
  ) st
 WHERE p.libelle LIKE ANY (ARRAY['DOLIPRANE 500MG%', 'DOLIPRANE 1G%',
                                 'DOLIPRANE 250MG%', 'PARACETAMOL 1G%'])
   AND NOT EXISTS (
       SELECT 1 FROM stock_produit sp
        WHERE sp.produit_id = p.id AND sp.storage_id = st.id
   );

WITH vedettes AS (
    SELECT sp.produit_id,
           sp.storage_id,
           greatest(600 - sp.qty_stock, 0) AS complement
      FROM stock_produit sp
      JOIN produit p ON p.id = sp.produit_id
      JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'PRINCIPAL'
     WHERE p.libelle LIKE ANY (ARRAY['DOLIPRANE 500MG%', 'DOLIPRANE 1G%',
                                     'DOLIPRANE 250MG%', 'PARACETAMOL 1G%'])
),
lot_cible AS (
    -- Le lot le plus lointain a peremption : on gonfle celui qui a le moins de
    -- chances de partir en peremption pendant la campagne.
    --
    -- Les lots SERIALISES sont ecartes : un serial FMD identifie une boite, et le
    -- controle « Lot serialise de faible quantite » veille a ce qu'ils n'en
    -- portent jamais plus de trois. Y verser six cents unites detruirait le sens
    -- meme de la serialisation.
    SELECT DISTINCT ON (v.produit_id, v.storage_id)
           v.produit_id, v.storage_id, v.complement, lsl.lot_id
      FROM vedettes v
      JOIN lot l ON l.produit_id = v.produit_id AND l.serial_number IS NULL
      JOIN lot_stock_location lsl ON lsl.lot_id = l.id AND lsl.storage_id = v.storage_id
     WHERE v.complement > 0
     ORDER BY v.produit_id, v.storage_id, l.expiry_date DESC NULLS LAST
),
maj_emplacement AS (
    UPDATE lot_stock_location lsl
       SET qty = lsl.qty + c.complement,
           updated_at = now()
      FROM lot_cible c
     WHERE lsl.lot_id = c.lot_id AND lsl.storage_id = c.storage_id
    RETURNING lsl.lot_id, c.complement
),
maj_lot AS (
    UPDATE lot l
       SET quantity = l.quantity + m.complement,
           current_quantity = coalesce(l.current_quantity, 0) + m.complement,
           updated = now()
      FROM maj_emplacement m
     WHERE l.id = m.lot_id
    RETURNING 1
)
UPDATE stock_produit sp
   SET qty_stock = sp.qty_stock + c.complement,
       qty_virtual = sp.qty_virtual + c.complement,
       updated_at = now()
  FROM lot_cible c
 WHERE sp.produit_id = c.produit_id AND sp.storage_id = c.storage_id;

DO $$
DECLARE
    v_manquants text;
BEGIN
    SELECT string_agg(v.motif, ', ') INTO v_manquants
      FROM (VALUES ('DOLIPRANE 500MG%'), ('DOLIPRANE 1G%'), ('PARACETAMOL 1G%')) AS v(motif)
     WHERE NOT EXISTS (
         SELECT 1
           FROM stock_produit sp
           JOIN produit p ON p.id = sp.produit_id
           JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'PRINCIPAL'
          WHERE p.libelle LIKE v.motif AND sp.qty_stock >= 100
     );
    IF v_manquants IS NOT NULL THEN
        RAISE EXCEPTION 'Produit(s) vedette(s) sans stock vendable : %', v_manquants;
    END IF;
    RAISE NOTICE 'Produits vedettes approvisionnes pour la campagne.';
END $$;
