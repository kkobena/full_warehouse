-- Taux de couverture standard d'un organisme (ex. 80 % pour la CNPS), proposé comme valeur de départ
-- à la saisie d'un client assuré (docs/PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-TIERS-PAYANT.md § 9).
--
-- C'est une valeur SUGGÉRÉE : le taux porté par client_tiers_payant reste la donnée qui sert au calcul
-- du remboursement. Nullable et sans valeur par défaut — on ne suppose jamais 100 % en silence.

ALTER TABLE tiers_payant
  ADD COLUMN taux_couverture_defaut integer
    CONSTRAINT tiers_payant_taux_couverture_defaut_check CHECK (taux_couverture_defaut BETWEEN 0 AND 100);

-- Contrôle de doublon sur l'identifiant contribuable (NCC) : recherche insensible à la casse.
CREATE INDEX tiers_payant_ncc_idx ON tiers_payant (upper(ncc)) WHERE ncc IS NOT NULL AND ncc <> '';
