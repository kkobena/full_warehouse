-- ============================================================================
-- 99_verification.sql — Contrôles d'intégrité du jeu de démonstration
--
-- Spécification exécutable des invariants du §4 du plan. Chaque contrôle
-- alimente une ligne de résultat ; le script sort en ERREUR si l'un d'eux
-- échoue, ce qui interrompt run_all.sql.
--
-- Deux formes de contrôle :
--     verif_vide(section, nom, sql)        → la requête ne doit rien renvoyer
--     verif_compte(section, nom, sql, min) → le compte doit atteindre un seuil
--
-- ÉTAT : couvre les étapes 1 (config, fournisseurs, produits, rayons),
--        2 (clients, organismes tiers-payants, contrats)
--        3 (commandes fournisseurs et lignes)
--        4 (lots, péremptions, sérials FMD, réceptions, stock)
--        5 (caisses, ventes, FEFO, encaissements, ventilation tiers-payant)
--        6 (répartitions TVA, consommations, magasin dépôt)
--        et 7 (destruction, facturation, numérotation, mouvements produit).
-- Les contrôles des étapes suivantes sont listés en fin de fichier et seront
-- activés au fur et à mesure.
-- ============================================================================

\i _header.sql

\echo '>> 99_verification : contrôles d''intégrité'

DROP TABLE IF EXISTS tmp_verif;
CREATE TEMP TABLE tmp_verif (
    ordre   serial,
    section text,
    nom     text,
    statut  text,
    detail  text
);

-- « Ne doit rien renvoyer ». La requête est passée en texte et exécutée telle
-- quelle, ce qui permet de garder les assertions lisibles et copiables.
CREATE OR REPLACE FUNCTION pg_temp.verif_vide(p_section text, p_nom text, p_sql text)
RETURNS void AS $$
DECLARE n bigint;
BEGIN
    EXECUTE 'SELECT count(*) FROM (' || p_sql || ') AS anomalies' INTO n;
    INSERT INTO tmp_verif (section, nom, statut, detail)
    VALUES (p_section, p_nom,
            CASE WHEN n = 0 THEN 'OK' ELSE 'ECHEC' END,
            CASE WHEN n = 0 THEN '' ELSE n || ' ligne(s) en anomalie' END);
EXCEPTION WHEN OTHERS THEN
    INSERT INTO tmp_verif (section, nom, statut, detail)
    VALUES (p_section, p_nom, 'ERREUR', SQLERRM);
END $$ LANGUAGE plpgsql;

-- « Doit atteindre un seuil ».
CREATE OR REPLACE FUNCTION pg_temp.verif_compte(p_section text, p_nom text, p_sql text, p_min bigint)
RETURNS void AS $$
DECLARE n bigint;
BEGIN
    EXECUTE 'SELECT count(*) FROM (' || p_sql || ') AS c' INTO n;
    INSERT INTO tmp_verif (section, nom, statut, detail)
    VALUES (p_section, p_nom,
            CASE WHEN n >= p_min THEN 'OK' ELSE 'ECHEC' END,
            n || ' (minimum attendu : ' || p_min || ')');
EXCEPTION WHEN OTHERS THEN
    INSERT INTO tmp_verif (section, nom, statut, detail)
    VALUES (p_section, p_nom, 'ERREUR', SQLERRM);
END $$ LANGUAGE plpgsql;


-- ===========================================================================
-- ALERTES DE STOCK
-- ===========================================================================
-- Les trois types d'alerte doivent avoir des cas : sans quoi le rapport
-- « Alertes de Stock » ne peut demontrer ni ses compteurs, ni son filtre.
SELECT pg_temp.verif_compte('stock', 'Des produits en rupture', $q$
    SELECT sp.produit_id
      FROM stock_produit sp
      JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'PRINCIPAL'
      JOIN produit p ON p.id = sp.produit_id AND p.status = 'ENABLE'
     WHERE sp.qty_stock <= 0
$q$, 10);

SELECT pg_temp.verif_compte('stock', 'Des produits sous leur seuil mini', $q$
    SELECT sp.produit_id
      FROM stock_produit sp
      JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'PRINCIPAL'
      JOIN produit p ON p.id = sp.produit_id AND p.status = 'ENABLE'
     WHERE sp.qty_stock > 0 AND sp.qty_stock < p.qty_seuil_mini
$q$, 10);

SELECT pg_temp.verif_compte('stock', 'Des lots proches de la péremption', $q$
    SELECT l.id FROM lot l
     WHERE l.statut = 'AVAILABLE' AND l.current_quantity > 0
       AND l.expiry_date IS NOT NULL
       AND l.expiry_date < CURRENT_DATE + INTERVAL '3 months'
$q$, 10);


-- ===========================================================================
-- DEMARQUE ET AJUSTEMENTS
-- ===========================================================================
SELECT pg_temp.verif_compte('demarque', 'Des bons d''ajustement existent', $q$
    SELECT id FROM ajust
$q$, 20);

-- Le rapport ne retient que les sorties de bons CLOTURES : sans elles, l'ecran
-- s'ouvre sur « Aucune demarque sur cette periode » quelle que soit la periode.
SELECT pg_temp.verif_compte('demarque', 'Des lignes de demarque exploitables', $q$
    SELECT l.id FROM ajust a
      JOIN ajustement l ON l.ajust_id = a.id
     WHERE a.statut = 'CLOSED' AND l.type_ajust = 'AJUSTEMENT_OUT'
$q$, 20);

SELECT pg_temp.verif_vide('demarque', 'Stock après = stock avant + mouvement', $q$
    SELECT id FROM ajustement WHERE stock_after <> stock_before + qty_mvt
$q$);

SELECT pg_temp.verif_vide('demarque', 'Signe du mouvement conforme au type', $q$
    SELECT id FROM ajustement
     WHERE (type_ajust = 'AJUSTEMENT_OUT' AND qty_mvt >= 0)
        OR (type_ajust = 'AJUSTEMENT_IN'  AND qty_mvt <= 0)
$q$);

SELECT pg_temp.verif_vide('demarque', 'Toute sortie porte un motif', $q$
    SELECT id FROM ajustement
     WHERE type_ajust = 'AJUSTEMENT_OUT' AND motif_ajustement_id IS NULL
$q$);

-- Plusieurs motifs distincts : la ventilation du rapport n'a d'interet que si
-- elle a plus d'une ligne.
SELECT pg_temp.verif_compte('demarque', 'La demarque se ventile sur plusieurs motifs', $q$
    SELECT DISTINCT l.motif_ajustement_id FROM ajust a
      JOIN ajustement l ON l.ajust_id = a.id
     WHERE a.statut = 'CLOSED' AND l.type_ajust = 'AJUSTEMENT_OUT'
       AND l.motif_ajustement_id IS NOT NULL
$q$, 3);


-- ===========================================================================
-- CONFIGURATION
-- ===========================================================================
SELECT pg_temp.verif_vide('config', 'Gestion de lot activée', $q$
    SELECT name FROM app_configuration
     WHERE name IN ('APP_GESTION_LOT', 'APP_GESTION_LOT_INVENTAIRE')
       AND value <> '1'
$q$);

SELECT pg_temp.verif_compte('config', 'Les deux clés de lot existent', $q$
    SELECT 1 FROM app_configuration
     WHERE name IN ('APP_GESTION_LOT', 'APP_GESTION_LOT_INVENTAIRE')
$q$, 2);

-- À zéro, le budget est illimité et son indicateur disparaît : ACH-20 ne serait
-- alors plus démontrable malgré un chargement techniquement valide.
SELECT pg_temp.verif_compte('config', 'Budget mensuel de commande configuré', $q$
    SELECT 1 FROM app_configuration
     WHERE name = 'APP_BUDGET_MENSUEL_COMMANDE'
       AND value ~ '^[0-9]+$'
       AND value::numeric > 0
$q$, 1);


-- ===========================================================================
-- FOURNISSEURS  (§3.3)
-- ===========================================================================
SELECT pg_temp.verif_compte('fournisseurs', 'Principaux présents', $q$
    SELECT 1 FROM fournisseur WHERE parent_id IS NULL
$q$, 5);

SELECT pg_temp.verif_compte('fournisseurs', 'Agences présentes', $q$
    SELECT 1 FROM fournisseur WHERE parent_id IS NOT NULL
$q$, 12);

-- La hiérarchie est à deux niveaux : une agence ne peut pas avoir d'agence.
SELECT pg_temp.verif_vide('fournisseurs', 'Hiérarchie à deux niveaux', $q$
    SELECT a.id FROM fournisseur a
      JOIN fournisseur p ON p.id = a.parent_id
     WHERE p.parent_id IS NOT NULL
$q$);

SELECT pg_temp.verif_vide('fournisseurs', 'Aucun fournisseur son propre parent', $q$
    SELECT id FROM fournisseur WHERE parent_id = id
$q$);

-- Cas de repli : au moins un principal sans agence, pour que le chemin
-- « commande passée directement au principal » soit représenté.
SELECT pg_temp.verif_compte('fournisseurs', 'Au moins un principal sans agence', $q$
    SELECT p.id FROM fournisseur p
     WHERE p.parent_id IS NULL
       AND NOT EXISTS (SELECT 1 FROM fournisseur a WHERE a.parent_id = p.id)
$q$, 1);

SELECT pg_temp.verif_vide('fournisseurs', 'Code renseigné (colonne NOT NULL)', $q$
    SELECT id FROM fournisseur WHERE code IS NULL OR btrim(code) = ''
$q$);


-- ===========================================================================
-- RÉPARTITIONS RAYON / RÉSERVE
--
-- L'écran « Répartition & Transferts » et les suggestions de réassort n'ont rien
-- à montrer sans cet historique — et un mouvement dont les stocks avant/après ne
-- se recoupent pas ferait douter de tout le tableau.
-- ===========================================================================
SELECT pg_temp.verif_compte('repartitions', 'Mouvements rayon / réserve', $q$
    SELECT 1 FROM repartition_stock_produit
$q$, 20);

SELECT pg_temp.verif_compte('repartitions', 'Mouvements automatiques', $q$
    SELECT 1 FROM repartition_stock_produit WHERE type_repartition = 0
$q$, 5);

SELECT pg_temp.verif_compte('repartitions', 'Mouvements manuels', $q$
    SELECT 1 FROM repartition_stock_produit WHERE type_repartition = 1
$q$, 5);

SELECT pg_temp.verif_vide('repartitions', 'Stocks avant/après cohérents', $q$
    SELECT id FROM repartition_stock_produit
     WHERE source_init_stock - source_final_stock <> qty_mvt
        OR dest_final_stock - dest_init_stock <> qty_mvt
$q$);


-- ===========================================================================
-- PROPOSITIONS D'ACHAT
--
-- Sans elles, la moitié du module d'achat s'ouvre sur un écran vide : le manuel
-- montrerait « Aucun fournisseur » là où il doit montrer un réapprovisionnement.
-- ===========================================================================
SELECT pg_temp.verif_compte('suggestions', 'Propositions de réapprovisionnement', $q$
    SELECT 1 FROM suggestion
$q$, 3);

SELECT pg_temp.verif_vide('suggestions', 'Toute proposition a des lignes', $q$
    SELECT s.id FROM suggestion s
     WHERE NOT EXISTS (SELECT 1 FROM suggestion_line sl WHERE sl.suggestion_id = s.id)
$q$);

SELECT pg_temp.verif_compte('suggestions', 'Une proposition validée, prête à commander', $q$
    SELECT 1 FROM suggestion WHERE statut = 'VALIDEE'
$q$, 1);


-- ===========================================================================
-- PRODUITS  (§3.2)
-- ===========================================================================
SELECT pg_temp.verif_compte('produits', 'Catalogue complet', $q$
    SELECT 1 FROM produit
$q$, 600);

SELECT pg_temp.verif_vide('produits', 'Colonnes obligatoires renseignées', $q$
    SELECT id FROM produit
     WHERE libelle IS NULL OR type_produit IS NULL OR status IS NULL
        OR cost_amount IS NULL OR regular_unit_price IS NULL OR net_unit_price IS NULL
        OR item_qty IS NULL OR item_cost_amount IS NULL OR item_regular_unit_price IS NULL
        OR prix_mnp IS NULL OR deconditionnable IS NULL
        OR famille_id IS NULL OR tva_id IS NULL
        OR created_at IS NULL OR updated_at IS NULL
$q$);

-- Les montants sont des entiers en FCFA : un prix nul ou négatif est une erreur.
SELECT pg_temp.verif_vide('produits', 'Prix strictement positifs', $q$
    SELECT id FROM produit WHERE cost_amount <= 0 OR regular_unit_price <= 0
$q$);

-- Une officine ne vend pas à perte : la marge doit être positive.
SELECT pg_temp.verif_vide('produits', 'Prix de vente supérieur au prix d''achat', $q$
    SELECT id FROM produit WHERE regular_unit_price <= cost_amount
$q$);

SELECT pg_temp.verif_vide('produits', 'Libellés uniques par type', $q$
    SELECT libelle FROM produit
     GROUP BY libelle, type_produit HAVING count(*) > 1
$q$);

SELECT pg_temp.verif_vide('produits', 'Enums conformes au modèle', $q$
    SELECT id FROM produit
     WHERE type_produit NOT IN ('DETAIL', 'PACKAGE')
        OR status       NOT IN ('ENABLE', 'DISABLE', 'DELETED', 'CLOSED')
        OR statut_legal NOT IN ('SANS_LISTE', 'LISTE_I', 'LISTE_II', 'STUPEFIANTS', 'PSO')
$q$);

-- Déconditionnement : c'est la boîte qui se déconditionne, et l'unité qui
-- porte parent_id (SalesLineServiceImpl remonte au parent).
SELECT pg_temp.verif_compte('produits', 'Couples déconditionnables', $q$
    SELECT 1 FROM produit WHERE type_produit = 'PACKAGE' AND deconditionnable
$q$, 25);

SELECT pg_temp.verif_vide('produits', 'Tout enfant a un parent PACKAGE', $q$
    SELECT c.id FROM produit c
      JOIN produit p ON p.id = c.parent_id
     WHERE p.type_produit <> 'PACKAGE'
$q$);

-- DETAIL ne veut pas dire « vendu à l'unité » : c'est un DÉCONDITIONNÉ, l'unité
-- issue d'une boîte. Sans parent, il n'en est pas un — et l'application, qui
-- écarte les DETAIL du calcul de classe ABC et des suggestions de réappro,
-- laisserait le produit hors de toute gestion.
SELECT pg_temp.verif_vide('produits', 'Tout DETAIL est rattaché à une boîte', $q$
    SELECT id FROM produit WHERE type_produit = 'DETAIL' AND parent_id IS NULL
$q$);

-- Le nombre d'unités par boîte n'a de sens que si la boîte se déconditionne :
-- un produit délivré tel quel garde item_qty = 1.
SELECT pg_temp.verif_vide('produits', 'item_qty cohérent avec le type', $q$
    SELECT id FROM produit
     WHERE (deconditionnable AND item_qty <= 1)
        OR (type_produit = 'DETAIL' AND item_qty <> 1)
$q$);

SELECT pg_temp.verif_vide('produits', 'Pas de produit son propre parent', $q$
    SELECT id FROM produit WHERE parent_id = id
$q$);

-- Le chemin mixte doit être exercé : tous les produits ne sont pas suivis par lot.
SELECT pg_temp.verif_compte('produits', 'Produits hors gestion de lot', $q$
    SELECT 1 FROM produit WHERE NOT gestion_lot
$q$, 20);


-- ===========================================================================
-- SUBSTANCES ACTIVES (DCI)
-- ===========================================================================
SELECT pg_temp.verif_compte('dci', 'Substances actives créées', $q$
    SELECT 1 FROM dci
$q$, 55);

SELECT pg_temp.verif_vide('dci', 'Colonnes obligatoires renseignées', $q$
    SELECT id FROM dci
     WHERE code IS NULL OR btrim(code) = ''
        OR libelle IS NULL OR btrim(libelle) = ''
$q$);

SELECT pg_temp.verif_vide('dci', 'Code dans varchar(20)', $q$
    SELECT id FROM dci WHERE length(code) > 20
$q$);

SELECT pg_temp.verif_vide('dci', 'Code unique', $q$
    SELECT code FROM dci GROUP BY code HAVING count(*) > 1
$q$);

SELECT pg_temp.verif_vide('dci', 'Libellé unique', $q$
    SELECT libelle FROM dci GROUP BY libelle HAVING count(*) > 1
$q$);

-- Tout médicament doit porter sa molécule : sans elle, ni substitution
-- générique ni contrôle d'interaction ne sont possibles.
SELECT pg_temp.verif_vide('dci', 'Tout médicament a sa substance active', $q$
    SELECT p.id FROM produit p
      JOIN famille_produit f ON f.id = p.famille_id
     WHERE f.code IN ('1050', '1000', '1030', '1040')
       AND p.dci_id IS NULL
$q$);

-- À l'inverse, la parapharmacie et les accessoires n'en ont pas.
SELECT pg_temp.verif_vide('dci', 'La parapharmacie n''a pas de substance active', $q$
    SELECT p.id FROM produit p
      JOIN famille_produit f ON f.id = p.famille_id
     WHERE f.code IN ('3000', '8000', '5000', '6000')
       AND p.dci_id IS NOT NULL
$q$);

-- L'unité déconditionnée porte la même molécule que sa boîte : c'est la même
-- substance, seule la présentation change.
SELECT pg_temp.verif_vide('dci', 'L''unité hérite de la molécule de sa boîte', $q$
    SELECT c.id FROM produit c
      JOIN produit pk ON pk.id = c.parent_id
     WHERE c.dci_id IS DISTINCT FROM pk.dci_id
$q$);

-- L'intérêt du référentiel : plusieurs produits partagent une molécule et sont
-- donc substituables. Sans cela, l'écran de substitution ne propose jamais rien.
SELECT pg_temp.verif_compte('dci', 'Molécules portées par plusieurs produits', $q$
    SELECT dci_id FROM produit
     WHERE dci_id IS NOT NULL
     GROUP BY dci_id HAVING count(*) > 1
$q$, 20);

-- Le princeps, son générique et sa forme sirop doivent se retrouver ensemble.
SELECT pg_temp.verif_compte('dci', 'Princeps et générique partagent leur molécule', $q$
    SELECT p1.dci_id
      FROM produit p1
      JOIN produit p2 ON p2.dci_id = p1.dci_id
     WHERE p1.libelle LIKE 'PARACETAMOL %'
       AND p2.libelle LIKE 'DOLIPRANE %'
$q$, 1);


-- ===========================================================================
-- CATALOGUE DE SUBSTITUTION
-- ===========================================================================
SELECT pg_temp.verif_compte('substituts', 'Substitutions génériques', $q$
    SELECT 1 FROM substitut WHERE type_substitut = 'GENERIQUE'
$q$, 100);

SELECT pg_temp.verif_compte('substituts', 'Substitutions thérapeutiques', $q$
    SELECT 1 FROM substitut WHERE type_substitut = 'THERAPEUTIQUE'
$q$, 20);

SELECT pg_temp.verif_vide('substituts', 'Type conforme à la contrainte CHECK', $q$
    SELECT id FROM substitut
     WHERE type_substitut NOT IN ('GENERIQUE', 'THERAPEUTIQUE')
$q$);

SELECT pg_temp.verif_vide('substituts', 'Aucune substitution réflexive', $q$
    SELECT id FROM substitut WHERE produit_id = substitut_id
$q$);

-- La table est écrite dans un sens mais LUE DANS LES DEUX : stocker A→B et
-- B→A ferait apparaître le partenaire en double à l'écran.
SELECT pg_temp.verif_vide('substituts', 'Une seule ligne par paire', $q$
    SELECT s1.id FROM substitut s1
      JOIN substitut s2 ON s2.produit_id = s1.substitut_id
                       AND s2.substitut_id = s1.produit_id
$q$);

SELECT pg_temp.verif_vide('substituts', 'Unicité du couple', $q$
    SELECT produit_id FROM substitut
     GROUP BY produit_id, substitut_id HAVING count(*) > 1
$q$);

-- Un générique partage la molécule ; un thérapeutique, par définition, non.
SELECT pg_temp.verif_vide('substituts', 'Le type correspond à la molécule', $q$
    SELECT s.id FROM substitut s
      JOIN produit p ON p.id = s.produit_id
      JOIN produit q ON q.id = s.substitut_id
     WHERE (s.type_substitut = 'GENERIQUE'     AND p.dci_id IS DISTINCT FROM q.dci_id)
        OR (s.type_substitut = 'THERAPEUTIQUE' AND p.dci_id IS NOT DISTINCT FROM q.dci_id)
$q$);

-- Un générique remplace à dosage identique : PARACETAMOL 100MG ne substitue
-- pas PARACETAMOL 1G.
SELECT pg_temp.verif_vide('substituts', 'Générique à dosage identique', $q$
    SELECT s.id FROM substitut s
      JOIN produit p ON p.id = s.produit_id
      JOIN produit q ON q.id = s.substitut_id
     WHERE s.type_substitut = 'GENERIQUE'
       AND substring(p.libelle from '\m[0-9]+(?:MG|G|CH|ML)\M')
        IS DISTINCT FROM
           substring(q.libelle from '\m[0-9]+(?:MG|G|CH|ML)\M')
$q$);

-- Un thérapeutique se propose dans la même classe : le rayon en tient lieu.
SELECT pg_temp.verif_vide('substituts', 'Thérapeutique dans le même rayon', $q$
    SELECT s.id FROM substitut s
     WHERE s.type_substitut = 'THERAPEUTIQUE'
       AND NOT EXISTS (
           SELECT 1 FROM rayon_produit a
             JOIN rayon_produit b ON b.rayon_id = a.rayon_id
            WHERE a.produit_id = s.produit_id AND b.produit_id = s.substitut_id)
$q$);

-- Le catalogue doit couvrir plusieurs molécules, sinon la démonstration de la
-- substitution se limite à un seul cas.
SELECT pg_temp.verif_compte('substituts', 'Molécules couvertes', $q$
    SELECT DISTINCT p.dci_id FROM substitut s
      JOIN produit p ON p.id = s.produit_id
     WHERE s.type_substitut = 'GENERIQUE' AND p.dci_id IS NOT NULL
$q$, 10);


-- ===========================================================================
-- FOURNISSEUR_PRODUIT  (§3.3 — invariant central)
-- ===========================================================================
SELECT pg_temp.verif_compte('fournisseur_produit', 'Références présentes', $q$
    SELECT 1 FROM fournisseur_produit
$q$, 600);

-- INVARIANT DUR : V1.7.1 supprime tout fournisseur_produit porté par une
-- agence. En créer un ici produirait une base que la production nettoierait.
SELECT pg_temp.verif_vide('fournisseur_produit', 'Jamais rattaché à une agence', $q$
    SELECT fp.id FROM fournisseur_produit fp
      JOIN fournisseur f ON f.id = fp.fournisseur_id
     WHERE f.parent_id IS NOT NULL
$q$);

SELECT pg_temp.verif_vide('fournisseur_produit', 'Colonnes obligatoires renseignées', $q$
    SELECT id FROM fournisseur_produit
     WHERE code_cip IS NULL OR prix_achat IS NULL OR prix_uni IS NULL
$q$);

SELECT pg_temp.verif_vide('fournisseur_produit', 'Prix d''achat positif', $q$
    SELECT id FROM fournisseur_produit WHERE prix_achat <= 0
$q$);

-- Le CIP tient sur 7 chiffres (CIP7) et ne doit JAMAIS recevoir un EAN-13 :
-- c'est un code qu'on lit sur la boîte et qu'on saisit à la main.
SELECT pg_temp.verif_vide('fournisseur_produit', 'code_cip d''au plus 8 caractères', $q$
    SELECT id FROM fournisseur_produit WHERE length(code_cip) > 8
$q$);

-- L'EAN, lui, est bien le code-barres à 13 chiffres.
SELECT pg_temp.verif_vide('fournisseur_produit', 'code_ean sur 13 chiffres', $q$
    SELECT id FROM fournisseur_produit
     WHERE code_ean IS NOT NULL AND code_ean !~ '^[0-9]{13}$'
$q$);

SELECT pg_temp.verif_vide('fournisseur_produit', 'Unicité (code_cip, fournisseur)', $q$
    SELECT code_cip FROM fournisseur_produit
     GROUP BY code_cip, fournisseur_id HAVING count(*) > 1
$q$);

SELECT pg_temp.verif_vide('fournisseur_produit', 'Tout produit a un fournisseur', $q$
    SELECT p.id FROM produit p
     WHERE NOT EXISTS (SELECT 1 FROM fournisseur_produit fp WHERE fp.produit_id = p.id)
$q$);

SELECT pg_temp.verif_vide('fournisseur_produit', 'Fournisseur principal du produit renseigné', $q$
    SELECT p.id FROM produit p
     WHERE p.fournisseur_produit_principal_id IS NULL
$q$);

-- La colonne est UNIQUE : une même ligne ne peut être principale de deux produits.
SELECT pg_temp.verif_vide('fournisseur_produit', 'Le principal désigne bien le produit', $q$
    SELECT p.id FROM produit p
      JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
     WHERE fp.produit_id <> p.id
$q$);


-- ===========================================================================
-- RAYONS
-- ===========================================================================
SELECT pg_temp.verif_compte('rayons', 'Rayons commerciaux créés', $q$
    SELECT 1 FROM rayon r
      JOIN storage s ON s.id = r.storage_id
     WHERE s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1
       AND r.code <> 'SANS'
$q$, 12);

SELECT pg_temp.verif_vide('rayons', 'Tout produit est rangé', $q$
    SELECT p.id FROM produit p
     WHERE NOT EXISTS (SELECT 1 FROM rayon_produit rp WHERE rp.produit_id = p.id)
$q$);

-- Un produit dans deux rayons du même stockage serait ambigu à l'inventaire.
SELECT pg_temp.verif_vide('rayons', 'Un seul rayon par produit et par stockage', $q$
    SELECT rp.produit_id FROM rayon_produit rp
      JOIN rayon r ON r.id = rp.rayon_id
     GROUP BY rp.produit_id, r.storage_id HAVING count(*) > 1
$q$);


-- ===========================================================================
-- CLIENTS
-- ===========================================================================
SELECT pg_temp.verif_compte('clients', 'Clients comptant', $q$
    SELECT 1 FROM customer WHERE dtype = 'UninsuredCustomer'
$q$, 150);

SELECT pg_temp.verif_compte('clients', 'Assurés principaux', $q$
    SELECT 1 FROM customer WHERE dtype = 'AssuredCustomer' AND type_assure = 'PRINCIPAL'
$q$, 120);

SELECT pg_temp.verif_compte('clients', 'Ayants droit', $q$
    SELECT 1 FROM customer WHERE dtype = 'AssuredCustomer' AND type_assure = 'AYANT_DROIT'
$q$, 50);

SELECT pg_temp.verif_vide('clients', 'Discriminateur d''héritage valide', $q$
    SELECT id FROM customer
     WHERE dtype NOT IN ('Customer', 'AssuredCustomer', 'UninsuredCustomer')
$q$);

SELECT pg_temp.verif_vide('clients', 'Colonnes obligatoires renseignées', $q$
    SELECT id FROM customer
     WHERE code IS NULL OR btrim(code) = ''
        OR first_name IS NULL OR btrim(first_name) = ''
        OR last_name  IS NULL OR btrim(last_name)  = ''
        OR status IS NULL OR type_assure IS NULL
        OR created_at IS NULL OR updated_at IS NULL
$q$);

SELECT pg_temp.verif_vide('clients', 'Code client unique', $q$
    SELECT code FROM customer GROUP BY code HAVING count(*) > 1
$q$);

-- type_assure et assure_principal_id se répondent : un ayant droit sans
-- principal n'est rattachable à aucun contrat.
SELECT pg_temp.verif_vide('clients', 'Ayant droit rattaché à un principal', $q$
    SELECT id FROM customer
     WHERE (type_assure = 'AYANT_DROIT' AND assure_principal_id IS NULL)
        OR (type_assure = 'PRINCIPAL'   AND assure_principal_id IS NOT NULL)
$q$);

SELECT pg_temp.verif_vide('clients', 'Le principal d''un ayant droit est un assuré', $q$
    SELECT a.id FROM customer a
      JOIN customer p ON p.id = a.assure_principal_id
     WHERE p.dtype <> 'AssuredCustomer' OR p.type_assure <> 'PRINCIPAL'
$q$);

SELECT pg_temp.verif_vide('clients', 'Pas de client son propre principal', $q$
    SELECT id FROM customer WHERE assure_principal_id = id
$q$);

-- Un client comptant n'a pas d'attributs d'assuré : ce sont des colonnes de la
-- sous-classe AssuredCustomer, laissées nulles par l'héritage SINGLE_TABLE.
SELECT pg_temp.verif_vide('clients', 'Colonnes d''assuré nulles hors assurés', $q$
    SELECT id FROM customer
     WHERE dtype = 'UninsuredCustomer'
       AND (assure_principal_id IS NOT NULL OR num_ayant_droit IS NOT NULL)
$q$);

SELECT pg_temp.verif_vide('clients', 'Comptes clients rattachés et valides', $q$
    SELECT a.id FROM customer_account a
     WHERE a.customer_id IS NULL
        OR a.balance IS NULL
        OR a.account_type NOT IN ('CAUTION', 'CARNET')
$q$);


-- ===========================================================================
-- TIERS-PAYANTS ET CONTRATS
-- ===========================================================================
SELECT pg_temp.verif_compte('tiers-payants', 'Organismes créés', $q$
    SELECT 1 FROM tiers_payant
$q$, 12);

SELECT pg_temp.verif_compte('tiers-payants', 'Groupes d''organismes', $q$
    SELECT 1 FROM groupe_tiers_payant
$q$, 3);

SELECT pg_temp.verif_vide('tiers-payants', 'Enums conformes au modèle', $q$
    SELECT id FROM tiers_payant
     WHERE categorie NOT IN ('ASSURANCE', 'CARNET', 'DEPOT')
        OR statut    NOT IN ('ACTIF', 'DISABLED', 'LOCK')
$q$);

SELECT pg_temp.verif_vide('tiers-payants', 'Colonnes obligatoires renseignées', $q$
    SELECT id FROM tiers_payant
     WHERE name IS NULL OR full_name IS NULL OR user_id IS NULL
        OR created IS NULL OR updated IS NULL
$q$);

-- Au moins un organisme plafonné, sinon les écrans de suivi de plafond et le
-- chemin applyCeilings ne sont jamais exercés.
SELECT pg_temp.verif_compte('tiers-payants', 'Organismes avec plafond', $q$
    SELECT 1 FROM tiers_payant WHERE plafond_conso IS NOT NULL
$q$, 2);

-- Le seuil tient compte des porteurs de carnet : leur contrat couvre 100 %, il
-- ne se complète donc d'aucun second payeur (cf. 04_clients.sql).
SELECT pg_temp.verif_compte('tiers-payants', 'Contrats créés', $q$
    SELECT 1 FROM client_tiers_payant
$q$, 155);

-- Un contrat n'a de sens que sur un AssuredCustomer.
SELECT pg_temp.verif_vide('tiers-payants', 'Contrat porté par un assuré', $q$
    SELECT ctp.id FROM client_tiers_payant ctp
      JOIN customer c ON c.id = ctp.assured_customer_id
     WHERE c.dtype <> 'AssuredCustomer'
$q$);

-- L'entité déclare cette unicité, mais Flyway ne l'a jamais matérialisée :
-- aucun index ne la protège, d'où ce contrôle explicite.
SELECT pg_temp.verif_vide('tiers-payants', 'Unicité (organisme, assuré) — non protégée en base', $q$
    SELECT assured_customer_id FROM client_tiers_payant
     GROUP BY tierspayant_id, assured_customer_id HAVING count(*) > 1
$q$);

SELECT pg_temp.verif_vide('tiers-payants', 'Unicité (organisme, numéro) — non protégée en base', $q$
    SELECT num FROM client_tiers_payant
     GROUP BY tierspayant_id, num HAVING count(*) > 1
$q$);

-- Le cumul des taux gouverne la répartition : au-delà de 100 %, la part
-- patient deviendrait négative et le clamp à zéro masquerait l'incohérence.
SELECT pg_temp.verif_vide('tiers-payants', 'Cumul des taux par assuré <= 100 %', $q$
    SELECT assured_customer_id FROM client_tiers_payant
     GROUP BY assured_customer_id HAVING sum(taux) > 100
$q$);

SELECT pg_temp.verif_vide('tiers-payants', 'Taux dans les bornes', $q$
    SELECT id FROM client_tiers_payant WHERE taux <= 0 OR taux > 100
$q$);

-- La priorité ordonne les payeurs d'une même vente : deux payeurs de même
-- priorité rendraient la répartition non déterministe.
SELECT pg_temp.verif_vide('tiers-payants', 'Priorité unique par assuré', $q$
    SELECT assured_customer_id FROM client_tiers_payant
     GROUP BY assured_customer_id, priorite HAVING count(*) > 1
$q$);

SELECT pg_temp.verif_vide('tiers-payants', 'Priorité et statut conformes', $q$
    SELECT id FROM client_tiers_payant
     WHERE priorite NOT IN ('R0', 'R1', 'R2', 'R3')
        OR statut   NOT IN ('ACTIF', 'DISABLED', 'LOCK')
$q$);

-- @PrePersist alimente cet historique côté application ; l'insertion SQL
-- court-circuite le hook, l'historique doit donc être écrit par le script.
SELECT pg_temp.verif_vide('tiers-payants', 'Historique de taux initialisé', $q$
    SELECT id FROM client_tiers_payant
     WHERE taux_historique IS NULL
        OR json_array_length(taux_historique) = 0
$q$);

SELECT pg_temp.verif_vide('tiers-payants', 'Historique cohérent avec le taux courant', $q$
    SELECT ctp.id FROM client_tiers_payant ctp
     WHERE NOT EXISTS (
         SELECT 1 FROM json_array_elements(ctp.taux_historique) e
          WHERE (e->>'taux')::int = ctp.taux
     )
$q$);

-- Tout assuré principal doit être couvert, sinon il ne pourra porter aucune
-- vente tiers-payant à l'étape 5.
SELECT pg_temp.verif_vide('tiers-payants', 'Tout assuré principal a un contrat', $q$
    SELECT c.id FROM customer c
     WHERE c.dtype = 'AssuredCustomer' AND c.type_assure = 'PRINCIPAL'
       AND NOT EXISTS (SELECT 1 FROM client_tiers_payant ctp
                        WHERE ctp.assured_customer_id = c.id)
$q$);


-- ===========================================================================
-- COMMANDES  (§4.7)
-- ===========================================================================
SELECT pg_temp.verif_compte('commandes', 'Commandes créées', $q$
    SELECT 1 FROM commande
$q$, 120);

SELECT pg_temp.verif_compte('commandes', 'Lignes de commande', $q$
    SELECT 1 FROM order_line
$q$, 1000);

SELECT pg_temp.verif_vide('commandes', 'Colonnes obligatoires renseignées', $q$
    SELECT id FROM commande
     WHERE gross_amount IS NULL OR order_status IS NULL OR paiment_status IS NULL
        OR receipt_type IS NULL OR user_id IS NULL
        OR created_at IS NULL OR updated_at IS NULL
$q$);

SELECT pg_temp.verif_vide('commandes', 'Enums conformes au modèle', $q$
    SELECT id FROM commande
     WHERE order_status   NOT IN ('REQUESTED', 'RECEIVED', 'CLOSED', 'ARCHIVED')
        OR paiment_status NOT IN ('UNPAID', 'PAID', 'NOT_SOLD')
        OR receipt_type   NOT IN ('DIRECT', 'ORDER')
$q$);

-- La clé étrangère porte sur les DEUX colonnes : une ligne dont la date ne
-- correspond pas à celle de sa commande serait orpheline.
SELECT pg_temp.verif_vide('commandes', 'Clé composite ligne / commande cohérente', $q$
    SELECT ol.id FROM order_line ol
     WHERE NOT EXISTS (
         SELECT 1 FROM commande c
          WHERE c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     )
$q$);

SELECT pg_temp.verif_vide('commandes', 'order_date de la ligne = celle de la commande', $q$
    SELECT ol.id FROM order_line ol
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     WHERE ol.order_date <> c.order_date
$q$);

SELECT pg_temp.verif_vide('commandes', 'Unicité (commande, fournisseur_produit)', $q$
    SELECT commande_id FROM order_line
     GROUP BY commande_id, fournisseur_produit_id, order_date HAVING count(*) > 1
$q$);

-- INVARIANT AGENCE (§3.3) : la commande va chez l'agence, mais la ligne pointe
-- un fournisseur_produit du PRINCIPAL. La jointure traverse le lien parent.
SELECT pg_temp.verif_vide('commandes', 'Ligne rattachée au principal de l''agence commandée', $q$
    SELECT ol.id FROM order_line ol
      JOIN commande c     ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
      JOIN fournisseur ag ON ag.id = c.fournisseur_id
      JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
     WHERE fp.fournisseur_id <> COALESCE(ag.parent_id, ag.id)
$q$);

-- Le total de l'en-tête suit la quantité commandée avant réception, la
-- quantité reçue après (CommandServiceImpl.computeCommandeAmount).
SELECT pg_temp.verif_vide('commandes', 'Total achat = somme des lignes', $q$
    SELECT c.id FROM commande c
      JOIN order_line ol ON ol.commande_id = c.id AND ol.commande_order_date = c.order_date
     GROUP BY c.id, c.order_date, c.gross_amount, c.order_status
    HAVING c.gross_amount <> sum(
        CASE WHEN c.order_status = 'REQUESTED' THEN ol.quantity_requested
             ELSE COALESCE(ol.quantity_received, 0) END * ol.order_cost_amount)
$q$);

SELECT pg_temp.verif_vide('commandes', 'Total vente = somme des lignes', $q$
    SELECT c.id FROM commande c
      JOIN order_line ol ON ol.commande_id = c.id AND ol.commande_order_date = c.order_date
     GROUP BY c.id, c.order_date, c.order_amount, c.order_status
    HAVING c.order_amount <> sum(
        CASE WHEN c.order_status = 'REQUESTED' THEN ol.quantity_requested
             ELSE COALESCE(ol.quantity_received, 0) END * ol.order_unit_price)
$q$);

-- Malgré son nom, ht_amount porte le montant du bon de livraison : il vaut
-- gross_amount une fois réceptionné, zéro avant (buildDeliveryReceipt:634).
SELECT pg_temp.verif_vide('commandes', 'ht_amount = montant du bon après réception', $q$
    SELECT id FROM commande
     WHERE (order_status = 'REQUESTED' AND ht_amount <> 0)
        OR (order_status <> 'REQUESTED' AND ht_amount <> gross_amount)
$q$);

-- Cohérence statut / champs de réception.
SELECT pg_temp.verif_vide('commandes', 'REQUESTED : rien de réceptionné', $q$
    SELECT c.id FROM commande c
      JOIN order_line ol ON ol.commande_id = c.id AND ol.commande_order_date = c.order_date
     WHERE c.order_status = 'REQUESTED'
       AND (c.receipt_date IS NOT NULL
         OR c.receipt_reference IS NOT NULL
         OR ol.quantity_received IS NOT NULL)
$q$);

SELECT pg_temp.verif_vide('commandes', 'Réceptionnée : bon et quantités renseignés', $q$
    SELECT c.id FROM commande c
      JOIN order_line ol ON ol.commande_id = c.id AND ol.commande_order_date = c.order_date
     WHERE c.order_status IN ('RECEIVED', 'CLOSED')
       AND (c.receipt_date IS NULL
         OR c.receipt_reference IS NULL
         OR ol.quantity_received IS NULL)
$q$);

-- Un bon partiellement servi doit l'être LIGNE À LIGNE : si toutes ses lignes sont
-- courtes, son taux de service tombe à 0 % et l'écran ne le distingue plus d'un bon jamais
-- compté — le parcours du reliquat n'aurait alors aucun bon à ouvrir.
SELECT pg_temp.verif_compte('commandes', 'Un bon livré partiellement, mais pas sur toutes ses lignes', $q$
    SELECT c.id
      FROM commande c
      JOIN order_line ol ON ol.commande_id = c.id AND ol.commande_order_date = c.order_date
     WHERE c.order_status = 'RECEIVED' AND ol.is_updated
     GROUP BY c.id
    HAVING count(*) FILTER (WHERE ol.quantity_received < ol.quantity_requested) > 0
       AND count(*) FILTER (WHERE ol.quantity_received >= ol.quantity_requested) > 0
$q$, 1);

-- Les deux états de saisie doivent EXISTER : sans bon « à saisir », la saisie ligne à
-- ligne et « Tout valider » n'ont rien à exercer ; sans bon déjà saisi, aucune réception
-- n'est finalisable.
SELECT pg_temp.verif_compte('commandes', 'Des bons livrés restent à saisir', $q$
    SELECT 1 FROM order_line ol
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     WHERE c.order_status = 'RECEIVED' AND NOT ol.is_updated
$q$, 1);

-- Un bon compté ET servi en entier : c'est la réception qui se finalise directement, sans
-- reliquat ni « Tout valider » préalable. Sans lui, aucun parcours de finalisation simple.
SELECT pg_temp.verif_compte('commandes', 'Un bon livré, compté et servi en entier', $q$
    SELECT c.id
      FROM commande c
      JOIN order_line ol ON ol.commande_id = c.id AND ol.commande_order_date = c.order_date
     WHERE c.order_status = 'RECEIVED' AND ol.is_updated
     GROUP BY c.id
    HAVING count(*) FILTER (WHERE ol.quantity_received < ol.quantity_requested) = 0
$q$, 1);

SELECT pg_temp.verif_compte('commandes', 'Des bons livrés sont déjà saisis', $q$
    SELECT 1 FROM order_line ol
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     WHERE c.order_status = 'RECEIVED' AND ol.is_updated
$q$, 1);

-- Le rangement rayon → réserve proposé à la finalisation ne concerne que les produits dont
-- le stock rayon dépasse le maximum du rayon : sans un seul, l'écran ne s'ouvre jamais.
SELECT pg_temp.verif_compte('stock', 'Des rayons dépassent leur maximum', $q$
    SELECT sp.produit_id
      FROM stock_produit sp
      JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'PRINCIPAL'
     WHERE sp.stock_maxi > 0 AND sp.qty_stock > sp.stock_maxi
$q$, 1);

-- L'historique de prix est ce qu'on consulte avant de contester une facture : sans lui,
-- l'écran répond « Aucun changement enregistré » et le contrôle de concordance n'a rien à
-- quoi se comparer.
SELECT pg_temp.verif_compte('commandes', 'Historique de prix fournisseur-produit', $q$
    SELECT id FROM fournisseur_produit_price_history
$q$, 20);

SELECT pg_temp.verif_vide('commandes', 'Historique de prix cohérent avec la fiche', $q$
    SELECT h.id
      FROM fournisseur_produit_price_history h
      JOIN fournisseur_produit fp ON fp.id = h.fournisseur_produit_id
     WHERE h.new_prix_achat <> fp.prix_achat
        OR h.new_prix_uni <> fp.prix_uni
        OR h.old_prix_achat >= h.new_prix_achat
$q$);

-- Stock avant / après : l'écran du bon les affiche ligne à ligne. Un « stock
-- après » vide sur une commande réceptionnée trahit une ligne jamais passée par
-- StockEntryServiceImpl.saveItem.
SELECT pg_temp.verif_vide('commandes', 'Réceptionnée : stock avant et après renseignés', $q$
    SELECT ol.id FROM order_line ol
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     WHERE c.order_status IN ('RECEIVED', 'CLOSED')
       AND (ol.init_stock IS NULL OR ol.final_stock IS NULL)
$q$);

SELECT pg_temp.verif_vide('commandes', 'Stock après = stock avant + reçu + gratuit', $q$
    SELECT ol.id FROM order_line ol
     WHERE ol.final_stock IS NOT NULL
       AND ol.final_stock <> ol.init_stock
                             + COALESCE(ol.quantity_received, 0)
                             + COALESCE(ol.free_qty, 0)
$q$);

-- Avant réception, il n'y a pas encore de stock après.
SELECT pg_temp.verif_vide('commandes', 'REQUESTED : pas de stock après', $q$
    SELECT ol.id FROM order_line ol
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     WHERE c.order_status = 'REQUESTED' AND ol.final_stock IS NOT NULL
$q$);

SELECT pg_temp.verif_vide('commandes', 'Réception jamais supérieure à la commande', $q$
    SELECT id FROM order_line
     WHERE quantity_received IS NOT NULL AND quantity_received > quantity_requested
$q$);

SELECT pg_temp.verif_vide('commandes', 'Quantités et prix positifs', $q$
    SELECT id FROM order_line
     WHERE quantity_requested <= 0
        OR order_cost_amount <= 0
        OR order_unit_price <= 0
        OR COALESCE(quantity_received, 0) < 0
        OR free_qty < 0
$q$);

-- Une officine ne revend pas sous son prix d'achat.
SELECT pg_temp.verif_vide('commandes', 'Prix de vente supérieur au prix d''achat', $q$
    SELECT id FROM order_line WHERE order_unit_price <= order_cost_amount
$q$);

SELECT pg_temp.verif_vide('commandes', 'Chronologie : réception après commande', $q$
    SELECT id FROM commande
     WHERE receipt_date IS NOT NULL AND receipt_date < order_date
$q$);

-- Le tableau de bord d'accueil s'ouvre sur la journée : sans achat CLÔTURÉ daté
-- d'aujourd'hui, son indicateur « Achats fournisseurs » reste vide et l'écran
-- passe pour cassé.
SELECT pg_temp.verif_compte('commandes', 'Achat clôturé daté du jour', $q$
    SELECT 1 FROM commande WHERE order_status = 'CLOSED' AND order_date = CURRENT_DATE
$q$, 1);

SELECT pg_temp.verif_vide('commandes', 'Aucune date dans le futur', $q$
    SELECT id FROM commande
     WHERE order_date > CURRENT_DATE OR receipt_date > CURRENT_DATE
$q$);

-- Les péremptions ne sont connues qu'à la réception, et doivent être à venir.
SELECT pg_temp.verif_vide('commandes', 'Péremption renseignée seulement si réceptionnée', $q$
    SELECT ol.id FROM order_line ol
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     WHERE (c.order_status = 'REQUESTED' AND ol.date_peremption IS NOT NULL)
        OR (c.order_status <> 'REQUESTED' AND ol.date_peremption IS NULL)
$q$);

-- Les trois états doivent être représentés, sinon les écrans de suivi et le
-- module de réception n'ont rien à montrer.
SELECT pg_temp.verif_compte('commandes', 'Commandes soldées', $q$
    SELECT 1 FROM commande WHERE order_status = 'CLOSED'
$q$, 50);

SELECT pg_temp.verif_compte('commandes', 'Commandes réceptionnées', $q$
    SELECT 1 FROM commande WHERE order_status = 'RECEIVED'
$q$, 20);

SELECT pg_temp.verif_compte('commandes', 'Commandes en attente', $q$
    SELECT 1 FROM commande WHERE order_status = 'REQUESTED'
$q$, 15);

-- Les reliquats sont un cas réel du métier : au moins quelques commandes
-- doivent être partiellement servies.
SELECT pg_temp.verif_compte('commandes', 'Réceptions partielles (reliquats)', $q$
    SELECT 1 FROM order_line
     WHERE quantity_received IS NOT NULL AND quantity_received < quantity_requested
$q$, 10);


-- ===========================================================================
-- LOTS  (§4.9)
-- ===========================================================================
SELECT pg_temp.verif_compte('lots', 'Lots créés', $q$
    SELECT 1 FROM lot
$q$, 800);

SELECT pg_temp.verif_vide('lots', 'Colonnes obligatoires renseignées', $q$
    SELECT id FROM lot
     WHERE num_lot IS NULL OR produit_id IS NULL
        OR quantity IS NULL OR current_quantity IS NULL
        OR prixachat IS NULL OR prixunit IS NULL
        OR statut IS NULL OR created_date IS NULL
$q$);

SELECT pg_temp.verif_vide('lots', 'Statut conforme à la contrainte CHECK', $q$
    SELECT id FROM lot
     WHERE statut NOT IN ('IN_PROGRESS', 'AVAILABLE', 'SOLD', 'EXPIRED', 'DESTROYED')
$q$);

SELECT pg_temp.verif_vide('lots', 'Restant jamais supérieur au reçu', $q$
    SELECT id FROM lot WHERE current_quantity > quantity OR quantity <= 0
$q$);

-- Le statut est asservi à la quantité restante (LotServiceImpl.updateLots).
SELECT pg_temp.verif_vide('lots', 'Lot épuisé marqué SOLD', $q$
    SELECT id FROM lot WHERE current_quantity = 0 AND statut <> 'SOLD'
$q$);

SELECT pg_temp.verif_vide('lots', 'Lot AVAILABLE non épuisé', $q$
    SELECT id FROM lot WHERE statut = 'AVAILABLE' AND current_quantity <= 0
$q$);

-- Le statut dérive de la péremption : ils ne peuvent pas se contredire.
SELECT pg_temp.verif_vide('lots', 'Statut cohérent avec la péremption', $q$
    SELECT id FROM lot
     WHERE (statut = 'AVAILABLE' AND expiry_date < CURRENT_DATE)
        OR (statut IN ('EXPIRED', 'DESTROYED') AND expiry_date >= CURRENT_DATE)
$q$);

-- On ne réceptionne pas une marchandise déjà périmée.
SELECT pg_temp.verif_vide('lots', 'Péremption postérieure à la réception', $q$
    SELECT l.id FROM lot l
      JOIN order_line ol ON ol.id = l.order_line_id AND ol.order_date = l.commande_order_date
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     WHERE l.expiry_date <= c.receipt_date
$q$);

SELECT pg_temp.verif_vide('lots', 'Fabrication antérieure à la péremption', $q$
    SELECT id FROM lot
     WHERE manufacturing_date IS NOT NULL AND expiry_date IS NOT NULL
       AND manufacturing_date >= expiry_date
$q$);

SELECT pg_temp.verif_vide('lots', 'Numéro de lot unique par produit', $q$
    SELECT num_lot FROM lot GROUP BY num_lot, produit_id HAVING count(*) > 1
$q$);

-- Les lots doivent alimenter les écrans de péremption, sinon le module est vide.
SELECT pg_temp.verif_compte('lots', 'Lots périmés', $q$
    SELECT 1 FROM lot WHERE statut = 'EXPIRED'
$q$, 30);

SELECT pg_temp.verif_compte('lots', 'Lots à détruire', $q$
    SELECT 1 FROM lot WHERE statut = 'DESTROYED'
$q$, 10);

SELECT pg_temp.verif_compte('lots', 'Lots en alerte de péremption (< 90 j)', $q$
    SELECT 1 FROM lot
     WHERE statut = 'AVAILABLE' AND expiry_date < CURRENT_DATE + 90
$q$, 30);

-- Le stock hors lot doit exister : c'est le chemin mixte, réel en officine.
SELECT pg_temp.verif_vide('lots', 'Aucun lot sur un produit hors gestion de lot', $q$
    SELECT l.id FROM lot l
      JOIN produit p ON p.id = l.produit_id
     WHERE NOT p.gestion_lot
$q$);


-- ===========================================================================
-- TRAÇABILITÉ FMD  (§4.10)
-- ===========================================================================
SELECT pg_temp.verif_compte('fmd', 'Lots sérialisés', $q$
    SELECT 1 FROM lot WHERE serial_number IS NOT NULL
$q$, 100);

-- La sérialisation ne couvre que les produits sur ordonnance.
SELECT pg_temp.verif_vide('fmd', 'Sérial réservé aux produits sur ordonnance', $q$
    SELECT l.id FROM lot l
      JOIN produit p ON p.id = l.produit_id
     WHERE l.serial_number IS NOT NULL
       AND p.statut_legal NOT IN ('LISTE_I', 'LISTE_II', 'STUPEFIANTS', 'PSO')
$q$);

-- Un sérial identifie UNE BOÎTE : un lot sérialisé est de petite quantité.
SELECT pg_temp.verif_vide('fmd', 'Lot sérialisé de faible quantité', $q$
    SELECT id FROM lot WHERE serial_number IS NOT NULL AND quantity > 3
$q$);

SELECT pg_temp.verif_vide('fmd', 'Unicité du sérial par produit', $q$
    SELECT serial_number FROM lot
     WHERE serial_number IS NOT NULL
     GROUP BY serial_number, produit_id HAVING count(*) > 1
$q$);

SELECT pg_temp.verif_vide('fmd', 'Sérial dans varchar(50)', $q$
    SELECT id FROM lot WHERE length(serial_number) > 50
$q$);


-- ===========================================================================
-- RÉCEPTIONS ET EMPLACEMENTS  (§3.7)
-- ===========================================================================
SELECT pg_temp.verif_compte('emplacements', 'Réceptions tracées', $q$
    SELECT 1 FROM lot_reception
$q$, 500);

-- lot_reception.order_line_id est NOT NULL : seuls les lots issus d'une
-- commande en ont une. Les lots historiques n'en ont pas, par construction.
SELECT pg_temp.verif_vide('emplacements', 'Tout lot d''une commande a sa réception', $q$
    SELECT l.id FROM lot l
     WHERE l.order_line_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM lot_reception lr WHERE lr.lot_id = l.id)
$q$);

SELECT pg_temp.verif_vide('emplacements', 'Aucune réception sur une commande en attente', $q$
    SELECT lr.id FROM lot_reception lr
      JOIN commande c ON c.id = (SELECT ol.commande_id FROM order_line ol
                                  WHERE ol.id = lr.order_line_id
                                    AND ol.order_date = lr.commande_order_date)
                     AND c.order_date = lr.commande_order_date
     WHERE c.order_status = 'REQUESTED'
$q$);

-- Comptabilité à deux niveaux : le lot égale la somme de ses emplacements.
SELECT pg_temp.verif_vide('emplacements', 'Restant du lot = somme de ses emplacements', $q$
    SELECT l.id FROM lot l
      LEFT JOIN lot_stock_location lsl ON lsl.lot_id = l.id
     GROUP BY l.id, l.current_quantity
    HAVING l.current_quantity <> COALESCE(sum(lsl.qty), 0)
$q$);

-- Les lignes épuisées sont supprimées, jamais laissées à zéro.
SELECT pg_temp.verif_vide('emplacements', 'Aucun emplacement à zéro', $q$
    SELECT id FROM lot_stock_location WHERE qty <= 0
$q$);

SELECT pg_temp.verif_vide('emplacements', 'Un lot épuisé n''a plus d''emplacement', $q$
    SELECT l.id FROM lot l
      JOIN lot_stock_location lsl ON lsl.lot_id = l.id
     WHERE l.statut = 'SOLD'
$q$);

-- Un lot en cours de réception n'a pas encore d'emplacement : le crédit se
-- fait à la finalisation, en même temps que le passage à AVAILABLE.
SELECT pg_temp.verif_vide('emplacements', 'Un lot IN_PROGRESS n''a pas d''emplacement', $q$
    SELECT l.id FROM lot l
      JOIN lot_stock_location lsl ON lsl.lot_id = l.id
     WHERE l.statut = 'IN_PROGRESS'
$q$);

-- Les lots entrent par le stockage principal ; la réserve se remplit par
-- transfert. Un lot présent en réserve doit donc être passé par le rayon.
--
-- La ligne principale peut toutefois avoir DISPARU depuis : la vente sert au
-- rayon, et l'emplacement tombé à zéro est supprimé (jamais laissé à zéro).
-- Un lot entamé — current_quantity < quantity — a donc pu être vidé du rayon
-- tout en gardant sa part de réserve. C'est l'état normal d'un lot en fin de
-- vie, pas une réserve alimentée directement.
-- Cas nommé : le manuel montre les deux stocks d'un produit précis (REF-48), il
-- lui faut donc une réserve garantie et non tirée d'un « un produit sur cinq »
-- qui dépend de l'ordre d'insertion.
SELECT pg_temp.verif_compte('emplacements', 'Cas nommé ARNICA MONTANA 9CH en réserve', $q$
    SELECT 1
      FROM stock_produit sp
      JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'SAFETY_STOCK'
      JOIN produit p  ON p.id = sp.produit_id
     WHERE p.libelle LIKE 'ARNICA MONTANA 9CH%' AND sp.qty_stock > 0
$q$, 1);

SELECT pg_temp.verif_vide('emplacements', 'La réserve n''est alimentée que par transfert', $q$
    SELECT lsl.id FROM lot_stock_location lsl
      JOIN lot l ON l.id = lsl.lot_id
      JOIN storage s ON s.id = lsl.storage_id
     WHERE s.storage_type = 'SAFETY_STOCK'
       AND l.current_quantity = l.quantity
       AND NOT EXISTS (
           SELECT 1 FROM lot_stock_location p
             JOIN storage ps ON ps.id = p.storage_id
            WHERE p.lot_id = lsl.lot_id AND ps.storage_type = 'PRINCIPAL'
       )
$q$);


-- ===========================================================================
-- STOCK  (§4.8)
-- ===========================================================================
SELECT pg_temp.verif_compte('stock', 'Lignes de stock', $q$
    SELECT 1 FROM stock_produit
$q$, 600);

SELECT pg_temp.verif_vide('stock', 'Colonnes obligatoires renseignées', $q$
    SELECT id FROM stock_produit
     WHERE qty_stock IS NULL OR qty_virtual IS NULL OR qty_ug IS NULL
        OR version IS NULL OR created_at IS NULL OR updated_at IS NULL
$q$);

SELECT pg_temp.verif_vide('stock', 'Aucune quantité négative', $q$
    SELECT id FROM stock_produit
     WHERE qty_stock < 0 OR qty_virtual < 0 OR qty_ug < 0
$q$);

-- Le stock est PHYSIQUE : il compte tous les emplacements, y compris ceux des
-- lots périmés. Rien ne décrémente stock_produit quand un lot périme — la
-- marchandise reste en rayon jusqu'à destruction ou ajustement.
--
-- Une exception, et une seule : le RETOUR DE DÉPÔT. Le stock du dépôt est
-- tenu sans lot ; à son retour la marchandise réintègre l'officine par un
-- simple crédit de stock_produit — RetourDepotServiceImpl ne recrée aucun
-- emplacement, faute de lot à créditer. Ces quantités-là sont donc dans le
-- stock sans être dans les emplacements, et il faut les ajouter au total
-- attendu pour ne pas transformer un comportement de l'application en anomalie
-- du jeu de données.
SELECT pg_temp.verif_vide('stock', 'Stock = tous les emplacements du même stockage', $q$
    SELECT sp.id FROM stock_produit sp
      JOIN produit p ON p.id = sp.produit_id AND p.gestion_lot
      -- Stockages de l'officine seulement : le magasin DÉPÔT est tenu sans lot.
      JOIN storage s ON s.id = sp.storage_id AND s.magasin_id = 1
     WHERE sp.qty_stock <> COALESCE((
         SELECT sum(lsl.qty) FROM lot_stock_location lsl
           JOIN lot l ON l.id = lsl.lot_id
          WHERE l.produit_id = sp.produit_id
            AND lsl.storage_id = sp.storage_id), 0)
         + CASE WHEN s.storage_type = 'PRINCIPAL' THEN COALESCE((
             SELECT sum(i.qty_mvt) FROM retour_depot_item i
              WHERE i.produit_id = sp.produit_id), 0) ELSE 0 END
$q$);

-- Corollaire : une part du stock peut être invendable. Le signaler n'est pas
-- une erreur, mais l'absence totale de stock périmé le serait — le module de
-- péremption n'aurait alors rien à montrer.
SELECT pg_temp.verif_compte('stock', 'Stock immobilisé par des périmés', $q$
    SELECT 1 FROM lot_stock_location lsl
      JOIN lot l ON l.id = lsl.lot_id
     WHERE l.statut IN ('EXPIRED', 'DESTROYED')
$q$, 30);

SELECT pg_temp.verif_vide('stock', 'Tout produit a une ligne de stock', $q$
    SELECT p.id FROM produit p
     WHERE NOT EXISTS (SELECT 1 FROM stock_produit sp WHERE sp.produit_id = p.id)
$q$);

SELECT pg_temp.verif_vide('stock', 'Unicité (stockage, produit)', $q$
    SELECT produit_id FROM stock_produit
     GROUP BY storage_id, produit_id HAVING count(*) > 1
$q$);

-- La réserve doit être alimentée, sinon les suggestions de réassort rayon
-- n'ont aucune source et l'écran reste vide.
SELECT pg_temp.verif_compte('stock', 'Produits présents en réserve', $q$
    SELECT 1 FROM stock_produit sp
      JOIN storage s ON s.id = sp.storage_id
     WHERE s.storage_type = 'SAFETY_STOCK' AND sp.qty_stock > 0
$q$, 20);


-- ===========================================================================
-- CAISSES  (§3.8)
-- ===========================================================================
SELECT pg_temp.verif_compte('caisses', 'Caisses ouvertes sur la période', $q$
    SELECT 1 FROM cash_register
$q$, 140);

-- CashRegister.cashFund est @NotNull : une caisse sans fonds ne se charge pas.
SELECT pg_temp.verif_vide('caisses', 'Toute caisse a son fonds', $q$
    SELECT cr.id FROM cash_register cr
     WHERE NOT EXISTS (SELECT 1 FROM cash_fund f WHERE f.cash_register_id = cr.id)
$q$);

SELECT pg_temp.verif_vide('caisses', 'Une seule caisse ouverte', $q$
    SELECT 1 FROM cash_register WHERE statut = 'OPEN' GROUP BY 1 HAVING count(*) <> 1
$q$);

SELECT pg_temp.verif_vide('caisses', 'Chronologie ouverture / fermeture', $q$
    SELECT id FROM cash_register WHERE end_time IS NOT NULL AND end_time < begin_time
$q$);

-- Le montant final est le fonds initial augmenté des espèces encaissées.
SELECT pg_temp.verif_vide('caisses', 'Montant final = fonds + espèces encaissées', $q$
    SELECT cr.id FROM cash_register cr
     WHERE cr.final_amount <> cr.init_amount + COALESCE((
         SELECT sum(pt.paid_amount) FROM payment_transaction pt
          WHERE pt.cash_register_id = cr.id AND pt.payment_mode_code = 'CASH'), 0)
$q$);


-- ===========================================================================
-- VENTES  (§4.1 à §4.4)
-- ===========================================================================
SELECT pg_temp.verif_compte('ventes', 'Ventes créées', $q$
    SELECT 1 FROM sales
$q$, 2500);

SELECT pg_temp.verif_compte('ventes', 'Lignes de vente', $q$
    SELECT 1 FROM sales_line
$q$, 5000);

-- Les écrans quotidiens doivent être démontrables quelle que soit l'heure du chargement.
SELECT pg_temp.verif_compte('ventes', 'Ventes du jour', $q$
    SELECT 1 FROM sales WHERE sale_date = CURRENT_DATE AND dtype <> 'VenteDepot'
$q$, 12);

SELECT pg_temp.verif_compte('ventes', 'Ventes sur ordonnance du jour', $q$
    SELECT 1 FROM sales WHERE sale_date = CURRENT_DATE AND type_prescription = 'PRESCRIPTION'
$q$, 1);

SELECT pg_temp.verif_compte('ventes', 'Ventes tiers payant du jour', $q$
    SELECT 1 FROM sales WHERE sale_date = CURRENT_DATE AND dtype = 'ThirdPartySales'
$q$, 1);

SELECT pg_temp.verif_compte('ventes', 'Ventes dépôt du jour', $q$
    SELECT 1 FROM sales WHERE sale_date = CURRENT_DATE AND dtype = 'VenteDepot'
$q$, 1);

SELECT pg_temp.verif_vide('ventes', 'Discriminateur d''héritage renseigné', $q$
    SELECT id FROM sales
     WHERE dtype IS NULL
        OR dtype NOT IN ('CashSale', 'ThirdPartySales', 'VenteDepot')
$q$);

SELECT pg_temp.verif_vide('ventes', 'Enums conformes au modèle', $q$
    SELECT id FROM sales
     WHERE statut            NOT IN ('PROCESSING','PENDING','CLOSED','ACTIVE','CANCELED','REMOVED','DEVIS')
        OR payment_status    NOT IN ('PAYE','IMPAYE','ALL')
        OR nature_vente      NOT IN ('COMPTANT','ASSURANCE','CARNET')
        OR origine_vente     NOT IN ('DIRECT','DIVIS','IMPORTE')
        OR type_prescription NOT IN ('PRESCRIPTION','CONSEIL','DEPOT')
        OR ca                NOT IN ('CA','CA_DEPOT','CALLEBASE','TO_IGNORE','IMPORT')
$q$);

-- La clé étrangère porte sur les deux colonnes de la clé composite.
SELECT pg_temp.verif_vide('ventes', 'Clé composite ligne / vente cohérente', $q$
    SELECT sl.id FROM sales_line sl
     WHERE sl.sale_date <> sl.sales_sale_date
        OR NOT EXISTS (SELECT 1 FROM sales s
                        WHERE s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date)
$q$);

SELECT pg_temp.verif_vide('ventes', 'Unicité (produit, vente)', $q$
    SELECT sales_id FROM sales_line
     GROUP BY produit_id, sales_id, sale_date HAVING count(*) > 1
$q$);

-- Le total de la vente est la somme de ses lignes.
SELECT pg_temp.verif_vide('ventes', 'Vente = somme des lignes', $q$
    SELECT s.id FROM sales s
      JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
     GROUP BY s.id, s.sale_date, s.sales_amount
    HAVING s.sales_amount <> sum(sl.sales_amount)
$q$);

SELECT pg_temp.verif_vide('ventes', 'Montant de ligne = quantité × prix', $q$
    SELECT id FROM sales_line
     WHERE sales_amount <> quantity_requested * regular_unit_price
$q$);

SELECT pg_temp.verif_vide('ventes', 'HT + TVA = TTC', $q$
    SELECT id FROM sales WHERE ht_amount + tax_amount <> sales_amount
$q$);

-- Le HT s'arrondit AU PLAFOND, LIGNE PAR LIGNE, puis se somme : un calcul
-- global à partir du TTC donnerait un écart de quelques francs.
SELECT pg_temp.verif_vide('ventes', 'HT arrondi par ligne, au plafond', $q$
    SELECT s.id FROM sales s
      JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
     GROUP BY s.id, s.sale_date, s.ht_amount
    HAVING s.ht_amount <> sum(
        CASE WHEN sl.tax_value = 0 THEN sl.sales_amount
             ELSE ceil(sl.sales_amount::numeric / (1 + sl.tax_value / 100.0)) END)
$q$);

SELECT pg_temp.verif_vide('ventes', 'Net = TTC − remise', $q$
    SELECT id FROM sales WHERE net_amount <> sales_amount - discount_amount
$q$);

-- Les remises de la demonstration reprennent la grille produit : 5, 10 ou 15 %.
-- Un taux de 1 ou 2 % ne se verrait ni sur le ticket ni dans les etats.
SELECT pg_temp.verif_vide('ventes', 'Taux de remise pris dans la grille', $q$
    SELECT id FROM sales_line
     WHERE taux_remise NOT IN (0, 5, 10, 15)
$q$);

SELECT pg_temp.verif_vide('ventes', 'Remise de ligne = quantité × remise unitaire', $q$
    SELECT id FROM sales_line
     WHERE discount_amount <> quantity_requested * discount_unit_price
$q$);

SELECT pg_temp.verif_vide('ventes', 'Prix net = prix affiché − remise unitaire', $q$
    SELECT id FROM sales_line
     WHERE net_unit_price <> regular_unit_price - discount_unit_price
$q$);

-- Une remise ne se pose pas sur une vente tiers payant : la part payeur se
-- calcule sur le montant conventionne, et l'ecart retomberait sur le patient.
SELECT pg_temp.verif_vide('ventes', 'Aucune remise sur les ventes tiers payant', $q$
    SELECT id FROM sales
     WHERE dtype = 'ThirdPartySales' AND discount_amount > 0
$q$);

SELECT pg_temp.verif_compte('ventes', 'Des ventes remisées existent', $q$
    SELECT id FROM sales WHERE discount_amount > 0
$q$, 100);

-- Contrôle V2 de AuditDeclarationCaService : le montant déclarable de la vente
-- égale la somme de celui de ses lignes.
SELECT pg_temp.verif_vide('ventes', 'Montant déclarable = somme des lignes', $q$
    SELECT s.id FROM sales s
      JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
     WHERE s.dtype <> 'VenteDepot' AND s.statut = 'CLOSED'
     GROUP BY s.id, s.sale_date, s.amount_to_be_taken_into_account
    HAVING s.amount_to_be_taken_into_account <> sum(sl.amount_to_be_taken_into_account)
$q$);

-- Contrôle V1 : le déclarable d'une ligne ne dépasse pas son montant brut.
SELECT pg_temp.verif_vide('ventes', 'Déclarable de ligne dans ses bornes', $q$
    SELECT id FROM sales_line
     WHERE amount_to_be_taken_into_account < 0
        OR amount_to_be_taken_into_account > quantity_requested * regular_unit_price
$q$);

SELECT pg_temp.verif_vide('ventes', 'Coût de ligne = coût unitaire du produit', $q$
    SELECT sl.id FROM sales_line sl
      JOIN produit p ON p.id = sl.produit_id
     WHERE sl.cost_amount <> p.cost_amount
$q$);

SELECT pg_temp.verif_vide('ventes', 'Taux de TVA de ligne = celui du produit', $q$
    SELECT sl.id FROM sales_line sl
      JOIN produit p ON p.id = sl.produit_id
      JOIN tva t ON t.id = p.tva_id
     WHERE sl.tax_value <> t.taux
$q$);

SELECT pg_temp.verif_vide('ventes', 'Coût de la vente = somme des coûts de ligne', $q$
    SELECT s.id FROM sales s
      JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
     GROUP BY s.id, s.sale_date, s.cost_amount
    HAVING s.cost_amount <> sum(sl.quantity_requested * sl.cost_amount)
$q$);

SELECT pg_temp.verif_vide('ventes', 'Quantités positives et servies', $q$
    SELECT id FROM sales_line
     WHERE quantity_requested <= 0 OR quantity_sold > quantity_requested
$q$);

SELECT pg_temp.verif_vide('ventes', 'Aucune date de vente dans le futur', $q$
    SELECT id FROM sales WHERE sale_date > CURRENT_DATE
$q$);

-- number_transaction = yyyyMMdd + compteur du jour (SaleCommonService).
SELECT pg_temp.verif_vide('ventes', 'Numéro de transaction bien formé', $q$
    SELECT id FROM sales
     WHERE number_transaction !~ '^\d{8}\d{3,}$'
        OR left(number_transaction, 8) <> to_char(sale_date, 'YYYYMMDD')
$q$);

SELECT pg_temp.verif_vide('ventes', 'Vendeur, caissier et magasin renseignés', $q$
    SELECT id FROM sales
     WHERE seller_id IS NULL OR caissier_id IS NULL OR user_id IS NULL OR magasin_id IS NULL
$q$);


-- ===========================================================================
-- ENCAISSEMENTS  (§4.4)
-- ===========================================================================
SELECT pg_temp.verif_vide('encaissements', 'Somme des règlements = montant réglé', $q$
    SELECT s.id FROM sales s
      JOIN payment_transaction pt ON pt.sale_id = s.id AND pt.sale_date = s.sale_date
     GROUP BY s.id, s.sale_date, s.payroll_amount
    HAVING sum(pt.paid_amount) <> s.payroll_amount
$q$);

SELECT pg_temp.verif_vide('encaissements', 'Statut de règlement cohérent', $q$
    SELECT id FROM sales
     WHERE (payment_status = 'PAYE'   AND rest_to_pay <> 0)
        OR (payment_status = 'IMPAYE' AND rest_to_pay = 0)
$q$);

-- Exceptions admises : la vente tiers-payant couverte à 100 % n'a rien à
-- encaisser, la vente dépôt reste due par construction, et la vente différée
-- est hors du champ de ce contrôle — soldée ou non, elle n'a JAMAIS de
-- payment_transaction portant son sale_id : son règlement est un
-- DifferePayment rattaché au CLIENT (14b_reglements.sql), relié aux ventes par
-- differe_payment_item. Le contrôle qui lui correspond est plus bas.
SELECT pg_temp.verif_vide('encaissements', 'Toute vente à encaisser a un règlement', $q$
    SELECT s.id FROM sales s
     WHERE s.amount_to_be_paid > 0
       AND s.dtype <> 'VenteDepot'
       AND NOT s.differe
       AND NOT EXISTS (SELECT 1 FROM payment_transaction pt
                        WHERE pt.sale_id = s.id AND pt.sale_date = s.sale_date)
$q$);

-- ---------------------------------------------------------------------------
-- Règlements différés et règlements de factures (14b_reglements.sql)
-- ---------------------------------------------------------------------------
SELECT pg_temp.verif_vide('encaissements', 'Vente différée soldée a son règlement', $q$
    SELECT s.id FROM sales s
     WHERE s.differe AND s.payment_status = 'PAYE'
       AND NOT EXISTS (SELECT 1 FROM differe_payment_item i
                        WHERE i.sale_id = s.id AND i.sale_sale_date = s.sale_date)
$q$);

SELECT pg_temp.verif_vide('encaissements', 'Vente différée non soldée sans règlement', $q$
    SELECT s.id FROM sales s
     WHERE s.differe AND s.payment_status = 'IMPAYE'
       AND EXISTS (SELECT 1 FROM differe_payment_item i
                    WHERE i.sale_id = s.id AND i.sale_sale_date = s.sale_date)
$q$);

SELECT pg_temp.verif_vide('encaissements', 'Règlement différé = somme de ses items', $q$
    SELECT pt.id FROM payment_transaction pt
      JOIN differe_payment_item i ON i.differe_payment_id = pt.id
                                 AND i.differe_payment_transaction_date = pt.transaction_date
     WHERE pt.dtype = 'DifferePayment'
     GROUP BY pt.id, pt.paid_amount
    HAVING sum(i.paid_amount) <> pt.paid_amount
$q$);

-- Le défaut d'origine : un statut de facture affirmant un règlement dont il
-- n'existait aucune trace.
SELECT pg_temp.verif_vide('encaissements', 'Facture réglée a son encaissement', $q$
    SELECT f.id FROM facture_tiers_payant f
     WHERE f.montant_regle > 0
       AND NOT EXISTS (SELECT 1 FROM payment_transaction pt
                        WHERE pt.dtype = 'InvoicePayment'
                          AND pt.facture_tierspayant_id = f.id
                          AND pt.facture_tierspayant_invoice_date = f.invoice_date)
$q$);

-- Le montant réglé d'une facture a deux sources, et non une seule : ce qui a
-- été encaissé, et ce qu'un avoir imputé a effacé sans qu'un franc circule.
-- Ne comparer qu'à l'encaissement faisait passer toute imputation pour une
-- incohérence.
SELECT pg_temp.verif_vide('encaissements', 'Encaissement + avoirs imputés = montant réglé', $q$
    SELECT pt.id FROM payment_transaction pt
      JOIN facture_tiers_payant f ON f.id = pt.facture_tierspayant_id
                                 AND f.invoice_date = pt.facture_tierspayant_invoice_date
     WHERE pt.dtype = 'InvoicePayment'
       AND pt.paid_amount + COALESCE((
             SELECT sum(a.montant_avoir)::int FROM avoir_tiers_payant a
              WHERE a.statut = 'IMPUTE'
                AND a.facture_imputation_id = f.id
                AND a.facture_imputation_date = f.invoice_date
           ), 0) <> f.montant_regle
$q$);

-- Un avoir imputé désigne la facture qu'il a soldée, et lui seul modifie un
-- solde sans encaissement : sans ce lien, le montant réglé serait inexplicable.
SELECT pg_temp.verif_vide('facturation', 'Avoir imputé rattaché à une facture', $q$
    SELECT id FROM avoir_tiers_payant
     WHERE (statut = 'IMPUTE' AND facture_imputation_id IS NULL)
        OR (statut <> 'IMPUTE' AND facture_imputation_id IS NOT NULL)
$q$);

-- Les quatre statuts doivent avoir un exemple, sinon une transition du cycle
-- de vie reste inatteignable à l'écran.
SELECT pg_temp.verif_compte('facturation', 'Statuts d''avoir représentés', $q$
    SELECT DISTINCT statut FROM avoir_tiers_payant
$q$, 4);

-- Des créances doivent subsister : sans elles, ni solde client, ni relance.
SELECT pg_temp.verif_compte('encaissements', 'Créances clients en cours', $q$
    SELECT 1 FROM sales WHERE differe AND payment_status = 'IMPAYE'
$q$, 20);

-- PaymentTransaction.cashRegister est optional = false.
SELECT pg_temp.verif_vide('encaissements', 'Tout règlement est rattaché à une caisse', $q$
    SELECT id FROM payment_transaction WHERE cash_register_id IS NULL
$q$);

-- Contrôle V2b : l'encaissement déclaré ne dépasse pas le CA déclaré.
SELECT pg_temp.verif_vide('encaissements', 'Encaissement déclaré <= CA déclaré', $q$
    SELECT s.id FROM sales s
      JOIN payment_transaction pt ON pt.sale_id = s.id AND pt.sale_date = s.sale_date
     GROUP BY s.id, s.sale_date, s.amount_to_be_taken_into_account
    HAVING sum(COALESCE(pt.amount_to_be_taken_into_account, pt.paid_amount))
           > s.amount_to_be_taken_into_account
$q$);

SELECT pg_temp.verif_vide('encaissements', 'Mode de règlement connu du référentiel', $q$
    SELECT pt.id FROM payment_transaction pt
     WHERE NOT EXISTS (SELECT 1 FROM payment_mode pm WHERE pm.code = pt.payment_mode_code)
$q$);


-- ===========================================================================
-- CONSOMMATION FEFO DES LOTS  (§4.9)
-- ===========================================================================
-- Le snapshot doit couvrir exactement la quantité vendue.
SELECT pg_temp.verif_vide('fefo', 'Lots vendus = quantité vendue', $q$
    SELECT sl.id FROM sales_line sl
     WHERE sl.quantity_sold > 0
       AND sl.quantity_sold <> COALESCE(
           (SELECT sum((e->>'quantity')::int)
              FROM jsonb_array_elements(sl.lots) e), 0)
$q$);

-- Le snapshot est un instantané : numéro et produit doivent concorder.
SELECT pg_temp.verif_vide('fefo', 'Snapshot cohérent avec la table lot', $q$
    SELECT sl.id FROM sales_line sl
     CROSS JOIN LATERAL jsonb_array_elements(sl.lots) e
      JOIN lot l ON l.id = (e->>'id')::int
     WHERE l.num_lot <> (e->>'numLot') OR l.produit_id <> sl.produit_id
$q$);

-- RÈGLE DES 90 JOURS : un lot trop proche de sa péremption n'est pas vendable.
SELECT pg_temp.verif_vide('fefo', 'Péremption > date de vente + 90 jours', $q$
    SELECT sl.id FROM sales_line sl
     CROSS JOIN LATERAL jsonb_array_elements(sl.lots) e
     WHERE (e->>'expiryDate')::date <= sl.sale_date + 90
$q$);

-- FEFO : péremption croissante à l'intérieur d'une ligne.
SELECT pg_temp.verif_vide('fefo', 'Lots consommés par péremption croissante', $q$
    SELECT sl.id FROM sales_line sl
     WHERE EXISTS (
         SELECT 1 FROM (
             SELECT (e->>'expiryDate')::date AS d,
                    lag((e->>'expiryDate')::date) OVER (ORDER BY ord) AS prev
               FROM jsonb_array_elements(sl.lots) WITH ORDINALITY AS t(e, ord)
         ) x WHERE x.d < x.prev)
$q$);

-- Le stock a effectivement été décrémenté : il ne reste jamais plus que ce qui
-- a été reçu moins ce qui a été vendu.
SELECT pg_temp.verif_vide('fefo', 'Aucun stock négatif après les ventes', $q$
    SELECT id FROM stock_produit WHERE qty_stock < 0
$q$);

SELECT pg_temp.verif_vide('fefo', 'Aucun lot négatif après les ventes', $q$
    SELECT id FROM lot WHERE current_quantity < 0
$q$);


-- ===========================================================================
-- VENTES TIERS-PAYANT  (§4.5)
-- ===========================================================================
SELECT pg_temp.verif_compte('tp-ventes', 'Ventes tiers-payant', $q$
    SELECT 1 FROM sales WHERE dtype = 'ThirdPartySales'
$q$, 1000);

SELECT pg_temp.verif_compte('tp-ventes', 'Ventes comptant', $q$
    SELECT 1 FROM sales WHERE dtype = 'CashSale'
$q$, 1000);

SELECT pg_temp.verif_vide('tp-ventes', 'Les deux parts couvrent le net', $q$
    SELECT id FROM sales
     WHERE dtype = 'ThirdPartySales'
       AND part_assure + part_tiers_payant <> net_amount
$q$);

SELECT pg_temp.verif_vide('tp-ventes', 'Part tiers-payant = somme des ventilations', $q$
    SELECT s.id FROM sales s
      JOIN third_party_sale_line t ON t.sale_id = s.id AND t.sale_sale_date = s.sale_date
     WHERE s.dtype = 'ThirdPartySales'
     GROUP BY s.id, s.sale_date, s.part_tiers_payant
    HAVING s.part_tiers_payant <> sum(t.montant)
$q$);

SELECT pg_temp.verif_vide('tp-ventes', 'Aucune ventilation sur une vente comptant', $q$
    SELECT t.id FROM third_party_sale_line t
      JOIN sales s ON s.id = t.sale_id AND s.sale_date = t.sale_sale_date
     WHERE s.dtype <> 'ThirdPartySales'
$q$);

-- taux est le taux EFFECTIF constaté, pas le taux contractuel (taux_vente).
SELECT pg_temp.verif_vide('tp-ventes', 'Taux effectif conforme au montant', $q$
    SELECT t.id FROM third_party_sale_line t
      JOIN sales s ON s.id = t.sale_id AND s.sale_date = t.sale_sale_date
     WHERE s.sales_amount > 0
       AND t.taux <> round(t.montant * 100.0 / s.sales_amount)
$q$);

SELECT pg_temp.verif_vide('tp-ventes', 'Aucun payeur au-delà du montant de la vente', $q$
    SELECT t.id FROM third_party_sale_line t
      JOIN sales s ON s.id = t.sale_id AND s.sale_date = t.sale_sale_date
     WHERE t.montant > s.sales_amount OR t.montant < 0
$q$);

-- APP_SANS_NUM_BON vaut 0 : le numéro de bon est obligatoire.
SELECT pg_temp.verif_vide('tp-ventes', 'Numéro de bon renseigné', $q$
    SELECT id FROM sales WHERE dtype = 'ThirdPartySales' AND num_bon IS NULL
$q$);

-- Le contrat mobilisé doit être celui de l'assuré porteur de la vente.
SELECT pg_temp.verif_vide('tp-ventes', 'Contrat rattaché à l''assuré de la vente', $q$
    SELECT t.id FROM third_party_sale_line t
      JOIN sales s ON s.id = t.sale_id AND s.sale_date = t.sale_sale_date
      JOIN client_tiers_payant ctp ON ctp.id = t.client_tiers_payant_id
     WHERE s.customer_id IS NOT NULL AND ctp.assured_customer_id <> s.customer_id
$q$);

-- Un ayant droit est toujours rattaché à l'assuré qui porte la vente.
SELECT pg_temp.verif_vide('tp-ventes', 'Ayant droit rattaché au porteur du contrat', $q$
    SELECT s.id FROM sales s
      JOIN customer ad ON ad.id = s.ayant_droit_id
     WHERE ad.assure_principal_id <> s.customer_id
$q$);


-- ===========================================================================
-- RÉPARTITIONS TVA ET CONSOMMATIONS  (§4.5)
-- ===========================================================================
SELECT pg_temp.verif_vide('repartitions', 'Toute ventilation est répartie', $q$
    SELECT id FROM third_party_sale_line
     WHERE montant > 0 AND jsonb_array_length(repartitions) = 0
$q$);

-- La somme des répartitions doit retomber exactement sur le montant du payeur.
SELECT pg_temp.verif_vide('repartitions', 'Somme des répartitions = montant du payeur', $q$
    SELECT id FROM third_party_sale_line
     WHERE jsonb_array_length(repartitions) > 0
       AND montant <> (SELECT sum((e->>'montantTtc')::numeric)::int
                         FROM jsonb_array_elements(repartitions) e)
$q$);

-- HT + TVA doit retomber sur le TTC, à l'arrondi au centime près.
SELECT pg_temp.verif_vide('repartitions', 'HT + TVA = TTC dans chaque répartition', $q$
    SELECT t.id FROM third_party_sale_line t
     CROSS JOIN LATERAL jsonb_array_elements(t.repartitions) e
     WHERE abs(((e->>'montantHt')::numeric + (e->>'montantTva')::numeric)
               - (e->>'montantTtc')::numeric) > 0.01
$q$);

-- Les taux répartis doivent exister dans le référentiel de TVA.
SELECT pg_temp.verif_vide('repartitions', 'Taux de TVA connus du référentiel', $q$
    SELECT t.id FROM third_party_sale_line t
     CROSS JOIN LATERAL jsonb_array_elements(t.repartitions) e
     WHERE NOT EXISTS (SELECT 1 FROM tva v WHERE v.taux = (e->>'tva')::int)
$q$);

SELECT pg_temp.verif_vide('consommations', 'Consommation initialisée sur tout contrat', $q$
    SELECT id FROM client_tiers_payant WHERE consommation_json IS NULL
$q$);

-- Le cumul affiché doit correspondre aux ventilations du mois, sinon l'écran
-- de suivi contredit le journal des ventes.
SELECT pg_temp.verif_vide('consommations', 'Cumul mensuel = ventilations du mois', $q$
    SELECT ctp.id FROM client_tiers_payant ctp
     CROSS JOIN LATERAL json_array_elements(ctp.consommation_json) e
     WHERE (e->>'consommation')::bigint <> COALESCE((
         SELECT sum(t.montant) FROM third_party_sale_line t
          WHERE t.client_tiers_payant_id = ctp.id
            AND extract(year  FROM t.sale_date)::int = (e->>'year')::int
            AND extract(month FROM t.sale_date)::int = (e->>'month')::int), 0)
$q$);

SELECT pg_temp.verif_vide('consommations', 'Mois et années plausibles', $q$
    SELECT ctp.id FROM client_tiers_payant ctp
     CROSS JOIN LATERAL json_array_elements(ctp.consommation_json) e
     WHERE (e->>'month')::int NOT BETWEEN 1 AND 12
        OR (e->>'year')::int  NOT BETWEEN 2000 AND 2100
$q$);


-- ===========================================================================
-- VENTES DÉPÔT  (§4.6)
-- ===========================================================================
SELECT pg_temp.verif_compte('depot', 'Magasin dépôt créé', $q$
    SELECT 1 FROM magasin WHERE type_magasin = 'DEPOT'
$q$, 1);

-- Magasin.primaryStorage est une @JoinFormula sur magasin_id : sans stockage
-- propre, elle renvoie null et la finalisation échoue.
SELECT pg_temp.verif_vide('depot', 'Le dépôt a son stockage PRINCIPAL', $q$
    SELECT m.id FROM magasin m
     WHERE m.type_magasin = 'DEPOT'
       AND NOT EXISTS (SELECT 1 FROM storage s
                        WHERE s.magasin_id = m.id AND s.storage_type = 'PRINCIPAL')
$q$);

SELECT pg_temp.verif_compte('depot', 'Ventes dépôt', $q$
    SELECT 1 FROM sales WHERE dtype = 'VenteDepot'
$q$, 30);

SELECT pg_temp.verif_vide('depot', 'La vente dépôt cible un magasin DEPOT', $q$
    SELECT s.id FROM sales s
      LEFT JOIN magasin m ON m.id = s.depot_id
     WHERE s.dtype = 'VenteDepot'
       AND (s.depot_id IS NULL OR m.type_magasin <> 'DEPOT')
$q$);

-- Marqueurs imposés par construction : hors CA déclaré, due, sans client.
SELECT pg_temp.verif_vide('depot', 'Marqueurs comptables de la vente dépôt', $q$
    SELECT id FROM sales WHERE dtype = 'VenteDepot'
       AND (ca <> 'CA_DEPOT'
         OR amount_to_be_taken_into_account <> 0
         OR payment_status <> 'IMPAYE'
         OR rest_to_pay <> amount_to_be_paid
         OR customer_id IS NOT NULL)
$q$);

SELECT pg_temp.verif_vide('depot', 'Aucune autre vente ne porte CA_DEPOT', $q$
    SELECT id FROM sales WHERE ca = 'CA_DEPOT' AND dtype <> 'VenteDepot'
$q$);

-- La vente dépôt reste due : aucun règlement ne lui est rattaché.
SELECT pg_temp.verif_vide('depot', 'Aucun règlement sur une vente dépôt', $q$
    SELECT pt.id FROM payment_transaction pt
      JOIN sales s ON s.id = pt.sale_id AND s.sale_date = pt.sale_date
     WHERE s.dtype = 'VenteDepot'
$q$);

-- Les lignes gardent leur montant déclarable alors que la vente est à zéro :
-- c'est pourquoi le contrôle V2 de l'audit CA écarte ces ventes.
SELECT pg_temp.verif_vide('depot', 'Les lignes gardent leur montant déclarable', $q$
    SELECT sl.id FROM sales_line sl
      JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
     WHERE s.dtype = 'VenteDepot' AND sl.amount_to_be_taken_into_account = 0
$q$);

-- updateStockDepot ne crée ni lot ni emplacement : le stock du dépôt est
-- sans traçabilité fine.
SELECT pg_temp.verif_vide('depot', 'Le stock du dépôt est sans lot', $q$
    SELECT lsl.id FROM lot_stock_location lsl
      JOIN storage s ON s.id = lsl.storage_id
      JOIN magasin m ON m.id = s.magasin_id
     WHERE m.type_magasin = 'DEPOT'
$q$);

SELECT pg_temp.verif_compte('depot', 'Le dépôt détient du stock', $q$
    SELECT 1 FROM stock_produit sp
      JOIN storage s ON s.id = sp.storage_id
      JOIN magasin m ON m.id = s.magasin_id AND m.type_magasin = 'DEPOT'
     WHERE sp.qty_stock > 0
$q$, 20);

SELECT pg_temp.verif_compte('depot', 'Retours de dépôt', $q$
    SELECT 1 FROM retour_depot
$q$, 3);

-- qty_mvt porte un @Min(1) : on ne retourne jamais zéro.
SELECT pg_temp.verif_vide('depot', 'Quantité retournée strictement positive', $q$
    SELECT id FROM retour_depot_item WHERE qty_mvt < 1
$q$);

-- Le lien vers la vente d'origine est facultatif, mais s'il existe il doit
-- pointer une vente dépôt.
SELECT pg_temp.verif_vide('depot', 'Retour rattaché à une vente dépôt', $q$
    SELECT rd.id FROM retour_depot rd
     WHERE rd.vente_depot_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM sales s
                        WHERE s.id = rd.vente_depot_id
                          AND s.sale_date = rd.vente_depot_date
                          AND s.dtype = 'VenteDepot')
$q$);

SELECT pg_temp.verif_vide('depot', 'Chronologie : retour après transfert', $q$
    SELECT rd.id FROM retour_depot rd
      JOIN sales s ON s.id = rd.vente_depot_id AND s.sale_date = rd.vente_depot_date
     WHERE rd.date_mtv::date < s.sale_date
$q$);


-- ===========================================================================
-- DESTRUCTION DES PÉRIMÉS
-- ===========================================================================
SELECT pg_temp.verif_compte('destruction', 'Produits à détruire', $q$
    SELECT 1 FROM products_to_destroy
$q$, 30);

-- Les deux états doivent coexister : en attente et effectivement détruits.
SELECT pg_temp.verif_compte('destruction', 'Lignes en attente de destruction', $q$
    SELECT 1 FROM products_to_destroy WHERE NOT destroyed
$q$, 1);

SELECT pg_temp.verif_compte('destruction', 'Lignes effectivement détruites', $q$
    SELECT 1 FROM products_to_destroy WHERE destroyed
$q$, 1);

SELECT pg_temp.verif_vide('destruction', 'Quantités strictement positives', $q$
    SELECT id FROM products_to_destroy WHERE quantity < 1 OR stock_initial < 1
$q$);

SELECT pg_temp.verif_vide('destruction', 'Destruction postérieure à la péremption', $q$
    SELECT id FROM products_to_destroy
     WHERE datedestuction IS NOT NULL AND datedestuction < dateperemption
$q$);

-- Encore et toujours : le fournisseur_produit appartient à un principal.
SELECT pg_temp.verif_vide('destruction', 'Rattaché au fournisseur principal', $q$
    SELECT d.id FROM products_to_destroy d
      JOIN fournisseur_produit fp ON fp.id = d.fournisseur_produit_id
      JOIN fournisseur f ON f.id = fp.fournisseur_id
     WHERE f.parent_id IS NOT NULL
$q$);

-- Une destruction n'a de sens que sur un lot réellement périmé.
SELECT pg_temp.verif_vide('destruction', 'Le lot correspondant est périmé', $q$
    SELECT d.id FROM products_to_destroy d
      JOIN fournisseur_produit fp ON fp.id = d.fournisseur_produit_id
      JOIN lot l ON l.num_lot = d.numlot AND l.produit_id = fp.produit_id
     WHERE l.statut NOT IN ('EXPIRED', 'DESTROYED')
$q$);


-- ===========================================================================
-- REMISES, PLAFONDS ET TARIFS NÉGOCIÉS
--
-- Trois mécanismes de prix que rien n'exerçait avant 04b : sans ces données,
-- les écrans de vente correspondants sont muets et les scénarios VTE-03,
-- VTE-52 et VTE-53 ne peuvent pas être illustrés.
--
-- La remise attachée au CLIENT n'y figure pas : plus rien ne la lit dans le
-- calcul d'une vente, seule la grille produit s'applique.
-- ===========================================================================

-- La grille doit couvrir les deux régimes : un code VNO sans son pendant VO
-- laisserait les ventes ordonnancées sans remise.
-- La plus petite pièce en FCFA vaut 5 francs : un prix qui n'est pas un multiple
-- de 5 ne peut pas être rendu en espèces, et fait apparaître des arrondis que
-- personne ne sait expliquer au comptoir.
SELECT pg_temp.verif_vide('produits', 'Prix de vente multiples de 5', $q$
    SELECT id FROM produit WHERE regular_unit_price % 5 <> 0
$q$);

SELECT pg_temp.verif_vide('produits', 'Prix fournisseur multiples de 5', $q$
    SELECT id FROM fournisseur_produit WHERE prix_uni % 5 <> 0 OR prix_achat % 5 <> 0
$q$);

SELECT pg_temp.verif_vide('remises', 'Tarifs négociés multiples de 5', $q$
    SELECT id FROM produit_tiers_payant_prix WHERE prix_type = 'REFERENCE' AND price % 5 <> 0
$q$);

SELECT pg_temp.verif_compte('remises', 'Grille de remise produit renseignée', $q$
    SELECT 1 FROM grille_remise WHERE enable
$q$, 10);

SELECT pg_temp.verif_compte('remises', 'Produits répartis sur plusieurs codes remise', $q$
    SELECT 1 FROM produit GROUP BY code_remise
$q$, 3);

-- Un plafond sans consommation engagée ne se déclenche jamais : les deux
-- doivent exister pour que le plafonnement soit démontrable.
SELECT pg_temp.verif_compte('remises', 'Tiers payants à plafond client', $q$
    SELECT 1 FROM tiers_payant WHERE plafond_conso_client IS NOT NULL
$q$, 2);

SELECT pg_temp.verif_compte('remises', 'Assurés proches de leur plafond', $q$
    SELECT 1 FROM client_tiers_payant ctp
      JOIN tiers_payant tp ON tp.id = ctp.tierspayant_id
     WHERE tp.plafond_conso_client IS NOT NULL
       AND ctp.conso_mensuelle > tp.plafond_conso_client * 0.8
$q$, 2);

SELECT pg_temp.verif_compte('remises', 'Tarifs négociés par tiers payant', $q$
    SELECT 1 FROM produit_tiers_payant_prix WHERE enabled
$q$, 20);

-- Un prix de référence doit être INFÉRIEUR au prix catalogue, sinon il n'a
-- aucun effet et le scénario ne montre rien.
SELECT pg_temp.verif_vide('remises', 'Prix de référence inférieur au catalogue', $q$
    SELECT ptp.id FROM produit_tiers_payant_prix ptp
      JOIN LATERAL (
          SELECT x.prix_uni FROM fournisseur_produit x
            JOIN fournisseur f ON f.id = x.fournisseur_id AND f.parent_id IS NULL
           WHERE x.produit_id = ptp.produit_id ORDER BY x.id LIMIT 1
      ) fp ON true
     WHERE ptp.prix_type = 'REFERENCE' AND ptp.price >= fp.prix_uni
$q$);

-- ===========================================================================
-- FACTURATION TIERS-PAYANT
-- ===========================================================================
SELECT pg_temp.verif_compte('facturation', 'Factures émises', $q$
    SELECT 1 FROM facture_tiers_payant
$q$, 20);

SELECT pg_temp.verif_vide('facturation', 'Statut conforme à la contrainte CHECK', $q$
    SELECT id FROM facture_tiers_payant
     WHERE statut NOT IN ('PAID', 'NOT_PAID', 'PARTIALLY_PAID')
        OR origine_generation NOT IN ('MANUELLE', 'AUTO')
$q$);

-- groupe_tiers_payant_id sur une facture ne dit pas que l'organisme appartient
-- à un groupe — cela se lit sur tiers_payant — mais que la FACTURE est une
-- facture de groupe. Le contrôle exigeait l'inverse et codifiait la méprise :
-- recopié sur toutes les factures, ce champ rendait chacune indiscernable d'une
-- facture de groupe, vidait la bannière « Individuelles » et faisait annoncer à
-- la liste « Groupées » un total qu'elle n'affichait pas.
SELECT pg_temp.verif_vide('facturation', 'Facture individuelle sans marque de groupe', $q$
    SELECT f.id FROM facture_tiers_payant f
     WHERE f.tiers_payant_id IS NOT NULL
       AND f.groupe_tiers_payant_id IS NOT NULL
$q$);

-- Une facture de groupe vise le groupe, pas un organisme.
SELECT pg_temp.verif_vide('facturation', 'Facture de groupe correctement formée', $q$
    SELECT f.id FROM facture_tiers_payant f
     WHERE f.tiers_payant_id IS NULL
       AND (f.groupe_tiers_payant_id IS NULL
         OR NOT EXISTS (SELECT 1 FROM facture_tiers_payant fille
                         WHERE fille.groupe_facture_tiers_payant_id = f.id
                           AND fille.groupe_facture_tiers_payant_invoice_date = f.invoice_date))
$q$);

-- Sans facture de groupe, l'onglet « Groupées » et le règlement groupé
-- n'ont rien à montrer.
SELECT pg_temp.verif_compte('facturation', 'Factures de groupe émises', $q$
    SELECT f.id FROM facture_tiers_payant f
     WHERE f.tiers_payant_id IS NULL AND f.groupe_tiers_payant_id IS NOT NULL
$q$, 2);

-- La parente totalise ses filles : c'est ce que somme la bannière d'indicateurs
-- en mode « Groupées », et ce que la liste affiche en « Réglé ».
SELECT pg_temp.verif_vide('facturation', 'Facture de groupe = somme de ses filles', $q$
    SELECT p.id FROM facture_tiers_payant p
      JOIN facture_tiers_payant f
        ON f.groupe_facture_tiers_payant_id = p.id
       AND f.groupe_facture_tiers_payant_invoice_date = p.invoice_date
     WHERE p.tiers_payant_id IS NULL
     GROUP BY p.id, p.montant_net, p.montant_regle
    HAVING p.montant_net <> sum(f.montant_net)
        OR p.montant_regle <> sum(f.montant_regle)::int
$q$);

-- Le total de la facture doit égaler la somme des bons qu'elle regroupe.
SELECT pg_temp.verif_vide('facturation', 'Facture = somme des bons regroupés', $q$
    SELECT ft.id FROM facture_tiers_payant ft
      JOIN third_party_sale_line t ON t.facture_tiers_payant_id = ft.id
                                  AND t.invoice_date = ft.invoice_date
     GROUP BY ft.id, ft.montant_ttc
    HAVING ft.montant_ttc::int <> sum(t.montant)
$q$);

-- Un bon ne peut pas être facturé hors de la période qu'il couvre.
-- Les produits VEDETTES du manuel doivent rester vendables : trente-huit
-- parcours passent par DOLIPRANE 500MG. Un stock épuisé ferait échouer toute la
-- série des ventes sur « Stock insuffisant », ce qui se lit comme un défaut de
-- l'application alors que c'est le jeu de données qui est à sec.
SELECT pg_temp.verif_compte('stock', 'Produits vedettes vendables', $q$
    SELECT p.id
      FROM stock_produit sp
      JOIN produit p ON p.id = sp.produit_id
      JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'PRINCIPAL'
     WHERE sp.qty_stock >= 100
       AND (p.libelle LIKE 'DOLIPRANE 500MG%'
         OR p.libelle LIKE 'DOLIPRANE 1G%'
         OR p.libelle LIKE 'PARACETAMOL 1G%')
$q$, 3);

-- Un organisme SANS email doit subsister : la certification fiscale (FNE) le
-- refuse, et c'est le seul moyen de montrer ce garde-fou à l'écran.
SELECT pg_temp.verif_compte('facturation', 'Organisme sans email (garde-fou FNE)', $q$
    SELECT 1 FROM tiers_payant WHERE email IS NULL OR email = ''
$q$, 1);

-- Un parcours de règlement peut consommer une facture avant FAC-30 : deux cas
-- garantissent qu'il en reste une à refuser lors de la certification FNE.
SELECT pg_temp.verif_compte('facturation', 'Factures certifiables de l''organisme sans email', $q$
    SELECT f.id
      FROM facture_tiers_payant f
      JOIN tiers_payant tp ON tp.id = f.tiers_payant_id
     WHERE tp.name = 'ASACI'
       AND f.statut = 'NOT_PAID'
       AND NOT f.facture_provisoire
       AND f.fne_response IS NULL
$q$, 2);

-- ...et la majorité doit en avoir un, sinon la certification serait impossible
-- pour tout le monde et l'écran ne montrerait plus le cas nominal.
SELECT pg_temp.verif_compte('facturation', 'Organismes certifiables (téléphone + email)', $q$
    SELECT 1 FROM tiers_payant
     WHERE coalesce(email, '') <> '' AND coalesce(telephone, '') <> ''
$q$, 8);

SELECT pg_temp.verif_vide('facturation', 'Bons dans la période facturée', $q$
    SELECT t.id FROM third_party_sale_line t
      JOIN facture_tiers_payant ft ON ft.id = t.facture_tiers_payant_id
                                  AND ft.invoice_date = t.invoice_date
     WHERE t.sale_date < ft.debut_periode OR t.sale_date > ft.fin_periode
$q$);

SELECT pg_temp.verif_vide('facturation', 'Règlement dans les bornes du montant', $q$
    SELECT id FROM facture_tiers_payant
     WHERE montant_regle > montant_ttc OR montant_regle < 0
$q$);

-- Le règlement de la ligne doit suivre celui de sa facture.
SELECT pg_temp.verif_vide('facturation', 'Règlement de ligne cohérent avec la facture', $q$
    SELECT t.id FROM third_party_sale_line t
      JOIN facture_tiers_payant ft ON ft.id = t.facture_tiers_payant_id
                                  AND ft.invoice_date = t.invoice_date
     WHERE (ft.statut = 'PAID'     AND t.montant_regle <> t.montant)
        OR (ft.statut = 'NOT_PAID' AND t.montant_regle <> 0)
        OR t.montant_regle > t.montant
$q$);

-- La facture est émise après la période qu'elle couvre.
SELECT pg_temp.verif_vide('facturation', 'Émission postérieure à la période', $q$
    SELECT id FROM facture_tiers_payant
     WHERE invoice_date <= fin_periode OR debut_periode > fin_periode
$q$);

-- Le mois courant n'est pas facturable : il est encore en constitution.
SELECT pg_temp.verif_vide('facturation', 'Aucune facture sur le mois courant', $q$
    SELECT id FROM facture_tiers_payant
     WHERE debut_periode >= date_trunc('month', CURRENT_DATE)::date
$q$);


-- ===========================================================================
-- NUMÉROTATION  (§4.3)
-- ===========================================================================
-- La colonne de type s'appelle d_type, et porte un ENTIER (TypeReference).
SELECT pg_temp.verif_compte('numerotation', 'Compteurs présents', $q$
    SELECT 1 FROM reference
$q$, 100);

SELECT pg_temp.verif_vide('numerotation', 'Tout jour de vente a son compteur', $q$
    SELECT DISTINCT s.sale_date FROM sales s
     WHERE NOT EXISTS (SELECT 1 FROM reference r
                        WHERE r.mvt_date = s.sale_date AND r.d_type = 0)
$q$);

-- Un compteur inférieur au nombre de ventes du jour ferait réémettre à
-- l'application un numéro déjà présent.
SELECT pg_temp.verif_vide('numerotation', 'Compteur couvrant les ventes du jour', $q$
    SELECT s.sale_date FROM sales s
     GROUP BY s.sale_date
    HAVING count(*) > COALESCE((SELECT r.number_transac FROM reference r
                                 WHERE r.mvt_date = s.sale_date AND r.d_type = 0), 0)
$q$);

SELECT pg_temp.verif_vide('numerotation', 'Types de référence connus', $q$
    SELECT id FROM reference WHERE d_type NOT BETWEEN 0 AND 7
$q$);


-- ===========================================================================
-- MOUVEMENTS PRODUIT
-- ===========================================================================
SELECT pg_temp.verif_compte('mouvements', 'Mouvements enregistrés', $q$
    SELECT 1 FROM inventory_transaction
$q$, 5000);

SELECT pg_temp.verif_vide('mouvements', 'Type de mouvement connu du modèle', $q$
    SELECT id FROM inventory_transaction
     WHERE mouvement_type NOT IN (
        'SALE','DELETE_SALE','CANCEL_SALE','AJUSTEMENT_IN','AJUSTEMENT_OUT',
        'INVENTAIRE','COMMANDE','DECONDTION_IN','DECONDTION_OUT',
        'MOUVEMENT_STOCK_IN','MOUVEMENT_STOCK_OUT','ENTREE_STOCK',
        'RETRAIT_PERIME','RETOUR_DEPOT','RETOUR_FOURNISSEUR','RETOUR_CLIENT','DESTRUCTION')
$q$);

-- Le mouvement doit se refermer : après = avant ± quantité.
SELECT pg_temp.verif_vide('mouvements', 'Le mouvement se referme', $q$
    SELECT id FROM inventory_transaction
     WHERE (mouvement_type IN ('ENTREE_STOCK','RETOUR_DEPOT')
            AND quantity_after <> quantity_befor + quantity)
        OR (mouvement_type IN ('SALE','RETRAIT_PERIME')
            AND quantity_after <> quantity_befor - quantity)
$q$);

SELECT pg_temp.verif_vide('mouvements', 'Quantités valides', $q$
    SELECT id FROM inventory_transaction
     WHERE quantity <= 0 OR quantity_befor < 0 OR quantity_after < 0
$q$);

-- Toute vente doit avoir laissé une trace, sinon « Suivi article » ment.
SELECT pg_temp.verif_vide('mouvements', 'Toute ligne de vente a son mouvement', $q$
    SELECT sl.id FROM sales_line sl
     WHERE sl.quantity_sold > 0
       AND NOT EXISTS (SELECT 1 FROM inventory_transaction it
                        WHERE it.mouvement_type = 'SALE' AND it.entity_id = sl.id)
$q$);

SELECT pg_temp.verif_vide('mouvements', 'Aucune date dans le futur', $q$
    SELECT id FROM inventory_transaction WHERE transaction_date > CURRENT_DATE
$q$);


-- ===========================================================================
-- RÉFÉRENTIELS — le reset ne doit pas les avoir emportés
-- ===========================================================================
SELECT pg_temp.verif_compte('referentiels', 'Magasin officine', $q$
    SELECT 1 FROM magasin WHERE type_magasin = 'OFFICINE'
$q$, 1);

SELECT pg_temp.verif_compte('referentiels', 'Stockages de l''officine', $q$
    SELECT 1 FROM storage WHERE magasin_id = 1
$q$, 2);

SELECT pg_temp.verif_compte('referentiels', 'Comptes utilisateurs', $q$
    SELECT 1 FROM app_user
$q$, 3);

SELECT pg_temp.verif_compte('referentiels', 'Taux de TVA', $q$
    SELECT 1 FROM tva
$q$, 3);

SELECT pg_temp.verif_compte('referentiels', 'Familles de produits', $q$
    SELECT 1 FROM famille_produit
$q$, 20);

SELECT pg_temp.verif_compte('referentiels', 'Modes de paiement', $q$
    SELECT 1 FROM payment_mode
$q$, 8);

SELECT pg_temp.verif_compte('referentiels', 'Menus de navigation', $q$
    SELECT 1 FROM nav_item
$q$, 1);


-- ===========================================================================
-- Restitution
-- ===========================================================================
\echo ''
\pset format aligned
SELECT section, nom, statut, detail
  FROM tmp_verif
 ORDER BY ordre;

\echo ''
SELECT statut, count(*) AS nombre
  FROM tmp_verif
 GROUP BY statut
 ORDER BY statut;
\echo ''

DO $$
DECLARE
    v_echecs int;
    v_detail text;
BEGIN
    SELECT count(*) INTO v_echecs FROM tmp_verif WHERE statut <> 'OK';
    IF v_echecs > 0 THEN
        SELECT string_agg(format('%s / %s : %s', section, nom, detail), E'\n  ')
          INTO v_detail
          FROM tmp_verif WHERE statut <> 'OK';
        RAISE EXCEPTION E'% contrôle(s) en échec :\n  %', v_echecs, v_detail;
    END IF;
    RAISE NOTICE 'Tous les contrôles sont au vert.';
END $$;

\echo '<< 99_verification : terminé'
