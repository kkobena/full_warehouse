-- ============================================================
-- V1.2.3 — Paramètres gestion lot inventaire
--
-- 1. APP_GESTION_LOT_INVENTAIRE : activer/désactiver la gestion
--    des lots durant l'inventaire (0 = non, 1 = oui)
-- 2. APP_MODE_SAISIE_LOT_INVENTAIRE : mode d'affichage de la
--    saisie par lot (LOT_PLAT, MODAL, EXPANSION)
-- ============================================================
alter table app_configuration
  alter column other_value type varchar(100) ;
INSERT INTO app_configuration (name, value, description, value_type, other_value)
VALUES (
    'APP_GESTION_LOT_INVENTAIRE',
    '0',
    'Activer la gestion des lots durant l''inventaire (0=non, 1=oui)',
    'NUMBER',
    'LOT_PLAT,MODAL,EXPANSION'
)
ON CONFLICT (name) DO NOTHING;

INSERT INTO app_configuration (name, value, description, value_type, other_value)
VALUES (
    'APP_MODE_SAISIE_LOT_INVENTAIRE',
    'LOT_PLAT',
    'Mode de saisie des lots en inventaire (LOT_PLAT, MODAL, EXPANSION)',
    'STRING',
    'LOT_PLAT,MODAL,EXPANSION'
)
ON CONFLICT (name) DO NOTHING;
