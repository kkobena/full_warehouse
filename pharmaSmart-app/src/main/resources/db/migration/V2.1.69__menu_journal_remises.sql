-- Journal des remises, promotions et UG (docs/PLAN-JOURNAL-REMISES.md) : son sous-menu dans la
-- Gestion Courante, juste après « Ventes » et avant « Mouvements Caisse », avec un onglet par
-- partie ; et l'onglet « Journal » du module Promotions.

-- La place juste après « Ventes » : les entrées suivantes de la Gestion Courante reculent d'un cran.
-- Rien n'est codé en dur : l'ordre se lit sur « ventes », que V2.1.42 a déjà fait suivre d'« exports ».
UPDATE nav_item n
SET ordre = n.ordre + 1
FROM nav_item v
WHERE v.code = 'ventes'
  AND n.parent_id = v.parent_id
  AND n.ordre > v.ordre
  AND NOT EXISTS (SELECT 1 FROM nav_item j WHERE j.code = 'journal-remises');

INSERT INTO nav_item (code, libelle, titre_long, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'journal-remises', 'Journal des remises', 'Journal des remises, promotions et UG', 'pi pi-percentage',
       '/journal-remises', v.parent_id, v.ordre + 1, 2, 'ROUTE', TRUE
FROM nav_item v
WHERE v.code = 'ventes'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item (code, libelle, titre_long, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT s.code, s.libelle, s.titre_long, s.icon, NULL, p.id, s.ordre, 3, 'SECTION', TRUE
FROM nav_item p
       CROSS JOIN (VALUES ('journal-remises.synthese', 'Synthèse', 'Ce que les remises ont coûté, ce que les UG ont rapporté', 'pi pi-chart-bar', 10),
                          ('journal-remises.promotions', 'Promotions', 'Les lignes remisées par une promotion, unités offertes comprises', 'pi pi-tags', 20),
                          ('journal-remises.remises', 'Remises produit', 'Les lignes remisées par une grille, seule ou cumulée', 'pi pi-percentage', 30),
                          ('journal-remises.ug', 'UG vendues', 'Les unités gratuites du fournisseur vendues, et leur marge', 'pi pi-gift', 40))
  AS s(code, libelle, titre_long, icon, ordre)
WHERE p.code = 'journal-remises'
ON CONFLICT (code) DO NOTHING;

-- Module Promotions : la liste actuelle et le journal des promotions deviennent deux onglets.
INSERT INTO nav_item (code, libelle, titre_long, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT s.code, s.libelle, s.titre_long, s.icon, NULL, p.id, s.ordre, 3, 'SECTION', TRUE
FROM nav_item p
       CROSS JOIN (VALUES ('promotions.liste', 'Promotions', 'Créer, valider, suivre les promotions', 'pi pi-tags', 10),
                          ('promotions.journal', 'Journal', 'Les lignes remisées par les promotions', 'pi pi-list', 20))
  AS s(code, libelle, titre_long, icon, ordre)
WHERE p.code = 'promotions'
ON CONFLICT (code) DO NOTHING;

-- La liste garde exactement les droits que chaque rôle avait déjà sur le module.
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT l.id, r.role_name, r.can_display, r.can_access, r.can_create, r.can_edit, r.can_delete, r.can_export, r.can_execute
FROM nav_item l
       JOIN nav_item p ON p.code = 'promotions'
       JOIN nav_item_role r ON r.nav_item_id = p.id
WHERE l.code = 'promotions.liste'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- Administrateur : tout. Titulaire : lecture et export, le journal ne se modifie pas. Les autres rôles
-- n'y ont pas accès par défaut ; l'administrateur l'ouvre depuis l'écran des rôles.
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code = 'journal-remises'
   OR code LIKE 'journal-remises.%'
   OR code = 'promotions.journal'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item
WHERE code = 'journal-remises'
   OR code LIKE 'journal-remises.%'
   OR code = 'promotions.journal'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
