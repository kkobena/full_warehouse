-- Pose une version FICTIVE « ESSAI-E2E » du référentiel d'interactions, pour les parcours e2e du
-- contrôle d'ordonnance (VTE-73, regression/ordonnance-controle-et-suivi). Le jeu de démonstration
-- ne charge aucune interaction : sans cette version, le contrôle n'a rien à signaler.
--
-- Une seule règle : AMOXICILLINE x IBUPROFÈNE en contre-indication (CI). Elle est FAUSSE sur le plan
-- médical — c'est précisément pourquoi la source s'appelle ESSAI-E2E et s'affiche dans la modale.
--
-- Chaque côté passe par une CLASSE plutôt que par une molécule : la BDPM décline une même substance
-- sous plusieurs libellés (sel, forme hydratée…), et le produit du panier peut pointer vers l'un ou
-- l'autre. La classe les regroupe tous.
--
-- La version est PUBLIÉE : le contrôle ne lit que les versions publiées.
--
-- Défait par nettoyer.sql (la suppression de la version emporte classes, rattachements et
-- interaction par ON DELETE CASCADE). Toujours rejouer nettoyer.sql AVANT : la contrainte
-- (source, version) refuserait une seconde pose.
--
--   docker exec -i pharma_smart_postgres psql -U postgres -d pharma_smart -v ON_ERROR_STOP=1 < scripts/essai-ordonnance/preparer.sql
SET search_path = pharma_smart, public;

WITH version AS (
  INSERT INTO referentiel_interaction_version (source, version, importe_par, publie, publie_le, publie_par, commentaire)
  VALUES ('ESSAI-E2E', '1', 'e2e', TRUE, now() AT TIME ZONE 'UTC', 'e2e',
          'Version fictive des parcours e2e — ne pas publier en production')
  RETURNING id
),
classes AS (
  INSERT INTO classe_interaction (version_id, libelle)
  SELECT version.id, c.libelle
    FROM version, (VALUES ('ESSAI AMOXICILLINE'), ('ESSAI IBUPROFENE')) AS c (libelle)
  RETURNING id, libelle
),
rattachements AS (
  INSERT INTO dci_classe (classe_id, ref_dci_id)
  SELECT classes.id, d.id
    FROM classes
    JOIN ref_dci d ON d.libelle_normalise LIKE '%' || ref_normaliser(
           CASE classes.libelle WHEN 'ESSAI AMOXICILLINE' THEN 'AMOXICILLINE' ELSE 'IBUPROFÈNE' END) || '%'
  RETURNING classe_id
)
INSERT INTO interaction (version_id, a_classe_id, b_classe_id, niveau, mecanisme, conduite)
SELECT version.id, a.id, b.id, 'CI',
       'Interaction fictive posée par les parcours e2e.',
       'Ne pas associer, sauf motif tracé.'
  FROM version
  JOIN classes a ON a.libelle = 'ESSAI AMOXICILLINE'
  JOIN classes b ON b.libelle = 'ESSAI IBUPROFENE'
 -- Rattachements matérialisés avant l'interaction : une classe vide ne déclencherait rien.
 WHERE (SELECT count(*) FROM rattachements) > 0;

-- Garde-fou : une classe sans molécule rendrait le parcours muet, sans dire pourquoi.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
      FROM classe_interaction c
      JOIN referentiel_interaction_version v ON v.id = c.version_id
     WHERE v.source = 'ESSAI-E2E'
       AND NOT EXISTS (SELECT 1 FROM dci_classe dc WHERE dc.classe_id = c.id)
  ) THEN
    RAISE EXCEPTION 'preparer.sql : une classe d''essai ne rattache aucune DCI (ref_dci vide ou libellé inattendu)';
  END IF;
END $$;
