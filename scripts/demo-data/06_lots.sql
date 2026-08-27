-- ============================================================================
-- 06_lots.sql — Lots, cohortes de péremption et numéros de série FMD
--
-- PIÈGE DE NOMMAGE, vérifié sur la base : les colonnes de prix de « lot »
-- s'appellent « prixachat » et « prixunit », SANS underscore. L'entité déclare
-- les champs sans @Column, Hibernate a simplement mis le camelCase en
-- minuscules. À ne pas confondre avec lot_reception.prix_achat, qui en a un
-- parce que LotReception, lui, déclare @Column(name = "prix_achat").
--
-- Deux chemins de création de lot coexistent dans l'application (§4.10) :
--   * réception en masse   → quantité complète, JAMAIS de numéro de série ;
--   * scan DataMatrix      → une boîte à la fois, numéro de série si présent.
-- Un sérial FMD identifie UNE BOÎTE, pas un lot de fabrication : les lots
-- sérialisés sont donc de très petite quantité.
--
-- Les lot_reception et lot_stock_location sont posés par 07_stock.sql : à la
-- finalisation d'une réception, l'application fait les trois ensemble.
-- ============================================================================

\i _header.sql

\echo '>> 06_lots : lots, péremptions, sérials FMD'

-- ---------------------------------------------------------------------------
-- 1. Lots issus des réceptions
--
-- Un lot par ligne réceptionnée, plus un second lot « scanné » sur une partie
-- des produits sur ordonnance. La somme des quantités d'une ligne est égale à
-- sa quantité reçue : le stock ne peut pas sortir de nulle part.
--
-- Seuls les produits suivis par lot en reçoivent : accessoires et parfumerie
-- (gestion_lot = false) restent hors de ce mécanisme.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_lot_src AS
SELECT
    ol.id                       AS order_line_id,
    ol.commande_order_date,
    ol.quantity_received,
    ol.order_cost_amount,
    ol.order_unit_price,
    c.receipt_date,
    p.id                        AS produit_id,
    p.statut_legal,
    -- Une ligne sur deux portant un produit sur ordonnance reçoit en plus un
    -- lot « scanné » : c'est ce qui alimente la traçabilité FMD.
    (p.statut_legal IN ('LISTE_I', 'LISTE_II', 'STUPEFIANTS', 'PSO')
     AND ol.quantity_received >= 4
     AND ol.id % 2 = 0)         AS avec_scan
FROM order_line ol
JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
JOIN produit p ON p.id = fp.produit_id
WHERE c.order_status IN ('RECEIVED', 'CLOSED')
  AND ol.quantity_received > 0
  AND p.gestion_lot;

CREATE TEMP TABLE tmp_lot AS
SELECT
    s.produit_id,
    s.order_line_id,
    s.commande_order_date,
    s.receipt_date,
    s.order_cost_amount,
    s.order_unit_price,
    v.est_scanne,
    -- Le lot scanné prend 1 à 3 unités, le lot en masse tout le reste.
    CASE WHEN v.est_scanne THEN 1 + (s.order_line_id % 3)
         WHEN s.avec_scan  THEN s.quantity_received - (1 + (s.order_line_id % 3))
         ELSE s.quantity_received END AS quantity,
    s.statut_legal,
    row_number() OVER (PARTITION BY s.produit_id
                       ORDER BY s.receipt_date, s.order_line_id, v.est_scanne) AS rang_produit
FROM tmp_lot_src s
CROSS JOIN LATERAL (
    SELECT false AS est_scanne
    UNION ALL
    SELECT true WHERE s.avec_scan
) v;

-- ---------------------------------------------------------------------------
-- 2. Péremptions
--
-- La date est tirée d'une cohorte, puis BORNÉE à receipt_date + 15 jours : on
-- ne réceptionne pas une marchandise déjà périmée. Le statut est ensuite
-- DÉRIVÉ de la date obtenue, jamais posé indépendamment — c'est ce qui garantit
-- qu'ils ne peuvent pas se contredire.
-- ---------------------------------------------------------------------------
ALTER TABLE tmp_lot ADD COLUMN expiry_date date;
ALTER TABLE tmp_lot ADD COLUMN statut text;

UPDATE tmp_lot t
   SET expiry_date = GREATEST(
        CASE (t.produit_id * 7 + t.rang_produit * 3) % 20
            WHEN 0 THEN CURRENT_DATE - (170 + (t.produit_id % 40))   -- destruction
            WHEN 1 THEN CURRENT_DATE - (10 + (t.produit_id % 130))   -- périmé
            WHEN 2 THEN CURRENT_DATE - (10 + (t.produit_id % 130))
            WHEN 3 THEN CURRENT_DATE + (7 + (t.produit_id % 80))     -- alerte
            WHEN 4 THEN CURRENT_DATE + (7 + (t.produit_id % 80))
            WHEN 5 THEN CURRENT_DATE + (90 + (t.produit_id % 175))   -- vigilance
            WHEN 6 THEN CURRENT_DATE + (90 + (t.produit_id % 175))
            WHEN 7 THEN CURRENT_DATE + (90 + (t.produit_id % 175))
            WHEN 8 THEN CURRENT_DATE + (90 + (t.produit_id % 175))
            ELSE        CURRENT_DATE + (270 + (t.produit_id % 440))  -- sain
        END,
        t.receipt_date + 15
   );

UPDATE tmp_lot
   SET statut = CASE WHEN expiry_date < CURRENT_DATE - 150 THEN 'DESTROYED'
                     WHEN expiry_date < CURRENT_DATE       THEN 'EXPIRED'
                     ELSE 'AVAILABLE' END;

-- ---------------------------------------------------------------------------
-- 3. Lots « historiques », sans commande d'origine
--
-- order_line_id est NULLABLE : un lot peut exister sans rattachement à une
-- réception tracée. Ces lots représentent le stock antérieur à l'historique de
-- démonstration — c'est ce qui permet d'avoir de la marchandise périmée depuis
-- longtemps, que 180 jours de commandes ne peuvent pas produire.
--
-- N'ayant pas de ligne de commande, ils n'auront pas de lot_reception.
-- ---------------------------------------------------------------------------
INSERT INTO tmp_lot (
    produit_id, order_line_id, commande_order_date, receipt_date,
    order_cost_amount, order_unit_price, est_scanne, quantity,
    statut_legal, rang_produit, expiry_date, statut
)
SELECT
    p.id, NULL, NULL, NULL,
    p.cost_amount, p.regular_unit_price, false,
    5 + (p.id % 26),
    p.statut_legal,
    1000 + row_number() OVER (ORDER BY p.id),
    CURRENT_DATE - (160 + (p.id % 200)),
    CASE WHEN CURRENT_DATE - (160 + (p.id % 200)) < CURRENT_DATE - 150
         THEN 'DESTROYED' ELSE 'EXPIRED' END
FROM produit p
WHERE p.gestion_lot
  AND p.id % 11 = 0
LIMIT 60;

-- ---------------------------------------------------------------------------
-- 4. Insertion
--
-- num_lot est unique par produit ; serial_number l'est aussi, via un index
-- unique PARTIEL qui ignore les NULL. Le sérial est dérivé d'un md5 pour être
-- à la fois déterministe et sans collision à l'intérieur d'un produit.
-- ---------------------------------------------------------------------------
INSERT INTO lot (
    num_lot, produit_id, order_line_id, commande_order_date,
    quantity, current_quantity, quantity_received_ug,
    prixachat, prixunit,                    -- ← sans underscore, voir en-tête
    expiry_date, manufacturing_date, statut, serial_number,
    created_date, updated
)
SELECT
    'L' || COALESCE(to_char(t.receipt_date, 'YYMM'), 'HIST')
        || lpad(t.rang_produit::text, 4, '0'),
    t.produit_id,
    t.order_line_id,
    t.commande_order_date,
    t.quantity,
    -- Aucune vente n'a encore eu lieu : le restant égale le reçu.
    t.quantity,
    0,
    t.order_cost_amount,
    t.order_unit_price,
    t.expiry_date,
    -- Fabrication deux ans avant péremption.
    t.expiry_date - 730,
    t.statut,
    -- Sérial FMD : uniquement sur les lots scannés, donc sur ordonnance.
    CASE WHEN t.est_scanne
         THEN 'SN' || upper(substr(md5(t.produit_id::text || '-' || t.rang_produit::text), 1, 18))
         ELSE NULL END,
    COALESCE(t.receipt_date, CURRENT_DATE - 400) + TIME '10:00:00',
    COALESCE(t.receipt_date, CURRENT_DATE - 400) + TIME '10:00:00'
FROM tmp_lot t
WHERE t.quantity > 0
ON CONFLICT (num_lot, produit_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. Alignement de la péremption portée par la ligne de commande
--
-- order_line.date_peremption ne peut porter qu'une valeur, alors qu'une ligne
-- peut avoir plusieurs lots : on retient la plus proche, celle qui commande la
-- sortie en FEFO.
-- ---------------------------------------------------------------------------
UPDATE order_line ol
   SET date_peremption = m.expiry_min
  FROM (
      SELECT order_line_id, commande_order_date, min(expiry_date) AS expiry_min
        FROM lot
       WHERE order_line_id IS NOT NULL
       GROUP BY order_line_id, commande_order_date
  ) m
 WHERE m.order_line_id = ol.id
   AND m.commande_order_date = ol.order_date;

DROP TABLE tmp_lot;
DROP TABLE tmp_lot_src;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_lots      int;
    v_serial    int;
    v_expired   int;
    v_destroyed int;
    v_incoh     int;
    v_avant     int;
BEGIN
    SELECT count(*) INTO v_lots      FROM lot;
    SELECT count(*) INTO v_serial    FROM lot WHERE serial_number IS NOT NULL;
    SELECT count(*) INTO v_expired   FROM lot WHERE statut = 'EXPIRED';
    SELECT count(*) INTO v_destroyed FROM lot WHERE statut = 'DESTROYED';

    -- Le statut est dérivé de la date : ils ne peuvent pas diverger.
    SELECT count(*) INTO v_incoh FROM lot
     WHERE (statut = 'AVAILABLE' AND expiry_date < CURRENT_DATE)
        OR (statut IN ('EXPIRED', 'DESTROYED') AND expiry_date >= CURRENT_DATE);

    -- On ne réceptionne pas une marchandise déjà périmée.
    SELECT count(*) INTO v_avant FROM lot l
      JOIN order_line ol ON ol.id = l.order_line_id AND ol.order_date = l.commande_order_date
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     WHERE l.expiry_date <= c.receipt_date;

    IF v_lots < 800 THEN RAISE EXCEPTION 'Lots : % (attendu >= 800)', v_lots; END IF;
    IF v_serial < 100 THEN RAISE EXCEPTION 'Lots sérialisés : % (attendu >= 100)', v_serial; END IF;
    IF v_expired < 30 THEN RAISE EXCEPTION 'Lots périmés : % (attendu >= 30)', v_expired; END IF;
    IF v_destroyed < 10 THEN RAISE EXCEPTION 'Lots à détruire : % (attendu >= 10)', v_destroyed; END IF;
    IF v_incoh > 0 THEN RAISE EXCEPTION '% lot(s) dont le statut contredit la péremption', v_incoh; END IF;
    IF v_avant > 0 THEN RAISE EXCEPTION '% lot(s) périmé(s) avant leur réception', v_avant; END IF;

    RAISE NOTICE '% lots (% sérialisés, % périmés, % à détruire).',
        v_lots, v_serial, v_expired, v_destroyed;
END $$;

\echo '<< 06_lots : terminé'
