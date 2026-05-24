-- ═══════════════════════════════════════════════════════════════════════════════
-- Module Retours client & Clôture avoirs
--   • ventes.retours-client     (SECTION, ordre 75 — entre avoirs 70 et kpi 80)
--   • ventes.retours-client.create    (ACTION — créer un retour)
--   • ventes.avoirs.cloturer          (ACTION — clôturer un avoir)
-- ═══════════════════════════════════════════════════════════════════════════════

-- ── ventes.retours-client ─────────────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'ventes.retours-client',
       'Retours clients',
       'pi pi-undo',
       NULL,
       id,
       75,
       3,
       'SECTION',
       TRUE
FROM nav_item WHERE code = 'ventes'
ON CONFLICT (code) DO NOTHING;

-- ── ventes.retours-client.create ─────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'ventes.retours-client.create',
       'Créer un retour client',
       'pi pi-plus',
       NULL,
       id,
       10,
       4,
       'ACTION',
       TRUE
FROM nav_item WHERE code = 'ventes.retours-client'
ON CONFLICT (code) DO NOTHING;

-- ── ventes.avoirs.cloturer ────────────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'ventes.avoirs.cloturer',
       'Clôturer un avoir',
       'pi pi-check-circle',
       NULL,
       id,
       10,
       4,
       'ACTION',
       TRUE
FROM nav_item WHERE code = 'ventes.avoirs'
ON CONFLICT (code) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Permissions ROLE_ADMIN : accès complet
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN (
    'ventes.retours-client',
    'ventes.retours-client.create',
    'ventes.avoirs.cloturer'
)
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Permissions ROLE_CAISSIER
--   • retours-client  : affichage + création + exécution
--   • avoirs.cloturer : exécution uniquement
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_CAISSIER', TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, TRUE
FROM nav_item
WHERE code IN ('ventes.retours-client', 'ventes.retours-client.create')
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_CAISSIER', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE
FROM nav_item
WHERE code = 'ventes.avoirs.cloturer'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Permissions ROLE_PHARMACIEN : lecture + export (supervision)
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item
WHERE code IN (
    'ventes.retours-client',
    'ventes.retours-client.create',
    'ventes.avoirs.cloturer'
)
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
