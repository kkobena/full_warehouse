-- Pilotage : l'axe MAGASIN disparaît (une seule officine, les autres magasins sont des dépôts, suivis à part).
-- Une vue ventilée par magasin repasse sur la famille ; un second axe magasin est retiré.
UPDATE pilotage_vue
SET axe = 'FAMILLE'
WHERE axe = 'MAGASIN';

UPDATE pilotage_vue
SET axe_croise = NULL
WHERE axe_croise = 'MAGASIN'
   OR axe_croise = axe;
