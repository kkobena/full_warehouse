-- ============================================================================
-- 17c_reassort.sql — Produits favoris à deux stocks, et suggestions de réassort
--
-- Deux types de suggestion (SuggestionReassortServiceImpl) :
--
--   RAYON    le rayon est sous son seuil et la réserve a de quoi le remplir.
--            Ligne : stock_produit_id = ligne de stock du RAYON (destination),
--                    stock_src_produit_id = celle de la RÉSERVE,
--                    quantité = min(stock_reassort du rayon, stock de la réserve).
--
--   RESERVE  le rayon déborde de son maximum et la réserve est sous son seuil.
--            Ligne : stock_produit_id = ligne de la RÉSERVE (destination),
--                    stock_src_produit_id = celle du RAYON,
--                    quantité = stock du rayon - stock_maxi du rayon.
--
-- Une ligne n'a de sens que si le produit existe dans DEUX stockages. Les produits
-- favoris du comptoir (17b) en ont donc un au rayon ET un en réserve, sans quoi la
-- proposition de réassort n'a rien à proposer : l'un des favoris est même à zéro au
-- rayon avec de la marchandise en réserve — le cas que la suggestion RAYON résout.
--
-- Une suggestion OPEN par type et par magasin (c'est ce que cherche
-- findOneByStatutAndMagasinIdAndTypeReassort), plus un historique de suggestions
-- CLOSED (validées par le pharmacien) étalé sur toute la période.
--
-- La comptabilité à deux niveaux reste respectée : toute marchandise ajoutée en réserve
-- l'est sur un LOT et sur son emplacement, jamais sur le seul compteur de stock.
-- ============================================================================

\i _header.sql

\echo '>> 17c_reassort : favoris à deux stocks, suggestions de réassort'

-- ---------------------------------------------------------------------------
-- 1. Favoris : une ligne de stock en réserve, approvisionnée
-- ---------------------------------------------------------------------------
SELECT to_regclass('produit_favori') IS NOT NULL AS table_favoris \gset
\if :table_favoris

CREATE TEMP TABLE tmp_rs_favori AS
SELECT f.produit_id,
       p.gestion_lot, p.cost_amount, p.regular_unit_price,
       (SELECT id FROM storage WHERE storage_type = 'PRINCIPAL'    AND magasin_id = 1 LIMIT 1) AS rayon_id,
       (SELECT id FROM storage WHERE storage_type = 'SAFETY_STOCK' AND magasin_id = 1 LIMIT 1) AS reserve_id,
       20 + (f.produit_id % 15) AS qte_reserve
  FROM produit_favori f
  JOIN produit p ON p.id = f.produit_id;

-- La ligne de stock de la réserve, si elle manque.
INSERT INTO stock_produit (produit_id, storage_id, qty_stock, qty_virtual, qty_ug,
                           seuil_mini, stock_maxi, stock_reassort, version,
                           last_modified_by, created_at, updated_at)
SELECT t.produit_id, t.reserve_id, 0, 0, 0, 0, 0, 0, 0, 'system',
       NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 40)), NOW()
  FROM tmp_rs_favori t
 WHERE NOT EXISTS (SELECT 1 FROM stock_produit x
                    WHERE x.produit_id = t.produit_id AND x.storage_id = t.reserve_id);

-- Un lot reçu directement en réserve pour les favoris dont la réserve est vide, quand le
-- produit est suivi par lot ; un simple complément de stock sinon.
INSERT INTO lot (num_lot, produit_id, quantity, current_quantity, quantity_received_ug,
                 prixachat, prixunit, expiry_date, manufacturing_date, statut,
                 created_date, updated)
SELECT 'LRES' || lpad(t.produit_id::text, 5, '0'), t.produit_id,
       -- Reçu plus que ce qu'il en reste : le rayon a déjà écoulé sa part. Sans cet écart, un lot
       -- entièrement en réserve ressemblerait à une entrée directe, que l'application interdit.
       t.qte_reserve + 6, t.qte_reserve, 0,
       t.cost_amount, t.regular_unit_price,
       CURRENT_DATE + (400 + t.produit_id % 300),
       CURRENT_DATE + (400 + t.produit_id % 300) - 730,
       'AVAILABLE', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'
  FROM tmp_rs_favori t
  JOIN stock_produit r ON r.produit_id = t.produit_id AND r.storage_id = t.reserve_id
 WHERE t.gestion_lot AND r.qty_stock = 0
ON CONFLICT (num_lot, produit_id) DO NOTHING;

INSERT INTO lot_stock_location (lot_id, storage_id, qty, updated_at)
SELECT l.id, t.reserve_id, l.current_quantity, NOW()
  FROM tmp_rs_favori t
  JOIN lot l ON l.produit_id = t.produit_id
            AND l.num_lot = 'LRES' || lpad(t.produit_id::text, 5, '0')
ON CONFLICT (lot_id, storage_id) DO NOTHING;

UPDATE stock_produit r
   SET qty_stock = r.qty_stock + t.qte_reserve,
       qty_virtual = r.qty_virtual + t.qte_reserve,
       updated_at = NOW()
  FROM tmp_rs_favori t
 WHERE r.produit_id = t.produit_id AND r.storage_id = t.reserve_id
   AND r.qty_stock = 0;

DROP TABLE tmp_rs_favori;
\endif

-- ---------------------------------------------------------------------------
-- 2. Quantité de réassort du rayon
--
-- Tout produit présent dans les deux stockages a une quantité à réassortir en rayon :
-- sans elle, createRayonSuggestionReassort s'arrête d'emblée.
-- ---------------------------------------------------------------------------
UPDATE stock_produit ray
   SET stock_reassort = GREATEST(6, COALESCE(ray.seuil_mini, 0)),
       updated_at = NOW()
  FROM storage sr, stock_produit res, storage ss
 WHERE sr.id = ray.storage_id AND sr.storage_type = 'PRINCIPAL' AND sr.magasin_id = 1
   AND res.produit_id = ray.produit_id
   AND ss.id = res.storage_id AND ss.storage_type = 'SAFETY_STOCK' AND ss.magasin_id = 1
   AND res.qty_stock > 0;

-- ---------------------------------------------------------------------------
-- 3. Suggestion RAYON ouverte
--
-- Les produits dont le rayon est sous le seuil alors que la réserve est garnie. Les favoris
-- d'abord (dont celui qui est à zéro au rayon), puis des produits ordinaires.
-- Le seuil du rayon est relevé juste au-dessus du stock actuel : c'est ce qui le met « sous
-- seuil » sans toucher à une quantité que les lots justifient.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_rs_rayon AS
SELECT ray.id AS dest_id, res.id AS src_id, ray.produit_id, ray.qty_stock AS rayon_qty,
       res.qty_stock AS reserve_qty, ray.stock_reassort,
       row_number() OVER (ORDER BY (fav.produit_id IS NULL), ray.qty_stock, ray.produit_id) AS rang
  FROM stock_produit ray
  JOIN storage sr ON sr.id = ray.storage_id AND sr.storage_type = 'PRINCIPAL' AND sr.magasin_id = 1
  JOIN stock_produit res ON res.produit_id = ray.produit_id
  JOIN storage ss ON ss.id = res.storage_id AND ss.storage_type = 'SAFETY_STOCK' AND ss.magasin_id = 1
  JOIN produit p ON p.id = ray.produit_id AND p.status = 'ENABLE'
  LEFT JOIN (SELECT produit_id FROM produit_favori) fav ON fav.produit_id = ray.produit_id
 WHERE res.qty_stock > 0
   AND ray.qty_stock <= 40;

DELETE FROM tmp_rs_rayon WHERE rang > 14;

UPDATE stock_produit ray
   SET seuil_mini = GREATEST(COALESCE(ray.seuil_mini, 0), ray.qty_stock + 4),
       updated_at = NOW()
  FROM tmp_rs_rayon t
 WHERE ray.id = t.dest_id;

UPDATE produit p
   SET qty_seuil_mini = GREATEST(COALESCE(p.qty_seuil_mini, 0), t.rayon_qty + 4)
  FROM tmp_rs_rayon t
 WHERE p.id = t.produit_id;

INSERT INTO suggestion_reassort (reference, created_at, updated_at, magasin_id,
                                 last_user_edit_id, type_reassort, statut)
SELECT to_char(CURRENT_DATE, 'YYYYMMDD') || '0001',
       NOW() - INTERVAL '3 hours', NOW() - INTERVAL '1 hour', 1,
       (SELECT id FROM app_user WHERE login = 'admin'), 'RAYON', 'OPEN';

INSERT INTO ligne_reassort (quantity, updated_at, reassort_id, stock_produit_id, stock_src_produit_id)
SELECT LEAST(t.stock_reassort, t.reserve_qty), NOW() - INTERVAL '1 hour',
       (SELECT id FROM suggestion_reassort WHERE type_reassort = 'RAYON' AND statut = 'OPEN'),
       t.dest_id, t.src_id
  FROM tmp_rs_rayon t;

DROP TABLE tmp_rs_rayon;

-- ---------------------------------------------------------------------------
-- 4. Suggestion RESERVE ouverte
--
-- Le rayon déborde de son maximum, la réserve est sous son seuil. On y prend des produits
-- qui n'appartiennent à aucun bon en cours de réception : le rangement de ces bons-là
-- (ACH-46, ACH-75) a ses propres débordements, que ce jeu laisse intacts.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_rs_reserve AS
SELECT ray.id AS src_id, res.id AS dest_id, ray.produit_id, ray.qty_stock AS rayon_qty,
       res.qty_stock AS reserve_qty,
       row_number() OVER (ORDER BY ray.qty_stock DESC, ray.produit_id) AS rang
  FROM stock_produit ray
  JOIN storage sr ON sr.id = ray.storage_id AND sr.storage_type = 'PRINCIPAL' AND sr.magasin_id = 1
  JOIN stock_produit res ON res.produit_id = ray.produit_id
  JOIN storage ss ON ss.id = res.storage_id AND ss.storage_type = 'SAFETY_STOCK' AND ss.magasin_id = 1
  JOIN produit p ON p.id = ray.produit_id AND p.status = 'ENABLE'
 WHERE ray.qty_stock >= 12
   AND res.qty_stock > 0
   AND NOT EXISTS (SELECT 1 FROM ligne_reassort lr WHERE lr.stock_produit_id IN (ray.id, res.id))
   AND NOT EXISTS (SELECT 1 FROM order_line ol
                     JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
                     JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
                    WHERE fp.produit_id = ray.produit_id AND c.order_status = 'RECEIVED');

DELETE FROM tmp_rs_reserve WHERE rang > 10;

-- Le maximum du rayon passe aux deux tiers du stock : il déborde ; la réserve est relevée
-- au-dessus de son stock : elle est sous son seuil.
UPDATE stock_produit ray
   SET stock_maxi = GREATEST(1, (ray.qty_stock * 2) / 3),
       seuil_mini = LEAST(COALESCE(NULLIF(ray.seuil_mini, 0), 5), ray.qty_stock - 1),
       updated_at = NOW()
  FROM tmp_rs_reserve t
 WHERE ray.id = t.src_id;

UPDATE stock_produit res
   SET seuil_mini = res.qty_stock + 8,
       updated_at = NOW()
  FROM tmp_rs_reserve t
 WHERE res.id = t.dest_id;

INSERT INTO suggestion_reassort (reference, created_at, updated_at, magasin_id,
                                 last_user_edit_id, type_reassort, statut)
SELECT to_char(CURRENT_DATE, 'YYYYMMDD') || '0002',
       NOW() - INTERVAL '2 hours', NOW() - INTERVAL '30 minutes', 1,
       (SELECT id FROM app_user WHERE login = 'admin'), 'RESERVE', 'OPEN';

INSERT INTO ligne_reassort (quantity, updated_at, reassort_id, stock_produit_id, stock_src_produit_id)
SELECT t.rayon_qty - GREATEST(1, (t.rayon_qty * 2) / 3), NOW() - INTERVAL '30 minutes',
       (SELECT id FROM suggestion_reassort WHERE type_reassort = 'RESERVE' AND statut = 'OPEN'),
       t.dest_id, t.src_id
  FROM tmp_rs_reserve t;

DROP TABLE tmp_rs_reserve;

-- ---------------------------------------------------------------------------
-- 5. Historique : suggestions validées (CLOSED)
--
-- Une par quinzaine environ sur toute la période, alternant RAYON et RESERVE, de deux à
-- six lignes chacune, sur des produits qui ont une ligne de réserve.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_rs_hist AS
SELECT g.n,
       (CURRENT_DATE - (21 + (g.n * (pg_temp.horizon() - 30)) / 70))::date AS jour,
       CASE WHEN g.n % 2 = 0 THEN 'RAYON' ELSE 'RESERVE' END AS type_reassort
  FROM generate_series(0, 69) g(n);

INSERT INTO suggestion_reassort (reference, created_at, updated_at, magasin_id,
                                 last_user_edit_id, type_reassort, statut)
SELECT to_char(h.jour, 'YYYYMMDD') || lpad((1 + h.n % 3)::text, 4, '0'),
       h.jour + TIME '07:45:00', h.jour + TIME '09:10:00', 1,
       (SELECT id FROM app_user WHERE login = CASE WHEN h.n % 3 = 0 THEN 'rkouassi' ELSE 'admin' END),
       h.type_reassort, 'CLOSED'
  FROM tmp_rs_hist h;

CREATE TEMP TABLE tmp_rs_paire AS
SELECT ray.id AS ray_id, res.id AS res_id, ray.produit_id,
       row_number() OVER (ORDER BY ray.produit_id) AS rang,
       count(*) OVER () AS total
  FROM stock_produit ray
  JOIN storage sr ON sr.id = ray.storage_id AND sr.storage_type = 'PRINCIPAL' AND sr.magasin_id = 1
  JOIN stock_produit res ON res.produit_id = ray.produit_id
  JOIN storage ss ON ss.id = res.storage_id AND ss.storage_type = 'SAFETY_STOCK' AND ss.magasin_id = 1
 WHERE res.qty_stock > 0;

INSERT INTO ligne_reassort (quantity, updated_at, reassort_id, stock_produit_id, stock_src_produit_id)
SELECT DISTINCT ON (s.id, CASE WHEN h.type_reassort = 'RAYON' THEN p.ray_id ELSE p.res_id END)
       3 + (h.n * 5 + k * 7) % 18,
       s.updated_at,
       s.id,
       CASE WHEN h.type_reassort = 'RAYON' THEN p.ray_id ELSE p.res_id END,
       CASE WHEN h.type_reassort = 'RAYON' THEN p.res_id ELSE p.ray_id END
  FROM tmp_rs_hist h
  JOIN suggestion_reassort s ON s.statut = 'CLOSED'
                            AND s.type_reassort = h.type_reassort
                            AND s.created_at = h.jour + TIME '07:45:00'
  CROSS JOIN LATERAL generate_series(1, 2 + h.n % 5) AS k
  JOIN tmp_rs_paire p ON p.rang = 1 + ((h.n * 13 + k * 29) % p.total);

DROP TABLE tmp_rs_paire;
DROP TABLE tmp_rs_hist;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_open_rayon int; v_open_reserve int; v_closed int; v_lignes int;
    v_orient int; v_qte int; v_fav int; v_ecart int; v_ref int;
BEGIN
    SELECT count(*) INTO v_open_rayon FROM suggestion_reassort
     WHERE statut = 'OPEN' AND type_reassort = 'RAYON';
    SELECT count(*) INTO v_open_reserve FROM suggestion_reassort
     WHERE statut = 'OPEN' AND type_reassort = 'RESERVE';
    SELECT count(*) INTO v_closed FROM suggestion_reassort WHERE statut = 'CLOSED';
    SELECT count(*) INTO v_lignes FROM ligne_reassort;

    -- Source et destination dans les deux stockages voulus, pour un même produit.
    SELECT count(*) INTO v_orient
      FROM ligne_reassort lr
      JOIN suggestion_reassort s ON s.id = lr.reassort_id
      JOIN stock_produit d  ON d.id = lr.stock_produit_id
      JOIN storage sd ON sd.id = d.storage_id
      JOIN stock_produit o  ON o.id = lr.stock_src_produit_id
      JOIN storage so ON so.id = o.storage_id
     WHERE d.produit_id <> o.produit_id
        OR (s.type_reassort = 'RAYON'   AND NOT (sd.storage_type = 'PRINCIPAL'    AND so.storage_type = 'SAFETY_STOCK'))
        OR (s.type_reassort = 'RESERVE' AND NOT (sd.storage_type = 'SAFETY_STOCK' AND so.storage_type = 'PRINCIPAL'));

    SELECT count(*) INTO v_qte FROM ligne_reassort WHERE quantity IS NULL OR quantity <= 0;

    -- Chaque favori doit être stocké dans les deux stockages, avec de la marchandise en réserve.
    IF to_regclass('produit_favori') IS NOT NULL THEN
        SELECT count(*) INTO v_fav FROM produit_favori f
         WHERE NOT EXISTS (SELECT 1 FROM stock_produit r JOIN storage s ON s.id = r.storage_id
                            WHERE r.produit_id = f.produit_id AND s.storage_type = 'SAFETY_STOCK'
                              AND r.qty_stock > 0)
            OR NOT EXISTS (SELECT 1 FROM stock_produit r JOIN storage s ON s.id = r.storage_id
                            WHERE r.produit_id = f.produit_id AND s.storage_type = 'PRINCIPAL');
    ELSE
        v_fav := 0;
    END IF;

    -- Comptabilité à deux niveaux, y compris pour les lots reçus en réserve.
    SELECT count(*) INTO v_ecart FROM (
        SELECT l.id FROM lot l LEFT JOIN lot_stock_location lsl ON lsl.lot_id = l.id
         GROUP BY l.id, l.current_quantity
        HAVING l.current_quantity <> COALESCE(sum(lsl.qty), 0)) x;

    SELECT count(*) - count(DISTINCT reference) INTO v_ref FROM suggestion_reassort;

    IF v_open_rayon <> 1 THEN RAISE EXCEPTION 'Suggestions RAYON ouvertes : % (attendu 1)', v_open_rayon; END IF;
    IF v_open_reserve <> 1 THEN RAISE EXCEPTION 'Suggestions RESERVE ouvertes : % (attendu 1)', v_open_reserve; END IF;
    IF v_closed < 30 THEN RAISE EXCEPTION 'Suggestions validées : % (attendu >= 30)', v_closed; END IF;
    IF v_orient > 0 THEN RAISE EXCEPTION '% ligne(s) de réassort mal orientée(s)', v_orient; END IF;
    IF v_qte > 0 THEN RAISE EXCEPTION '% ligne(s) de réassort sans quantité', v_qte; END IF;
    IF v_fav > 0 THEN RAISE EXCEPTION '% favori(s) sans stock rayon et réserve', v_fav; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% lot(s) dont le restant contredit les emplacements', v_ecart; END IF;
    IF v_ref > 0 THEN RAISE EXCEPTION 'Références de réassort en double'; END IF;

    RAISE NOTICE 'Réassort : % ouverte(s) rayon, % réserve, % validée(s), % ligne(s).',
                 v_open_rayon, v_open_reserve, v_closed, v_lignes;
END $$;

\echo '<< 17c_reassort : terminé'
