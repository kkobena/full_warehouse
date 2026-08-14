
CREATE TABLE IF NOT EXISTS license_state
(
    id                         INTEGER     NOT NULL PRIMARY KEY,
    license_token              TEXT        NULL,
    license_id                 VARCHAR(64) NULL,
    expires_at                 DATE        NULL,
    activated_at               TIMESTAMP WITH TIME ZONE NULL,
    activated_by               VARCHAR(50) NULL,
    -- Sonde anti-recul d'horloge : rafraîchie au démarrage et toutes les 15 min.
    last_seen_instant          TIMESTAMP WITH TIME ZONE NULL,
    hardware_fingerprint       VARCHAR(128) NULL,
    -- Début de divergence d'empreinte : c'est cette date qui ouvre le délai de régularisation
    -- de 14 jours avant blocage (§3.4, couche 2). NULL = aucune divergence en cours.
    fingerprint_mismatch_since TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT ck_license_state_singleton CHECK (id = 1)
);


CREATE TABLE IF NOT EXISTS license_audit
(
    id         BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(40)              NOT NULL,
    detail     TEXT                     NULL,
    license_id VARCHAR(64)              NULL,
    user_login VARCHAR(50)              NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_license_audit_created_at ON license_audit (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_license_audit_event_type ON license_audit (event_type);


ALTER TABLE nav_item
    ADD COLUMN IF NOT EXISTS required_feature VARCHAR(40) NULL;


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'gestion-licence',
       'Gérer ma licence',
       'pi pi-key',
       '/admin/license',
       id,
       3,
       2,
       'ROUTE',
       TRUE
FROM nav_item
WHERE code = 'administration' ON CONFLICT (code) DO NOTHING;

-- Réservée à ROLE_ADMIN : le dépôt d'un fichier de licence est une opération d'administration.
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete,
                           can_export, can_execute)
SELECT id,
       'ROLE_ADMIN',
       TRUE,
       TRUE,
       TRUE,
       FALSE,
       FALSE,
       FALSE,
       TRUE
FROM nav_item
WHERE code = 'gestion-licence' ON CONFLICT (nav_item_id, role_name) DO
UPDATE SET can_display = TRUE, can_access = TRUE, can_create = TRUE, can_execute = TRUE;


INSERT INTO nav_item (code, libelle, icon, router_link, parent_id, ordre, niveau, target_type, actif)
SELECT 'pr-gere-licence',
       'Activer et renouveler la licence',
       NULL,
       NULL,
       id,
       1,
       3,
       'ACTION',
       TRUE
FROM nav_item
WHERE code = 'gestion-licence' ON CONFLICT (code) DO NOTHING;

-- Accordé d'office à ROLE_ADMIN : le comportement antérieur est ainsi préservé à la migration,
-- aucun administrateur ne perd l'accès. Les autres rôles se configurent depuis l'application.
INSERT INTO nav_item_role (nav_item_id, role_name, can_display, can_access, can_create, can_edit, can_delete,
                           can_export, can_execute)
SELECT id,
       'ROLE_ADMIN',
       TRUE,
       TRUE,
       FALSE,
       FALSE,
       FALSE,
       FALSE,
       TRUE
FROM nav_item
WHERE code = 'pr-gere-licence' ON CONFLICT (nav_item_id, role_name) DO
UPDATE SET can_display = TRUE, can_access = TRUE, can_execute = TRUE;


UPDATE nav_item
SET router_link = '/licence'
WHERE code = 'gestion-licence';

