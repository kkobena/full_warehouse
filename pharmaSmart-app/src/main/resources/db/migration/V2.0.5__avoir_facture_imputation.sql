-- ============================================================================
-- Avoirs de tiers payant : garder la trace de la facture imputée
--
-- L'imputation d'un avoir demandait pourtant une facture cible à l'écran — et
-- la jetait : le service se contentait de faire passer l'avoir au statut
-- IMPUTE, sans rien déduire nulle part. Un avoir « imputé » ne réduisait donc
-- ce que doit personne, et l'on ne pouvait pas dire, plus tard, sur quelle
-- facture il avait été porté.
--
-- Ces deux colonnes portent la clé composite de la facture d'imputation ; elles
-- restent nulles tant que l'avoir n'est pas imputé, ce qui est le cas de tous
-- les avoirs existants.
-- ============================================================================

ALTER TABLE avoir_tiers_payant
  ADD COLUMN IF NOT EXISTS facture_imputation_id   BIGINT,
  ADD COLUMN IF NOT EXISTS facture_imputation_date DATE;

ALTER TABLE avoir_tiers_payant
  ADD CONSTRAINT fk_avoir_facture_imputation
    FOREIGN KEY (facture_imputation_id, facture_imputation_date)
      REFERENCES facture_tiers_payant (id, invoice_date);

CREATE INDEX IF NOT EXISTS avoir_facture_imputation_idx
  ON avoir_tiers_payant (facture_imputation_id, facture_imputation_date);
