-- ============================================================================
-- 04_clients.sql — Clients, organismes tiers-payants et contrats
--
-- Rappels du modèle :
--   * customer est en héritage SINGLE_TABLE : dtype vaut 'UninsuredCustomer'
--     ou 'AssuredCustomer' (varchar(31), contrainte CHECK) ;
--   * customer.code est UNIQUE et NOT NULL ;
--   * un ayant droit est un AssuredCustomer dont assure_principal_id pointe
--     l'assuré principal ;
--   * les contrats (client_tiers_payant) sont portés par les PRINCIPAUX : la
--     couverture d'un ayant droit passe par le contrat de son principal.
--
-- DEUX PIÈGES, vérifiés sur la base (voir §4.5 du plan) :
--   1. La colonne est « tierspayant_id », pas « tiers_payant_id » : l'entité
--      déclare le champ sans @JoinColumn, Hibernate n'a pas séparé les mots.
--   2. Les @UniqueConstraint de ClientTiersPayant N'EXISTENT PAS en base —
--      Flyway possède le DDL et ne les a jamais créées. On ne peut donc pas
--      s'appuyer sur ON CONFLICT ; l'unicité reste une règle métier, garantie
--      par construction ici et contrôlée par 99_verification.sql.
-- ============================================================================

\i _header.sql

\echo '>> 04_clients : clients, tiers-payants, contrats'

-- ---------------------------------------------------------------------------
-- Répertoire nominatif
--
-- Les combinaisons sont déterministes : deux exécutions produisent le même
-- fichier client, ce qui rend les écarts diagnosticables.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_noms AS
SELECT
    ARRAY['KOUAME','KOFFI','YAO','KOUADIO','KONAN','ADAMA','IBRAHIM','SEYDOU',
          'MAMADOU','ABDOULAYE','ARISTIDE','SERGE','OLIVIER','PATRICE','DESIRE',
          'ARSENE','FIRMIN','LASSINA','MOUSSA','SOULEYMANE']::text[] AS prenoms_h,
    ARRAY['AMINATA','FATOUMATA','MARIAM','AWA','KADIDIA','AKISSI','ADJOUA',
          'AFFOUE','CHANTAL','VERONIQUE','SYLVIE','NADEGE','MARIE','CLARISSE',
          'ESTHER','SALIMATA','ROSINE','GENEVIEVE','PRISCILLE','HABIBA']::text[] AS prenoms_f,
    ARRAY['KOUASSI','TRAORE','DIABATE','OUATTARA','BAMBA','COULIBALY','KONE',
          'DIALLO','TOURE','CISSE','GNAGNE','ASSI','BROU','N''GUESSAN','YEBOUE',
          'TANOH','ZADI','DAGO','ADJE','SORO','FOFANA','KEITA','SANGARE','DOUMBIA']::text[] AS noms;

-- ---------------------------------------------------------------------------
-- 1. Clients comptant (150) — UninsuredCustomer
--
-- type_assure est NOT NULL même hors assurance : on pose PRINCIPAL.
-- ---------------------------------------------------------------------------
INSERT INTO customer (
    dtype, code, first_name, last_name, phone, email,
    status, type_assure, created_at, updated_at
)
SELECT
    'UninsuredCustomer',
    'CLI' || lpad(i::text, 6, '0'),
    CASE WHEN i % 2 = 0
         THEN n.prenoms_h[1 + (i % array_length(n.prenoms_h, 1))]
         ELSE n.prenoms_f[1 + (i % array_length(n.prenoms_f, 1))] END,
    n.noms[1 + ((i / 3) % array_length(n.noms, 1))],
    '+225 0' || (1 + i % 7) || ' ' || lpad(((i * 37) % 100)::text, 2, '0')
             || ' ' || lpad(((i * 53) % 100)::text, 2, '0')
             || ' ' || lpad(((i * 71) % 100)::text, 2, '0')
             || ' ' || lpad(((i * 91) % 100)::text, 2, '0'),
    CASE WHEN i % 4 = 0
         THEN 'client' || i || '@example.ci'
         ELSE NULL END,
    -- Quelques comptes désactivés : les écrans de filtre doivent avoir matière.
    CASE WHEN i % 25 = 0 THEN 'DISABLE' ELSE 'ENABLE' END,
    'PRINCIPAL',
    NOW() - (INTERVAL '1 day' * (400 - i)),
    NOW() - (INTERVAL '1 day' * (400 - i))
FROM tmp_noms n
CROSS JOIN LATERAL generate_series(1, 150) AS i
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Assurés principaux (120) — AssuredCustomer
-- ---------------------------------------------------------------------------
INSERT INTO customer (
    dtype, code, first_name, last_name, phone, email,
    status, type_assure, sexe, dat_naiss, created_at, updated_at
)
SELECT
    'AssuredCustomer',
    'ASS' || lpad(i::text, 6, '0'),
    CASE WHEN i % 2 = 0
         THEN n.prenoms_h[1 + (i % array_length(n.prenoms_h, 1))]
         ELSE n.prenoms_f[1 + (i % array_length(n.prenoms_f, 1))] END,
    n.noms[1 + ((i / 2) % array_length(n.noms, 1))],
    '+225 0' || (1 + i % 7) || ' ' || lpad(((i * 41) % 100)::text, 2, '0')
             || ' ' || lpad(((i * 59) % 100)::text, 2, '0')
             || ' ' || lpad(((i * 73) % 100)::text, 2, '0')
             || ' ' || lpad(((i * 97) % 100)::text, 2, '0'),
    CASE WHEN i % 3 = 0 THEN 'assure' || i || '@example.ci' ELSE NULL END,
    CASE WHEN i % 40 = 0 THEN 'DISABLE' ELSE 'ENABLE' END,
    'PRINCIPAL',
    CASE WHEN i % 2 = 0 THEN 'M' ELSE 'F' END,
    -- Adultes, de 22 à 79 ans.
    (CURRENT_DATE - (INTERVAL '1 day' * (8000 + (i * 173) % 21000)))::date,
    NOW() - (INTERVAL '1 day' * (400 - i)),
    NOW() - (INTERVAL '1 day' * (400 - i))
FROM tmp_noms n
CROSS JOIN LATERAL generate_series(1, 120) AS i
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. Ayants droit (50) — rattachés à un assuré principal
--
-- type_assure = AYANT_DROIT et assure_principal_id renseigné : les deux vont
-- ensemble, ThirdPartySales.ayant_droit_id s'appuie dessus.
-- ---------------------------------------------------------------------------
INSERT INTO customer (
    dtype, code, first_name, last_name, phone,
    status, type_assure, sexe, dat_naiss,
    assure_principal_id, num_ayant_droit, created_at, updated_at
)
SELECT
    'AssuredCustomer',
    'AYD' || lpad(i::text, 6, '0'),
    CASE WHEN i % 2 = 0
         THEN n.prenoms_h[1 + ((i * 3) % array_length(n.prenoms_h, 1))]
         ELSE n.prenoms_f[1 + ((i * 3) % array_length(n.prenoms_f, 1))] END,
    -- Un ayant droit porte le nom de son principal.
    p.last_name,
    NULL,
    'ENABLE',
    'AYANT_DROIT',
    CASE WHEN i % 2 = 0 THEN 'M' ELSE 'F' END,
    -- Conjoints et enfants : de 2 à 60 ans.
    (CURRENT_DATE - (INTERVAL '1 day' * (700 + (i * 211) % 21000)))::date,
    p.id,
    'AD' || lpad(i::text, 8, '0'),
    NOW() - (INTERVAL '1 day' * (300 - i)),
    NOW() - (INTERVAL '1 day' * (300 - i))
FROM tmp_noms n
CROSS JOIN LATERAL generate_series(1, 50) AS i
JOIN customer p ON p.code = 'ASS' || lpad((1 + ((i - 1) * 2))::text, 6, '0')
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Comptes clients (40) — carnets, pour les ventes différées
--
-- CashSale.account les référence ; le module « règlements différés » s'appuie
-- dessus. Solde à zéro : il sera mouvementé par les ventes.
-- ---------------------------------------------------------------------------
INSERT INTO customer_account (customer_id, account_type, balance, enabled, created_at, updated_at)
SELECT c.id, 'CARNET', 0, true,
       NOW() - INTERVAL '300 days', NOW() - INTERVAL '300 days'
FROM customer c
WHERE c.dtype = 'UninsuredCustomer'
  AND c.status = 'ENABLE'
  AND (regexp_replace(c.code, '\D', '', 'g'))::int % 4 = 1
  AND NOT EXISTS (SELECT 1 FROM customer_account a WHERE a.customer_id = c.id);

-- ---------------------------------------------------------------------------
-- 5. Groupes d'organismes
-- ---------------------------------------------------------------------------
INSERT INTO groupe_tiers_payant (name, adresse, telephone, ordre_tris_facture)
VALUES
    ('MUTUELLES PUBLIQUES', 'Plateau, Abidjan',  '+225 27 20 21 30 00', 'NOM_TIER'),
    ('ASSURANCES PRIVEES',  'Cocody, Abidjan',   '+225 27 22 44 55 00', 'DATE_FACTURE'),
    ('ENTREPRISES',         'Marcory, Abidjan',  '+225 27 21 26 40 00', 'MONTANT')
ON CONFLICT (name) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. Organismes tiers-payants (12)
--
-- user_id est NOT NULL : on rattache au compte administrateur.
-- Deux organismes portent un plafond de consommation, pour que les écrans de
-- suivi de plafond ne soient pas uniformément vides.
-- ---------------------------------------------------------------------------
INSERT INTO tiers_payant (
    name, full_name, categorie, statut, code_organisme, ncc,
    adresse, telephone, email,
    groupe_tiers_payant_id, user_id,
    plafond_conso, plafond_absolu, plafond_conso_client, plafond_journalier_client,
    remise_forfaitaire, nbre_bons_max_sur_fact, montant_max_sur_fact, nbre_bordereau,
    delai_reglement, periodicite_facture_definitive, model_facture,
    to_be_exclude, created, updated
)
SELECT
    v.name, v.full_name, v.categorie, 'ACTIF', v.code_org, v.ncc,
    v.adresse, v.tel, v.email,
    g.id, u.id,
    v.plafond, v.plafond IS NOT NULL, v.plafond_client, v.plafond_jour,
    0, v.nbre_bons, v.montant_max, 0,
    v.delai, 'MENSUEL', 'default',
    false, NOW() - INTERVAL '400 days', NOW() - INTERVAL '400 days'
FROM (VALUES
    ('CNAM',        'CAISSE NATIONALE D''ASSURANCE MALADIE',   'ASSURANCE', 'CNAM01',  'CI0001234A',
     'Plateau, Abidjan',  '+225 27 20 25 40 00', 'facturation@cnam.example',      'MUTUELLES PUBLIQUES', NULL::bigint, NULL::int, NULL::int, 200, 5000000::bigint, 30),
    ('MUGEFCI',     'MUTUELLE GENERALE DES FONCTIONNAIRES',    'ASSURANCE', 'MUGEF01', 'CI0001235B',
     'Plateau, Abidjan',  '+225 27 20 22 15 00', 'facturation@mugefci.example',   'MUTUELLES PUBLIQUES', 40000000, 500000, 60000, 150, 4000000, 30),
    ('CNPS',        'CAISSE NATIONALE DE PREVOYANCE SOCIALE',  'ASSURANCE', 'CNPS01',  'CI0001236C',
     'Plateau, Abidjan',  '+225 27 20 25 20 00', 'sante@cnps.example',            'MUTUELLES PUBLIQUES', NULL, NULL, NULL, 120, 3000000, 30),
    ('SUNU',        'SUNU ASSURANCES VIE CI',                  'ASSURANCE', 'SUNU01',  'CI0001237D',
     'Cocody, Abidjan',   '+225 27 22 40 12 00', 'sante@sunu.example',            'ASSURANCES PRIVEES',  NULL, NULL, NULL, 100, 2500000, 45),
    ('NSIA',        'NSIA ASSURANCES CI',                      'ASSURANCE', 'NSIA01',  'CI0001238E',
     'Plateau, Abidjan',  '+225 27 20 31 66 00', 'sante@nsia.example',            'ASSURANCES PRIVEES',  25000000, 400000, 50000, 100, 2500000, 45),
    ('ATLANTIQUE',  'ATLANTIQUE ASSURANCES CI',                'ASSURANCE', 'ATL01',   'CI0001239F',
     'Plateau, Abidjan',  '+225 27 20 30 22 00', 'sante@atlantique.example',      'ASSURANCES PRIVEES',  NULL, NULL, NULL, 80, 2000000, 60),
    ('COLINA',      'COLINA ASSURANCES CI',                    'ASSURANCE', 'COL01',   'CI0001240G',
     'Marcory, Abidjan',  '+225 27 21 75 11 00', 'sante@colina.example',          'ASSURANCES PRIVEES',  NULL, NULL, NULL, 80, 2000000, 60),
    ('SAHAM',       'SAHAM ASSURANCE CI',                      'ASSURANCE', 'SAH01',   'CI0001241H',
     'Cocody, Abidjan',   '+225 27 22 48 30 00', 'sante@saham.example',           'ASSURANCES PRIVEES',  NULL, NULL, NULL, 80, 2000000, 45),
    ('ASACI',       'ASSURANCES SANTE DE COTE D''IVOIRE',      'ASSURANCE', 'ASA01',   'CI0001242I',
     'Yopougon, Abidjan', '+225 27 23 45 60 00', 'facturation@asaci.example',     'ASSURANCES PRIVEES',  NULL, NULL, NULL, 60, 1500000, 45),
    ('SIR SANTE',   'MUTUELLE DU PERSONNEL SIR',               'ASSURANCE', 'SIR01',   'CI0001243J',
     'Vridi, Abidjan',    '+225 27 21 75 40 00', 'mutuelle@sir.example',          'ENTREPRISES',         NULL, NULL, NULL, 50, 1200000, 30),
    ('CIE SANTE',   'MUTUELLE DU PERSONNEL CIE',               'ASSURANCE', 'CIE01',   'CI0001244K',
     'Treichville, Abidjan', '+225 27 21 23 50 00', 'mutuelle@cie.example',       'ENTREPRISES',         NULL, NULL, NULL, 50, 1200000, 30),
    -- Carnet : couverture par carnet de bons, pas par assurance.
    ('CARNET SOC',  'CARNET SOCIETES PARTENAIRES',             'CARNET',    'CAR01',   'CI0001245L',
     'Plateau, Abidjan',  '+225 27 20 33 44 00', 'carnet@partenaires.example',    'ENTREPRISES',         NULL, NULL, NULL, 40, 800000, 15)
  ) AS v(name, full_name, categorie, code_org, ncc, adresse, tel, email,
         groupe, plafond, plafond_client, plafond_jour, nbre_bons, montant_max, delai)
JOIN groupe_tiers_payant g ON g.name = v.groupe
CROSS JOIN LATERAL (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1) u
ON CONFLICT (name) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 7. Contrats — client_tiers_payant
--
-- Portés par les assurés PRINCIPAUX : un ayant droit est couvert par le
-- contrat de son principal, via ThirdPartySales.ayant_droit_id.
--
-- Répartition des payeurs, déterministe :
--     rang 1 → priorité R0, organisme principal    (tous)
--     rang 2 → priorité R1, complémentaire         (1 assuré sur 3)
--     rang 3 → priorité R2, sur-complémentaire     (1 assuré sur 12)
--
-- La priorité gouverne l'ordre de service dans le calcul de répartition
-- (TiersPayantCalculationService trie par priorite croissante) : le cumul des
-- taux d'un même assuré ne doit jamais dépasser 100 %.
--
-- Pas de ON CONFLICT : aucun index unique n'existe sur (tierspayant_id,
-- assured_customer_id). Le NOT EXISTS assure la rejouabilité.
-- ---------------------------------------------------------------------------
INSERT INTO client_tiers_payant (
    assured_customer_id, tierspayant_id, num, priorite, statut, taux,
    conso_mensuelle, consommation_json, taux_historique, created, updated
)
SELECT
    c.id,
    tp.id,
    -- Numéro d'adhérent, unique par organisme.
    tp.code_organisme || '-' || lpad(c.rang_client::text, 6, '0'),
    r.priorite,
    CASE WHEN c.status = 'DISABLE' THEN 'DISABLED' ELSE 'ACTIF' END,
    r.taux,
    0,
    '[]'::json,
    -- @PrePersist de ClientTiersPayant alimente cet historique ; une insertion
    -- SQL court-circuite le hook, il faut donc l'écrire nous-mêmes.
    json_build_array(json_build_object(
        'updatedAt', to_char(NOW() - INTERVAL '350 days', 'YYYY-MM-DD"T"HH24:MI:SS'),
        'taux', r.taux
    )),
    NOW() - INTERVAL '350 days',
    NOW() - INTERVAL '350 days'
FROM (
    SELECT id, status,
           row_number() OVER (ORDER BY code) AS rang_client
      FROM customer
     WHERE dtype = 'AssuredCustomer' AND type_assure = 'PRINCIPAL'
) c
CROSS JOIN LATERAL (VALUES
    (1, 'R0'::text, 70),
    (2, 'R1'::text, 20),
    (3, 'R2'::text, 10)
  ) AS r(rang, priorite, taux)
CROSS JOIN LATERAL (
    SELECT t.id, t.code_organisme,
           row_number() OVER (ORDER BY t.id) AS rang_org
      FROM tiers_payant t
     WHERE t.statut = 'ACTIF'
) tp
WHERE
    -- Nombre de payeurs de cet assuré.
    r.rang <= CASE WHEN c.rang_client % 12 = 0 THEN 3
                   WHEN c.rang_client % 3  = 0 THEN 2
                   ELSE 1 END
    -- Organisme retenu pour ce rang : deux payeurs d'un même assuré sont
    -- toujours des organismes distincts.
    AND tp.rang_org = 1 + ((c.rang_client + (r.rang - 1) * 5) % 12)
    AND NOT EXISTS (
        SELECT 1 FROM client_tiers_payant x
         WHERE x.assured_customer_id = c.id AND x.tierspayant_id = tp.id
    );

DROP TABLE tmp_noms;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_comptant  int;
    v_principal int;
    v_ayant     int;
    v_org       int;
    v_contrats  int;
    v_exces     int;
BEGIN
    SELECT count(*) INTO v_comptant  FROM customer WHERE dtype = 'UninsuredCustomer';
    SELECT count(*) INTO v_principal FROM customer WHERE dtype = 'AssuredCustomer' AND type_assure = 'PRINCIPAL';
    SELECT count(*) INTO v_ayant     FROM customer WHERE dtype = 'AssuredCustomer' AND type_assure = 'AYANT_DROIT';
    SELECT count(*) INTO v_org       FROM tiers_payant;
    SELECT count(*) INTO v_contrats  FROM client_tiers_payant;

    -- Le cumul des taux conditionne toute la répartition tiers-payant : au-delà
    -- de 100 %, la part patient serait négative et le clamp à zéro masquerait
    -- une incohérence.
    SELECT count(*) INTO v_exces FROM (
        SELECT assured_customer_id FROM client_tiers_payant
         GROUP BY assured_customer_id HAVING sum(taux) > 100
    ) x;

    IF v_comptant < 150 THEN RAISE EXCEPTION 'Clients comptant : % (attendu 150)', v_comptant; END IF;
    IF v_principal < 120 THEN RAISE EXCEPTION 'Assurés principaux : % (attendu 120)', v_principal; END IF;
    IF v_ayant < 50 THEN RAISE EXCEPTION 'Ayants droit : % (attendu 50)', v_ayant; END IF;
    IF v_org < 12 THEN RAISE EXCEPTION 'Organismes : % (attendu 12)', v_org; END IF;
    IF v_exces > 0 THEN RAISE EXCEPTION '% assuré(s) avec un cumul de taux > 100 %%', v_exces; END IF;

    RAISE NOTICE '% comptant, % principaux, % ayants droit, % organismes, % contrats.',
        v_comptant, v_principal, v_ayant, v_org, v_contrats;
END $$;

\echo '<< 04_clients : terminé'
