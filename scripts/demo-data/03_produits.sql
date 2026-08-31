-- ============================================================================
-- 03_produits.sql — Catalogue produits, codes fournisseurs et rayons
--
-- Rappels du modèle (§3.2 du plan) :
--   * les montants sont des ENTIERS, en FCFA — aucune décimale ;
--   * produit n'a ni code CIP ni magasin_id ; le CIP vit sur fournisseur_produit ;
--   * contrainte d'unicité (libelle, type_produit) ;
--   * famille_id et tva_id sont obligatoires ;
--   * les familles et TVA sont créées par Flyway : on les retrouve par leur
--     code / taux, jamais par un identifiant codé en dur.
--
-- Les libellés sont construits par combinaison (base, dosage, conditionnement).
-- L'index i est décomposé en base numérique variable, ce qui garantit
-- l'unicité du triplet tant que nb <= |bases| x |dosages| x |conditionnements|.
-- ============================================================================

\i _header.sql

\echo '>> 03_produits : catalogue, codes fournisseurs, rayons'

-- ---------------------------------------------------------------------------
-- Rattachement à la substance active (02b_dci.sql)
--
-- Deux cas se présentent :
--   * le libellé de base EST déjà la molécule (médicaments France, génériques,
--     souches homéopathiques) — le rattachement est une identité ;
--   * le libellé de base est un NOM DE MARQUE (spécialités publiques) — il faut
--     une correspondance explicite vers la molécule.
--
-- Les suffixes « GE » (générique) et « SIROP » (forme déconditionnable) sont
-- retirés avant la recherche : ce sont des variantes de présentation, pas des
-- substances différentes.
--
-- L'effet recherché : DOLIPRANE, PARACETAMOL, PARACETAMOL GE et PARACETAMOL
-- SIROP pointent la MÊME DCI. C'est ce qui rend la substitution générique
-- démontrable — sans cela, l'écran de substitution ne propose jamais rien.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_dci_marque (marque text PRIMARY KEY, molecule text NOT NULL);

INSERT INTO tmp_dci_marque VALUES
    ('AUGMENTIN',  'AMOXICILLINE ACIDE CLAVULANIQUE'),
    ('CLAMOXYL',   'AMOXICILLINE'),
    ('DOLIPRANE',  'PARACETAMOL'),
    ('EFFERALGAN', 'PARACETAMOL'),
    ('ADVIL',      'IBUPROFENE'),
    ('SPASFON',    'PHLOROGLUCINOL'),
    ('SMECTA',     'DIOSMECTITE'),
    ('GAVISCON',   'ALGINATE DE SODIUM'),
    ('MOPRAL',     'OMEPRAZOLE'),
    ('VOLTARENE',  'DICLOFENAC'),
    ('ZYRTEC',     'CETIRIZINE'),
    ('CLARITYNE',  'LORATADINE'),
    ('VENTOLINE',  'SALBUTAMOL'),
    ('SERETIDE',   'SALMETEROL FLUTICASONE'),
    ('LEVOTHYROX', 'LEVOTHYROXINE'),
    ('GLUCOPHAGE', 'METFORMINE'),
    ('AMLOR',      'AMLODIPINE'),
    ('TAHOR',      'ATORVASTATINE'),
    ('KARDEGIC',   'ACIDE ACETYLSALICYLIQUE'),
    ('PLAVIX',     'CLOPIDOGREL');

-- Résout un libellé de base vers l'identifiant de sa substance active.
-- Renvoie NULL pour la parapharmacie et les accessoires, qui n'en ont pas.
CREATE OR REPLACE FUNCTION pg_temp.dci_de(p_base text) RETURNS int AS $$
    SELECT d.id
      FROM dci d
     WHERE d.libelle = COALESCE(
         (SELECT m.molecule FROM tmp_dci_marque m
           WHERE m.marque = regexp_replace(p_base, ' (GE|SIROP)$', '')),
         regexp_replace(p_base, ' (GE|SIROP)$', '')
     )
     LIMIT 1;
$$ LANGUAGE sql STABLE;

-- ---------------------------------------------------------------------------
-- Configuration par famille
--
-- cost_min / cost_max : fourchette de prix d'achat unitaire, en FCFA.
-- marge_pct           : marge appliquée pour obtenir le prix de vente.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_fam_cfg (
    fam_code      text    NOT NULL,
    nb            int     NOT NULL,
    bases         text[]  NOT NULL,
    dosages       text[]  NOT NULL,
    conds         text[]  NOT NULL,
    cost_min      int     NOT NULL,
    cost_max      int     NOT NULL,
    marge_pct     int     NOT NULL,
    tva_taux      int     NOT NULL,
    statut_legal  text    NOT NULL,
    forme_libelle text
);

INSERT INTO tmp_fam_cfg VALUES
-- Médicaments France : le gros du catalogue, TVA 0, majoritairement sur liste.
('1050', 250,
 ARRAY['PARACETAMOL','IBUPROFENE','AMOXICILLINE','METRONIDAZOLE','CIPROFLOXACINE',
       'OMEPRAZOLE','METFORMINE','AMLODIPINE','LOSARTAN','ATORVASTATINE',
       'CETIRIZINE','LORATADINE','DICLOFENAC','TRAMADOL','PREDNISOLONE',
       'FUROSEMIDE','SPIRONOLACTONE','SALBUTAMOL','BECLOMETASONE','AZITHROMYCINE',
       'CEFTRIAXONE','DOXYCYCLINE','FLUCONAZOLE','ACICLOVIR','ARTEMETHER',
       'QUININE','ALBENDAZOLE','MEBENDAZOLE','RANITIDINE','DOMPERIDONE',
       'BROMAZEPAM','DIAZEPAM','CARBAMAZEPINE','ACIDE FOLIQUE','FER FOLATE',
       'INSULINE HUMAINE','GLIBENCLAMIDE','ENALAPRIL','BISOPROLOL','WARFARINE'],
 ARRAY['100MG','200MG','250MG','500MG','1G','5MG','10MG','20MG'],
 ARRAY['B/10','B/20','B/16','B/30'],
 150, 4500, 28, 0, 'LISTE_I', 'Comprimés'),

-- Spécialités publiques : produits plus chers, marge plus faible.
('1000', 80,
 ARRAY['AUGMENTIN','CLAMOXYL','DOLIPRANE','EFFERALGAN','ADVIL',
       'SPASFON','SMECTA','GAVISCON','MOPRAL','VOLTARENE',
       'ZYRTEC','CLARITYNE','VENTOLINE','SERETIDE','LEVOTHYROX',
       'GLUCOPHAGE','AMLOR','TAHOR','KARDEGIC','PLAVIX'],
 ARRAY['250MG','500MG','1G','5MG','50MG','100MG'],
 ARRAY['B/12','B/14','B/28','FL 100ML'],
 800, 22000, 18, 0, 'LISTE_I', 'Comprimés'),

-- Génériques : marge intermédiaire.
('1030', 60,
 ARRAY['PARACETAMOL GE','IBUPROFENE GE','AMOXICILLINE GE','OMEPRAZOLE GE',
       'METFORMINE GE','AMLODIPINE GE','CETIRIZINE GE','DICLOFENAC GE',
       'CIPROFLOXACINE GE','AZITHROMYCINE GE','FLUCONAZOLE GE','DOXYCYCLINE GE'],
 ARRAY['100MG','250MG','500MG','1G','10MG'],
 ARRAY['B/10','B/20','B/30'],
 90, 2200, 32, 0, 'LISTE_II', 'Comprimés'),

-- Homéopathie : TVA 18, hors liste, forte marge.
('1040', 50,
 ARRAY['ARNICA MONTANA','BELLADONNA','NUX VOMICA','PULSATILLA','SULFUR',
       'IGNATIA AMARA','GELSEMIUM','APIS MELLIFICA','RHUS TOX','CHAMOMILLA'],
 ARRAY['5CH','7CH','9CH','15CH','30CH'],
 ARRAY['TUBE GR','DOSE','GOUTTES 30ML'],
 700, 3500, 45, 18, 'SANS_LISTE', 'Granulés'),

-- Diététique infantile : TVA 18, hors liste.
('5000', 40,
 ARRAY['LAIT 1ER AGE','LAIT 2EME AGE','LAIT CROISSANCE','FARINE INFANTILE',
       'CEREALES BEBE','LAIT HYPOALLERGENIQUE','LAIT ANTI-REGURGITATION','LAIT SANS LACTOSE'],
 ARRAY['400G','800G','900G','1KG'],
 ARRAY['BOITE','SACHET'],
 2500, 14000, 30, 18, 'SANS_LISTE', 'Poudres'),

-- Diététique adulte.
('6000', 20,
 ARRAY['COMPLEMENT PROTEINE','VITAMINE C','MULTIVITAMINES','MAGNESIUM B6',
       'OMEGA 3','CALCIUM VITAMINE D'],
 ARRAY['500MG','1000MG','60 UNITES'],
 ARRAY['B/30','B/60','POT 300G'],
 1800, 12000, 38, 18, 'SANS_LISTE', 'Gélules'),

-- Parfumerie : forte marge.
('3000', 20,
 ARRAY['CREME HYDRATANTE','LAIT CORPOREL','GEL DOUCHE','SHAMPOOING',
       'DEODORANT','EAU MICELLAIRE'],
 ARRAY['50ML','100ML','200ML','400ML'],
 ARRAY['FLACON','TUBE','POT'],
 1500, 18000, 55, 18, 'SANS_LISTE', 'Crèmes'),

-- Accessoires : hors gestion de lot (voir plus bas).
('8000', 30,
 ARRAY['PANSEMENT ADHESIF','COMPRESSE STERILE','BANDE DE CREPE','THERMOMETRE',
       'SERINGUE','GANTS LATEX','ALCOOL 70','COTON HYDROPHILE','SPARADRAP','MASQUE CHIRURGICAL'],
 ARRAY['PETIT','MOYEN','GRAND'],
 ARRAY['B/5','B/10','B/100','UNITE'],
 300, 9000, 48, 18, 'SANS_LISTE', NULL);

-- ---------------------------------------------------------------------------
-- 1. Produits PACKAGE non déconditionnables (550)
--
-- Le catalogue courant : ce qu'on achète au grossiste et qu'on délivre tel
-- quel — une boîte de DOLIPRANE, un tube d'ARNICA, une bande de crêpe, une
-- boîte de lait infantile. Ce sont des produits à part entière : ils ont leur
-- fournisseur, ils se commandent, ils se classent (ABC), ils entrent dans le
-- calcul de réapprovisionnement.
--
-- Ils ont porté 'DETAIL' par erreur. DETAIL ne désigne PAS « vendu à l'unité »
-- mais un DÉCONDITIONNÉ — l'unité issue d'une boîte, qui porte parent_id et ne
-- se commande jamais (§2 ci-dessous). L'application s'appuie sur cette
-- convention : ClassificationCriticiteService et SemoisCalculationService
-- écartent les DETAIL du calcul de classe et des suggestions de réappro, et le
-- catalogue les range sous le filtre « Déconditionnés ». Typés DETAIL, 550 des
-- 600 produits en étaient donc silencieusement exclus.
--
-- item_qty vaut 1 : le nombre d'unités par boîte n'a de sens que pour un
-- produit déconditionnable, et ceux-ci ne le sont pas.
--
-- Prix déterministes : le multiplicateur premier étale les valeurs dans la
-- fourchette sans recourir à random(), pour que deux exécutions produisent
-- exactement le même catalogue.
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
    p.libelle,
    'PACKAGE',
    'ENABLE',
    p.cost,
    p.prix,
    p.prix,
    1, 0, 0, 0,
    false, true,
    -- Les accessoires et la parfumerie ne sont pas suivis par lot : c'est le
    -- cas réel en officine, et le chemin mixte doit être exercé.
    p.fam_code NOT IN ('8000', '3000'),
    -- Thermosensibles : insulines et quelques spécialités.
    p.base LIKE 'INSULINE%',
    p.tva_taux > 0,
    p.statut_legal,
    CASE WHEN p.i % 17 = 0 THEN 'A_PLUS'
         WHEN p.i % 5  = 0 THEN 'A'
         WHEN p.i % 3  = 0 THEN 'B'
         ELSE 'C' END,
    'CODE_0',
    CASE WHEN p.i % 5 = 0 THEN 20 ELSE 10 END,
    CASE WHEN p.i % 5 = 0 THEN 10 ELSE 5 END,
    0,
    '340' || lpad((100000 + p.rn)::text, 10, '0'),
    f.id,
    t.id,
    fp.id,
    -- Nul pour la parapharmacie et les accessoires : ils n'ont pas de molécule.
    pg_temp.dci_de(p.base),
    NOW() - INTERVAL '200 days',
    NOW() - INTERVAL '200 days'
FROM (
    SELECT
        c.fam_code,
        c.tva_taux,
        c.statut_legal,
        c.forme_libelle,
        i,
        row_number() OVER (ORDER BY c.fam_code, i) AS rn,
        c.bases[1 + (i % array_length(c.bases, 1))] AS base,
        c.bases[1 + (i % array_length(c.bases, 1))]
            || ' ' || c.dosages[1 + ((i / array_length(c.bases, 1)) % array_length(c.dosages, 1))]
            || ' ' || c.conds  [1 + ((i / (array_length(c.bases, 1) * array_length(c.dosages, 1)))
                                     % array_length(c.conds, 1))]
            AS libelle,
        -- Prix d'achat étalé sur TOUTE la fourchette, arrondi à 5 F.
        --
        -- (i * 7919) % nb est une permutation de 0..nb-1 : 7919 est premier et
        -- ne divise aucun des effectifs, donc le pas est premier avec nb. On
        -- mélange ainsi l'ordre sans recourir à random(), puis on projette sur
        -- la fourchette. Un simple modulo de la fourchette ne couvrirait qu'une
        -- fraction de celle-ci dès que l'effectif est petit.
        (5 * round((
            c.cost_min
            + ((i * 7919) % c.nb)::numeric * (c.cost_max - c.cost_min) / greatest(c.nb - 1, 1)
        ) / 5.0))::int AS cost,
        (5 * round((
            (c.cost_min
             + ((i * 7919) % c.nb)::numeric * (c.cost_max - c.cost_min) / greatest(c.nb - 1, 1))
            * (100 + c.marge_pct) / 100.0
        ) / 5.0))::int AS prix
    FROM tmp_fam_cfg c
    CROSS JOIN LATERAL generate_series(0, c.nb - 1) AS i
) p
JOIN famille_produit f ON f.code = p.fam_code
JOIN tva            t ON t.taux = p.tva_taux
LEFT JOIN form_produit fp ON fp.libelle = p.forme_libelle
ON CONFLICT (libelle, type_produit) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Couples PACKAGE / DETAIL déconditionnables (25 + 25)
--
-- Le PACKAGE est la boîte, le DETAIL son unité de délivrance. C'est le DETAIL
-- qui porte parent_id : SalesLineServiceImpl remonte au parent quand le stock
-- unitaire est insuffisant, pour déclencher un déconditionnement.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_decond (
    base       text NOT NULL,
    item_qty   int  NOT NULL,
    cost_boite int  NOT NULL,
    marge_pct  int  NOT NULL
);

-- Le libellé combine base et conditionnement : les deux indices doivent varier
-- indépendamment, sinon (base, item_qty) ne prend que 5 valeurs distinctes au
-- lieu de 25 et le ON CONFLICT en écarte silencieusement 20.
--   base     <- i % 5        (change à chaque ligne)
--   item_qty <- (i / 5) % 5  (change tous les 5)
INSERT INTO tmp_decond
SELECT
    (ARRAY['AMOXICILLINE SIROP','METRONIDAZOLE SIROP','PARACETAMOL SIROP',
           'IBUPROFENE SIROP','COTRIMOXAZOLE SIROP'])[1 + (i % 5)],
    (ARRAY[10, 12, 20, 24, 30])[1 + ((i / 5) % 5)],
    -- Même principe d'étalement que ci-dessus : permutation puis projection
    -- sur la fourchette 3 000 – 15 000 F.
    (5 * round((3000 + ((i * 7919) % 25)::numeric * 12000 / 24) / 5.0))::int,
    30
FROM generate_series(0, 24) AS i;

-- 2a. Les PACKAGE
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
    d.base || ' ' || d.item_qty || ' UNITES BOITE',
    'PACKAGE',
    'ENABLE',
    d.cost_boite,
    (5 * round((d.cost_boite * (100 + d.marge_pct) / 100.0 / 5.0)))::int,
    (5 * round((d.cost_boite * (100 + d.marge_pct) / 100.0 / 5.0)))::int,
    d.item_qty,
    (d.cost_boite / d.item_qty)::int,
    ((5 * round((d.cost_boite * (100 + d.marge_pct) / 100.0 / 5.0)))::int / d.item_qty)::int,
    0,
    true,  -- c'est la boîte qui se déconditionne
    true, true, false, false,
    'LISTE_II',
    'B',
    'CODE_0',
    10, 5, 0,
    '340' || lpad((900000 + row_number() OVER (ORDER BY d.base, d.item_qty))::text, 10, '0'),
    f.id, t.id, fp.id,
    -- « AMOXICILLINE SIROP » et « AMOXICILLINE » partagent la même molécule.
    pg_temp.dci_de(d.base),
    NOW() - INTERVAL '200 days',
    NOW() - INTERVAL '200 days'
FROM tmp_decond d
JOIN famille_produit f ON f.code = '1050'
JOIN tva            t ON t.taux = 0
LEFT JOIN form_produit fp ON fp.libelle = 'Flacons'
ON CONFLICT (libelle, type_produit) DO NOTHING;

-- 2b. Les DETAIL enfants, rattachés à leur PACKAGE
INSERT INTO produit (
    libelle, type_produit, status,
    cost_amount, regular_unit_price, net_unit_price,
    item_qty, item_cost_amount, item_regular_unit_price, prix_mnp,
    deconditionnable, chiffre, gestion_lot, thermosensible, remisable,
    statut_legal, classe_criticite, code_remise,
    qty_appro, qty_seuil_mini, seuil_decond,
    parent_id, famille_id, tva_id, forme_id, dci_id,
    created_at, updated_at
)
SELECT
    d.base || ' ' || d.item_qty || ' UNITES DETAIL',
    'DETAIL',
    'ENABLE',
    -- Même règle sur les prix déduits d'un conditionnement : multiples de 5.
    greatest(5, (5 * round((pk.cost_amount::numeric / pk.item_qty) / 5.0))::int),
    greatest(5, (5 * round((pk.regular_unit_price::numeric / pk.item_qty) / 5.0))::int),
    greatest(5, (5 * round((pk.regular_unit_price::numeric / pk.item_qty) / 5.0))::int),
    1, 0, 0, 0,
    false, true, true, false, false,
    'LISTE_II',
    'B',
    'CODE_0',
    10, 5,
    -- Seuil déclenchant le déconditionnement d'une boîte supplémentaire.
    greatest(2, (pk.item_qty / 4)::int),
    -- L'unite herite de la molecule de sa boite : c'est la meme substance.
    pk.id, pk.famille_id, pk.tva_id, pk.forme_id, pk.dci_id,
    NOW() - INTERVAL '200 days',
    NOW() - INTERVAL '200 days'
FROM tmp_decond d
JOIN produit pk
  ON pk.libelle = d.base || ' ' || d.item_qty || ' UNITES BOITE'
 AND pk.type_produit = 'PACKAGE'
ON CONFLICT (libelle, type_produit) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. fournisseur_produit — TOUJOURS sur un fournisseur PRINCIPAL
--
-- Le CIP est un code national : il est identique chez tous les fournisseurs
-- d'un même produit. La contrainte d'unicité porte sur (code_cip,
-- fournisseur_id), ce qui l'autorise.
--
-- CIP et EAN ne sont PAS le même code, et la colonne les distingue : le CIP
-- tient sur 7 chiffres (CIP7), l'EAN sur 13. Les deux portaient ici le même
-- EAN-13, ce qui donnait des « CIP » de treize chiffres — impossibles à saisir
-- au comptoir, et faux dès qu'on les compare à un bon de commande.
--
-- Répartition : chaque produit est référencé chez 1 à 3 principaux, choisis de
-- façon déterministe à partir de son identifiant.
-- ---------------------------------------------------------------------------
INSERT INTO fournisseur_produit (
    produit_id, fournisseur_id, code_cip, code_ean,
    prix_achat, prix_uni, qte_colis, qte_minimale_commande,
    created_date, last_modified_date
)
SELECT
    p.id,
    f.id,
    -- CIP7 : sept chiffres, le format que porte une boîte française.
    lpad((1000000 + p.id)::text, 7, '0'),
    -- EAN-13 : le code-barres, treize chiffres, préfixe 340 (France).
    '340' || lpad((100000 + p.id)::text, 10, '0'),
    -- Le prix d'achat varie légèrement d'un grossiste à l'autre.
    -- Arrondi au multiple de 5 : la plus petite pièce en FCFA vaut 5 francs, et un prix qui
    -- ne l'est pas produit des totaux impossibles à rendre en espèces.
    greatest(5, (5 * round((p.cost_amount * (100 + ((p.id + f.rang * 7) % 9) - 4) / 100.0) / 5.0))::int),
    p.regular_unit_price,
    CASE WHEN p.id % 7 = 0 THEN 10 WHEN p.id % 3 = 0 THEN 5 ELSE 1 END,
    CASE WHEN p.id % 11 = 0 THEN 5 ELSE 0 END,
    NOW() - INTERVAL '200 days',
    NOW() - INTERVAL '200 days'
FROM produit p
-- Nombre de fournisseurs par produit : 1 (60 %), 2 (30 %), 3 (10 %).
CROSS JOIN LATERAL generate_series(0, CASE WHEN p.id % 10 = 0        THEN 2
                                           WHEN p.id % 10 IN (1,2,3) THEN 1
                                           ELSE 0 END) AS k
CROSS JOIN LATERAL (
    SELECT pr.id,
           row_number() OVER (ORDER BY pr.odre, pr.id) AS rang,
           count(*)     OVER ()                        AS total
      FROM fournisseur pr
     WHERE pr.parent_id IS NULL          -- <- invariant : jamais une agence
) f
-- Le fournisseur est choisi par décalage tournant, et non par les premiers
-- rangs : sinon les derniers principaux ne référenceraient aucun produit et
-- les commandes qui leur sont passées (étape 3) seraient vides.
WHERE f.rang = 1 + ((p.id + k) % f.total)
ON CONFLICT (produit_id, fournisseur_id) DO NOTHING;

-- Fournisseur principal du produit : le premier référencé.
UPDATE produit p
   SET fournisseur_produit_principal_id = fp.id
  FROM (
      SELECT DISTINCT ON (produit_id) id, produit_id
        FROM fournisseur_produit
       ORDER BY produit_id, id
  ) fp
 WHERE fp.produit_id = p.id
   AND p.fournisseur_produit_principal_id IS NULL;

-- ---------------------------------------------------------------------------
-- Stupéfiants et psychotropes
--
-- Le catalogue ne connaissait que SANS_LISTE, LISTE_I et LISTE_II : deux
-- statuts manquaient, et avec eux tout ce qu'ils entraînent — traçabilité du
-- lot obligatoire (`StatutLegal.isTracabilityLotObligatoire`) et surtout
-- INTERDICTION DE RETOUR (`isRetourInterdit`). L'écran de retour client refuse
-- ces lignes et les oriente vers la destruction réglementaire : sans un seul
-- produit concerné, ce refus ne pouvait ni s'observer ni s'illustrer.
--
-- Les molécules retenues sont celles qui portent réellement ces statuts en
-- officine ; leur nombre reste faible, comme dans la réalité.
-- ---------------------------------------------------------------------------
UPDATE produit
   SET statut_legal = 'STUPEFIANTS'
 WHERE libelle LIKE 'TRAMADOL%';

UPDATE produit
   SET statut_legal = 'PSO'
 WHERE libelle LIKE 'DIAZEPAM%'
    OR libelle LIKE 'BROMAZEPAM%';

DO $$
DECLARE v_stup int; v_pso int;
BEGIN
    SELECT count(*) INTO v_stup FROM produit WHERE statut_legal = 'STUPEFIANTS';
    SELECT count(*) INTO v_pso  FROM produit WHERE statut_legal = 'PSO';
    IF v_stup = 0 OR v_pso = 0 THEN
        RAISE EXCEPTION 'Aucun stupéfiant (%) ou psychotrope (%) : le refus de retour ne pourra pas être montré', v_stup, v_pso;
    END IF;
    RAISE NOTICE '   % stupéfiant(s), % psychotrope(s)', v_stup, v_pso;
END $$;

-- ---------------------------------------------------------------------------
-- 4. Rayons commerciaux et affectation des produits
--
-- Les rayons « SANS EMPLACEMENT » (id 2 et 3) existent déjà, posés par Flyway.
-- Unicité : (code, storage_id) et (libelle, storage_id).
--
-- type_zone est lu par Hibernate en @Enumerated(STRING) : toute valeur absente
-- de TypeZone fait échouer le chargement de l'entité côté application, pas au
-- moment de l'INSERT. Les valeurs admises sont AMBIANT, FROID, OTC, ORDONNANCE,
-- TOXIQUE, RESERVE, PARA — « VENTE », employé ici auparavant, n'en fait pas
-- partie et provoquait « No enum constant TypeZone.VENTE » à l'ouverture de
-- l'écran des rayons.
--
-- La répartition ci-dessous couvre cinq des sept valeurs. TOXIQUE et RESERVE
-- restent inutilisés : ils supposeraient des rayons dédiés, et donc de revoir
-- l'affectation des produits juste en dessous.
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
-- DO UPDATE et non DO NOTHING : rayon figure dans la liste de préservation de
-- 00_reset.sql, donc les lignes survivent au reset. Avec DO NOTHING, une valeur
-- erronée déjà chargée — comme le « VENTE » d'avant — resterait en base malgré
-- toutes les relances. Le script doit converger, pas seulement s'abstenir.
-- Seules les colonnes non uniques sont réécrites : toucher libelle risquerait
-- de heurter l'unicité (libelle, storage_id) depuis une autre ligne.
ON CONFLICT (code, storage_id) DO UPDATE
   SET type_zone = EXCLUDED.type_zone,
       position  = EXCLUDED.position;

-- Affectation : un rayon et un seul par produit, déduit de sa famille.
INSERT INTO rayon_produit (produit_id, rayon_id)
SELECT p.id, r.id
  FROM produit p
  JOIN famille_produit f ON f.id = p.famille_id
  JOIN storage s ON s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1
  JOIN rayon   r ON r.storage_id = s.id
   AND r.code = CASE f.code
        WHEN '1040' THEN 'HOME'
        WHEN '5000' THEN 'NUTR'
        WHEN '6000' THEN 'NUTR'
        WHEN '3000' THEN 'PARA'
        WHEN '8000' THEN 'ACCE'
        ELSE CASE
            WHEN p.libelle ~ '^(AMOXICILLINE|CIPROFLOXACINE|METRONIDAZOLE|AZITHROMYCINE|CEFTRIAXONE|DOXYCYCLINE|AUGMENTIN|CLAMOXYL|COTRIMOXAZOLE)' THEN 'ANTB'
            WHEN p.libelle ~ '^(PARACETAMOL|IBUPROFENE|DICLOFENAC|TRAMADOL|DOLIPRANE|EFFERALGAN|ADVIL|VOLTARENE|SPASFON)' THEN 'ANTA'
            WHEN p.libelle ~ '^(AMLODIPINE|LOSARTAN|ATORVASTATINE|FUROSEMIDE|SPIRONOLACTONE|ENALAPRIL|BISOPROLOL|WARFARINE|AMLOR|TAHOR|KARDEGIC|PLAVIX)' THEN 'CARD'
            WHEN p.libelle ~ '^(METFORMINE|INSULINE|GLIBENCLAMIDE|GLUCOPHAGE|LEVOTHYROX)' THEN 'DIAB'
            WHEN p.libelle ~ '^(OMEPRAZOLE|RANITIDINE|DOMPERIDONE|SMECTA|GAVISCON|MOPRAL)' THEN 'GAST'
            WHEN p.libelle ~ '^(CETIRIZINE|LORATADINE|SALBUTAMOL|BECLOMETASONE|ZYRTEC|CLARITYNE|VENTOLINE|SERETIDE)' THEN 'RESP'
            ELSE 'DIVE'
        END
   END
ON CONFLICT (produit_id, rayon_id) DO NOTHING;

DROP FUNCTION pg_temp.dci_de(text);
DROP TABLE tmp_dci_marque;
DROP TABLE tmp_fam_cfg;
DROP TABLE tmp_decond;

\echo '<< 03_produits : terminé'
