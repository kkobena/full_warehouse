-- ============================================================================
-- 03b_substituts.sql — Catalogue de substitution
--
-- substitut(produit_id, substitut_id, type_substitut) — unique sur le couple,
-- type contraint par CHECK à GENERIQUE ou THERAPEUTIQUE.
--
-- SENS DE LA RELATION : la table est écrite dans un sens
-- (existsByProduitAndSubstitut) mais LUE DANS LES DEUX
-- (findAllByProduitId et findAllBySubstitutId). On ne pose donc QU'UNE ligne
-- par paire, dans un sens canonique (identifiant le plus petit d'abord).
-- En poser deux ferait apparaître chaque partenaire en double à la lecture.
--
-- En production, la table s'alimente au fil de l'eau : PharmaMlHttpClientService
-- y crée une ligne quand une substitution proposée par le grossiste est
-- acceptée. Elle est donc naturellement CREUSE — un catalogue exhaustif de
-- toutes les équivalences théoriques ne ressemblerait pas à une base réelle.
--
-- Ne pas confondre avec substitution_proposee, qui trace les propositions de
-- remplacement d'un grossiste sur une commande : autre concern, autre table.
-- ============================================================================

\i _header.sql

\echo '>> 03b_substituts : catalogue de substitution'

-- ---------------------------------------------------------------------------
-- Décomposition du libellé
--
-- Les libellés sont construits « BASE DOSAGE CONDITIONNEMENT ». La base peut
-- contenir des espaces (ACIDE FOLIQUE, PARACETAMOL GE) : on ne peut donc pas
-- découper au premier espace. On s'appuie sur le dosage, seul jeton de forme
-- reconnaissable — un nombre suivi de MG, G, CH ou ML.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_sub_prod AS
SELECT
    p.id,
    p.dci_id,
    substring(p.libelle from '\m[0-9]+(?:MG|G|CH|ML)\M')                  AS dosage,
    regexp_replace(p.libelle, '\s+\m[0-9]+(?:MG|G|CH|ML)\M.*$', '')       AS base
FROM produit p
WHERE p.status = 'ENABLE';

CREATE INDEX ON tmp_sub_prod (dci_id, dosage);

-- ---------------------------------------------------------------------------
-- 1. Substitutions GÉNÉRIQUES
--
-- Même molécule ET même dosage, sous une marque différente. Les trois
-- conditions comptent :
--   * même DCI seule ne suffit pas — PARACETAMOL 100MG ne remplace pas
--     PARACETAMOL 1G ;
--   * base différente évite d'apparier deux conditionnements du même produit
--     (B/10 et B/20), qui ne sont pas une substitution mais un choix de boîte.
--
-- Résultat attendu : PARACETAMOL, DOLIPRANE, EFFERALGAN et PARACETAMOL GE en
-- 500 MG forment un groupe substituable.
-- ---------------------------------------------------------------------------
INSERT INTO substitut (produit_id, substitut_id, type_substitut)
SELECT a.id, b.id, 'GENERIQUE'
FROM tmp_sub_prod a
JOIN tmp_sub_prod b
  ON b.dci_id = a.dci_id
 AND b.dosage = a.dosage
 AND b.base  <> a.base
 AND b.id     > a.id          -- sens canonique : une seule ligne par paire
WHERE a.dci_id IS NOT NULL
  AND a.dosage IS NOT NULL
ON CONFLICT (produit_id, substitut_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Substitutions THÉRAPEUTIQUES
--
-- Molécules différentes d'une même classe — ce qu'un pharmacien propose quand
-- la molécule prescrite est en rupture. Le rayon commercial sert de proxy de
-- classe thérapeutique : c'est lui qui regroupe antalgiques, antibiotiques,
-- cardiologie, etc.
--
-- Volontairement CREUX : un produit sur sept, et un seul substitut chacun.
-- Un catalogue exhaustif apparierait tous les antibiotiques entre eux, ce
-- qu'aucune officine ne saisit.
-- ---------------------------------------------------------------------------
INSERT INTO substitut (produit_id, substitut_id, type_substitut)
SELECT DISTINCT ON (v.a_id) v.a_id, v.b_id, 'THERAPEUTIQUE'
FROM (
    SELECT
        a.id AS a_id,
        b.id AS b_id,
        row_number() OVER (PARTITION BY a.id ORDER BY b.id) AS rang
    FROM tmp_sub_prod a
    JOIN rayon_produit rpa ON rpa.produit_id = a.id
    JOIN tmp_sub_prod b    ON b.id > a.id
    JOIN rayon_produit rpb ON rpb.produit_id = b.id AND rpb.rayon_id = rpa.rayon_id
    JOIN rayon r           ON r.id = rpa.rayon_id AND r.code <> 'SANS'
    WHERE a.dci_id IS NOT NULL
      AND b.dci_id IS NOT NULL
      AND b.dci_id <> a.dci_id       -- molécule différente : c'est ce qui
                                     -- distingue le thérapeutique du générique
      AND a.id % 7 = 0
) v
WHERE v.rang = 1
ON CONFLICT (produit_id, substitut_id) DO NOTHING;

DROP TABLE tmp_sub_prod;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_total int; v_gen int; v_ther int;
    v_reflexif int; v_double int; v_incoh int;
BEGIN
    SELECT count(*) INTO v_total FROM substitut;
    SELECT count(*) INTO v_gen   FROM substitut WHERE type_substitut = 'GENERIQUE';
    SELECT count(*) INTO v_ther  FROM substitut WHERE type_substitut = 'THERAPEUTIQUE';

    -- Un produit ne se substitue pas à lui-même.
    SELECT count(*) INTO v_reflexif FROM substitut WHERE produit_id = substitut_id;

    -- La lecture se faisant dans les deux sens, stocker A→B et B→A ferait
    -- apparaître le partenaire en double.
    SELECT count(*) INTO v_double FROM substitut s1
      JOIN substitut s2 ON s2.produit_id = s1.substitut_id
                       AND s2.substitut_id = s1.produit_id;

    -- Le type doit correspondre à la réalité du lien : même molécule pour un
    -- générique, molécule différente pour un thérapeutique.
    SELECT count(*) INTO v_incoh FROM substitut s
      JOIN produit p ON p.id = s.produit_id
      JOIN produit q ON q.id = s.substitut_id
     WHERE (s.type_substitut = 'GENERIQUE'     AND p.dci_id IS DISTINCT FROM q.dci_id)
        OR (s.type_substitut = 'THERAPEUTIQUE' AND p.dci_id IS NOT DISTINCT FROM q.dci_id);

    IF v_gen < 100 THEN RAISE EXCEPTION 'Substitutions génériques : % (attendu >= 100)', v_gen; END IF;
    IF v_ther < 20 THEN RAISE EXCEPTION 'Substitutions thérapeutiques : % (attendu >= 20)', v_ther; END IF;
    IF v_reflexif > 0 THEN RAISE EXCEPTION '% substitution(s) réflexive(s)', v_reflexif; END IF;
    IF v_double > 0 THEN RAISE EXCEPTION '% paire(s) stockée(s) dans les deux sens', v_double; END IF;
    IF v_incoh > 0 THEN RAISE EXCEPTION '% substitution(s) dont le type contredit la molécule', v_incoh; END IF;

    RAISE NOTICE '% substitutions (% génériques, % thérapeutiques).', v_total, v_gen, v_ther;
END $$;

\echo '<< 03b_substituts : terminé'
