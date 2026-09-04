\i _header.sql

-- ============================================================================
-- 14c_avoirs.sql — Avoirs de tiers payant dans les quatre statuts
--
-- L'ecran « Avoirs / Notes de credit » s'ouvrait sur une liste vide : aucun
-- avoir n'existait dans la demo, si bien que la barre d'indicateurs
-- (brouillons / emis / imputes / annules) ne s'affichait meme pas, et qu'aucun
-- des boutons de cycle de vie — emettre, imputer, annuler — n'etait atteignable.
--
-- Le cycle de vie se lit dans les statuts et il faut donc des avoirs dans
-- chacun :
--
--   DRAFT   modifiable, sans effet comptable        -> peut etre emis ou annule
--   EMIS    fige                                    -> peut etre impute ou annule
--   IMPUTE  a reduit le solde d'une facture         -> terminal
--   ANNULE  abandonne avant imputation              -> terminal
--
-- Deux points de modelisation a ne pas reinventer :
--
--   * La facture d'origine se reference par sa CLE COMPOSITE
--     (facture_origine_id, facture_origine_date) — la table facture_tiers_payant
--     est partitionnee par date, son identifiant seul ne designe rien.
--   * Le numero suit le format « AV-<annee>_<0000> », lu par le compteur du
--     service (SPLIT_PART sur le souligne). Un numero d'une autre forme ferait
--     repartir la numerotation a 1 et violerait l'unicite.
--
-- Un avoir n'a de sens que sur une facture DEJA REGLEE, au moins en partie :
-- on ne rend pas ce qui n'a pas ete verse. Les factures retenues sont donc
-- celles portant un montant_regle, et le montant de l'avoir en reste une
-- fraction — la meme regle que celle appliquee par la modal de creation.
--
-- Se place apres 14b_reglements.sql : les factures doivent etre reglees.
-- ============================================================================

\echo '>> 14c_avoirs : avoirs de tiers payant'

DELETE FROM avoir_line;
DELETE FROM avoir_tiers_payant;

DO $$
DECLARE
    v_user_id  INTEGER;
    v_rang     INTEGER := 0;
    v_statut   TEXT;
    v_montant  NUMERIC(15,2);
    v_cible    RECORD;
    f          RECORD;
    v_motifs   TEXT[] := ARRAY[
        'Rejet partiel de l''organisme : trois lignes non prises en charge',
        'Erreur de tarification sur le taux de prise en charge',
        'Produit retourne par l''assure apres facturation',
        'Doublon de facturation sur la periode precedente',
        'Ecart constate au rapprochement, regularise par avoir'
    ];
BEGIN
    SELECT id INTO v_user_id FROM app_user ORDER BY id LIMIT 1;

    FOR f IN
        SELECT ftp.id, ftp.invoice_date, ftp.montant_regle, ftp.montant_ttc
          FROM facture_tiers_payant ftp
         WHERE ftp.montant_regle > 1000
           AND ftp.tiers_payant_id IS NOT NULL
           -- Pas sur une facture FILLE d'un groupe. AvoirServiceImpl n'impute
           -- que sur la facture visee : le montant regle de la parente, lui, ne
           -- bouge pas, et son total se met a contredire la somme de ses filles
           -- -- ce que la liste « Groupees » affiche tel quel. C'est un manque
           -- de l'application, pas un etat a fabriquer dans la demonstration.
           AND ftp.groupe_facture_tiers_payant_id IS NULL
           -- De la marge sous le montant total : une imputation ne doit pas
           -- solder la facture ici. Une facture soldee par avoir passe a PAID
           -- alors que ses LIGNES portent encore un reste a payer — l'imputation
           -- ne les ventile pas. C'est un ecart du modele, pas un jeu de
           -- donnees a fabriquer.
           AND ftp.montant_regle + round(ftp.montant_regle / 4.0) < ftp.montant_ttc
         ORDER BY ftp.invoice_date DESC, ftp.id DESC
         LIMIT 10
    LOOP
        v_rang := v_rang + 1;

        -- Trois brouillons, trois emis, deux imputes, deux annules : de quoi
        -- montrer chaque transition sans qu'un parcours epuise le stock du
        -- suivant.
        v_statut := CASE
            WHEN v_rang <= 3 THEN 'DRAFT'
            WHEN v_rang <= 6 THEN 'EMIS'
            WHEN v_rang <= 8 THEN 'IMPUTE'
            ELSE 'ANNULE'
        END;

        -- Un quart de ce qui a ete regle, arrondi : un avoir credible reste
        -- tres en dessous de la facture.
        v_montant := round(f.montant_regle / 4.0, 2);

        INSERT INTO avoir_tiers_payant (
            num_avoir, facture_origine_id, facture_origine_date,
            montant_avoir, montant_tva, montant_ht,
            motif, avoir_date, statut, user_id, created, updated
        ) VALUES (
            'AV-' || to_char(CURRENT_DATE, 'YYYY') || '_' || to_char(v_rang, 'FM0000'),
            f.id, f.invoice_date,
            v_montant,
            round(v_montant * 0.18 / 1.18, 2),
            round(v_montant / 1.18, 2),
            v_motifs[1 + (v_rang % array_length(v_motifs, 1))],
            CURRENT_DATE - (v_rang * 3),
            v_statut,
            v_user_id,
            now(), now()
        );

        -- Un avoir impute a REELLEMENT reduit le solde d'une facture : sans
        -- cet effet, le statut mentirait exactement comme le faisait le
        -- service avant sa correction.
        IF v_statut = 'IMPUTE' THEN
            SELECT id, invoice_date, montant_regle, montant_ttc
              INTO v_cible
              FROM facture_tiers_payant
             WHERE id = f.id AND invoice_date = f.invoice_date;

            UPDATE avoir_tiers_payant
               SET facture_imputation_id   = v_cible.id,
                   facture_imputation_date = v_cible.invoice_date
             WHERE num_avoir = 'AV-' || to_char(CURRENT_DATE, 'YYYY') || '_' || to_char(v_rang, 'FM0000');

            UPDATE facture_tiers_payant
               SET montant_regle = montant_regle + v_montant::int,
                   statut = 'PARTIALLY_PAID',
                   updated = now()
             WHERE id = v_cible.id AND invoice_date = v_cible.invoice_date;
        END IF;
    END LOOP;

    RAISE NOTICE '% avoir(s) de tiers payant generes.', v_rang;
END $$;

-- ---------------------------------------------------------------------------
-- Controles
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_total   INTEGER;
    v_manque  TEXT;
    v_num     INTEGER;
BEGIN
    SELECT count(*) INTO v_total FROM avoir_tiers_payant;
    IF v_total = 0 THEN
        RAISE EXCEPTION 'Aucun avoir genere : l''ecran Avoirs resterait vide';
    END IF;

    -- Chaque statut doit etre represente, sinon un parcours du cycle de vie
    -- n'a rien sur quoi agir.
    SELECT string_agg(s, ', ') INTO v_manque
      FROM unnest(ARRAY['DRAFT', 'EMIS', 'IMPUTE', 'ANNULE']) s
     WHERE NOT EXISTS (SELECT 1 FROM avoir_tiers_payant a WHERE a.statut = s);
    IF v_manque IS NOT NULL THEN
        RAISE EXCEPTION 'Statut(s) d''avoir sans exemple : %', v_manque;
    END IF;

    -- Le compteur du service lit le suffixe apres le souligne : un numero
    -- d'une autre forme le ferait repartir de 1.
    SELECT count(*) INTO v_num FROM avoir_tiers_payant
     WHERE num_avoir !~ ('^AV-' || to_char(CURRENT_DATE, 'YYYY') || '_[0-9]{4}$');
    IF v_num > 0 THEN
        RAISE EXCEPTION '% avoir(s) hors format AV-<annee>_<0000>', v_num;
    END IF;

    -- Un avoir impute designe la facture qu'il a soldee.
    SELECT count(*) INTO v_num FROM avoir_tiers_payant
     WHERE statut = 'IMPUTE' AND facture_imputation_id IS NULL;
    IF v_num > 0 THEN
        RAISE EXCEPTION '% avoir(s) impute(s) sans facture d''imputation', v_num;
    END IF;

    RAISE NOTICE '% avoir(s) : controles OK.', v_total;
END $$;

\echo '<< 14c_avoirs : termine'
