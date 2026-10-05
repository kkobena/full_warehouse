-- ============================================================================
-- 20_referentiel_bdpm.sql — Rapprochement du catalogue et du référentiel médicament
--
-- Le référentiel (ref_*) est chargé par Flyway, pas par ces scripts. Ici on joue ce que
-- fait l'application une fois le catalogue constitué :
--
--   1. ref_lier_dci()                 relie les DCI du référentiel à celles du catalogue ;
--   2. ref_rapprocher_produits()      propose, pour chaque produit, la spécialité du référentiel
--                                     qui lui correspond (statuts SUR, A_VERIFIER, PAR_DCI,
--                                     NON_TROUVE, avec un score) ;
--   3. des décisions du pharmacien    VALIDE sur une partie des correspondances sûres, REJETE
--                                     sur quelques propositions douteuses, le reste EN_ATTENTE.
--
-- Les trois décisions coexistent à dessein : l'écran de revue montre alors une file de travail
-- (EN_ATTENTE), un historique (VALIDE) et ce que le pharmacien a écarté (REJETE).
-- Les décisions VALIDE et REJETE ne sont jamais recalculées (voir la fonction).
-- ============================================================================

\i _header.sql

\echo '>> 20_referentiel_bdpm : rapprochement produits / spécialités'

SELECT ref_rapprocher_produits(NULL, TRUE) AS propositions_ecrites;

-- Validation d'environ deux correspondances sûres sur trois.
UPDATE produit_ref_specialite r
   SET decision = 'VALIDE',
       decide_le = (NOW() AT TIME ZONE 'UTC') - (INTERVAL '1 day' * (r.produit_id % 40))
 WHERE r.statut = 'SUR'
   AND r.cis IS NOT NULL
   AND r.produit_id % 3 <> 0;

-- Quelques propositions à vérifier que le pharmacien écarte.
UPDATE produit_ref_specialite r
   SET decision = 'REJETE',
       decide_le = (NOW() AT TIME ZONE 'UTC') - (INTERVAL '1 day' * (r.produit_id % 25))
 WHERE r.statut = 'A_VERIFIER'
   AND r.produit_id % 4 = 0;

DO $$
DECLARE
    v_total int; v_sur int; v_valide int; v_rejete int; v_attente int; v_dci int; v_cis int;
BEGIN
    SELECT count(*) INTO v_total FROM produit_ref_specialite;
    SELECT count(*) INTO v_sur FROM produit_ref_specialite WHERE statut = 'SUR';
    SELECT count(*) INTO v_valide FROM produit_ref_specialite WHERE decision = 'VALIDE';
    SELECT count(*) INTO v_rejete FROM produit_ref_specialite WHERE decision = 'REJETE';
    SELECT count(*) INTO v_attente FROM produit_ref_specialite WHERE decision = 'EN_ATTENTE';
    SELECT count(*) INTO v_dci FROM ref_dci WHERE dci_id IS NOT NULL;

    -- Une correspondance renvoie à une spécialité qui existe.
    SELECT count(*) INTO v_cis FROM produit_ref_specialite r
     WHERE r.cis IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM ref_specialite s WHERE s.cis = r.cis);

    IF v_total < 600 THEN RAISE EXCEPTION 'Propositions de rapprochement : % (attendu >= 600)', v_total; END IF;
    IF v_sur < 100 THEN RAISE EXCEPTION 'Correspondances sûres : % (attendu >= 100)', v_sur; END IF;
    IF v_valide = 0 OR v_rejete = 0 OR v_attente = 0 THEN
        RAISE EXCEPTION 'Décisions : % validée(s), % rejetée(s), % en attente — les trois états doivent exister',
                        v_valide, v_rejete, v_attente;
    END IF;
    IF v_dci < 300 THEN RAISE EXCEPTION 'DCI du référentiel reliées : % (attendu >= 300)', v_dci; END IF;
    IF v_cis > 0 THEN RAISE EXCEPTION '% proposition(s) vers une spécialité inconnue', v_cis; END IF;

    RAISE NOTICE '% propositions (% sûres) : % validée(s), % rejetée(s), % en attente.',
                 v_total, v_sur, v_valide, v_rejete, v_attente;
END $$;

\echo '<< 20_referentiel_bdpm : terminé'
