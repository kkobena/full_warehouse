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

-- Budget mensuel de commande.
--
-- La valeur livrée est 0, c'est-à-dire ILLIMITÉ : l'indicateur de budget ne
-- s'affiche alors jamais, et l'écran de réapprovisionnement ne montre pas le
-- garde-fou dont il dispose (ACH-20). On pose un plafond de 12 000 000 F, en
-- dessous de ce que la démonstration engage dans le mois — c'est le cas qu'il
-- faut voir : celui où l'officine dépasse son enveloppe et doit arbitrer.
UPDATE app_configuration SET value = '12000000', updated = NOW()
 WHERE name = 'APP_BUDGET_MENSUEL_COMMANDE';

-- Allocation automatique du fonds de caisse.
--
-- Livree a 0 : le caissier doit alors ouvrir sa caisse lui-meme et saisir son
-- fonds. L'officine de demonstration a fait le choix inverse -- le responsable
-- alloue un fonds par defaut, et la caisse s'ouvre a la premiere vente -- parce
-- que c'est l'organisation la plus repandue : le caissier ne detient pas
-- l'argent du fonds, il le recoit.
--
-- `other_value` porte le MONTANT alloue : sans lui, l'ouverture automatique se
-- ferait a zero et le premier rendu de monnaie serait impossible.
UPDATE app_configuration
   SET value = '1', other_value = '50000', updated = NOW()
 WHERE name = 'APP_CASH_FUND';

-- Contrôle : les trois clés doivent exister et valoir 1.
DO $$
DECLARE v_manquantes text;
BEGIN
    SELECT string_agg(k, ', ') INTO v_manquantes
      FROM unnest(ARRAY['APP_GESTION_LOT', 'APP_GESTION_LOT_INVENTAIRE', 'APP_CASH_FUND']) k
     WHERE NOT EXISTS (
         SELECT 1 FROM app_configuration c WHERE c.name = k AND c.value = '1'
     );
    IF v_manquantes IS NOT NULL THEN
        RAISE EXCEPTION 'Clé(s) de configuration absente(s) ou non activée(s) : %', v_manquantes;
    END IF;
END $$;


-- ---------------------------------------------------------------------------
-- Planifications : rattraper ce qu'un reset anterieur a emporte
--
-- Les lignes de planification viennent de la migration V1.4.6, pas de la demo :
-- une par periodicite et par nature de facture, plus celle de la certification
-- FNE. Le reset les preservait pas jusqu'ici et les a donc videes sur les bases
-- deja montees ; Flyway, lui, ne rejoue pas une migration appliquee, si bien que
-- l'onglet « Automatisation » restait desert sans espoir de retour.
--
-- Ce bloc les repose si elles manquent, et ne fait rien sinon : la contrainte
-- d'unicite (periodicite, facture_provisoire) garantit qu'on n'en double aucune.
-- ---------------------------------------------------------------------------
INSERT INTO planification_facturation
    (libelle, periodicite, heure_declenchement, actif, facture_provisoire,
     derniere_periode_fin, created, updated)
VALUES
    ('Facturation mensuelle',                'MENSUEL',   '09:00:00', FALSE, FALSE,
     (date_trunc('month', CURRENT_DATE) - INTERVAL '1 day')::DATE, NOW(), NOW()),
    ('Facturation quinzainiere',             'QUINZAINE', '09:00:00', FALSE, FALSE,
     (date_trunc('month', CURRENT_DATE) - INTERVAL '1 day')::DATE, NOW(), NOW()),
    ('Facturation bimensuelle',              'BIMENSUEL', '09:00:00', FALSE, FALSE,
     (date_trunc('month', CURRENT_DATE) - INTERVAL '1 day')::DATE, NOW(), NOW()),
    ('Facturation mensuelle (provisoire)',   'MENSUEL',   '09:00:00', FALSE, TRUE,
     (date_trunc('month', CURRENT_DATE) - INTERVAL '1 day')::DATE, NOW(), NOW()),
    ('Facturation quinzainiere (provisoire)','QUINZAINE', '09:00:00', FALSE, TRUE,
     (date_trunc('month', CURRENT_DATE) - INTERVAL '1 day')::DATE, NOW(), NOW()),
    ('Facturation bimensuelle (provisoire)', 'BIMENSUEL', '09:00:00', FALSE, TRUE,
     (date_trunc('month', CURRENT_DATE) - INTERVAL '1 day')::DATE, NOW(), NOW())
ON CONFLICT (periodicite, facture_provisoire) DO NOTHING;

INSERT INTO planification_certification_fne
    (libelle, heure_declenchement, actif, prochaine_execution, created, updated)
SELECT 'Certification FNE automatique', '09:00:00', FALSE,
       (CURRENT_DATE + INTERVAL '1 day')::TIMESTAMP + TIME '02:00:00', NOW(), NOW()
 WHERE NOT EXISTS (SELECT 1 FROM planification_certification_fne);

-- Une planification active : l'ecran doit montrer un prochain declenchement et
-- offrir l'execution manuelle, qui n'existe que sur une ligne active.
-- La prochaine echeance se calcule d'ordinaire a l'activation, cote serveur : en
-- l'activant ici, il faut la poser, sans quoi la colonne « Prochain declenchement »
-- reste muette sur une planification pourtant en veille.
UPDATE planification_facturation
   SET actif = TRUE,
       prochaine_execution = (date_trunc('month', CURRENT_DATE) + INTERVAL '1 month')::TIMESTAMP
                             + TIME '09:00:00'
 WHERE periodicite = 'MENSUEL' AND facture_provisoire = FALSE;

DO $$
DECLARE v_n INTEGER;
BEGIN
    SELECT count(*) INTO v_n FROM planification_facturation;
    IF v_n < 6 THEN
        RAISE EXCEPTION 'Seulement % planification(s) : onglet Automatisation incomplet', v_n;
    END IF;
    SELECT count(*) INTO v_n FROM planification_facturation WHERE actif;
    IF v_n = 0 THEN
        RAISE EXCEPTION 'Aucune planification active : ni prochain declenchement, ni execution manuelle';
    END IF;
END $$;

\echo '<< 01_config : terminé'
