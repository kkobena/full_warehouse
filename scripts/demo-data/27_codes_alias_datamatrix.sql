-- ============================================================================
-- 27_codes_alias_datamatrix.sql — Codes alternatifs des boîtes, et de quoi
-- imprimer l'étiquette interne à DataMatrix
--
-- En Côte d'Ivoire les grossistes n'intègrent pas les codes du DataMatrix du
-- fabricant : le code encodé sur la boîte n'est pas celui du catalogue. C'est
-- `produit_code_alias` (migration V2.1.45) qui mémorise cet écart, et c'est
-- l'étiquette interne du lot 3 qui réencode le code du catalogue, avec le lot
-- et la péremption, dans un GS1 DataMatrix que l'application sait relire.
-- Voir docs/PLAN-DATAMATRIX-MARCHE-CI-V2.md.
--
-- Sans cette table remplie, l'onglet « Codes alternatifs » de la fiche produit
-- est vide, la modale de résolution d'un code inconnu n'a aucun précédent à
-- montrer, et rien ne justifie à l'écran qu'on imprime une étiquette interne.
--
-- PIÈGE DE LONGUEUR : `code` est un varchar(20). Un code interne de grossiste
-- s'y tient, la trame DataMatrix complète NON — et ce n'est pas elle qu'on
-- mémorise, seulement le code produit qu'elle contient.
--
-- UNICITÉ : (produit_id, code), pas (code). Deux produits peuvent donc porter
-- le MÊME code, et c'est voulu : chez les grossistes ivoiriens deux
-- conditionnements d'une même molécule se partagent parfois un EAN. L'écran
-- demande alors lequel est le bon — cas nommé plus bas.
--
-- Les cas nommés s'ancrent sur des LIBELLÉS (produits de tête garantis par
-- 03c_produits_bdpm.sql), jamais sur des identifiants : ceux-ci ne sont pas
-- stables d'un chargement à l'autre.
--
-- La table est vidée par 00_reset (elle n'est pas dans la liste conservée).
-- ============================================================================

\i _header.sql

\echo '>> 27_codes_alias : codes alternatifs des boîtes (DataMatrix marché CI)'

-- La table vient de la migration V2.1.45, que le backend applique à son démarrage : un
-- chargement lancé avant lui ne doit pas s'arrêter ici (ON_ERROR_STOP laisserait une base à
-- moitié chargée), il saute simplement les codes alternatifs.
SELECT to_regclass('produit_code_alias') IS NOT NULL AS table_alias \gset
\if :table_alias

-- ---------------------------------------------------------------------------
-- 0. Clé de contrôle GS1
--
-- Les codes sont écrits par leur CORPS, sans clé, et la clé est calculée :
-- les exemples de la v1 du plan portaient des clés fausses, qu'`isValidEan13`
-- rejetait. Pondération 3-1 depuis le dernier chiffre du corps, puis
-- complément à la dizaine.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION pg_temp.cle_gs1(corps text) RETURNS text
    LANGUAGE sql IMMUTABLE AS $$
    SELECT ((10 - (sum(substring(corps FROM i FOR 1)::int
                       * CASE WHEN (length(corps) - i) % 2 = 0 THEN 3 ELSE 1 END)::int % 10)) % 10)::text
      FROM generate_series(1, length(corps)) AS g(i)
$$;

CREATE OR REPLACE FUNCTION pg_temp.code_gs1(corps text) RETURNS text
    LANGUAGE sql IMMUTABLE AS $$
    SELECT corps || pg_temp.cle_gs1(corps)
$$;

-- ---------------------------------------------------------------------------
-- 1. Cas nommés — un par situation que le plan décrit
--
-- Chaque ligne vise UN produit, par le préfixe de son libellé. Le commentaire
-- dit ce que le cas démontre à l'écran ; la recette (plan §10) s'y réfère.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_alias_nomme (
    ancre       text,
    code        text,
    code_type   text,
    source      text,
    created_by  text,
    jours       int,     -- ancienneté de la mémorisation, en jours
    propos      text
);

INSERT INTO tmp_alias_nomme (ancre, code, code_type, source, created_by, jours, propos) VALUES
    -- Chiffre indicateur 1 : extractEan13FromGtin renvoie null, le GTIN-14 brut ne correspond
    -- à rien en base. Sans alias, ce scan ne résout jamais rien.
    ('DOLIPRANE ADULTES 1000 mg', pg_temp.code_gs1('1735316300603'), 'GTIN_14', 'MANUAL', 'kobys', 42,
     'GTIN-14 du fabricant, indicateur non nul : aucun EAN-13 ne s''en extrait'),
    -- Boîte conditionnée en Côte d'Ivoire : préfixe GS1 618 (et non 611, qui est le Maroc).
    ('EFFERALGAN 500 mg', pg_temp.code_gs1('618123456789'), 'EAN_13', 'MANUAL', 'kkone', 31,
     'EAN-13 d''une boîte conditionnée en CI, absent de la fiche fournisseur'),
    -- Le cas qui a motivé le composant rawCode : hors de toute nomenclature, parse() le
    -- rejetait avant même la recherche.
    ('TRAMADOL ARROW L.P. 150 mg', 'GROS/A-7788', 'CUSTOM', 'MANUAL', 'atraore', 23,
     'code interne d''un grossiste, hors nomenclature : remonté en code brut'),
    -- Produit d'import français : son CIP-13 est sur la boîte, mais le grossiste local ne l'a
    -- pas saisi. Reprise d'un fichier fournisseur, d'où la source IMPORT.
    ('METFORMINE VIATRIS 1000 mg', pg_temp.code_gs1('340093339993'), 'CIP_13', 'IMPORT', 'admin', 97,
     'CIP-13 d''un produit d''import, repris d''un fichier fournisseur'),
    -- Deux produits, UN SEUL code : la modale doit proposer les deux et laisser trancher.
    ('AMOXICILLINE TEVA 1 g', pg_temp.code_gs1('618987654321'), 'EAN_13', 'MANUAL', 'mdiallo', 12,
     'EAN partagé par deux conditionnements : désambiguïsation à l''écran (1/2)'),
    ('AMOXICILLINE ZENTIVA LAB 1 g', pg_temp.code_gs1('618987654321'), 'EAN_13', 'MANUAL', 'mdiallo', 12,
     'EAN partagé par deux conditionnements : désambiguïsation à l''écran (2/2)'),
    -- Code court d'échantillon : huit chiffres, détectés comme EAN-8 et non comme une trame GS1.
    ('AUGMENTIN 1 g/125 mg', pg_temp.code_gs1('9638507'), 'EAN_8', 'MANUAL', 'ybrou', 58,
     'EAN-8 d''une boîte d''échantillon');

INSERT INTO produit_code_alias (produit_id, code, code_type, source, created_at, created_by)
SELECT p.id, n.code, n.code_type, n.source,
       (CURRENT_DATE - n.jours)::timestamp + time '09:30',
       n.created_by
  FROM tmp_alias_nomme n
  JOIN LATERAL (
      SELECT p.id
        FROM produit p
       WHERE p.libelle LIKE n.ancre || '%'
       ORDER BY p.id
       LIMIT 1
  ) p ON true
ON CONFLICT (produit_id, code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Volume — ce qu'une officine accumule en quelques mois de réceptions
--
-- Trois familles, sur trois fractions disjointes du catalogue suivi par lot,
-- pour qu'un produit ne porte pas trois alias à la fois sans raison.
--
-- La date de mémorisation est répartie sur la fenêtre récente : l'onglet
-- « Codes alternatifs » trie par date décroissante, une date unique le
-- rendrait muet.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_alias_volume AS
WITH suivis AS (
    SELECT p.id AS produit_id, fp.code_ean, row_number() OVER (ORDER BY p.id) AS rang
      FROM produit p
      JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
     WHERE p.gestion_lot
       AND p.status = 'ENABLE'
       AND fp.code_ean ~ '^\d{13}$'
)
-- GTIN-14 du carton : même code article, chiffre indicateur à 1. C'est le cas le plus
-- fréquent — le fabricant encode le niveau supérieur de conditionnement.
SELECT produit_id,
       pg_temp.code_gs1('1' || substring(code_ean FROM 1 FOR 12)) AS code,
       'GTIN_14' AS code_type,
       'MANUAL'  AS source,
       ((rang * 7) % 170)::int AS jours
  FROM suivis WHERE rang % 7 = 0
UNION ALL
-- Code interne du grossiste, alphanumérique : seize caractères au plus, le champ en tient vingt.
SELECT produit_id,
       'GR' || lpad(produit_id::text, 5, '0') || '-CI',
       'CUSTOM',
       'MANUAL',
       ((rang * 11) % 170)::int
  FROM suivis WHERE rang % 11 = 0
UNION ALL
-- EAN-13 d'un reconditionnement local, préfixe GS1 Côte d'Ivoire.
SELECT produit_id,
       pg_temp.code_gs1('618' || lpad(produit_id::text, 9, '0')),
       'EAN_13',
       'IMPORT',
       ((rang * 13) % 170)::int
  FROM suivis WHERE rang % 13 = 0;

INSERT INTO produit_code_alias (produit_id, code, code_type, source, created_at, created_by)
SELECT v.produit_id, v.code, v.code_type, v.source,
       (CURRENT_DATE - v.jours)::timestamp + time '08:00' + (v.produit_id % 480) * interval '1 minute',
       -- Les codes appris au scan le sont par qui réceptionne ; les imports, par l'administrateur.
       CASE WHEN v.source = 'IMPORT' THEN 'admin'
            ELSE (ARRAY['kobys', 'kkone', 'atraore', 'mdiallo', 'ybrou'])[1 + v.produit_id % 5] END
  FROM tmp_alias_volume v
ON CONFLICT (produit_id, code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. Récapitulatif — où aller imprimer l'étiquette
--
-- L'étiquette interne n'a de sens qu'avec un lot : le script nomme donc les
-- deux points d'entrée, calculés sur les données réellement chargées plutôt
-- que supposés.
-- ---------------------------------------------------------------------------
\echo ''
\echo '   cas nommés (fiche produit > onglet « Codes alternatifs ») :'
SELECT p.libelle, a.code, a.code_type, a.source, n.propos
  FROM tmp_alias_nomme n
  JOIN produit_code_alias a ON a.code = n.code
  JOIN produit p ON p.id = a.produit_id AND p.libelle LIKE n.ancre || '%'
 ORDER BY p.libelle;

\echo ''
\echo '   codes alternatifs par nature :'
SELECT code_type, source, count(*) AS codes
  FROM produit_code_alias
 GROUP BY code_type, source
 ORDER BY code_type, source;

\echo ''
\echo '   étiquette « lot » depuis la FICHE PRODUIT (planche courte, à préférer) :'
SELECT p.libelle, fp.code_cip AS code_imprime, l.num_lot, l.expiry_date, l.current_quantity
  FROM produit p
  JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
  JOIN lot l ON l.produit_id = p.id
 WHERE p.libelle LIKE 'METFORMINE VIATRIS 1000 mg%'
   AND l.statut = 'AVAILABLE'
   AND l.current_quantity > 0
   AND l.expiry_date > CURRENT_DATE
 ORDER BY l.expiry_date
 LIMIT 5;

\echo ''
\echo '   étiquette « lot » depuis un BON DE LIVRAISON (une étiquette par lot reçu) :'
SELECT c.receipt_reference, c.order_date, count(l.id) AS etiquettes
  FROM commande c
  JOIN order_line ol ON ol.commande_id = c.id AND ol.commande_order_date = c.order_date
  JOIN lot l ON l.order_line_id = ol.id
  JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
 WHERE c.order_status = 'CLOSED'
   AND coalesce(fp.code_cip, '') <> ''
   AND l.num_lot IS NOT NULL
   AND l.expiry_date IS NOT NULL
 GROUP BY c.receipt_reference, c.order_date
HAVING count(l.id) BETWEEN 2 AND 12
 ORDER BY count(l.id), c.order_date DESC
 LIMIT 3;

\else
\echo '   table produit_code_alias absente (migration V2.1.45 non appliquée) : codes alternatifs ignorés'
\endif
