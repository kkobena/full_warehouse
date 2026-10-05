-- ============================================================================
-- 03_parapharmacie.sql — Rayons, et petite parapharmacie FICTIVE
--
-- Les médicaments du catalogue viennent du référentiel BDPM (03c_produits_bdpm.sql), avec
-- leurs vrais noms, CIP et EAN13. Ce que la BDPM ne connaît pas — savons, pommades
-- cosmétiques, préservatifs, laits infantiles, compléments, pansements, petit matériel —
-- est posé ici : une centaine de produits à noms réalistes, SANS marque réelle.
--
-- Leurs codes sont fictifs mais reconnaissables : des EAN13 valides (somme de contrôle
-- exacte) dans la plage 299…, réservée par GS1 à l'usage interne d'un magasin. Aucun ne
-- peut donc désigner un produit du commerce. Le « CIP » du fournisseur (sept chiffres au
-- plus pour l'application) est un code interne en 9…
--
-- Rappels du modèle :
--   * les montants sont des ENTIERS, en FCFA ; les prix, multiples de 5 F ;
--   * contrainte d'unicité (libelle, type_produit) ;
--   * parfumerie (3000) et accessoires (8000) sont hors gestion de lot : le chemin mixte,
--     avec et sans lot, est un cas réel en officine ;
--   * la TVA est à 18 % : seuls les médicaments en sont exonérés.
--
-- Les rayons commerciaux, communs à tout le catalogue, sont créés ici.
-- ============================================================================

\i _header.sql

\echo '>> 03_parapharmacie : rayons et parapharmacie fictive'

-- EAN13 valide : douze chiffres + clé de contrôle.
CREATE OR REPLACE FUNCTION pg_temp.ean13(p12 text) RETURNS text
    LANGUAGE sql IMMUTABLE AS $$
    SELECT p12 || ((10 - (SELECT sum(substr(p12, i, 1)::int * CASE WHEN i % 2 = 1 THEN 1 ELSE 3 END)
                            FROM generate_series(1, 12) i) % 10) % 10)::text
$$;

-- ---------------------------------------------------------------------------
-- 1. Catalogue fictif : (libellé, famille, prix d'achat, marge %)
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_para (libelle text, fam text, cout int, marge int, rang serial);

-- Parfumerie et soins : une base × deux contenances.
INSERT INTO tmp_para (libelle, fam, cout, marge)
SELECT b.nom || ' ' || t.taille, '3000',
       (5 * round(b.cout * t.coef / 5.0))::int, 55
  FROM (VALUES
        ('CREME HYDRATANTE VISAGE', 2200), ('CREME MAINS NOURRISSANTE', 1500), ('LAIT CORPOREL', 2400),
        ('GEL DOUCHE DOUX', 1800), ('SAVON DE MARSEILLE', 900), ('SAVON LIQUIDE MAINS', 1100),
        ('SHAMPOOING DOUX', 2000), ('SHAMPOOING ANTIPELLICULAIRE', 3200), ('DEODORANT ROLL-ON', 1700),
        ('DENTIFRICE FLUORE', 1300), ('CREME SOLAIRE SPF50', 5200), ('POMMADE CICATRISANTE', 2600),
        ('POMMADE ANTI-DEMANGEAISONS', 2300), ('BAUME A LEVRES', 800), ('VASELINE PURE', 700),
        ('SPRAY ANTI-MOUSTIQUES', 2900), ('EAU MICELLAIRE', 2700), ('HUILE DE MASSAGE', 3100)
       ) AS b(nom, cout)
 CROSS JOIN (VALUES ('100ML', 1.0), ('250ML', 1.9)) AS t(taille, coef);

-- Accessoires, hygiène, dispositifs.
INSERT INTO tmp_para (libelle, fam, cout, marge) VALUES
 ('PRESERVATIF MASCULIN B/3', '8000', 600, 60),
 ('PRESERVATIF MASCULIN B/12', '8000', 2100, 60),
 ('PRESERVATIF MASCULIN B/24', '8000', 3800, 60),
 ('PRESERVATIF EXTRA FIN B/12', '8000', 2600, 60),
 ('GEL LUBRIFIANT 50ML', '8000', 1400, 55),
 ('GEL LUBRIFIANT 100ML', '8000', 2300, 55),
 ('TEST DE GROSSESSE', '8000', 900, 60),
 ('TEST D OVULATION B/5', '8000', 2800, 55),
 ('SERUM PHYSIOLOGIQUE UNIDOSES B/20', '8000', 1000, 50),
 ('ALCOOL 70 DEGRES 250ML', '8000', 600, 50),
 ('EAU OXYGENEE 125ML', '8000', 450, 50),
 ('ANTISEPTIQUE LOCAL 100ML', '8000', 1500, 50),
 ('COTON HYDROPHILE 100G', '8000', 700, 50),
 ('COTON HYDROPHILE 250G', '8000', 1400, 50),
 ('PANSEMENT ADHESIF B/20', '8000', 800, 55),
 ('PANSEMENT ADHESIF WATERPROOF B/10', '8000', 1100, 55),
 ('COMPRESSES STERILES B/10', '8000', 500, 55),
 ('COMPRESSES STERILES B/50', '8000', 1900, 55),
 ('BANDE DE CREPE 7CM', '8000', 600, 55),
 ('BANDE DE CREPE 10CM', '8000', 850, 55),
 ('SPARADRAP HYPOALLERGENIQUE 5M', '8000', 700, 55),
 ('THERMOMETRE DIGITAL', '8000', 1800, 60),
 ('THERMOMETRE FRONTAL INFRAROUGE', '8000', 9500, 45),
 ('TENSIOMETRE BRAS AUTOMATIQUE', '8000', 22000, 35),
 ('LECTEUR DE GLYCEMIE KIT', '8000', 12000, 40),
 ('BANDELETTES GLYCEMIE B/50', '8000', 6500, 30),
 ('GANTS D EXAMEN B/100', '8000', 2400, 50),
 ('MASQUES CHIRURGICAUX B/50', '8000', 1800, 50),
 ('SERINGUE 5ML B/100', '8000', 4200, 40),
 ('COUCHES ADULTES TAILLE M B/14', '8000', 4800, 40),
 ('CANNE REGLABLE', '8000', 6500, 45),
 ('MOUSTIQUAIRE 2 PLACES', '8000', 4500, 50),
 ('BOUILLOTTE EN CAOUTCHOUC', '8000', 2200, 55),
 ('INHALATEUR NASAL MENTHOL', '8000', 900, 55),
 -- Diététique et puériculture (suivies par lot : dates limites de consommation).
 ('LAIT 1ER AGE 400G', '5000', 3800, 30),
 ('LAIT 1ER AGE 900G', '5000', 8200, 28),
 ('LAIT 2EME AGE 400G', '5000', 3600, 30),
 ('LAIT 2EME AGE 900G', '5000', 7900, 28),
 ('LAIT DE CROISSANCE 900G', '5000', 7400, 28),
 ('CEREALES BEBE VANILLE 250G', '5000', 2100, 35),
 ('CEREALES BEBE 8 CEREALES 400G', '5000', 3000, 35),
 ('PETITS POTS LEGUMES 130G', '5000', 650, 40),
 ('COUCHES BEBE TAILLE 3 X40', '5000', 4900, 35),
 ('COUCHES BEBE TAILLE 4 X36', '5000', 5100, 35),
 ('LINGETTES BEBE X72', '5000', 1500, 45),
 ('LAIT DE TOILETTE BEBE 250ML', '5000', 2300, 45),
 ('BIBERON VERRE 250ML', '5000', 2400, 50),
 ('TETINE SILICONE X2', '5000', 1200, 55),
 -- Compléments alimentaires.
 ('VITAMINE C 500 B/30', '6000', 1900, 45),
 ('VITAMINE D3 B/30', '6000', 2400, 45),
 ('MAGNESIUM B6 B/60', '6000', 3200, 45),
 ('FER ET ACIDE FOLIQUE B/30', '6000', 2800, 45),
 ('OMEGA 3 B/60', '6000', 5600, 40),
 ('MULTIVITAMINES ADULTE B/30', '6000', 3900, 45),
 ('MULTIVITAMINES ENFANT SIROP 150ML', '6000', 3300, 45),
 ('ZINC B/30', '6000', 2600, 45),
 ('CALCIUM VITAMINE D3 B/60', '6000', 3600, 45),
 ('PROBIOTIQUES B/14', '6000', 5200, 40),
 ('SIROP APPETIT ENFANT 150ML', '6000', 2900, 45),
 ('GINSENG TONIQUE B/20 AMPOULES', '6000', 4200, 40),
 ('TISANE DIGESTIVE B/20', '6000', 1100, 50),
 ('TISANE SOMMEIL B/20', '6000', 1200, 50);

-- ---------------------------------------------------------------------------
-- 2. Insertion
-- ---------------------------------------------------------------------------
INSERT INTO produit (
    libelle, type_produit, status,
    cost_amount, regular_unit_price, net_unit_price,
    item_qty, item_cost_amount, item_regular_unit_price, prix_mnp,
    deconditionnable, chiffre, gestion_lot, thermosensible, remisable,
    statut_legal, classe_criticite, code_remise,
    qty_appro, qty_seuil_mini, seuil_decond,
    code_ean_labo, famille_id, tva_id, forme_id, dci_id,
    created_at, updated_at
)
SELECT
    p.libelle, 'PACKAGE', 'ENABLE',
    p.cout, (5 * round(p.cout * (100 + p.marge) / 100.0 / 5.0))::int,
    (5 * round(p.cout * (100 + p.marge) / 100.0 / 5.0))::int,
    1, 0, 0, 0,
    false, true,
    p.fam NOT IN ('8000', '3000'),
    false, true,
    'SANS_LISTE',
    CASE WHEN p.rang % 11 = 0 THEN 'A' WHEN p.rang % 4 = 0 THEN 'B' ELSE 'C' END,
    'CODE_0',
    CASE WHEN p.rang % 5 = 0 THEN 20 ELSE 10 END,
    CASE WHEN p.rang % 5 = 0 THEN 10 ELSE 5 END,
    0,
    pg_temp.ean13('299' || lpad((100000 + p.rang)::text, 9, '0')),
    f.id, t.id,
    (SELECT fp.id FROM form_produit fp WHERE fp.libelle = CASE
         WHEN p.libelle LIKE 'CREME%' OR p.libelle LIKE 'LAIT CORPOREL%' THEN 'Crèmes'
         WHEN p.libelle LIKE 'POMMADE%' OR p.libelle LIKE 'VASELINE%' OR p.libelle LIKE 'BAUME%' THEN 'Pommades'
         WHEN p.libelle LIKE 'GEL%' THEN 'Gels'
         WHEN p.libelle LIKE 'SPRAY%' OR p.libelle LIKE 'INHALATEUR%' THEN 'Sprays'
         WHEN p.libelle LIKE 'SIROP%' OR p.libelle LIKE '%SIROP%' THEN 'Flacons'
         END),
    NULL,
    NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 40)),
    NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 40))
FROM tmp_para p
JOIN famille_produit f ON f.code = p.fam
JOIN tva t ON t.taux = 18
ON CONFLICT (libelle, type_produit) DO NOTHING;

-- Circuit fournisseur : principaux uniquement, 1 (60 %), 2 (30 %) ou 3 (10 %) par produit.
INSERT INTO fournisseur_produit (
    produit_id, fournisseur_id, code_cip, code_ean,
    prix_achat, prix_uni, qte_colis, qte_minimale_commande,
    created_date, last_modified_date
)
SELECT
    p.id, f.id,
    -- Pas de CIP en parapharmacie : un code interne à sept chiffres, en 9… (les vrais CIP7
    -- commencent par 3 ou 4), et le code-barres fictif en EAN.
    lpad((9000000 + p.id)::text, 7, '0'),
    p.code_ean_labo,
    greatest(5, (5 * round((p.cost_amount * (100 + ((p.id + f.rang * 7) % 9) - 4) / 100.0) / 5.0))::int),
    p.regular_unit_price,
    CASE WHEN p.id % 7 = 0 THEN 10 WHEN p.id % 3 = 0 THEN 5 ELSE 1 END,
    CASE WHEN p.id % 11 = 0 THEN 5 ELSE 0 END,
    NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 40)),
    NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 40))
FROM produit p
CROSS JOIN LATERAL generate_series(0, CASE WHEN p.id % 10 = 0        THEN 2
                                           WHEN p.id % 10 IN (1,2,3) THEN 1
                                           ELSE 0 END) AS k
CROSS JOIN LATERAL (
    SELECT pr.id,
           row_number() OVER (ORDER BY pr.odre, pr.id) AS rang,
           count(*)     OVER ()                        AS total
      FROM fournisseur pr
     WHERE pr.parent_id IS NULL
) f
WHERE p.code_ean_labo LIKE '299%'
  AND f.rang = 1 + ((p.id + k) % f.total)
ON CONFLICT (produit_id, fournisseur_id) DO NOTHING;

UPDATE produit p
   SET fournisseur_produit_principal_id = fp.id
  FROM (SELECT DISTINCT ON (produit_id) id, produit_id
          FROM fournisseur_produit ORDER BY produit_id, id) fp
 WHERE fp.produit_id = p.id
   AND p.fournisseur_produit_principal_id IS NULL;

DROP TABLE tmp_para;

-- ---------------------------------------------------------------------------
-- 3. Rayons commerciaux
--
-- Les rayons « SANS EMPLACEMENT » (id 2 et 3) existent déjà, posés par Flyway.
-- Unicité : (code, storage_id) et (libelle, storage_id).
--
-- type_zone est lu par Hibernate en @Enumerated(STRING) : toute valeur absente de TypeZone
-- fait échouer le chargement de l'entité côté application. Valeurs admises : AMBIANT, FROID,
-- OTC, ORDONNANCE, TOXIQUE, RESERVE, PARA.
-- ---------------------------------------------------------------------------
INSERT INTO rayon (code, libelle, to_exclude, storage_id, type_zone, position)
SELECT v.code, v.libelle, false, s.id, v.type_zone, v.position
  FROM (VALUES
    ('ANTA', 'ANTALGIQUES ET ANTIPYRETIQUES', 'OTC',        'A1'),
    ('ANTB', 'ANTIBIOTIQUES',                 'ORDONNANCE', 'A2'),
    ('CARD', 'CARDIOLOGIE ET TENSION',        'ORDONNANCE', 'B1'),
    -- Insulines et analogues : chaîne du froid.
    ('DIAB', 'DIABETE ET METABOLISME',        'FROID',      'B2'),
    ('GAST', 'GASTRO-ENTEROLOGIE',            'OTC',        'C1'),
    ('RESP', 'RESPIRATOIRE ET ALLERGIE',      'OTC',        'C2'),
    ('DERM', 'DERMATOLOGIE',                  'OTC',        'D1'),
    ('HOME', 'HOMEOPATHIE',                   'OTC',        'D2'),
    ('NUTR', 'NUTRITION ET DIETETIQUE',       'PARA',       'E1'),
    ('PARA', 'PARAPHARMACIE',                 'PARA',       'E2'),
    ('ACCE', 'ACCESSOIRES ET DISPOSITIFS',    'PARA',       'F1'),
    ('DIVE', 'DIVERS',                        'AMBIANT',    'F2')
  ) AS v(code, libelle, type_zone, position)
  JOIN storage s ON s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1
-- DO UPDATE et non DO NOTHING : rayon figure dans la liste de préservation de 00_reset.sql,
-- donc les lignes survivent au reset. Le script doit converger, pas seulement s'abstenir.
ON CONFLICT (code, storage_id) DO UPDATE
   SET type_zone = EXCLUDED.type_zone,
       position  = EXCLUDED.position;

INSERT INTO rayon_produit (produit_id, rayon_id)
SELECT p.id, r.id
  FROM produit p
  JOIN famille_produit f ON f.id = p.famille_id
  JOIN storage s ON s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1
  JOIN rayon   r ON r.storage_id = s.id
   AND r.code = CASE f.code
        WHEN '5000' THEN 'NUTR'
        WHEN '6000' THEN 'NUTR'
        WHEN '3000' THEN 'PARA'
        WHEN '8000' THEN 'ACCE'
        END
ON CONFLICT (produit_id, rayon_id) DO NOTHING;

DO $$
DECLARE v_nb int; v_ean int;
BEGIN
    SELECT count(*) INTO v_nb FROM produit WHERE code_ean_labo LIKE '299%';
    -- Chaque code-barres fictif doit être un EAN13 valide : clé de contrôle exacte.
    SELECT count(*) INTO v_ean FROM produit
     WHERE code_ean_labo LIKE '299%' AND code_ean_labo <> pg_temp.ean13(left(code_ean_labo, 12));
    IF v_nb < 90 THEN RAISE EXCEPTION 'Parapharmacie : % produits (attendu >= 90)', v_nb; END IF;
    IF v_ean > 0 THEN RAISE EXCEPTION '% EAN13 fictif(s) à la clé de contrôle fausse', v_ean; END IF;
    RAISE NOTICE '% produits de parapharmacie fictifs.', v_nb;
END $$;

\echo '<< 03_parapharmacie : terminé'
