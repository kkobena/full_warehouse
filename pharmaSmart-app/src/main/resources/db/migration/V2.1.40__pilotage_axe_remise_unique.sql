-- Pilotage : les quatre axes de remise (code, tranche, octroi, autorisant) deviennent un seul axe REMISE, par tranche de taux.
-- Les vues enregistrées qui les citaient passent sur REMISE ; un second axe identique au premier n'a plus de sens et est retiré.
UPDATE pilotage_vue
SET axe = 'REMISE'
WHERE axe IN ('TRANCHE_REMISE', 'CODE_REMISE', 'OCTROI_REMISE', 'AUTORISANT');

UPDATE pilotage_vue
SET axe_croise = 'REMISE'
WHERE axe_croise IN ('TRANCHE_REMISE', 'CODE_REMISE', 'OCTROI_REMISE', 'AUTORISANT');

UPDATE pilotage_vue
SET axe_croise = NULL
WHERE axe_croise = axe;
