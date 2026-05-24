-- ═══════════════════════════════════════════════════════════════════════════════
-- Module Finances — intégration dans facturation et mvt-caisse
-- ── facturation : comptes-fournisseurs (ordre 80) + remises-rfa (ordre 90)
-- ── mvt-caisse  : declaration-tva (ordre 80)     + export-comptable (ordre 90)
-- ═══════════════════════════════════════════════════════════════════════════════

-- ── facturation.comptes-fournisseurs ─────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'facturation.comptes-fournisseurs',
       'Comptes fournisseurs',
       'pi pi-truck',
       NULL,
       id,
       80,
       3,
       'SECTION',
       TRUE
FROM nav_item WHERE code = 'facturation'
ON CONFLICT (code) DO NOTHING;

-- ── facturation.remises-rfa ───────────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'facturation.remises-rfa',
       'Remises & RFA',
       'pi pi-tag',
       NULL,
       id,
       90,
       3,
       'SECTION',
       TRUE
FROM nav_item WHERE code = 'facturation'
ON CONFLICT (code) DO NOTHING;

-- ── mvt-caisse.declaration-tva ───────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'mvt-caisse.declaration-tva',
       'Déclaration TVA',
       'pi pi-percentage',
       NULL,
       id,
       80,
       3,
       'SECTION',
       TRUE
FROM nav_item WHERE code = 'mvt-caisse'
ON CONFLICT (code) DO NOTHING;

-- ── mvt-caisse.export-comptable ───────────────────────────────────────────────
INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'mvt-caisse.export-comptable',
       'Export comptable',
       'pi pi-download',
       NULL,
       id,
       90,
       3,
       'SECTION',
       TRUE
FROM nav_item WHERE code = 'mvt-caisse'
ON CONFLICT (code) DO NOTHING;

-- ── Permissions ROLE_ADMIN : accès complet ────────────────────────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_ADMIN', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
FROM nav_item
WHERE code IN (
    'facturation.comptes-fournisseurs',
    'facturation.remises-rfa',
    'mvt-caisse.declaration-tva',
    'mvt-caisse.export-comptable'
)
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ── Permissions ROLE_PHARMACIEN ───────────────────────────────────────────────
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_PHARMACIEN', TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE
FROM nav_item
WHERE code IN (
    'facturation.comptes-fournisseurs',
    'facturation.remises-rfa',
    'mvt-caisse.declaration-tva',
    'mvt-caisse.export-comptable'
)
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

-- ── Permissions ROLE_RESPONSABLE_COMMANDE : comptes fournisseurs uniquement ───
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete, can_export, can_execute)
SELECT id, 'ROLE_RESPONSABLE_COMMANDE', TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE
FROM nav_item
WHERE code = 'facturation.comptes-fournisseurs'
ON CONFLICT (nav_item_id, role_name) DO NOTHING;

ALTER TABLE groupe_fournisseur
  ADD COLUMN IF NOT EXISTS jours_credit   INTEGER DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS jours_critique INTEGER DEFAULT NULL;

COMMENT ON COLUMN groupe_fournisseur.jours_credit   IS 'Délai de crédit accordé par ce groupe de fournisseurs (jours) ; NULL = utilise le paramètre APP_AP_DEFAULT_CREDIT_DAYS';
COMMENT ON COLUMN groupe_fournisseur.jours_critique IS 'Délai supplémentaire après échéance avant passage au statut CRITIQUE pour ce groupe (jours) ; NULL = utilise APP_AP_DEFAULT_CRITIQUE_DAYS';

ALTER TABLE fournisseur
  ADD COLUMN IF NOT EXISTS jours_credit   INTEGER DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS jours_critique INTEGER DEFAULT NULL;

COMMENT ON COLUMN fournisseur.jours_credit   IS 'Délai de crédit accordé par ce fournisseur (jours) ; prioritaire sur le groupe ; NULL = hérite du groupe ou du paramètre APP_AP_DEFAULT_CREDIT_DAYS';
COMMENT ON COLUMN fournisseur.jours_critique IS 'Délai supplémentaire après échéance avant passage au statut CRITIQUE pour ce fournisseur (jours) ; prioritaire sur le groupe ; NULL = hérite du groupe ou de APP_AP_DEFAULT_CRITIQUE_DAYS';

-- Palier et taux RFA (Remises de Fin d'Année) par fournisseur et par groupe fournisseur
ALTER TABLE fournisseur
  ADD COLUMN IF NOT EXISTS palier_rfa BIGINT  DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS taux_rfa   INTEGER DEFAULT NULL;

COMMENT ON COLUMN fournisseur.palier_rfa IS 'Seuil d''achat annuel (montant HT, en centimes) a partir duquel la RFA s''applique pour ce fournisseur ; NULL = pas de palier defini';
COMMENT ON COLUMN fournisseur.taux_rfa   IS 'Taux de Remise de Fin d''Annee en pourcentage entier (ex. 3 = 3 %%) applicable une fois le palier_rfa atteint ; NULL = pas de RFA';

ALTER TABLE groupe_fournisseur
  ADD COLUMN IF NOT EXISTS palier_rfa BIGINT  DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS taux_rfa   INTEGER DEFAULT NULL;

COMMENT ON COLUMN groupe_fournisseur.palier_rfa IS 'Seuil d''achat annuel (montant HT, en centimes) a partir duquel la RFA s''applique pour ce groupe ; utilise si non defini sur le fournisseur ; NULL = pas de palier';
COMMENT ON COLUMN groupe_fournisseur.taux_rfa   IS 'Taux de Remise de Fin d''Annee en pourcentage entier (ex. 3 = 3 %%) pour ce groupe ; utilise si non defini sur le fournisseur ; NULL = pas de RFA';


-- Paramètres applicatifs par défaut pour le module Comptes Fournisseurs (AP)
INSERT INTO app_configuration(name, description, value, other_value, value_type)
VALUES ('APP_AP_DEFAULT_CREDIT_DAYS',
        'Délai de crédit fournisseur par défaut (jours) — utilisé si non défini sur le fournisseur ou son groupe',
        '30', null, 'NUMBER')
  ON CONFLICT (name) DO NOTHING;

INSERT INTO app_configuration(name, description, value, other_value, value_type)
VALUES ('APP_AP_DEFAULT_CRITIQUE_DAYS',
        'Délai supplémentaire (jours) après échéance avant passage au statut CRITIQUE — utilisé si non défini sur le fournisseur ou son groupe',
        '30', null, 'NUMBER')
  ON CONFLICT (name) DO NOTHING;
