-- Stock et valorisation à un instant T, à partir du journal des mouvements (inventory_transaction) et des photos
-- (stock_produit_snapshot). Corrige les fonctions de V1.2.6 :
--   * sans photo antérieure à T, elles partaient de 0 : on remonte désormais le temps depuis la photo suivante, ou depuis
--     le stock courant ;
--   * les mouvements INVENTAIRE étaient exclus : ils comptent comme les autres (datés au comptage, ils précèdent la photo
--     de clôture d'inventaire et ne sont donc jamais comptés deux fois) ;
--   * le stock négatif était ramené à 0 : il est conservé (règle de gestion : dette d'avoir) ;
--   * les UG manquaient : la photo les porte désormais, la valorisation les inclut ;
--   * created_at (sans fuseau) est écrit en UTC par l'application : il est lu comme tel, quel que soit le fuseau de la session.

ALTER TABLE stock_produit_snapshot ADD COLUMN IF NOT EXISTS qty_ug INTEGER;

DROP FUNCTION IF EXISTS fn_stock_bilan_periode(INT, TIMESTAMPTZ, TIMESTAMPTZ, INT[]);
DROP FUNCTION IF EXISTS fn_stock_valuation_at_time(INT, INT, TIMESTAMPTZ);
DROP FUNCTION IF EXISTS fn_stock_valuation_bulk(INT, INT[], TIMESTAMPTZ);

-- ─── Socle : stock et UG de chaque ligne de stock à T ───────────────────────────────────────────────────────────────
-- Ancre : la dernière photo avant T (on ajoute les mouvements jusqu'à T), sinon la première photo après T ou, à défaut, le
-- stock courant (on retranche les mouvements postérieurs à T). Les UG ne sont pas journalisées : celles de l'ancre servent.
CREATE OR REPLACE FUNCTION fn_stock_quantites_at_time(
  p_at TIMESTAMPTZ,
  p_magasin_id INT DEFAULT NULL,
  p_produit_ids INT[] DEFAULT NULL
)
  RETURNS TABLE
          (
            produit_id INT,
            storage_id INT,
            qty_stock  INT,
            qty_ug     INT
          )
  LANGUAGE sql
  STABLE
AS
$$
WITH scope AS (SELECT sp.produit_id, sp.storage_id, sp.qty_stock, sp.qty_ug
               FROM stock_produit sp
                      JOIN storage s ON s.id = sp.storage_id
               WHERE (p_magasin_id IS NULL OR s.magasin_id = p_magasin_id)
                 AND (p_produit_ids IS NULL OR sp.produit_id = ANY (p_produit_ids))),
     photo_avant AS (SELECT DISTINCT ON (sn.produit_id, sn.storage_id) sn.produit_id, sn.storage_id, sn.qty_stock, sn.qty_ug,
                                                                       sn.snapshot_date
                     FROM stock_produit_snapshot sn
                            JOIN scope sc ON sc.produit_id = sn.produit_id AND sc.storage_id = sn.storage_id
                     WHERE sn.snapshot_date <= p_at
                     ORDER BY sn.produit_id, sn.storage_id, sn.snapshot_date DESC),
     photo_apres AS (SELECT DISTINCT ON (sn.produit_id, sn.storage_id) sn.produit_id, sn.storage_id, sn.qty_stock, sn.qty_ug,
                                                                       sn.snapshot_date
                     FROM stock_produit_snapshot sn
                            JOIN scope sc ON sc.produit_id = sn.produit_id AND sc.storage_id = sn.storage_id
                     WHERE sn.snapshot_date > p_at
                     ORDER BY sn.produit_id, sn.storage_id, sn.snapshot_date),
     ancre AS (SELECT sc.produit_id,
                      sc.storage_id,
                      COALESCE(av.qty_stock, ap.qty_stock, sc.qty_stock)                               AS qty_ancre,
                      COALESCE(av.qty_ug, ap.qty_ug, sc.qty_ug, 0)                                     AS qty_ug,
                      av.produit_id IS NOT NULL                                                        AS vers_l_avant,
                      COALESCE(av.snapshot_date, p_at)                                                 AS borne_basse,
                      CASE WHEN av.produit_id IS NOT NULL THEN p_at ELSE COALESCE(ap.snapshot_date, 'infinity') END AS borne_haute
               FROM scope sc
                      LEFT JOIN photo_avant av ON av.produit_id = sc.produit_id AND av.storage_id = sc.storage_id
                      LEFT JOIN photo_apres ap ON ap.produit_id = sc.produit_id AND ap.storage_id = sc.storage_id),
     deltas AS (SELECT a.produit_id, a.storage_id, SUM(it.quantity_after - it.quantity_befor) AS delta
                FROM ancre a
                       JOIN inventory_transaction it ON it.produit_id = a.produit_id AND it.storage_id = a.storage_id
                WHERE it.created_at AT TIME ZONE 'UTC' > a.borne_basse
                  AND it.created_at AT TIME ZONE 'UTC' <= a.borne_haute
                GROUP BY a.produit_id, a.storage_id)
SELECT a.produit_id,
       a.storage_id,
       (a.qty_ancre + CASE WHEN a.vers_l_avant THEN 1 ELSE -1 END * COALESCE(d.delta, 0))::INT,
       a.qty_ug::INT
FROM ancre a
       LEFT JOIN deltas d ON d.produit_id = a.produit_id AND d.storage_id = a.storage_id;
$$;

-- Signature conservée : la vue v_stock_ecart_journal l'appelle.
CREATE OR REPLACE FUNCTION fn_stock_at_time(
  p_produit_id INT,
  p_storage_id INT,
  p_at TIMESTAMPTZ DEFAULT NOW()
)
  RETURNS INT
  LANGUAGE sql
  STABLE AS
$$
SELECT COALESCE((SELECT q.qty_stock
                 FROM fn_stock_quantites_at_time(p_at, NULL, ARRAY [p_produit_id]) q
                 WHERE q.storage_id = p_storage_id), 0);
$$;

-- ─── Valorisation à T ───────────────────────────────────────────────────────────────────────────────────────────────
-- Prix du dernier mouvement avant T, sinon prix courant du fournisseur principal. Valeur = (stock + UG) × prix, comme la
-- valorisation du stock ; seules les lignes de quantité positive sont valorisées. p_magasin_id NULL : tous les magasins.
CREATE OR REPLACE FUNCTION fn_stock_valuation_bulk(
  p_magasin_id INT,
  p_produit_ids INT[] DEFAULT NULL,
  p_at TIMESTAMPTZ DEFAULT NOW()
)
  RETURNS TABLE
          (
            produit_id   INT,
            storage_id   INT,
            qty_stock    INT,
            qty_ug       INT,
            prix_achat   INT,
            prix_vente   INT,
            valeur_achat BIGINT,
            valeur_vente BIGINT
          )
  LANGUAGE sql
  STABLE
AS
$$
WITH quantites AS (SELECT q.* FROM fn_stock_quantites_at_time(p_at, p_magasin_id, p_produit_ids) q),
     dernier_prix AS (SELECT DISTINCT ON (it.produit_id, it.storage_id) it.produit_id, it.storage_id, it.cost_amount, it.regular_unit_price
                      FROM inventory_transaction it
                             JOIN quantites q ON q.produit_id = it.produit_id AND q.storage_id = it.storage_id
                      WHERE it.created_at AT TIME ZONE 'UTC' <= p_at
                      ORDER BY it.produit_id, it.storage_id, it.created_at DESC),
     prix AS (SELECT q.produit_id,
                     q.storage_id,
                     q.qty_stock,
                     q.qty_ug,
                     COALESCE(dp.cost_amount, fp.prix_achat, 0)        AS prix_achat,
                     COALESCE(dp.regular_unit_price, fp.prix_uni, 0)   AS prix_vente
              FROM quantites q
                     JOIN produit p ON p.id = q.produit_id
                     LEFT JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
                     LEFT JOIN dernier_prix dp ON dp.produit_id = q.produit_id AND dp.storage_id = q.storage_id)
SELECT produit_id,
       storage_id,
       qty_stock,
       qty_ug,
       prix_achat,
       prix_vente,
       ((qty_stock + qty_ug)::BIGINT * prix_achat),
       ((qty_stock + qty_ug)::BIGINT * prix_vente)
FROM prix
WHERE qty_stock + qty_ug > 0;
$$;

CREATE OR REPLACE FUNCTION fn_stock_valuation_at_time(
  p_produit_id INT,
  p_storage_id INT,
  p_at TIMESTAMPTZ DEFAULT NOW()
)
  RETURNS TABLE
          (
            qty_stock    INT,
            qty_ug       INT,
            prix_achat   INT,
            prix_vente   INT,
            valeur_achat BIGINT,
            valeur_vente BIGINT
          )
  LANGUAGE sql
  STABLE
AS
$$
SELECT v.qty_stock, v.qty_ug, v.prix_achat, v.prix_vente, v.valeur_achat, v.valeur_vente
FROM fn_stock_valuation_bulk(NULL, ARRAY [p_produit_id], p_at) v
WHERE v.storage_id = p_storage_id;
$$;

-- ─── Bilan d'une période ────────────────────────────────────────────────────────────────────────────────────────────
-- Les écarts d'inventaire forment leur propre colonne : sans eux, début + entrées − sorties + ajustements ne retombait pas
-- sur la fin. Quantités hors UG (les UG ne sont pas journalisées) ; valeurs UG comprises.
CREATE OR REPLACE FUNCTION fn_stock_bilan_periode(
  p_magasin_id INT,
  p_date_debut TIMESTAMPTZ,
  p_date_fin TIMESTAMPTZ,
  p_produit_ids INT[] DEFAULT NULL
)
  RETURNS TABLE
          (
            produit_id         INT,
            storage_id         INT,
            qty_debut          INT,
            prix_achat_debut   INT,
            valeur_achat_debut BIGINT,
            valeur_vente_debut BIGINT,
            entrees            BIGINT,
            sorties            BIGINT,
            ajustements        BIGINT,
            inventaires        BIGINT,
            qty_fin            INT,
            prix_achat_fin     INT,
            valeur_achat_fin   BIGINT,
            valeur_vente_fin   BIGINT
          )
  LANGUAGE sql
  STABLE
AS
$$
WITH debut AS (SELECT * FROM fn_stock_valuation_bulk(p_magasin_id, p_produit_ids, p_date_debut)),
     fin AS (SELECT * FROM fn_stock_valuation_bulk(p_magasin_id, p_produit_ids, p_date_fin)),
     qty_debut AS (SELECT * FROM fn_stock_quantites_at_time(p_date_debut, p_magasin_id, p_produit_ids)),
     qty_fin AS (SELECT * FROM fn_stock_quantites_at_time(p_date_fin, p_magasin_id, p_produit_ids)),
     mvt AS (SELECT it.produit_id,
                    it.storage_id,
                    SUM(CASE
                          WHEN it.mouvement_type NOT IN ('AJUSTEMENT_IN', 'AJUSTEMENT_OUT', 'INVENTAIRE')
                            AND it.quantity_after > it.quantity_befor
                            THEN it.quantity_after - it.quantity_befor
                          ELSE 0 END)                                                             AS entrees,
                    SUM(CASE
                          WHEN it.mouvement_type NOT IN ('AJUSTEMENT_IN', 'AJUSTEMENT_OUT', 'INVENTAIRE')
                            AND it.quantity_after < it.quantity_befor
                            THEN it.quantity_befor - it.quantity_after
                          ELSE 0 END)                                                             AS sorties,
                    SUM(CASE
                          WHEN it.mouvement_type IN ('AJUSTEMENT_IN', 'AJUSTEMENT_OUT')
                            THEN it.quantity_after - it.quantity_befor
                          ELSE 0 END)                                                             AS ajustements,
                    SUM(CASE
                          WHEN it.mouvement_type = 'INVENTAIRE' THEN it.quantity_after - it.quantity_befor
                          ELSE 0 END)                                                             AS inventaires
             FROM inventory_transaction it
                    JOIN qty_fin q ON q.produit_id = it.produit_id AND q.storage_id = it.storage_id
             WHERE it.created_at AT TIME ZONE 'UTC' > p_date_debut
               AND it.created_at AT TIME ZONE 'UTC' <= p_date_fin
             GROUP BY it.produit_id, it.storage_id)
SELECT q.produit_id,
       q.storage_id,
       COALESCE(qd.qty_stock, 0),
       COALESCE(d.prix_achat, 0),
       COALESCE(d.valeur_achat, 0),
       COALESCE(d.valeur_vente, 0),
       COALESCE(m.entrees, 0),
       COALESCE(m.sorties, 0),
       COALESCE(m.ajustements, 0),
       COALESCE(m.inventaires, 0),
       q.qty_stock,
       COALESCE(f.prix_achat, 0),
       COALESCE(f.valeur_achat, 0),
       COALESCE(f.valeur_vente, 0)
FROM qty_fin q
       LEFT JOIN qty_debut qd ON qd.produit_id = q.produit_id AND qd.storage_id = q.storage_id
       LEFT JOIN debut d ON d.produit_id = q.produit_id AND d.storage_id = q.storage_id
       LEFT JOIN fin f ON f.produit_id = q.produit_id AND f.storage_id = q.storage_id
       LEFT JOIN mvt m ON m.produit_id = q.produit_id AND m.storage_id = q.storage_id
WHERE COALESCE(qd.qty_stock, 0) <> 0
   OR q.qty_stock <> 0
   OR m.produit_id IS NOT NULL;
$$;
