-- Dashboard personnalisable — socle des droits par widget (phase 0).
--
-- Chaque widget sera un nav_item de type WIDGET, code « widget.<clé> », rangé sous une catégorie.
-- can_display sur ce nav_item = droit d'ajouter le widget et d'en lire les données. Les conteneurs
-- ci-dessous (SECTION) servent seulement à ranger les widgets dans l'écran d'attribution des menus :
-- ils ne s'affichent jamais dans la navigation et n'accordent aucun droit.

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
VALUES ('dashboard-perso.widgets', 'Widgets du tableau de bord', 'pi pi-th-large', NULL, NULL, 990, 1, 'SECTION', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT c.code, c.libelle, c.icon, NULL, p.id, c.ordre, 2, 'SECTION', TRUE
FROM (VALUES ('dashboard-perso.widgets.ventes', 'Ventes', 'pi pi-shopping-cart', 10),
             ('dashboard-perso.widgets.caisse', 'Caisse', 'pi pi-wallet', 20),
             ('dashboard-perso.widgets.stock', 'Stock', 'pi pi-box', 30),
             ('dashboard-perso.widgets.achats', 'Achats', 'pi pi-truck', 40),
             ('dashboard-perso.widgets.finances', 'Finances', 'pi pi-chart-line', 50),
             ('dashboard-perso.widgets.clients', 'Clients', 'pi pi-users', 60)) AS c(code, libelle, icon, ordre)
         CROSS JOIN nav_item p
WHERE p.code = 'dashboard-perso.widgets'
ON CONFLICT (code) DO NOTHING;

-- Les layouts enregistrés par les utilisateurs recevaient component_key = 'ROUTE' par défaut
-- alors qu'ils portent une grille : on leur donne la clé du dashboard personnalisable.
UPDATE dashboard_layout
SET component_key = 'CUSTOM'
WHERE user_id IS NOT NULL
  AND is_route = FALSE
  AND component_key = 'ROUTE';
