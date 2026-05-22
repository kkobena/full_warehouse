
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
VALUES ('comptabilite', 'Comptabilité', 'pi pi-book', '/comptabilite', NULL, 85, 2, 'ROUTE', TRUE)
ON CONFLICT (code) DO NOTHING;


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'comptabilite.balance', 'Balance caisse', 'pi pi-calculator', NULL, id, 10, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'comptabilite'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'comptabilite.taxe-report', 'Rapport TVA', 'pi pi-file-pdf', NULL, id, 20, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'comptabilite'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'comptabilite.tableau-pharmacien', 'Tableau pharmacien', 'pi pi-table', NULL, id, 30, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'comptabilite'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'comptabilite.recapitulatif-caisse', 'Récapitulatif de caisse', 'pi pi-chart-bar', NULL, id, 40, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'comptabilite'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
  SELECT 'comptabilite.raport-activite', 'Rapport d''activité', 'pi pi-chart-line', NULL, id, 50, 3, 'SECTION', TRUE
  FROM nav_item WHERE code = 'comptabilite'
  ON CONFLICT (code) DO NOTHING;


INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
  SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
  FROM nav_item
  WHERE code IN (
    'comptabilite',
    'comptabilite.balance',
    'comptabilite.taxe-report',
    'comptabilite.tableau-pharmacien',
    'comptabilite.recapitulatif-caisse',
    'comptabilite.raport-activite'
  )
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
  SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
  FROM nav_item
  WHERE code IN (
    'comptabilite',
    'comptabilite.balance',
    'comptabilite.taxe-report',
    'comptabilite.tableau-pharmacien',
    'comptabilite.recapitulatif-caisse',
    'comptabilite.raport-activite'
  )
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;



INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-stock.stock-abc', 'Analyse ABC', 'pi pi-chart-pie', NULL, id, 45, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-stock'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-stock.stock-abc'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-stock.stock-abc'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-finance.profitability', 'Analyse de Rentabilité', 'pi pi-chart-line', NULL, id, 15, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-finance'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-finance.profitability'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-finance.profitability'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-finance.creances', 'Créances TP', 'pi pi-clock', NULL, id, 25, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-finance'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-finance.creances'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-finance.creances'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-finance.situation-creances', 'Situation Créances', 'pi pi-table', NULL, id, 35, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-finance'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-finance.situation-creances'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-finance.situation-creances'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-finance.vieillissement-differes', 'Différés Clients', 'pi pi-users', NULL, id, 45, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-finance'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-finance.vieillissement-differes'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-finance.vieillissement-differes'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-finance.avoirs-analytics', 'Avoirs TP', 'pi pi-undo', NULL, id, 55, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-finance'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-finance.avoirs-analytics'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-finance.avoirs-analytics'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-finance.taux-recouvrement-tp', 'Taux Recouvrement', 'pi pi-percentage', NULL, id, 65, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-finance'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-finance.taux-recouvrement-tp'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-finance.taux-recouvrement-tp'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.sales-by-staff', 'Performance Vendeurs', 'pi pi-users', NULL, id, 80, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-ventes.sales-by-staff'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-ventes.sales-by-staff'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.seasonality', 'Saisonnalité CA', 'pi pi-chart-line', NULL, id, 90, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-ventes.seasonality'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-ventes.seasonality'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.generics-substitution', 'Génériques & Substitution', 'pi pi-sync', NULL, id, 100, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-ventes.generics-substitution'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-ventes.generics-substitution'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.remises-analysis', 'Analyse des Remises', 'pi pi-tag', NULL, id, 110, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-ventes.remises-analysis'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-ventes.remises-analysis'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-ventes.client-retention', 'Rétention Clients', 'pi pi-heart', NULL, id, 120, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-ventes'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-ventes.client-retention'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-ventes.client-retention'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-stock.demarque', 'Démarque & Ajustements', 'pi pi-minus-circle', NULL, id, 50, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-stock'
  ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item WHERE code = 'rapport-stock.demarque'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item WHERE code = 'rapport-stock.demarque'
  ON CONFLICT (nav_item_id, role_name) DO NOTHING;
