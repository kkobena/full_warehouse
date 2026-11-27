-- Promotions : décisions de l'officine du 2026-11-27 (docs/PLAN-GESTION-PROMOTIONS.md, §8).
--
-- Q1 : c'est le pharmacien qui choisit les articles qu'il promeut, médicament à prix administré
-- compris. Le logiciel le signale (création, listes, validation), il ne le refuse plus : le réglage
-- qui autorisait la TVA 0 n'a plus d'objet.
DELETE FROM app_configuration WHERE name = 'APP_PROMOTION_TVA_ZERO_AUTORISEE';

-- Q2 : les promotions s'appliquent aux ventes carnet, et, sur une vente assurance, aux seules lignes
-- non remboursées. Le patient paie ces lignes nettes de leur promotion : la facture du tiers payant
-- (part du patient sur la partie remboursable) a besoin de cette remise pour ne pas la compter.
ALTER TABLE sales ADD COLUMN IF NOT EXISTS remise_non_rembourse INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN sales.remise_non_rembourse IS
  'Remise (promotion) des lignes non remboursees d''une vente a credit : le patient paie montant_non_rembourse moins elle.';
