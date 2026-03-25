-- ============================================================
-- Stock virtuel & exclusion mois de rupture VMM
-- ============================================================

-- Ajout du flag "mois de rupture fournisseur" dans les agrégations mensuelles.
-- Un mois est marqué si une rupture encore active (product_still_out_of_stock=TRUE)
-- avait débuté avant la fin du mois.
-- Le calcul VMM (SEMOIS) exclura ces mois pour ne pas sous-estimer la consommation réelle.
-- Le flag est remis à FALSE automatiquement dès que la rupture est résolue
-- (product_still_out_of_stock=FALSE) lors de la prochaine agrégation mensuelle.

ALTER TABLE ventes_mensuelles_agregees
    ADD COLUMN IF NOT EXISTS est_rupture_fournisseur BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN ventes_mensuelles_agregees.est_rupture_fournisseur
    IS 'TRUE si le produit avait une rupture fournisseur encore active (product_still_out_of_stock=TRUE) '
       'débutée avant la fin du mois. Exclu du calcul VMM SEMOIS. '
       'Remis à FALSE automatiquement quand la rupture est résolue.';

-- Index pour accélérer le filtre sur les mois valides lors du calcul VMM
CREATE INDEX IF NOT EXISTS idx_ventes_mensuelles_rupture_produit
    ON ventes_mensuelles_agregees (produit_id, est_rupture_fournisseur, annee_mois);

-- Backfill rétroactif : uniquement les ruptures ENCORE ACTIVES débutées avant la fin du mois.
-- Les ruptures résolues (product_still_out_of_stock=FALSE) ne biaisant plus les données,
-- on ne les applique pas rétroactivement.
UPDATE ventes_mensuelles_agregees vma
SET est_rupture_fournisseur = TRUE,
    updated_at              = NOW()
WHERE EXISTS (
    SELECT 1
    FROM   rupture r
    WHERE  r.produit_id                 = vma.produit_id
      AND  r.product_still_out_of_stock = TRUE
      AND  r.date_mtv                   < (TO_DATE(vma.annee_mois, 'YYYY-MM') + INTERVAL '1 month')
);
