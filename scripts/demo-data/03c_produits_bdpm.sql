-- ============================================================================
-- 03c_produits_bdpm.sql — Catalogue des médicaments, issu du référentiel BDPM
--
-- Les médicaments du catalogue sont de VRAIES spécialités, prises dans ref_specialite
-- (migrations V2.1.19 et V2.1.28 à V2.1.31), avec leurs vrais noms, leur CIP7 et leur EAN13
-- (CIP13) — lus dans 03c_data_cip_bdpm.sql, extrait de CIS_CIP_bdpm.txt — et leur molécule
-- (ref_specialite_composition → ref_dci → dci).
--
-- Mille produits, de cinq provenances :
--
--   * des groupes génériques COMPLETS — un princeps et jusqu'à quatre génériques — pour que
--     la substitution se démontre avec des noms réels : le princeps et ses génériques
--     partagent leur molécule et leur dosage, comme dans le référentiel ;
--   * des PRODUITS DE TÊTE, imposés par les parcours (DOLIPRANE, EFFERALGAN, TRAMADOL,
--     DIAZEPAM, BROMAZEPAM…) avec leur groupe ;
--   * de l'HOMÉOPATHIE (TVA 18 %, sans liste), dont un ARNICA ;
--   * des ASSOCIATIONS à deux molécules ;
--   * des spécialités isolées, sans groupe générique.
--
-- Famille : princeps 1050, génériques 1030, spécialités isolées 1000, homéopathie 1040.
-- Prix : fictifs (le référentiel public n'en porte pas), multiples de 5 F.
--
-- Une partie du catalogue (25 boîtes) se déconditionne, avec son unité DETAIL.
-- La parapharmacie, absente de la BDPM, est dans 03_parapharmacie.sql.
-- ============================================================================

\i _header.sql

\echo '>> 03c_produits_bdpm : médicaments du référentiel (vrais CIP / EAN13)'

\i 03c_data_cip_bdpm.sql

SELECT ref_lier_dci() AS dci_reliees;

-- ---------------------------------------------------------------------------
-- 1. Candidats : spécialités commercialisées, au CIP réel, au libellé raisonnable
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_bdpm_c AS
SELECT s.cis, s.libelle, s.forme,
       s.groupe_generique_id AS gid, s.type_generique AS tg,
       (SELECT count(*) FROM ref_specialite_composition x
         WHERE x.cis = s.cis AND x.nature = 'SA')::int AS nsa,
       (s.composition ILIKE '%HOMÉOPATHIQUES%') AS homeo,
       -- Produits de tête : deux spécialités par marque ou molécule imposée. PARACETAMOL
       -- en est : les parcours y passent, et le contrôle « Princeps et générique partagent
       -- leur molécule » de 99_verification.sql le rapproche de DOLIPRANE.
       (s.libelle ~ '^(DOLIPRANE|EFFERALGAN|PARACETAMOL|TRAMADOL|DIAZEPAM|BROMAZEPAM|AUGMENTIN|ATORVASTATINE|ADVIL|SPASFON|AMOXICILLINE|ZYRTEC|VOLTARENE|ARNICA|ARTEMETHER|METFORMINE) ') AS tete
  FROM ref_specialite s
  JOIN tmp_cip_bdpm k ON k.cis = s.cis
 WHERE s.commercialisee
   AND length(s.libelle) <= 120
   AND s.libelle NOT LIKE '%  %';

-- ---------------------------------------------------------------------------
-- 2. Sélection
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_bdpm_sel AS
WITH tete AS (
    SELECT c.*, 0 AS prio FROM (
        -- Mono-molécule d'abord : une association (DOLIPRANE CODEINE…) prendrait pour DCI
        -- principale sa première substance par code, pas forcément celle de la marque.
        SELECT c.*, row_number() OVER (PARTITION BY substring(c.libelle FROM '^[A-Z]+')
                                       ORDER BY (c.nsa <> 1), md5(c.cis)) AS n
          FROM tmp_bdpm_c c WHERE c.tete AND c.nsa >= 1
    ) c WHERE c.n <= 2
),
groupes AS (
    SELECT g.gid,
           row_number() OVER (ORDER BY min(CASE WHEN t.cis IS NULL THEN 1 ELSE 0 END), md5(g.gid::text)) AS rg
      FROM tmp_bdpm_c g
      LEFT JOIN tete t ON t.cis = g.cis
     WHERE g.gid IS NOT NULL AND g.nsa = 1 AND g.tg IN ('PRINCEPS', 'GENERIQUE')
     GROUP BY g.gid
    HAVING count(*) >= 3 AND bool_or(g.tg = 'PRINCEPS')
),
membres AS (
    SELECT c.*, 1 AS prio,
           row_number() OVER (PARTITION BY c.gid
                              ORDER BY (c.tg <> 'PRINCEPS'), (NOT c.tete), md5(c.cis)) AS rm
      FROM tmp_bdpm_c c
      JOIN groupes g ON g.gid = c.gid AND g.rg <= 190
     WHERE c.nsa = 1 AND c.tg IN ('PRINCEPS', 'GENERIQUE')
),
homeo AS (
    SELECT c.*, 2 AS prio, 1 AS rm FROM (
        SELECT c.*, row_number() OVER (ORDER BY (c.libelle LIKE 'ARNICA%') DESC, md5(c.cis)) AS n
          FROM tmp_bdpm_c c WHERE c.homeo AND c.nsa >= 1
    ) c WHERE c.n <= 60
),
assoc AS (
    SELECT c.*, 3 AS prio, 1 AS rm FROM (
        SELECT c.*, row_number() OVER (ORDER BY md5(c.cis)) AS n
          FROM tmp_bdpm_c c WHERE c.nsa = 2 AND NOT c.homeo
    ) c WHERE c.n <= 14
),
isoles AS (
    SELECT c.*, 4 AS prio, 1 AS rm FROM tmp_bdpm_c c
     WHERE c.gid IS NULL AND c.nsa = 1 AND NOT c.homeo
),
tous AS (
    SELECT cis, libelle, forme, gid, tg, nsa, homeo, prio, 1 AS rm FROM tete
    UNION ALL SELECT cis, libelle, forme, gid, tg, nsa, homeo, prio, rm FROM membres WHERE rm <= 5
    UNION ALL SELECT cis, libelle, forme, gid, tg, nsa, homeo, prio, rm FROM homeo
    UNION ALL SELECT cis, libelle, forme, gid, tg, nsa, homeo, prio, rm FROM assoc
    UNION ALL SELECT cis, libelle, forme, gid, tg, nsa, homeo, prio, rm FROM isoles
),
uniques AS (
    SELECT DISTINCT ON (libelle) * FROM tous ORDER BY libelle, prio, md5(cis)
)
SELECT u.*, row_number() OVER (ORDER BY u.prio, md5(u.cis)) AS rang
  FROM uniques u;

-- Surplus d'isolées : on garde de quoi atteindre mille produits (marge pour les rejets de DCI).
DELETE FROM tmp_bdpm_sel WHERE rang > 1080;

-- ---------------------------------------------------------------------------
-- 3. Substances actives : toute molécule retenue existe dans le catalogue des DCI
-- ---------------------------------------------------------------------------
-- Le référentiel en compte plus que le catalogue ; on crée celles qui manquent (code
-- « BDPM » + identifiant), comme le fait l'application en validant un rapprochement.
INSERT INTO dci (code, libelle)
SELECT 'BDPM' || lpad(d.id::text, 6, '0'), d.libelle
  FROM ref_dci d
 WHERE d.dci_id IS NULL
   AND d.id IN (SELECT c.dci_id FROM ref_specialite_composition c
                  JOIN tmp_bdpm_sel s ON s.cis = c.cis WHERE c.nature = 'SA')
ON CONFLICT DO NOTHING;

SELECT ref_lier_dci() AS dci_reliees_apres_creation;

-- Première substance active de chaque spécialité (ordre du code substance), et liaison.
CREATE TEMP TABLE tmp_bdpm_sa AS
SELECT DISTINCT ON (c.cis, d.dci_id)
       c.cis, d.dci_id, c.dosage_valeur, c.dosage_unite,
       dense_rank() OVER (PARTITION BY c.cis ORDER BY c.substance_code) AS rang
  FROM ref_specialite_composition c
  JOIN ref_dci d ON d.id = c.dci_id AND d.dci_id IS NOT NULL
  JOIN tmp_bdpm_sel s ON s.cis = c.cis
 WHERE c.nature = 'SA'
 ORDER BY c.cis, d.dci_id, c.substance_code;

-- Une spécialité sans molécule rattachable n'a pas sa place : ni substitution ni contrôle d'interaction.
DELETE FROM tmp_bdpm_sel s WHERE NOT EXISTS (SELECT 1 FROM tmp_bdpm_sa a WHERE a.cis = s.cis AND a.rang = 1);
-- Une association dont une molécule est rejetée perdrait la moitié de sa composition : on la retire aussi.
DELETE FROM tmp_bdpm_sel s
 WHERE s.nsa = 2 AND (SELECT count(*) FROM tmp_bdpm_sa a WHERE a.cis = s.cis) < 2;

DELETE FROM tmp_bdpm_sel WHERE rang > (SELECT rang FROM (
    SELECT rang, row_number() OVER (ORDER BY prio, md5(cis)) AS n FROM tmp_bdpm_sel) x WHERE n = 1000);

-- ---------------------------------------------------------------------------
-- 4. Prix de référence : un prix de groupe pour les génériques, par hachage pour le reste
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_bdpm_prix AS
SELECT s.cis,
       CASE
         WHEN s.homeo THEN (5 * round((1200 + (h.v % 20) * 100) / 5.0))::int
         WHEN s.gid IS NOT NULL AND s.tg = 'PRINCEPS'
              THEN (5 * round((1000 + (g.v % 80) * 100) / 5.0))::int
         WHEN s.gid IS NOT NULL
              THEN (5 * round((1000 + (g.v % 80) * 100) * 0.65 / 5.0))::int
         ELSE (5 * round((800 + (h.v % 140) * 100) / 5.0))::int
       END AS cout
  FROM tmp_bdpm_sel s
 CROSS JOIN LATERAL (SELECT ('x' || substr(md5(s.cis), 1, 6))::bit(24)::int AS v) h
 CROSS JOIN LATERAL (SELECT ('x' || substr(md5(COALESCE(s.gid, 0)::text), 1, 6))::bit(24)::int AS v) g;

-- ---------------------------------------------------------------------------
-- 5. Produits
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
    s.libelle, 'PACKAGE', 'ENABLE',
    k.cout,
    (5 * round(k.cout * CASE WHEN s.homeo THEN 1.45 ELSE 1.30 END / 5.0))::int,
    (5 * round(k.cout * CASE WHEN s.homeo THEN 1.45 ELSE 1.30 END / 5.0))::int,
    1, 0, 0, 0,
    false, true, true,
    -- Insulines : chaîne du froid.
    s.libelle ~* 'INSULINE|LANTUS|LEVEMIR|NOVORAPID|HUMALOG|TOUJEO|ABASAGLAR',
    s.homeo,
    CASE WHEN s.homeo THEN 'SANS_LISTE'
         WHEN s.tg = 'PRINCEPS' OR s.tg IS NULL THEN 'LISTE_I'
         ELSE 'LISTE_II' END,
    CASE WHEN s.rang % 17 = 0 THEN 'A_PLUS' WHEN s.rang % 5 = 0 THEN 'A'
         WHEN s.rang % 3 = 0 THEN 'B' ELSE 'C' END,
    'CODE_0',
    CASE WHEN s.rang % 5 = 0 THEN 20 ELSE 10 END,
    CASE WHEN s.rang % 5 = 0 THEN 10 ELSE 5 END,
    0,
    ck.cip13,
    f.id, t.id,
    (SELECT fp.id FROM form_produit fp WHERE fp.libelle = CASE
        WHEN s.forme ~* 'comprimé' THEN 'Comprimés'
        WHEN s.forme ~* 'gélule|capsule' THEN 'Gélules'
        WHEN s.forme ~* 'sirop|solution buvable|suspension buvable|gouttes' THEN 'Flacons'
        WHEN s.forme ~* 'sachet|poudre' THEN 'Sachets'
        WHEN s.forme ~* 'crème' THEN 'Crèmes'
        WHEN s.forme ~* '\mgel\M' THEN 'Gels'
        WHEN s.forme ~* 'pommade' THEN 'Pommades'
        WHEN s.forme ~* 'suppositoire' THEN 'Suppositoires'
        WHEN s.forme ~* 'collyre' THEN 'Collyres'
        WHEN s.forme ~* 'injectable|seringue' THEN 'Injectables'
        WHEN s.forme ~* 'inhal|spray|pulvérisation' THEN 'Sprays'
        WHEN s.forme ~* 'patch|dispositif transdermique' THEN 'Patchs'
        WHEN s.forme ~* 'granul' THEN 'Granulés'
        WHEN s.forme ~* 'solution' THEN 'Solutions'
        END),
    a.dci_id,
    NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 40)),
    NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 40))
FROM tmp_bdpm_sel s
JOIN tmp_bdpm_prix k ON k.cis = s.cis
JOIN tmp_cip_bdpm ck ON ck.cis = s.cis
JOIN tmp_bdpm_sa a ON a.cis = s.cis AND a.rang = 1
JOIN famille_produit f ON f.code = CASE WHEN s.homeo THEN '1040'
                                        WHEN s.gid IS NULL THEN '1000'
                                        WHEN s.tg = 'PRINCEPS' THEN '1050'
                                        ELSE '1030' END
JOIN tva t ON t.taux = CASE WHEN s.homeo THEN 18 ELSE 0 END
ON CONFLICT (libelle, type_produit) DO NOTHING;

-- Stupéfiants et psychotropes : la traçabilité du lot est obligatoire et le retour interdit
-- (StatutLegal.isRetourInterdit). Sans un seul produit concerné, le refus de retour ne peut
-- ni s'observer ni s'illustrer.
UPDATE produit SET statut_legal = 'STUPEFIANTS'
 WHERE libelle LIKE 'TRAMADOL%' AND libelle IN (SELECT libelle FROM tmp_bdpm_sel);
UPDATE produit SET statut_legal = 'PSO'
 WHERE (libelle LIKE 'DIAZEPAM%' OR libelle LIKE 'BROMAZEPAM%')
   AND libelle IN (SELECT libelle FROM tmp_bdpm_sel);
-- Les antalgiques courants se délivrent sans ordonnance.
UPDATE produit SET statut_legal = 'SANS_LISTE'
 WHERE libelle ~ '^(DOLIPRANE|EFFERALGAN|ADVIL|SPASFON|PARACETAMOL) '
   AND libelle IN (SELECT libelle FROM tmp_bdpm_sel);

-- Produits tout juste créés.
CREATE TEMP TABLE tmp_bdpm_produit AS
SELECT p.id AS produit_id, s.cis, ck.cip7, ck.cip13
  FROM produit p
  JOIN tmp_bdpm_sel s ON s.libelle = p.libelle AND p.type_produit = 'PACKAGE'
  JOIN tmp_cip_bdpm ck ON ck.cis = s.cis
 WHERE NOT EXISTS (SELECT 1 FROM fournisseur_produit fp WHERE fp.produit_id = p.id);

-- ---------------------------------------------------------------------------
-- 6. Molécules (produit_dci), dosage compris : une ligne par substance, rang 1 = principale
-- ---------------------------------------------------------------------------
INSERT INTO produit_dci (produit_id, dci_id, rang, dosage_valeur, dosage_unite)
SELECT b.produit_id, a.dci_id, a.rang::smallint, a.dosage_valeur, left(a.dosage_unite, 10)
  FROM tmp_bdpm_produit b
  JOIN tmp_bdpm_sa a ON a.cis = b.cis
 WHERE NOT EXISTS (SELECT 1 FROM produit_dci pd WHERE pd.produit_id = b.produit_id);

-- ---------------------------------------------------------------------------
-- 7. Circuit fournisseur : principaux seulement, 1 (60 %), 2 (30 %) ou 3 (10 %) par produit
-- ---------------------------------------------------------------------------
INSERT INTO fournisseur_produit (
    produit_id, fournisseur_id, code_cip, code_ean,
    prix_achat, prix_uni, qte_colis, qte_minimale_commande,
    created_date, last_modified_date
)
SELECT
    p.id, f.id, b.cip7, b.cip13,
    greatest(5, (5 * round((p.cost_amount * (100 + ((p.id + f.rang * 7) % 9) - 4) / 100.0) / 5.0))::int),
    p.regular_unit_price,
    CASE WHEN p.id % 7 = 0 THEN 10 WHEN p.id % 3 = 0 THEN 5 ELSE 1 END,
    0,
    NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 40)),
    NOW() - (INTERVAL '1 day' * (pg_temp.horizon() + 40))
FROM tmp_bdpm_produit b
JOIN produit p ON p.id = b.produit_id
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
WHERE f.rang = 1 + ((p.id + k) % f.total)
ON CONFLICT (produit_id, fournisseur_id) DO NOTHING;

UPDATE produit p
   SET fournisseur_produit_principal_id = fp.id
  FROM (SELECT DISTINCT ON (produit_id) id, produit_id
          FROM fournisseur_produit ORDER BY produit_id, id) fp
 WHERE fp.produit_id = p.id
   AND p.fournisseur_produit_principal_id IS NULL;

-- ---------------------------------------------------------------------------
-- 8. Rayons : par le début du libellé ; l'homéopathie a le sien, le reste va en « Divers »
-- ---------------------------------------------------------------------------
INSERT INTO rayon_produit (produit_id, rayon_id)
SELECT b.produit_id, r.id
  FROM tmp_bdpm_produit b
  JOIN produit p ON p.id = b.produit_id
  JOIN famille_produit f ON f.id = p.famille_id
  JOIN storage s ON s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1
  JOIN rayon r ON r.storage_id = s.id AND r.code = CASE
        WHEN f.code = '1040' THEN 'HOME'
        WHEN p.libelle ~* '^(AMOXICILLINE|AUGMENTIN|CLAMOXYL|CIPROFLOXACINE|METRONIDAZOLE|AZITHROMYCINE|CEFTRIAXONE|CEFIXIME|DOXYCYCLINE|COTRIMOXAZOLE|OFLOXACINE|CLARITHROMYCINE|ERYTHROMYCINE)' THEN 'ANTB'
        WHEN p.libelle ~* '^(DOLIPRANE|EFFERALGAN|PARACETAMOL|IBUPROFENE|ADVIL|DICLOFENAC|VOLTARENE|TRAMADOL|KETOPROFENE|NAPROXENE|SPASFON)' THEN 'ANTA'
        WHEN p.libelle ~* '^(AMLODIPINE|LOSARTAN|ATORVASTATINE|FUROSEMIDE|BISOPROLOL|RAMIPRIL|VALSARTAN|SIMVASTATINE|CLOPIDOGREL|IRBESARTAN|PERINDOPRIL|PRAVASTATINE|ROSUVASTATINE)' THEN 'CARD'
        WHEN p.libelle ~* '^(METFORMINE|GLICLAZIDE|LEVOTHYROX|INSULINE|GLIMEPIRIDE|SITAGLIPTINE)' THEN 'DIAB'
        WHEN p.libelle ~* '^(OMEPRAZOLE|PANTOPRAZOLE|ESOMEPRAZOLE|DOMPERIDONE|LANSOPRAZOLE|RANITIDINE)' THEN 'GAST'
        WHEN p.libelle ~* '^(CETIRIZINE|ZYRTEC|LORATADINE|DESLORATADINE|MONTELUKAST|SALBUTAMOL|VENTOLINE)' THEN 'RESP'
        WHEN p.libelle ~* '(crème|pommade|gel)' THEN 'DERM'
        ELSE 'DIVE' END
ON CONFLICT (produit_id, rayon_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 9. Déconditionnement : 25 boîtes de comprimés ou gélules, chacune avec son unité DETAIL
--
-- C'est la boîte qui se déconditionne ; l'unité porte parent_id et ne se commande jamais
-- (ClassificationCriticiteService et SemoisCalculationService l'écartent). Elle hérite de la
-- molécule et de la famille de sa boîte.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_decond AS
SELECT p.id AS boite_id,
       (ARRAY[10, 12, 20, 24, 30])[1 + (row_number() OVER (ORDER BY md5(p.id::text)))::int % 5] AS item_qty
  FROM produit p
  JOIN tmp_bdpm_sel s ON s.libelle = p.libelle
  JOIN famille_produit f ON f.id = p.famille_id AND f.code IN ('1030', '1050')
 WHERE s.forme ~* '^(comprimé|gélule)'
   AND p.statut_legal NOT IN ('STUPEFIANTS', 'PSO')
 ORDER BY md5(p.id::text)
 LIMIT 25;

UPDATE produit p
   SET deconditionnable = true,
       item_qty = d.item_qty,
       item_cost_amount = greatest(5, (5 * round(p.cost_amount::numeric / d.item_qty / 5.0))::int),
       item_regular_unit_price = greatest(5, (5 * round(p.regular_unit_price::numeric / d.item_qty / 5.0))::int)
  FROM tmp_decond d
 WHERE d.boite_id = p.id;

INSERT INTO produit (
    libelle, type_produit, status,
    cost_amount, regular_unit_price, net_unit_price,
    item_qty, item_cost_amount, item_regular_unit_price, prix_mnp,
    deconditionnable, chiffre, gestion_lot, thermosensible, remisable,
    statut_legal, classe_criticite, code_remise,
    qty_appro, qty_seuil_mini, seuil_decond,
    code_ean_labo, parent_id, famille_id, tva_id, forme_id, dci_id,
    created_at, updated_at
)
SELECT
    left(pk.libelle, 240) || ' - UNITE', 'DETAIL', 'ENABLE',
    pk.item_cost_amount, pk.item_regular_unit_price, pk.item_regular_unit_price,
    1, 0, 0, 0,
    false, true, true, false, false,
    pk.statut_legal, 'B', 'CODE_0',
    10, 5, greatest(2, (d.item_qty / 4)::int),
    pg_temp.ean13('298' || lpad(pk.id::text, 9, '0')),
    pk.id, pk.famille_id, pk.tva_id, pk.forme_id, pk.dci_id,
    pk.created_at, pk.updated_at
  FROM tmp_decond d
  JOIN produit pk ON pk.id = d.boite_id
ON CONFLICT (libelle, type_produit) DO NOTHING;

INSERT INTO produit_dci (produit_id, dci_id, rang)
SELECT c.id, c.dci_id, 1 FROM produit c
 WHERE c.type_produit = 'DETAIL' AND c.dci_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM produit_dci pd WHERE pd.produit_id = c.id);

-- Une unité se référence chez les mêmes fournisseurs que sa boîte, sous un code interne (CIP en 8…, EAN
-- en 298… : plages d'usage interne, jamais un code du commerce).
INSERT INTO fournisseur_produit (
    produit_id, fournisseur_id, code_cip, code_ean,
    prix_achat, prix_uni, qte_colis, qte_minimale_commande,
    created_date, last_modified_date
)
SELECT c.id, fp.fournisseur_id, lpad((8000000 + c.id)::text, 7, '0'), c.code_ean_labo,
       greatest(5, (5 * round(fp.prix_achat::numeric / pk.item_qty / 5.0))::int),
       c.regular_unit_price, 1, 0, fp.created_date, fp.last_modified_date
  FROM produit c
  JOIN produit pk ON pk.id = c.parent_id
  JOIN fournisseur_produit fp ON fp.produit_id = pk.id
 WHERE c.type_produit = 'DETAIL'
ON CONFLICT (produit_id, fournisseur_id) DO NOTHING;

UPDATE produit p
   SET fournisseur_produit_principal_id = fp.id
  FROM (SELECT DISTINCT ON (produit_id) id, produit_id
          FROM fournisseur_produit ORDER BY produit_id, id) fp
 WHERE fp.produit_id = p.id
   AND p.fournisseur_produit_principal_id IS NULL;

INSERT INTO rayon_produit (produit_id, rayon_id)
SELECT c.id, rp.rayon_id FROM produit c JOIN rayon_produit rp ON rp.produit_id = c.parent_id
 WHERE c.type_produit = 'DETAIL'
ON CONFLICT (produit_id, rayon_id) DO NOTHING;

DROP TABLE tmp_decond;
DROP TABLE tmp_bdpm_produit;
DROP TABLE tmp_bdpm_prix;
DROP TABLE tmp_bdpm_sa;
DROP TABLE tmp_bdpm_sel;
DROP TABLE tmp_bdpm_c;

DO $$
DECLARE
    v_nb int; v_sans_dci int; v_sans_fp int; v_stup int; v_pso int; v_assoc int; v_homeo int;
    v_groupes int; v_dec int; v_parac int;
BEGIN
    SELECT count(*) INTO v_nb FROM produit WHERE libelle IN (SELECT libelle FROM ref_specialite);
    SELECT count(*) INTO v_sans_dci FROM produit p
     WHERE p.libelle IN (SELECT libelle FROM ref_specialite) AND p.dci_id IS NULL;
    SELECT count(*) INTO v_sans_fp FROM produit p
     WHERE p.type_produit = 'PACKAGE' AND p.fournisseur_produit_principal_id IS NULL;
    SELECT count(*) INTO v_stup FROM produit WHERE statut_legal = 'STUPEFIANTS';
    SELECT count(*) INTO v_pso FROM produit WHERE statut_legal = 'PSO';
    SELECT count(*) INTO v_assoc FROM (SELECT produit_id FROM produit_dci GROUP BY produit_id HAVING count(*) > 1) x;
    SELECT count(*) INTO v_homeo FROM produit p JOIN famille_produit f ON f.id = p.famille_id WHERE f.code = '1040';
    SELECT count(*) INTO v_groupes FROM (SELECT dci_id FROM produit WHERE dci_id IS NOT NULL
                                          GROUP BY dci_id HAVING count(*) >= 3) x;
    SELECT count(*) INTO v_dec FROM produit WHERE type_produit = 'PACKAGE' AND deconditionnable;
    -- Même requête que le contrôle dci de 99_verification.sql : échouer ici, là où se
    -- décide le catalogue, plutôt qu'au bout de toute la chaîne.
    SELECT count(*) INTO v_parac FROM produit p1
      JOIN produit p2 ON p2.dci_id = p1.dci_id
     WHERE p1.libelle LIKE 'PARACETAMOL %' AND p2.libelle LIKE 'DOLIPRANE %';

    IF v_nb < 900 THEN RAISE EXCEPTION 'Produits issus du référentiel : % (attendu >= 900)', v_nb; END IF;
    IF v_sans_dci > 0 THEN RAISE EXCEPTION '% produit(s) du référentiel sans DCI', v_sans_dci; END IF;
    IF v_sans_fp > 0 THEN RAISE EXCEPTION '% produit(s) sans fournisseur principal', v_sans_fp; END IF;
    IF v_stup = 0 OR v_pso = 0 THEN
        RAISE EXCEPTION 'Aucun stupéfiant (%) ou psychotrope (%) : le refus de retour ne pourra pas être montré', v_stup, v_pso;
    END IF;
    IF v_assoc < 2 THEN RAISE EXCEPTION 'Associations à plusieurs molécules : % (attendu >= 2)', v_assoc; END IF;
    IF v_homeo < 20 THEN RAISE EXCEPTION 'Produits homéopathiques : % (attendu >= 20)', v_homeo; END IF;
    IF v_groupes < 20 THEN RAISE EXCEPTION 'Molécules à trois produits ou plus : % (attendu >= 20)', v_groupes; END IF;
    IF v_dec < 20 THEN RAISE EXCEPTION 'Boîtes déconditionnables : % (attendu >= 20)', v_dec; END IF;
    IF v_parac = 0 THEN RAISE EXCEPTION 'Aucun PARACETAMOL ne partage sa molécule avec un DOLIPRANE'; END IF;

    RAISE NOTICE '% produits du référentiel (% homéopathiques, % associations, % déconditionnables, % stupéfiants, % psychotropes).',
                 v_nb, v_homeo, v_assoc, v_dec, v_stup, v_pso;
END $$;

\echo '<< 03c_produits_bdpm : terminé'
