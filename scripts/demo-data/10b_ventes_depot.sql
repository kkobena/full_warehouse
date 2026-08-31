-- ============================================================================
-- 10b_ventes_depot.sql — Magasin dépôt, transferts et retours
--
-- Une « vente dépôt » N'EST PAS une vente : c'est un TRANSFERT facturé.
-- StockUpdateService.updateStockDepot INCRÉMENTE le stock du dépôt pendant que
-- la ligne décrémente celui de l'officine par le chemin normal :
--
--     stock officine (PRINCIPAL du magasin 1)      −= quantité
--     stock dépôt    (PRINCIPAL du magasin dépôt)  += quantité
--
-- C'est un approvisionnement de succursale doublé d'une créance.
--
-- Marqueurs posés par construction (§4.6) :
--     ca = 'CA_DEPOT'                       → hors CA déclaré
--     amount_to_be_taken_into_account = 0   → au niveau de la VENTE seulement,
--                                             les lignes gardent le leur
--     payment_status = 'IMPAYE'             → toujours, aucun règlement
--     customer_id = NULL
--
-- Le magasin dépôt a besoin de SON PROPRE storage PRINCIPAL :
-- Magasin.primaryStorage est une @JoinFormula filtrant sur magasin_id, sans
-- quoi elle renvoie null et la finalisation échoue.
--
-- Le stock du dépôt est SANS LOT : updateStockDepot ne touche ni lot ni
-- lot_stock_location. L'invariant « stock = somme des emplacements » ne
-- s'applique donc pas à ses stockages.
-- ============================================================================

\i _header.sql

\echo '>> 10b_ventes_depot : magasin dépôt, transferts, retours'

-- ---------------------------------------------------------------------------
-- 1. Le magasin dépôt et son stockage
--
-- magasin.name et full_name sont UNIQUES : le dépôt ne peut pas réutiliser
-- ceux de l'officine.
-- ---------------------------------------------------------------------------
INSERT INTO magasin (
    name, full_name, type_magasin, address, phone, email,
    note, welcome_message, registre, compte_contribuable
)
VALUES (
    'PHARMA SMART DEPOT',
    'PHARMA SMART — DEPOT DE YOPOUGON',
    'DEPOT',
    'Siporex, Yopougon, Abidjan',
    '27 23 45 12 00',
    'depot@pharma-smart.example',
    'Dépôt extension approvisionné par l''officine',
    'Bienvenue au dépôt',
    '', ''
)
ON CONFLICT (name) DO NOTHING;

INSERT INTO storage (name, storage_type, magasin_id)
SELECT 'Stock dépôt', 'PRINCIPAL', m.id
FROM magasin m WHERE m.type_magasin = 'DEPOT'
ON CONFLICT (storage_type, magasin_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. En-têtes des ventes dépôt (150 sur 180 jours)
--
-- Deux à trois transferts par semaine, aux jours d'ouverture de l'officine.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_vd AS
SELECT
    nextval('id_sale_seq') AS id,
    d.jour AS sale_date,
    row_number() OVER (ORDER BY d.jour) AS rang,
    -- Le transfert part à 07 h 30, avant l'ouverture. Pour la journée en cours,
    -- borné à l'heure du chargement : un mouvement daté dans le futur serait
    -- rejeté par 99_verification.sql, et incompréhensible à l'écran.
    LEAST(d.jour + TIME '07:30:00', date_trunc('minute', LOCALTIMESTAMP)) AS moment,
    cr.id AS cash_register_id,
    cr.user_id AS caissier_id,
    (SELECT id FROM magasin WHERE type_magasin = 'DEPOT') AS depot_id,
    (SELECT s.id FROM storage s
       JOIN magasin m ON m.id = s.magasin_id AND m.type_magasin = 'DEPOT'
      WHERE s.storage_type = 'PRINCIPAL' LIMIT 1) AS depot_storage_id
FROM (
    SELECT (CURRENT_DATE - (INTERVAL '1 day' * j))::date AS jour
      -- Depuis 0 : la journée du chargement doit figurer dans l'historique des
      -- transferts, au même titre que dans celui des ventes (09_ventes.sql).
      -- 0 % 3 = 0, elle est donc retenue par le filtre ci-dessous.
      FROM generate_series(0, 180) AS j
     -- Lundi fermé, comme partout ailleurs dans le jeu.
     WHERE extract(dow FROM (CURRENT_DATE - (INTERVAL '1 day' * j))) <> 1
       AND j % 3 = 0
) d
JOIN cash_register cr ON cr.begin_time::date = d.jour;

-- ---------------------------------------------------------------------------
-- 3. Lignes de transfert
--
-- Le stock disponible est celui qui RESTE après les ventes de l'étape 5 :
-- l'officine ne peut transférer que ce qu'elle a encore.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_vd_dispo AS
SELECT
    l.produit_id,
    sum(lsl.qty)::int AS reste,
    row_number() OVER (ORDER BY l.produit_id) AS rang,
    count(*) OVER () AS total
FROM lot l
JOIN lot_stock_location lsl ON lsl.lot_id = l.id
JOIN storage s ON s.id = lsl.storage_id AND s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1
WHERE l.statut = 'AVAILABLE'
  AND l.expiry_date > CURRENT_DATE + 90
GROUP BY l.produit_id
HAVING sum(lsl.qty) >= 20;

CREATE TEMP TABLE tmp_vd_ligne AS
SELECT DISTINCT ON (v.id, e.produit_id)
    nextval('id_sale_item_seq') AS id,
    v.id AS sales_id,
    v.sale_date,
    v.moment,
    e.produit_id,
    -- Transferts par petits lots, plafonnés au quart du reste.
    LEAST(GREATEST(2, e.reste / 8), 15) AS quantity
FROM tmp_vd v
CROSS JOIN LATERAL generate_series(1, 3 + (v.rang % 4)) AS n
JOIN tmp_vd_dispo e ON e.rang = 1 + ((v.rang * 11 + n * 173) % e.total)
ORDER BY v.id, e.produit_id;

-- Écrêtage : un produit ne peut pas être transféré au-delà de ce qui reste.
DELETE FROM tmp_vd_ligne t
USING (
    SELECT c.sales_id, c.produit_id
      FROM (
          SELECT sales_id, produit_id,
                 sum(quantity) OVER (PARTITION BY produit_id
                                     ORDER BY sale_date, sales_id
                                     ROWS UNBOUNDED PRECEDING) AS cumul
            FROM tmp_vd_ligne
      ) c
      JOIN tmp_vd_dispo d ON d.produit_id = c.produit_id
     WHERE c.cumul > d.reste / 2          -- au plus la moitié du reste
) trop
WHERE t.sales_id = trop.sales_id AND t.produit_id = trop.produit_id;

DELETE FROM tmp_vd v
WHERE NOT EXISTS (SELECT 1 FROM tmp_vd_ligne l WHERE l.sales_id = v.id);

-- ---------------------------------------------------------------------------
-- 4. Montants
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_vd_calc AS
SELECT
    l.*, p.regular_unit_price, p.cost_amount, t.taux AS tax_value,
    (l.quantity * p.regular_unit_price)::int AS sales_amount,
    CASE WHEN t.taux = 0 THEN (l.quantity * p.regular_unit_price)::int
         ELSE ceil((l.quantity * p.regular_unit_price)::numeric
                   / (1 + t.taux / 100.0))::int END AS ht_amount
FROM tmp_vd_ligne l
JOIN produit p ON p.id = l.produit_id
JOIN tva t ON t.id = p.tva_id;

-- ---------------------------------------------------------------------------
-- 5. Insertion des ventes dépôt
-- ---------------------------------------------------------------------------
INSERT INTO sales (
    dtype, id, sale_date, number_transaction,
    sales_amount, ht_amount, tax_amount, cost_amount, net_amount,
    discount_amount, amount_to_be_paid, payroll_amount, rest_to_pay, monnaie,
    amount_to_be_taken_into_account,
    statut, payment_status, nature_vente, origine_vente, type_prescription, ca,
    differe, canceled, copy, imported, to_ignore,
    magasin_id, user_id, seller_id, caissier_id, customer_id,
    cash_register_id, depot_id,
    created_at, updated_at, effective_update_date
)
SELECT
    'VenteDepot', v.id, v.sale_date,
    to_char(v.sale_date, 'YYYYMMDD') || lpad((900 + v.rang % 99)::text, 3, '0'),
    a.sales_amount, a.ht_amount, a.sales_amount - a.ht_amount, a.cost_amount,
    a.sales_amount,
    0,
    a.sales_amount,        -- montant dû par le dépôt
    0,                     -- rien n'est encaissé
    a.sales_amount,        -- rest_to_pay = amount_to_be_paid, posé en dur
    0,
    0,                     -- HORS CA DÉCLARÉ, au niveau de la vente
    'CLOSED',
    'IMPAYE',              -- toujours, par construction
    -- SaleDepotExtensionImpl:115 pose ASSURANCE avec un « TODO a supprimer ».
    -- Incohérent sémantiquement, mais c'est le comportement réel : le jeu de
    -- données l'imite pour ressembler à une base de production.
    'ASSURANCE',
    'DIRECT', 'DEPOT', 'CA_DEPOT',
    false, false, false, false, false,
    1, v.caissier_id, v.caissier_id, v.caissier_id, NULL,
    v.cash_register_id, v.depot_id,
    v.moment, v.moment, v.moment
FROM tmp_vd v
JOIN (
    SELECT sales_id,
           sum(sales_amount)::int           AS sales_amount,
           sum(ht_amount)::int              AS ht_amount,
           sum(quantity * cost_amount)::int AS cost_amount
      FROM tmp_vd_calc GROUP BY sales_id
) a ON a.sales_id = v.id;

-- Les lignes gardent leur montant déclarable, alors que la vente est à zéro :
-- c'est précisément pourquoi le contrôle V2 de AuditDeclarationCaService écarte
-- les ventes dépôt. Ne pas « corriger » cette asymétrie.
INSERT INTO sales_line (
    id, sale_date, sales_id, sales_sale_date, produit_id,
    quantity_requested, quantity_sold, quantity_ug, quantity_avoir,
    regular_unit_price, net_unit_price, discount_unit_price,
    discount_amount, sales_amount, tax_value, cost_amount,
    amount_to_be_taken_into_account, taux_remise, to_ignore,
    lots, rates, created_at, updated_at, effective_update_date
)
SELECT
    l.id, l.sale_date, l.sales_id, l.sale_date, l.produit_id,
    l.quantity, l.quantity, 0, 0,
    l.regular_unit_price, l.regular_unit_price, 0,
    0, l.sales_amount, l.tax_value, l.cost_amount,
    l.sales_amount, 0, false,
    '[]'::jsonb, '[]'::jsonb,
    l.moment, l.moment, l.moment
FROM tmp_vd_calc l;

-- ---------------------------------------------------------------------------
-- 6. Mouvement de stock : sortie officine, entrée dépôt
-- ---------------------------------------------------------------------------
-- Sortie officine : consommation FEFO des lots restants, même technique
-- d'allocation par recouvrement d'intervalles qu'à l'étape 5.
CREATE TEMP TABLE tmp_vd_lot AS
SELECT
    l.produit_id, l.id AS lot_id, l.num_lot, l.expiry_date,
    sum(lsl.qty) OVER (PARTITION BY l.produit_id
                       ORDER BY l.expiry_date, l.id ROWS UNBOUNDED PRECEDING) - lsl.qty AS deb,
    sum(lsl.qty) OVER (PARTITION BY l.produit_id
                       ORDER BY l.expiry_date, l.id ROWS UNBOUNDED PRECEDING)           AS fin
FROM lot l
JOIN lot_stock_location lsl ON lsl.lot_id = l.id
JOIN storage s ON s.id = lsl.storage_id AND s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1
WHERE l.statut = 'AVAILABLE' AND l.expiry_date > CURRENT_DATE + 90;

CREATE TEMP TABLE tmp_vd_pos AS
SELECT
    l.id, l.produit_id, l.quantity,
    sum(l.quantity) OVER (PARTITION BY l.produit_id
                          ORDER BY l.sale_date, l.sales_id, l.id
                          ROWS UNBOUNDED PRECEDING) - l.quantity AS deb,
    sum(l.quantity) OVER (PARTITION BY l.produit_id
                          ORDER BY l.sale_date, l.sales_id, l.id
                          ROWS UNBOUNDED PRECEDING)              AS fin
FROM tmp_vd_calc l;

CREATE TEMP TABLE tmp_vd_alloc AS
SELECT p.id AS sales_line_id, d.lot_id, d.num_lot, d.expiry_date,
       (LEAST(p.fin, d.fin) - GREATEST(p.deb, d.deb))::int AS qty_prise
FROM tmp_vd_pos p
JOIN tmp_vd_lot d ON d.produit_id = p.produit_id AND p.deb < d.fin AND d.deb < p.fin;

UPDATE sales_line sl
   SET lots = a.lots
  FROM (
      SELECT sales_line_id,
             jsonb_agg(jsonb_build_object(
                 'id', lot_id, 'numLot', num_lot,
                 'quantity', qty_prise, 'expiryDate', expiry_date
             ) ORDER BY expiry_date, lot_id) AS lots
        FROM tmp_vd_alloc WHERE qty_prise > 0 GROUP BY sales_line_id
  ) a
 WHERE sl.id = a.sales_line_id;

UPDATE lot_stock_location lsl
   SET qty = lsl.qty - c.qte, updated_at = NOW()
  FROM (SELECT lot_id, sum(qty_prise)::int AS qte FROM tmp_vd_alloc
         WHERE qty_prise > 0 GROUP BY lot_id) c,
       storage s
 WHERE lsl.lot_id = c.lot_id
   AND s.id = lsl.storage_id AND s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1;

UPDATE lot l
   SET current_quantity = l.current_quantity - c.qte, updated = NOW()
  FROM (SELECT lot_id, sum(qty_prise)::int AS qte FROM tmp_vd_alloc
         WHERE qty_prise > 0 GROUP BY lot_id) c
 WHERE l.id = c.lot_id;

UPDATE lot SET statut = 'SOLD' WHERE current_quantity <= 0 AND statut = 'AVAILABLE';
DELETE FROM lot_stock_location WHERE qty <= 0;

UPDATE stock_produit sp
   SET qty_stock = sp.qty_stock - v.qte,
       qty_virtual = sp.qty_stock - v.qte,
       updated_at = NOW()
  FROM (SELECT produit_id, sum(quantity)::int AS qte FROM tmp_vd_calc GROUP BY produit_id) v,
       storage s
 WHERE sp.produit_id = v.produit_id
   AND s.id = sp.storage_id AND s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1;

-- Entrée dépôt : incrément direct, SANS lot (updateStockDepot n'en crée pas).
INSERT INTO stock_produit (
    produit_id, storage_id, qty_stock, qty_virtual, qty_ug,
    seuil_mini, stock_maxi, stock_reassort,
    version, last_modified_by, created_at, updated_at
)
SELECT
    v.produit_id, d.storage_id, v.qte, v.qte, 0,
    GREATEST(2, v.qte / 6), GREATEST(10, v.qte * 2), 0,
    0, 'system', NOW() - INTERVAL '180 days', NOW()
FROM (SELECT produit_id, sum(quantity)::int AS qte FROM tmp_vd_calc GROUP BY produit_id) v
CROSS JOIN LATERAL (
    SELECT s.id AS storage_id FROM storage s
      JOIN magasin m ON m.id = s.magasin_id AND m.type_magasin = 'DEPOT'
     WHERE s.storage_type = 'PRINCIPAL' LIMIT 1
) d
ON CONFLICT (storage_id, produit_id) DO UPDATE
   SET qty_stock = stock_produit.qty_stock + EXCLUDED.qty_stock,
       qty_virtual = stock_produit.qty_virtual + EXCLUDED.qty_virtual;

-- ---------------------------------------------------------------------------
-- 7. Retours de dépôt — le mouvement inverse
--
-- Le lien vers la vente d'origine est FACULTATIF : un retour peut être hors
-- vente. Le jeu de données représente les deux cas.
-- ---------------------------------------------------------------------------
INSERT INTO retour_depot (date_mtv, user_id, depot_id, vente_depot_id, vente_depot_date)
SELECT
    v.moment + INTERVAL '20 days',
    v.caissier_id,
    v.depot_id,
    -- Un retour sur trois est hors vente.
    CASE WHEN v.rang % 3 = 0 THEN NULL ELSE v.id END,
    CASE WHEN v.rang % 3 = 0 THEN NULL ELSE v.sale_date END
FROM tmp_vd v
WHERE v.rang % 7 = 0
  AND v.moment + INTERVAL '20 days' < NOW();

-- qty_mvt est contraint à >= 1 : on ne retourne jamais zéro.
INSERT INTO retour_depot_item (retour_depot_id, produit_id, qty_mvt, init_stock, after_stock, regular_unit_price)
SELECT
    rd.id, l.produit_id,
    GREATEST(1, l.quantity / 3),
    l.quantity,
    l.quantity - GREATEST(1, l.quantity / 3),
    l.regular_unit_price
FROM retour_depot rd
JOIN tmp_vd_calc l ON l.sales_id = rd.vente_depot_id AND l.sale_date = rd.vente_depot_date
WHERE rd.vente_depot_id IS NOT NULL;

-- Le retour réintègre l'officine et sort du dépôt.
UPDATE stock_produit sp
   SET qty_stock = sp.qty_stock - r.qte, qty_virtual = sp.qty_stock - r.qte, updated_at = NOW()
  FROM (SELECT i.produit_id, sum(i.qty_mvt)::int AS qte
          FROM retour_depot_item i GROUP BY i.produit_id) r,
       storage s, magasin m
 WHERE sp.produit_id = r.produit_id
   AND s.id = sp.storage_id AND m.id = s.magasin_id AND m.type_magasin = 'DEPOT';

UPDATE stock_produit sp
   SET qty_stock = sp.qty_stock + r.qte, qty_virtual = sp.qty_stock + r.qte, updated_at = NOW()
  FROM (SELECT i.produit_id, sum(i.qty_mvt)::int AS qte
          FROM retour_depot_item i GROUP BY i.produit_id) r,
       storage s
 WHERE sp.produit_id = r.produit_id
   AND s.id = sp.storage_id AND s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1;

DROP TABLE tmp_vd_alloc;
DROP TABLE tmp_vd_pos;
DROP TABLE tmp_vd_lot;
DROP TABLE tmp_vd_calc;
DROP TABLE tmp_vd_ligne;
DROP TABLE tmp_vd_dispo;
DROP TABLE tmp_vd;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_depot int; v_ventes int; v_retours int;
    v_marqueurs int; v_paie int; v_lot_depot int; v_neg int;
BEGIN
    SELECT count(*) INTO v_depot   FROM magasin WHERE type_magasin = 'DEPOT';
    SELECT count(*) INTO v_ventes  FROM sales WHERE dtype = 'VenteDepot';
    SELECT count(*) INTO v_retours FROM retour_depot;

    SELECT count(*) INTO v_marqueurs FROM sales
     WHERE dtype = 'VenteDepot'
       AND (ca <> 'CA_DEPOT' OR amount_to_be_taken_into_account <> 0
         OR payment_status <> 'IMPAYE' OR rest_to_pay <> amount_to_be_paid
         OR customer_id IS NOT NULL);

    -- Une vente dépôt reste due : elle n'a aucun règlement.
    SELECT count(*) INTO v_paie FROM payment_transaction pt
      JOIN sales s ON s.id = pt.sale_id AND s.sale_date = pt.sale_date
     WHERE s.dtype = 'VenteDepot';

    -- Le stock du dépôt est sans lot.
    SELECT count(*) INTO v_lot_depot FROM lot_stock_location lsl
      JOIN storage s ON s.id = lsl.storage_id
      JOIN magasin m ON m.id = s.magasin_id
     WHERE m.type_magasin = 'DEPOT';

    SELECT count(*) INTO v_neg FROM stock_produit WHERE qty_stock < 0;

    IF v_depot <> 1 THEN RAISE EXCEPTION 'Magasins dépôt : % (attendu 1)', v_depot; END IF;
    IF v_ventes < 30 THEN RAISE EXCEPTION 'Ventes dépôt : % (attendu >= 30)', v_ventes; END IF;
    IF v_retours < 3 THEN RAISE EXCEPTION 'Retours dépôt : % (attendu >= 3)', v_retours; END IF;
    IF v_marqueurs > 0 THEN RAISE EXCEPTION '% vente(s) dépôt aux marqueurs incorrects', v_marqueurs; END IF;
    IF v_paie > 0 THEN RAISE EXCEPTION '% règlement(s) sur une vente dépôt', v_paie; END IF;
    IF v_lot_depot > 0 THEN RAISE EXCEPTION '% emplacement(s) de lot sur un stockage dépôt', v_lot_depot; END IF;
    IF v_neg > 0 THEN RAISE EXCEPTION '% ligne(s) de stock négative(s)', v_neg; END IF;

    RAISE NOTICE '% ventes dépôt, % retours.', v_ventes, v_retours;
END $$;

\echo '<< 10b_ventes_depot : terminé'
