
-- Place libérée en position 1 : les entrées de premier niveau qui suivent les actions de vente reculent d'un cran.
UPDATE nav_item
SET ordre = ordre + 1
WHERE parent_id IS NULL
  AND ordre >= 1
  AND NOT EXISTS (SELECT 1 FROM nav_item p WHERE p.code = 'pilotage');

INSERT INTO nav_item (code, libelle, titre_long, icon, router_link, parent_id, ordre, niveau, target_type, actif)
VALUES ('pilotage', 'Pilotage', 'Pilotage de l''officine', 'pi pi-gauge', '/pilotage', NULL, 1, 1, 'ROUTE', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, titre_long, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT s.code, s.libelle, s.titre_long, s.icon, NULL, p.id, s.ordre, 3, 'SECTION', TRUE
FROM nav_item p
       CROSS JOIN (VALUES ('pilotage.tableau-de-bord', 'Tableau de bord', 'Comment va l''officine ?', 'pi pi-gauge', 10),
                          ('pilotage.analyser', 'Analyser', 'Pourquoi ça bouge ?', 'pi pi-chart-scatter', 20),
                          ('pilotage.comparer-annees', 'Comparer les années', 'Où en est-on par rapport aux années passées ?', 'pi pi-chart-line', 30),
                          ('pilotage.rentabilite-remises', 'Rentabilité & remises', 'Où est-ce que je gagne ou perds de l''argent ?', 'pi pi-percentage', 40),
                          ('pilotage.achats-stock', 'Achats & stock', 'Mes achats et mon stock sont-ils maîtrisés ?', 'pi pi-box', 50),
                          ('pilotage.tresorerie-tiers-payant', 'Trésorerie & tiers payant', 'Mon argent rentre-t-il ?', 'pi pi-wallet', 60),
                          ('pilotage.clients-equipe', 'Clients & équipe', 'Qui vient, qui vend ?', 'pi pi-users', 70),
                          ('pilotage.objectifs', 'Objectifs', 'Suis-je en avance ou en retard ?', 'pi pi-flag', 80))
  AS s(code, libelle, titre_long, icon, ordre)
WHERE p.code = 'pilotage'
ON CONFLICT (code) DO NOTHING;

-- Titulaire et administrateur : tout le pilotage, export compris. Les autres rôles n'y ont pas accès par défaut ;
-- l'administrateur ouvre ce qu'il veut depuis l'écran des rôles.
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code = 'pilotage'
   OR code LIKE 'pilotage.%'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item
WHERE code = 'pilotage'
   OR code LIKE 'pilotage.%'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
