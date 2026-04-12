
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'ventes.journal',  'Journal des ventes', 'pi pi-book',      NULL, id, 10, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'ventes.en-cours', 'Ventes en cours',    'pi pi-clock',     NULL, id, 20, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'ventes.presales', 'Pré-ventes',         'pi pi-bookmark',  NULL, id, 30, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'ventes.devis',    'Proformas',          'pi pi-file-edit', NULL, id, 40, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'ventes'
  ON CONFLICT (code) DO NOTHING;

-- ── Actions dans "Journal des ventes" ─────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'ventes.journal.export', 'Exporter PDF', 'pi pi-file-pdf', NULL, id, 10, 4, 'ACTION', TRUE
  FROM nav_item WHERE code = 'ventes.journal'
  ON CONFLICT (code) DO NOTHING;

-- ── Actions dans "Ventes en cours" ────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'ventes.en-cours.delete', 'Annuler une vente', 'pi pi-trash', NULL, id, 10, 4, 'ACTION', TRUE
  FROM nav_item WHERE code = 'ventes.en-cours'
  ON CONFLICT (code) DO NOTHING;

-- ── Actions dans "Pré-ventes" ─────────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'ventes.presales.delete', 'Supprimer une pré-vente', 'pi pi-trash', NULL, id, 10, 4, 'ACTION', TRUE
  FROM nav_item WHERE code = 'ventes.presales'
  ON CONFLICT (code) DO NOTHING;

-- ── Actions dans "Proformas" ──────────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'ventes.devis.delete', 'Supprimer un proforma', 'pi pi-trash', NULL, id, 10, 4, 'ACTION', TRUE
  FROM nav_item WHERE code = 'ventes.devis'
  ON CONFLICT (code) DO NOTHING;

-- ── Assignation ROLE_ADMIN : accès complet ────────────────────────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
  SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
  FROM nav_item
  WHERE code IN (
    'ventes.journal',
    'ventes.en-cours',
    'ventes.presales',
    'ventes.devis',
    'ventes.journal.export',
    'ventes.en-cours.delete',
    'ventes.presales.delete',
    'ventes.devis.delete'
  )
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ── Assignation ROLE_CAISSIER : accès complet ────────────────────────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_CAISSIER', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN (
               'ventes.journal',
               'ventes.en-cours',
               'ventes.devis',
               'ventes.en-cours.delete',
               'ventes.devis.delete',
               'ventes.journal.export',
               'ventes.en-cours.delete',
               'ventes.devis.delete'
  )
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;



-- ── Assignation ROLE_VENDEUR : accès complet ────────────────────────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_VENDEUR', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN (
               'ventes.presales',
               'ventes.devis',
               'ventes.presales.delete',
               'ventes.devis.delete'
  )
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;





INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'commande.dashboard',        'Tableau de bord',         'pi pi-th-large',  NULL, id, 10, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'commande'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'commande.suggestions',      'Commandes & Réceptions',  'pi pi-lightbulb', NULL, id, 20, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'commande'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'commande.repartition-stock','Répartition & Transferts', 'pi pi-sync',     NULL, id, 30, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'commande'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'commande.retour-fournisseur','Retours fournisseurs',    'pi pi-replay',   NULL, id, 40, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'commande'
  ON CONFLICT (code) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Onglets de la page Facturation (parent : factures)
-- ═══════════════════════════════════════════════════════════════════════════════

-- Ordre métier officine : édition (acte d'entrée) → factures → historique
--   → rapprochement → récapitulatif → avoirs → automatisation (config)
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'facturation.edition',       'Édition de factures',   'pi pi-file-plus',   NULL, id, 10, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'factures'
  ON CONFLICT (code) DO UPDATE SET ordre = 10;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'facturation.factures',      'Factures',              'pi pi-list',        NULL, id, 20, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'factures'
  ON CONFLICT (code) DO UPDATE SET ordre = 20;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'facturation.historique',    'Historique règlements', 'pi pi-history',     NULL, id, 30, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'factures'
  ON CONFLICT (code) DO UPDATE SET ordre = 30;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'facturation.rapprochement', 'Rapprochement',         'pi pi-arrows-h',    NULL, id, 40, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'factures'
  ON CONFLICT (code) DO UPDATE SET ordre = 40;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'facturation.recapitulatif', 'Récapitulatif',         'pi pi-chart-bar',   NULL, id, 50, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'factures'
  ON CONFLICT (code) DO UPDATE SET ordre = 50;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'facturation.avoirs',        'Avoirs',                'pi pi-file-minus',  NULL, id, 60, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'factures'
  ON CONFLICT (code) DO UPDATE SET ordre = 60;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'facturation.automatisation','Automatisation',         'pi pi-cog',        NULL, id, 70, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'factures'
  ON CONFLICT (code) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Onglets de la page Inventaire (parent : inventaire)
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'inventaire.en-cours', 'En cours',  'pi pi-refresh', NULL, id, 10, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'inventaire'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'inventaire.tournant', 'Tournant',  'pi pi-sync',    NULL, id, 20, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'inventaire'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'inventaire.clotures', 'Clôturés',  'pi pi-lock',    NULL, id, 30, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'inventaire'
  ON CONFLICT (code) DO NOTHING;

-- ── Assignation ROLE_ADMIN : accès complet ────────────────────────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN (
               'commande.dashboard',
               'commande.suggestions',
               'commande.repartition-stock',
               'commande.retour-fournisseur',
               'facturation.factures',
               'facturation.historique',
               'facturation.edition',
               'facturation.recapitulatif',
               'facturation.rapprochement',
               'facturation.avoirs',
               'facturation.automatisation',
               'inventaire.en-cours',
               'inventaire.tournant',
               'inventaire.clotures'
  )
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;



INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'peremptions.lot-perimes',   'Produits périmés',  'pi pi-calendar-times', NULL, id, 10, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'peremptions'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'peremptions.lot-a-detruire','Lots à détruire',   'pi pi-trash',          NULL, id, 20, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'peremptions'
  ON CONFLICT (code) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Onglets Rapports Stock (parent : rapport-stock)
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-stock.stock-alerts',      'Alertes de Stock',          'pi pi-exclamation-triangle', NULL, id, 10, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-stock'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-stock.stock-valuation',   'Valorisation du Stock',     'pi pi-dollar',               NULL, id, 20, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-stock'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-stock.recap-produit-vendu','Récap Produits Vendus/Invendus','pi pi-chart-bar',        NULL, id, 30, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-stock'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-stock.stock-rotation',    'Rotation du Stock',         'pi pi-sync',                 NULL, id, 40, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-stock'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-stock.abc-pareto',        'Analyse ABC Pareto',        'pi pi-sort-amount-down',     NULL, id, 50, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-stock'
  ON CONFLICT (code) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Onglets Rapports Ventes / CA (parent : rapport-ventes)
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.dashboard-ca',  'Dashboard CA',             'pi pi-chart-bar',   NULL, id, 10, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.sales-summary', 'Synthèse des Ventes',      'pi pi-shopping-cart',NULL, id, 20, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.top-products',  'Top Produits',             'pi pi-star-fill',   NULL, id, 30, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.profitability', 'Analyse de Rentabilité',   'pi pi-chart-line',  NULL, id, 40, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.comparative',   'Tableaux Comparatifs',     'pi pi-chart-bar',   NULL, id, 50, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.sales-forecast','Prévisions de Ventes',     'pi pi-chart-line',  NULL, id, 60, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.market-basket', 'Analyse du Panier',        'pi pi-shopping-bag',NULL, id, 70, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Onglets Mouvements de Caisse (parent : mvt-caisse)
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'mvt-caisse.mvt-caisse',          'Mouvements de caisse',   'pi pi-arrows-h',   NULL, id, 10, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'mvt-caisse'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'mvt-caisse.balance',             'Balance caisse',         'pi pi-calculator', NULL, id, 20, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'mvt-caisse'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'mvt-caisse.taxe-report',         'Rapport TVA',            'pi pi-file-pdf',   NULL, id, 30, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'mvt-caisse'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'mvt-caisse.tableau-pharmacien',  'Tableau pharmacien',     'pi pi-table',      NULL, id, 40, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'mvt-caisse'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'mvt-caisse.recapitulatif-caisse','Récapitulatif de caisse', 'pi pi-chart-bar',  NULL, id, 50, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'mvt-caisse'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'mvt-caisse.gestion-caisse',      'Gestion de caisse',      'pi pi-cog',        NULL, id, 60, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'mvt-caisse'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'mvt-caisse.raport-activite',     'Rapport d''activité',    'pi pi-chart-line', NULL, id, 70, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'mvt-caisse'
  ON CONFLICT (code) DO NOTHING;

-- ── Assignation ROLE_ADMIN : accès complet ────────────────────────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN (
               'peremptions.lot-perimes',
               'peremptions.lot-a-detruire',
               'rapport-stock.stock-alerts',
               'rapport-stock.stock-valuation',
               'rapport-stock.recap-produit-vendu',
               'rapport-stock.stock-rotation',
               'rapport-stock.abc-pareto',
               'rapport-ventes.dashboard-ca',
               'rapport-ventes.sales-summary',
               'rapport-ventes.top-products',
               'rapport-ventes.profitability',
               'rapport-ventes.comparative',
               'rapport-ventes.sales-forecast',
               'rapport-ventes.market-basket',
               'mvt-caisse.mvt-caisse',
               'mvt-caisse.balance',
               'mvt-caisse.taxe-report',
               'mvt-caisse.tableau-pharmacien',
               'mvt-caisse.recapitulatif-caisse',
               'mvt-caisse.gestion-caisse',
               'mvt-caisse.raport-activite'
  )
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;



INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'depot.liste-depots', 'Liste des dépôts', 'pi pi-book',     NULL, id, 10, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'depot'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'depot.stock-depot',  'Stock dépôt',      'pi pi-box',      NULL, id, 20, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'depot'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'depot.achat-depot',  'Achats dépôt',     'pi pi-truck',    NULL, id, 30, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'depot'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'depot.retour-depot', 'Retours dépôt',    'pi pi-undo',     NULL, id, 40, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'depot'
  ON CONFLICT (code) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Onglets Rapports Clients & Fournisseurs (parent : rapport-partners)
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-partners.customer-segmentation', 'Segmentation Clients',      'pi pi-user',  NULL, id, 10, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-partners'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-partners.supplier-performance',  'Performance Fournisseurs',  'pi pi-truck', NULL, id, 20, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-partners'
  ON CONFLICT (code) DO NOTHING;

-- ── Assignation ROLE_ADMIN : accès complet ────────────────────────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN (
               'depot.liste-depots',
               'depot.stock-depot',
               'depot.achat-depot',
               'depot.retour-depot',
               'rapport-partners.customer-segmentation',
               'rapport-partners.supplier-performance'
  )
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'ventes.journal.cancel', 'Annuler une vente', 'pi pi-trash', NULL, id, 20, 4, 'ACTION', TRUE
FROM nav_item WHERE code = 'ventes.journal'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'ventes.devis.export', 'Imprimer en PDF', 'pi pi-file-pdf', NULL, id, 20, 4, 'ACTION', TRUE
FROM nav_item WHERE code = 'ventes.devis'
  ON CONFLICT (code) DO NOTHING;


INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN (
               'ventes.journal.cancel',
               'ventes.devis.export'
  )
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE
FROM nav_item WHERE code = 'parametres'
  ON CONFLICT (nav_item_id, role_name) DO UPDATE
                                            SET can_display = TRUE, can_access = TRUE, can_create = TRUE,
                                            can_edit = TRUE, can_delete = TRUE, can_export = TRUE;


INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_RESPONSABLE_COMMANDE', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE
FROM nav_item WHERE code = 'parametres'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_RESPONSABLE_COMMANDE', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item
WHERE code IN ('remises', 'tableaux', 'tva', 'mode-payments')
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_RESPONSABLE_COMMANDE', TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE
FROM nav_item
WHERE code IN ('motif-ajustement', 'motif-retour-produit')
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN (
               'rayon', 'forme-produit', 'famille-produit', 'gamme-produit', 'laboratoire',
               'remises', 'tableaux', 'tva', 'mode-payments',
               'motif-ajustement', 'motif-retour-produit', 'parametres',
               'magasin', 'poste'
  )
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'differes.differes',   'Les différés',          'pi pi-calendar-times', NULL, id, 10, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'differes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'differes.historique', 'Historique règlements', 'pi pi-history',        NULL, id, 20, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'differes'
  ON CONFLICT (code) DO NOTHING;

-- ── ROLE_ADMIN ────────────────────────────────────────────────────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
  SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
  FROM nav_item
  WHERE code IN ('differes.differes', 'differes.historique')
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;



-- ── ROLE_PHARMACIEN — lecture + export (cohérent avec parent) ────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
  SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
  FROM nav_item
  WHERE code IN ('differes.differes', 'differes.historique')
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;




INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'tiers-payant.tiers-payant',       'Tiers payants',            'pi pi-shield', NULL, id, 10, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'tiers-payant'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'tiers-payant.groupe-tiers-payant', 'Groupe de tiers payants', 'pi pi-users',  NULL, id, 20, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'tiers-payant'
  ON CONFLICT (code) DO NOTHING;

-- ── ROLE_ADMIN ────────────────────────────────────────────────────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
  SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
  FROM nav_item
  WHERE code IN ('tiers-payant.tiers-payant', 'tiers-payant.groupe-tiers-payant')
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ── ROLE_CAISSIER — lecture + export (cohérent avec parent) ──────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
  SELECT id, 'ROLE_CAISSIER', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
  FROM nav_item
  WHERE code IN ('tiers-payant.tiers-payant', 'tiers-payant.groupe-tiers-payant')
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ── ROLE_PHARMACIEN — lecture + export (cohérent avec parent) ────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
  SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
  FROM nav_item
  WHERE code IN ('tiers-payant.tiers-payant', 'tiers-payant.groupe-tiers-payant')
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;
