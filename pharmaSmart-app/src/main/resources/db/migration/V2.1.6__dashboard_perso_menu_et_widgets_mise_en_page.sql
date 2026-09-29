-- Dashboard personnalisable — phase 1 : entrée de menu et widgets de mise en page.

-- ── Entrée de menu, sous « Rapports & Statistiques » ─────────────────────────
-- Accordée par défaut aux rôles qui voient déjà ce groupe ; l'administrateur l'étend ensuite.
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'dashboard-perso', 'Mon tableau de bord', 'pi pi-th-large', '/dashboard', id, 0, 2, 'ROUTE', TRUE
FROM nav_item
WHERE code = 'rapports'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT d.id, r.role_name, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE
FROM nav_item d
         JOIN nav_item g ON g.code = 'rapports'
         JOIN nav_item_role r ON r.nav_item_id = g.id AND r.can_display = TRUE
WHERE d.code = 'dashboard-perso'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ── Widgets de mise en page : sans données, autorisés à tous les rôles ────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT w.code, w.libelle, w.icon, NULL, p.id, w.ordre, 2, 'WIDGET', TRUE
FROM (VALUES ('widget.note', 'Note', 'pi pi-align-left', 900),
             ('widget.titre-section', 'Titre de section', 'pi pi-minus', 910)) AS w(code, libelle, icon, ordre)
         CROSS JOIN nav_item p
WHERE p.code = 'dashboard-perso.widgets'
ON CONFLICT (code) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT n.id, a.name, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE
FROM nav_item n
         CROSS JOIN authority a
WHERE n.code IN ('widget.note', 'widget.titre-section')
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
