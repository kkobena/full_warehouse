-- ═══════════════════════════════════════════════════════════════════════════
-- MIGRATION V1.2.7 — Fix get_product_movements_by_period
-- ═══════════════════════════════════════════════════════════════════════════
-- Corrections :
--   1. INVENTAIRE : MAX(quantity_after) au lieu de SUM(quantity) (delta)
--   2. Ajout paramètre optionnel p_storage_id (NULL = tous les storages)
-- ═══════════════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION get_product_movements_by_period(
  p_produit_id  INTEGER,
  p_magasin_id  INTEGER,
  p_date_debut  DATE,
  p_date_fin    DATE,
  p_storage_id  INTEGER DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql AS
$$
DECLARE
  result JSONB;
BEGIN
  WITH mouvements_ordonnes AS (
    SELECT *,
           ROW_NUMBER() OVER (
             PARTITION BY transaction_date, produit_id, magasin_id
             ORDER BY id
           ) AS rn_debut,
           ROW_NUMBER() OVER (
             PARTITION BY transaction_date, produit_id, magasin_id
             ORDER BY id DESC
           ) AS rn_fin
    FROM inventory_transaction
    WHERE produit_id       = p_produit_id
      AND magasin_id       = p_magasin_id
      AND transaction_date BETWEEN p_date_debut AND p_date_fin
      AND (p_storage_id IS NULL OR storage_id = p_storage_id)
  ),
  mouvements_detail AS (
    SELECT transaction_date,
           magasin_id,
           produit_id,
           mouvement_type,
           -- INVENTAIRE : stock compté (quantity_after), pas le delta signé
           CASE WHEN mouvement_type = 'INVENTAIRE'
                THEN MAX(quantity_after)
                ELSE SUM(quantity)
           END                                                 AS quantite,
           MIN(CASE WHEN rn_debut = 1 THEN quantity_befor END) AS quantite_debut,
           MAX(CASE WHEN rn_fin = 1 THEN quantity_after END)   AS quantite_fin
    FROM mouvements_ordonnes
    GROUP BY transaction_date, magasin_id, produit_id, mouvement_type
  ),
  mouvements_agg AS (
    SELECT transaction_date,
           magasin_id,
           produit_id,
           MAX(quantite_debut)                        AS quantite_debut,
           MAX(quantite_fin)                          AS quantite_fin,
           jsonb_object_agg(mouvement_type, quantite) AS mouvements
    FROM mouvements_detail
    GROUP BY transaction_date, magasin_id, produit_id
    ORDER BY transaction_date
  )
  SELECT jsonb_agg(
           jsonb_build_object(
             'mvtDate',    transaction_date,
             'initStock',  quantite_debut,
             'afterStock', quantite_fin,
             'mouvements', mouvements
           )
           ORDER BY transaction_date
         )
  INTO result
  FROM mouvements_agg;

  RETURN COALESCE(result, '[]'::jsonb);
END;
$$;
