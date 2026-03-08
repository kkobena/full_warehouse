ALTER TABLE client_tiers_payant
  ADD COLUMN IF NOT EXISTS taux_historique json;


DROP MATERIALIZED VIEW IF EXISTS mv_stock_alerts;

CREATE MATERIALIZED VIEW mv_stock_alerts AS
WITH lot_peremption AS (
  -- Date de péremption la plus proche parmi les lots encore en stock
  SELECT l.produit_id,
         MIN(l.expiry_date) AS nearest_expiry_date
  FROM lot l
  WHERE l.statut = 'AVAILABLE'
    AND l.current_quantity > 0
    AND l.expiry_date IS NOT NULL
  GROUP BY l.produit_id),
     stock AS (SELECT sp.produit_id,
                      COALESCE(SUM(sp.qty_stock + sp.qty_ug), 0) AS stock_quantity
               FROM stock_produit sp
                      JOIN storage s ON sp.storage_id = s.id
                      JOIN magasin m ON s.magasin_id = m.id
               WHERE m.id = 1
               GROUP BY sp.produit_id)
SELECT p.id                          AS produit_id,
       p.libelle,
       fp.code_cip,
       COALESCE(s.stock_quantity, 0) AS stock_quantity,
       p.qty_seuil_mini              AS seuil_min,
       lp.nearest_expiry_date        AS expiry_date,
       CASE
         WHEN COALESCE(s.stock_quantity, 0) <= 0
           THEN 'RUPTURE'
         WHEN COALESCE(s.stock_quantity, 0) < p.qty_seuil_mini
           THEN 'ALERTE'
         WHEN lp.nearest_expiry_date IS NOT NULL
           AND lp.nearest_expiry_date < (CURRENT_DATE + INTERVAL '3 months')
           THEN 'PEREMPTION'
         ELSE NULL
         END                         AS alert_type,
       now()                         AS last_updated
FROM produit p
       LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
       LEFT JOIN stock s ON p.id = s.produit_id
       LEFT JOIN lot_peremption lp ON p.id = lp.produit_id
WHERE p.status = 'ENABLE'
  AND (
  COALESCE(s.stock_quantity, 0) <= 0 -- RUPTURE
    OR COALESCE(s.stock_quantity, 0) < p.qty_seuil_mini -- ALERTE
    OR
  (lp.nearest_expiry_date IS NOT NULL AND lp.nearest_expiry_date < (CURRENT_DATE + INTERVAL '3 months')) -- PEREMPTION
  );

COMMENT ON MATERIALIZED VIEW mv_stock_alerts IS
  'Stock alerts: ruptures, low stock, and near expiration based on lot nearest expiry date';

-- Index unique : un seul row par produit désormais
CREATE UNIQUE INDEX idx_mv_stock_alerts_unique
  ON mv_stock_alerts (produit_id);

CREATE INDEX idx_mv_stock_alerts_type
  ON mv_stock_alerts (alert_type);

CREATE INDEX idx_mv_stock_alerts_expiry
  ON mv_stock_alerts (expiry_date)
  WHERE (expiry_date IS NOT NULL);

