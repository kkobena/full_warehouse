-- ============================================================================
-- 02b_dci.sql — Dénominations Communes Internationales
--
-- Référentiel des substances actives, prérequis du catalogue : produit.dci_id
-- y renvoie. Exécuté entre les fournisseurs (02) et les produits (03), qui en
-- dépendent.
--
-- La table EST alimentée par Flyway depuis la migration V1.9.4, qui y pose 1073
-- DCI issues du référentiel produit. Deux conséquences :
--   * `dci` figure dans la liste de préservation de 00_reset.sql — la tronquer
--     détruirait ces données sans retour, Flyway ne rejouant pas une version
--     déjà appliquée ;
--   * ce script devient un COMPLÉMENT, pas une initialisation. Le ON CONFLICT
--     ci-dessous laisse en place les libellés déjà posés par la migration et
--     n'ajoute que ceux qui manquent au jeu de démonstration.
-- 03_produits.sql résout la DCI par LIBELLÉ (pg_temp.dci_de) et non par code :
-- les deux jeux, aux codes de formes différentes, cohabitent sans conflit.
--
-- Contraintes : code (varchar(20)) et libelle sont tous deux UNIQUES et NOT NULL.
--
-- Les codes sont séquentiels et non des codes ATC. Un code ATC erroné dans un
-- jeu de démonstration serait une fausse précision : mieux vaut un identifiant
-- neutre, assumé comme tel, que sept caractères qui ont l'air officiels sans
-- l'être.
--
-- L'intérêt de ce référentiel n'est pas décoratif : plusieurs produits
-- partageant une même DCI sont SUBSTITUABLES entre eux. Le jeu de données le
-- construit délibérément — princeps, générique et forme sirop d'une même
-- molécule pointent la même ligne (voir 03_produits.sql).
-- ============================================================================

\i _header.sql

\echo '>> 02b_dci : substances actives'

INSERT INTO dci (code, libelle)
SELECT 'DCI' || lpad(row_number() OVER (ORDER BY v.libelle)::text, 4, '0'), v.libelle
FROM (VALUES
    -- Antalgiques et anti-inflammatoires
    ('PARACETAMOL'),
    ('IBUPROFENE'),
    ('DICLOFENAC'),
    ('TRAMADOL'),
    ('ACIDE ACETYLSALICYLIQUE'),
    -- Antibiotiques et anti-infectieux
    ('AMOXICILLINE'),
    ('AMOXICILLINE ACIDE CLAVULANIQUE'),
    ('METRONIDAZOLE'),
    ('CIPROFLOXACINE'),
    ('AZITHROMYCINE'),
    ('CEFTRIAXONE'),
    ('DOXYCYCLINE'),
    ('COTRIMOXAZOLE'),
    ('FLUCONAZOLE'),
    ('ACICLOVIR'),
    -- Antipaludiques et antiparasitaires
    ('ARTEMETHER'),
    ('QUININE'),
    ('ALBENDAZOLE'),
    ('MEBENDAZOLE'),
    -- Gastro-entérologie
    ('OMEPRAZOLE'),
    ('RANITIDINE'),
    ('DOMPERIDONE'),
    ('PHLOROGLUCINOL'),
    ('DIOSMECTITE'),
    ('ALGINATE DE SODIUM'),
    -- Métabolisme et endocrinologie
    ('METFORMINE'),
    ('GLIBENCLAMIDE'),
    ('INSULINE HUMAINE'),
    ('LEVOTHYROXINE'),
    -- Cardiologie
    ('AMLODIPINE'),
    ('LOSARTAN'),
    ('ATORVASTATINE'),
    ('ENALAPRIL'),
    ('BISOPROLOL'),
    ('FUROSEMIDE'),
    ('SPIRONOLACTONE'),
    ('WARFARINE'),
    ('CLOPIDOGREL'),
    -- Respiratoire et allergie
    ('SALBUTAMOL'),
    ('BECLOMETASONE'),
    ('SALMETEROL FLUTICASONE'),
    ('CETIRIZINE'),
    ('LORATADINE'),
    -- Corticoïdes
    ('PREDNISOLONE'),
    -- Neurologie et psychiatrie
    ('BROMAZEPAM'),
    ('DIAZEPAM'),
    ('CARBAMAZEPINE'),
    -- Vitamines et suppléments
    ('ACIDE FOLIQUE'),
    ('FER FOLATE'),
    -- Souches homéopathiques
    ('ARNICA MONTANA'),
    ('BELLADONNA'),
    ('NUX VOMICA'),
    ('PULSATILLA'),
    ('SULFUR'),
    ('IGNATIA AMARA'),
    ('GELSEMIUM'),
    ('APIS MELLIFICA'),
    ('RHUS TOX'),
    ('CHAMOMILLA')
  ) AS v(libelle)
ON CONFLICT (libelle) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_dci int;
    v_long int;
BEGIN
    SELECT count(*) INTO v_dci FROM dci;
    SELECT count(*) INTO v_long FROM dci WHERE length(code) > 20;

    IF v_dci < 55 THEN RAISE EXCEPTION 'Substances actives : % (attendu >= 55)', v_dci; END IF;
    IF v_long > 0 THEN RAISE EXCEPTION '% code(s) DCI dépassant varchar(20)', v_long; END IF;

    RAISE NOTICE '% substances actives.', v_dci;
END $$;

\echo '<< 02b_dci : terminé'
