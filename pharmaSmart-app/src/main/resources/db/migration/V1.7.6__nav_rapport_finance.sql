-- ═══════════════════════════════════════════════════════════════════════════════
-- Module Rapports Finance & Rentabilité
--   • rapport-finance  (niveau 2, ordre 3, /reports/finance)
--   • rapport-finance.pnl-analytique          (ordre 10)
--   • rapport-finance.vieillissement-creances (ordre 20)
--   • rapport-finance.concentration-payers    (ordre 30)
--   • rapport-finance.cash-flow-bfr           (ordre 40)
-- ═══════════════════════════════════════════════════════════════════════════════

-- ── rapport-finance (conteneur niveau 2) ─────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-finance',
       'Finance & Rentabilité',
       'pi pi-wallet',
       '/reports/finance',
       id,
       3,
       2,
       'ROUTE',
       TRUE
FROM nav_item WHERE code = 'rapports'
ON CONFLICT (code) DO NOTHING;

-- ── rapport-finance.pnl-analytique ───────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-finance.pnl-analytique', 'P&L Analytique', 'pi pi-chart-line', NULL, id, 10, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-finance'
ON CONFLICT (code) DO NOTHING;

-- ── rapport-finance.vieillissement-creances ──────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-finance.vieillissement-creances', 'Vieillissement Créances', 'pi pi-clock', NULL, id, 20, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-finance'
ON CONFLICT (code) DO NOTHING;

-- ── rapport-finance.concentration-payers ─────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-finance.concentration-payers', 'Concentration Payeurs', 'pi pi-users', NULL, id, 30, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-finance'
ON CONFLICT (code) DO NOTHING;

-- ── rapport-finance.cash-flow-bfr ────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'rapport-finance.cash-flow-bfr', 'BFR & Liquidité', 'pi pi-money-bill', NULL, id, 40, 3, 'SECTION', TRUE
FROM nav_item WHERE code = 'rapport-finance'
ON CONFLICT (code) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Permissions ROLE_ADMIN — accès complet au conteneur et aux 4 onglets
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN (
    'rapport-finance',
    'rapport-finance.pnl-analytique',
    'rapport-finance.vieillissement-creances',
    'rapport-finance.concentration-payers',
    'rapport-finance.cash-flow-bfr'
)
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════════
-- Permissions ROLE_PHARMACIEN — lecture + export (vue de gestion)
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE
FROM nav_item
WHERE code IN (
    'rapport-finance',
    'rapport-finance.pnl-analytique',
    'rapport-finance.vieillissement-creances',
    'rapport-finance.concentration-payers',
    'rapport-finance.cash-flow-bfr'
)
ON CONFLICT (nav_item_id, role_name) DO NOTHING;
