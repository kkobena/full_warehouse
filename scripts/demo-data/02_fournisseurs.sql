-- ============================================================================
-- 02_fournisseurs.sql — Grossistes et leurs agences
--
-- Modèle auto-référençant (voir §3.3 du plan) :
--   parent_id IS NULL      → fournisseur PRINCIPAL (ex-GroupeFournisseur)
--   parent_id IS NOT NULL  → AGENCE rattachée à un principal
--
-- Deux règles à ne pas inverser :
--   1. Les fournisseur_produit (codes CIP, prix) sont TOUJOURS portés par le
--      principal — V1.7.1 supprime ceux rattachés à une agence.
--   2. Les commandes se passent chez l'agence quand le principal en a.
--
-- Un principal (« DIVERS FOURNISSEURS ») est volontairement sans agence, pour
-- exercer le cas de repli où la commande va directement au principal.
--
-- Note : commande.agence_id existe en base (V1.7.0) mais n'est mappée par
-- aucune entité ni lue par aucune requête. Colonne morte, laissée à NULL.
-- ============================================================================

\i _header.sql

\echo '>> 02_fournisseurs : principaux et agences'

-- ---------------------------------------------------------------------------
-- Fournisseurs principaux
--
-- Ils portent la configuration commerciale : délais, fréquence de commande,
-- conditions de règlement et RFA. Les agences héritent de ces valeurs quand
-- les leurs sont nulles (SuggestionProduitServiceImpl).
-- ---------------------------------------------------------------------------
INSERT INTO fournisseur (
    libelle, code, odre, parent_id,
    delai_livraison_jours, frequence_commande_jours,
    jours_credit, jours_critique,
    palier_rfa, taux_rfa,
    addresse_postal, phone, mobile, email, site
) VALUES
    ('LABOREX-CI',          'LABOREXCI',  1, NULL, 1, 2, 45, 15, 250000000, 2,
     '01 BP 1237 Abidjan 01',  '+225 27 21 75 90 00', '+225 07 07 12 34 56', 'commandes@laborex-ci.example', 'www.laborex-ci.example'),
    ('DPCI',                'DPCI',       2, NULL, 1, 2, 30, 10, 180000000, 2,
     '01 BP 3456 Abidjan 01',  '+225 27 21 24 55 00', '+225 05 05 22 33 44', 'commandes@dpci.example', NULL),
    ('COPHARMED',           'COPHARMED',  3, NULL, 2, 3, 30, 10, 120000000, 1,
     '18 BP 890 Abidjan 18',   '+225 27 21 35 60 00', NULL, 'commandes@copharmed.example', NULL),
    ('UBIPHARM CI',         'UBIPHARMCI', 4, NULL, 2, 3, 60, 20, 300000000, 3,
     '01 BP 4477 Abidjan 01',  '+225 27 21 27 88 00', '+225 01 01 44 55 66', 'commandes@ubipharm-ci.example', 'www.ubipharm-ci.example'),
    -- Sans agence : les commandes lui sont passées directement.
    ('DIVERS FOURNISSEURS', 'DIVERS',   100, NULL, 5, 7, 15,  5, NULL, NULL,
     'Abidjan',                NULL, NULL, NULL, NULL)
ON CONFLICT (libelle) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Agences
--
-- delai_livraison_jours volontairement NULL sur une partie d'entre elles :
-- c'est le cas de repli sur le délai du parent, et il doit être représenté.
-- ---------------------------------------------------------------------------
INSERT INTO fournisseur (
    libelle, code, odre, parent_id,
    delai_livraison_jours, addresse_postal, phone, email
)
SELECT v.libelle, v.code, v.odre, p.id, v.delai, v.adresse, v.phone, v.email
  FROM (VALUES
    -- LABOREX-CI
    ('LABOREX-CI',  'LABOREX ABIDJAN TREICHVILLE', 'LBX-TRE', 11,    1, 'Bd de Marseille, Treichville, Abidjan', '+225 27 21 75 90 10', 'treichville@laborex-ci.example'),
    ('LABOREX-CI',  'LABOREX ABIDJAN COCODY',      'LBX-COC', 12,    1, 'Bd Latrille, Cocody, Abidjan',          '+225 27 22 44 12 10', 'cocody@laborex-ci.example'),
    ('LABOREX-CI',  'LABOREX BOUAKE',              'LBX-BKE', 13, NULL, 'Quartier Commerce, Bouaké',             '+225 27 31 63 20 10', 'bouake@laborex-ci.example'),
    -- DPCI
    ('DPCI',        'DPCI ABIDJAN MARCORY',        'DPC-MAR', 21,    1, 'Zone 4, Marcory, Abidjan',              '+225 27 21 24 55 10', 'marcory@dpci.example'),
    ('DPCI',        'DPCI ABIDJAN YOPOUGON',       'DPC-YOP', 22,    2, 'Siporex, Yopougon, Abidjan',            '+225 27 23 46 70 10', 'yopougon@dpci.example'),
    ('DPCI',        'DPCI DALOA',                  'DPC-DAL', 23, NULL, 'Quartier Commerce, Daloa',              '+225 27 32 78 40 10', NULL),
    -- COPHARMED
    ('COPHARMED',   'COPHARMED ABIDJAN PLATEAU',   'COP-PLA', 31,    2, 'Av. Franchet d''Esperey, Plateau, Abidjan', '+225 27 20 22 30 10', 'plateau@copharmed.example'),
    ('COPHARMED',   'COPHARMED ABIDJAN ADJAME',    'COP-ADJ', 32,    2, 'Bd Nangui Abrogoua, Adjamé, Abidjan',   '+225 27 20 37 45 10', NULL),
    ('COPHARMED',   'COPHARMED SAN-PEDRO',         'COP-SPE', 33, NULL, 'Zone portuaire, San-Pédro',             '+225 27 34 71 55 10', NULL),
    -- UBIPHARM CI
    ('UBIPHARM CI', 'UBIPHARM ABIDJAN KOUMASSI',   'UBI-KOU', 41,    2, 'Zone industrielle, Koumassi, Abidjan',  '+225 27 21 36 12 10', 'koumassi@ubipharm-ci.example'),
    ('UBIPHARM CI', 'UBIPHARM ABIDJAN ABOBO',      'UBI-ABO', 42,    3, 'Abobo Gare, Abidjan',                   '+225 27 24 39 80 10', NULL),
    ('UBIPHARM CI', 'UBIPHARM YAMOUSSOUKRO',       'UBI-YAM', 43, NULL, 'Quartier Millionnaire, Yamoussoukro',   '+225 27 30 64 25 10', NULL)
  ) AS v(parent_libelle, libelle, code, odre, delai, adresse, phone, email)
  JOIN fournisseur p ON p.libelle = v.parent_libelle AND p.parent_id IS NULL
ON CONFLICT (libelle) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_principaux int;
    v_agences    int;
    v_orphelines int;
BEGIN
    SELECT count(*) INTO v_principaux FROM fournisseur WHERE parent_id IS NULL;
    SELECT count(*) INTO v_agences    FROM fournisseur WHERE parent_id IS NOT NULL;

    -- Une agence d'agence n'a pas de sens : la hiérarchie est à deux niveaux.
    SELECT count(*) INTO v_orphelines
      FROM fournisseur a
      JOIN fournisseur p ON p.id = a.parent_id
     WHERE p.parent_id IS NOT NULL;

    IF v_principaux < 5 THEN
        RAISE EXCEPTION 'Fournisseurs principaux attendus : 5, obtenus : %', v_principaux;
    END IF;
    IF v_agences < 12 THEN
        RAISE EXCEPTION 'Agences attendues : 12, obtenues : %', v_agences;
    END IF;
    IF v_orphelines > 0 THEN
        RAISE EXCEPTION 'Hiérarchie sur plus de deux niveaux : % agence(s) rattachée(s) à une agence', v_orphelines;
    END IF;

    RAISE NOTICE '% principaux, % agences.', v_principaux, v_agences;
END $$;

\echo '<< 02_fournisseurs : terminé'
