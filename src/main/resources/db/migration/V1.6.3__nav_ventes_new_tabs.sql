-- ═══════════════════════════════════════════════════════════════════════════════
-- Nouveaux onglets du module Ventes :
--   • Annulations  (ordre 50)
--   • Ventes dépôt (ordre 60)
--   • Avoirs clients (ordre 70)
--   • Tableau de bord KPIs (ordre 80)
-- ═══════════════════════════════════════════════════════════════════════════════

-- ── ventes.annulations ───────────────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'ventes.annulations', 'Annulations', 'pi pi-ban', NULL, id, 50, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'ventes'
ON CONFLICT (code) DO NOTHING;

-- ── ventes.vente-depot ───────────────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'ventes.vente-depot', 'Ventes dépôt', 'pi pi-warehouse', NULL, id, 60, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'ventes'
ON CONFLICT (code) DO NOTHING;

-- ── ventes.avoirs ────────────────────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'ventes.avoirs', 'Avoirs clients', 'pi pi-replay', NULL, id, 70, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'ventes'
ON CONFLICT (code) DO NOTHING;

-- ── ventes.kpi ───────────────────────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'ventes.kpi', 'Tableau de bord', 'pi pi-chart-bar', NULL, id, 80, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'ventes'
ON CONFLICT (code) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Permissions ROLE_ADMIN — accès complet à tous les nouveaux onglets
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN (
    'ventes.annulations',
    'ventes.vente-depot',
    'ventes.avoirs',
    'ventes.kpi'
)
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Permissions ROLE_CAISSIER
--   • annulations  : consultation (can_display) — le caissier voit ses annulations
--   • vente-depot  : accès complet — le caissier gère les ventes dépôt
--   • avoirs       : consultation (can_display)
--   • kpi          : non accordé (vue de gestion réservée au pharmacien)
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_CAISSIER', TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE
FROM nav_item
WHERE code IN ('ventes.annulations', 'ventes.avoirs')
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_CAISSIER', TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, TRUE
FROM nav_item
WHERE code = 'ventes.vente-depot'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Permissions ROLE_PHARMACIEN — lecture + export sur tous les nouveaux onglets
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item
WHERE code IN (
    'ventes.annulations',
    'ventes.vente-depot',
    'ventes.avoirs',
    'ventes.kpi'
)
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
