UPDATE nav_item
SET actif = FALSE
WHERE code IN (
               'mvt-caisse.balance',
               'mvt-caisse.taxe-report',
               'mvt-caisse.tableau-pharmacien',
               'mvt-caisse.recapitulatif-caisse',
               'mvt-caisse.raport-activite',
               'mvt-caisse.declaration-tva',
               'mvt-caisse.export-comptable',
               'rapport-finance.avoirs-analytics',
               'rapport-finance.situation-creances',
               'rapport-finance.vieillissement-differes'
  );
UPDATE nav_item
SET libelle = 'BFR & Cycle d''exploitation'
WHERE code ='rapport-finance.cash-flow-bfr';
UPDATE nav_item
SET libelle = 'Marges & Résultat'
WHERE code ='rapport-finance.pnl-analytique';
