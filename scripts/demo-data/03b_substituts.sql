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

CREATE TEMP TABLE tmp_sub_prod AS
SELECT p.id, p.dci_id
FROM produit p
WHERE p.status = 'ENABLE' AND p.type_produit = 'PACKAGE';

CREATE INDEX ON tmp_sub_prod (dci_id);

-- ---------------------------------------------------------------------------
-- 1. Substitutions GÉNÉRIQUES
--
-- Elles viennent des groupes génériques du référentiel (ref_specialite.groupe_generique_id) :
-- un princeps et ses génériques ont même molécule et même dosage, par définition du groupe.
-- On n'apparie que le princeps avec chacun de ses génériques — jamais deux génériques entre
-- eux : le catalogue reste creux, comme en production, où il ne s'alimente que des
-- substitutions réellement acceptées.
-- ---------------------------------------------------------------------------
INSERT INTO substitut (produit_id, substitut_id, type_substitut)
SELECT DISTINCT least(a.id, b.id), greatest(a.id, b.id), 'GENERIQUE'
FROM produit a
JOIN ref_specialite sa ON sa.libelle = a.libelle AND sa.groupe_generique_id IS NOT NULL
JOIN ref_specialite sb ON sb.groupe_generique_id = sa.groupe_generique_id
                      AND sb.type_generique = 'GENERIQUE'
JOIN produit b ON b.libelle = sb.libelle AND b.id <> a.id
WHERE sa.type_generique = 'PRINCEPS'
  AND a.type_produit = 'PACKAGE' AND b.type_produit = 'PACKAGE'
  AND a.dci_id = b.dci_id
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
