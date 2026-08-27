-- ============================================================================
-- 01_config.sql — Paramétrage applicatif de la démo
--
-- Active la gestion de lot, qui est désactivée dans le référentiel livré.
-- Voir docs/PLAN-GENERATION-DONNEES-DEMO.md §9.
--
-- ATTENTION : ces clés sont mises en cache par l'application
-- (CacheConfiguration). Un redémarrage — ou une purge du cache — est
-- nécessaire si l'instance tourne déjà.
-- ============================================================================

\i _header.sql

\echo '>> 01_config : paramétrage'

-- Gestion de lot à la réception et à la vente.
UPDATE app_configuration SET value = '1', updated = NOW()
 WHERE name = 'APP_GESTION_LOT';

-- Saisie par lot en inventaire. Activée de pair : laisser l'inventaire en mode
-- non-lot alors que le stock est ventilé par lot produirait des écarts
-- artificiels à la clôture (InventoryCloseServiceImpl passe p_gestion_lot à la
-- procédure de clôture).
UPDATE app_configuration SET value = '1', updated = NOW()
 WHERE name = 'APP_GESTION_LOT_INVENTAIRE';

-- Contrôle : les deux clés doivent exister et valoir 1.
DO $$
DECLARE v_manquantes text;
BEGIN
    SELECT string_agg(k, ', ') INTO v_manquantes
      FROM unnest(ARRAY['APP_GESTION_LOT', 'APP_GESTION_LOT_INVENTAIRE']) k
     WHERE NOT EXISTS (
         SELECT 1 FROM app_configuration c WHERE c.name = k AND c.value = '1'
     );
    IF v_manquantes IS NOT NULL THEN
        RAISE EXCEPTION 'Clé(s) de configuration absente(s) ou non activée(s) : %', v_manquantes;
    END IF;
END $$;

\echo '<< 01_config : terminé'
