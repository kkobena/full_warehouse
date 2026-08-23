UPDATE nav_item
SET icon = 'pi pi-calculator'
WHERE code = 'comptabilite'
  AND icon = 'pi pi-book';

INSERT INTO app_configuration (name, value, description, value_type)
VALUES ('APP_DEVISE', 'FCFA',
        'Devise affichée à la suite des montants (libellé libre, ex. FCFA,F)', 'STRING')
ON CONFLICT (name) DO NOTHING;



ALTER TABLE nav_item ADD COLUMN IF NOT EXISTS titre_long varchar(150);

COMMENT ON COLUMN nav_item.titre_long IS
  'Titre de la barre d''outils de l''écran, quand il diffère du libellé du menu';

-- Les divergences constatées à ce jour, relevées dans les gabarits.
UPDATE nav_item SET titre_long = 'Ponction du chiffre d''affaires' WHERE code = 'declaration-ca.ponction';


UPDATE nav_item SET libelle = 'Unités gratuites vendues' WHERE code = 'declaration-ca.journal-ug';

-- Comptabilité : deux barres d'outils nomment plus longuement que le menu.
UPDATE nav_item SET titre_long = 'Balance de caisse' WHERE code = 'comptabilite.balance';
UPDATE nav_item SET titre_long = 'Tableau de bord pharmacien' WHERE code = 'comptabilite.tableau-pharmacien';


UPDATE nav_item SET titre_long = 'Dashboard Chiffre d''Affaires' WHERE code = 'rapport-ventes.dashboard-ca';
UPDATE nav_item SET titre_long = 'Synthèse des Ventes Quotidiennes' WHERE code = 'rapport-ventes.sales-summary';
UPDATE nav_item SET titre_long = 'Analyse de Rentabilité des Produits' WHERE code = 'rapport-ventes.profitability';
UPDATE nav_item SET titre_long = 'Tableaux Comparatifs CA' WHERE code = 'rapport-ventes.comparative';
UPDATE nav_item SET titre_long = 'Performance des Vendeurs' WHERE code = 'rapport-ventes.sales-by-staff';
UPDATE nav_item SET titre_long = 'Saisonnalité du Chiffre d''Affaires' WHERE code = 'rapport-ventes.seasonality';
UPDATE nav_item SET titre_long = 'Analyse de Rentabilité des Produits' WHERE code = 'rapport-finance.profitability';
UPDATE nav_item SET titre_long = 'Situation des Différés Clients' WHERE code = 'rapport-finance.vieillissement-differes';
UPDATE nav_item SET titre_long = 'Analyse des Avoirs Tiers-Payants' WHERE code = 'rapport-finance.avoirs-analytics';
UPDATE nav_item SET titre_long = 'Taux de Recouvrement Tiers-Payants' WHERE code = 'rapport-finance.taux-recouvrement-tp';
UPDATE nav_item SET titre_long = 'Récapitulatif Produits Vendus/Invendus' WHERE code = 'rapport-stock.recap-produit-vendu';
UPDATE nav_item SET titre_long = 'Démarque & Ajustements de Stock' WHERE code = 'rapport-stock.demarque';
UPDATE nav_item SET titre_long = 'Performance des Fournisseurs' WHERE code = 'rapport-partners.supplier-performance';
UPDATE nav_item SET titre_long = 'Analyse du Panier' WHERE code = 'rapport-ventes.market-basket';
UPDATE nav_item SET titre_long = 'Alertes Stock' WHERE code = 'rapport-stock.stock-alerts';
UPDATE nav_item SET titre_long = 'Segmentation Clients' WHERE code = 'rapport-partners.customer-segmentation';

-- Péremptions : le menu parle de produits, la barre de lots — c'est l'unité de gestion réelle.
UPDATE nav_item SET titre_long = 'Lots périmés' WHERE code = 'peremptions.lot-perimes';

-- Barres d'outils des écrans de gestion : elles nomment la fonction, le menu la situe.
UPDATE nav_item SET titre_long = 'Tableau de bord Approvisionnement' WHERE code = 'commande.dashboard';
UPDATE nav_item SET titre_long = 'Pilotage des stocks' WHERE code = 'commande.repartition-stock';
UPDATE nav_item SET titre_long = 'Achats dépôts' WHERE code = 'depot.achat-depot';
UPDATE nav_item SET titre_long = 'Gestion des Dépôts' WHERE code = 'depot.liste-depots';
UPDATE nav_item SET titre_long = 'Produits' WHERE code = 'depot.stock-depot';
UPDATE nav_item SET titre_long = 'Ventes à crédit (différés)' WHERE code = 'differes.differes';
UPDATE nav_item SET titre_long = 'Historique des règlements différés' WHERE code = 'differes.historique';
UPDATE nav_item SET titre_long = 'Avoirs / Notes de crédit' WHERE code = 'facturation.avoirs';
UPDATE nav_item SET titre_long = 'Factures tiers payants' WHERE code = 'facturation.factures';
UPDATE nav_item SET titre_long = 'Historique des règlements factures' WHERE code = 'facturation.historique';
UPDATE nav_item SET titre_long = 'État de rapprochement' WHERE code = 'facturation.rapprochement';
UPDATE nav_item SET titre_long = 'Récapitulatif mensuel' WHERE code = 'facturation.recapitulatif';
UPDATE nav_item SET titre_long = 'Lots à détruire' WHERE code = 'peremptions.lot-a-detruire';
UPDATE nav_item SET titre_long = 'Liste des remises produits' WHERE code = 'remise.remise-produit';
UPDATE nav_item SET titre_long = 'Menu groupe de tiers payant' WHERE code = 'tiers-payant.groupe-tiers-payant';
UPDATE nav_item SET titre_long = 'Liste des tiers payants' WHERE code = 'tiers-payant.tiers-payant';
UPDATE nav_item SET titre_long = 'Annulations de ventes' WHERE code = 'ventes.annulations';
UPDATE nav_item SET titre_long = 'Liste des Proformas' WHERE code = 'ventes.devis';

-- Inventaire : une seule barre pour trois onglets, son titre suit l'onglet ouvert.
UPDATE nav_item SET titre_long = 'Inventaires en cours' WHERE code = 'inventaire.en-cours';
UPDATE nav_item SET titre_long = 'Inventaire tournant' WHERE code = 'inventaire.tournant';
UPDATE nav_item SET titre_long = 'Inventaires clôturés' WHERE code = 'inventaire.clotures';
