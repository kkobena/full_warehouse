-- Ligne marquée « non remboursée » au comptoir : le tiers payant ne prend rien en charge dessus.
ALTER TABLE sales_line ADD COLUMN IF NOT EXISTS non_rembourse BOOLEAN NOT NULL DEFAULT FALSE;

-- Montant des lignes non remboursées d'une vente assurance, conservé pour les factures (montant « bon »).
ALTER TABLE sales ADD COLUMN IF NOT EXISTS montant_non_rembourse INTEGER NOT NULL DEFAULT 0;
