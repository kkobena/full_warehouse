\encoding UTF8
-- Contrôles après chargement : les comptes doivent correspondre à ceux de generer_sql.py.
\if :{?schema}
\else
  \set schema pharma_smart
\endif
SET search_path TO :"schema";

SELECT 'ref_groupe_generique' AS "table", count(*) FROM ref_groupe_generique
UNION ALL SELECT 'ref_substance', count(*) FROM ref_substance
UNION ALL SELECT 'ref_dci', count(*) FROM ref_dci
UNION ALL SELECT 'ref_specialite', count(*) FROM ref_specialite
UNION ALL SELECT 'ref_specialite_composition', count(*) FROM ref_specialite_composition
UNION ALL SELECT 'ref_specialite_rcp', count(*) FROM ref_specialite_rcp;

\echo 'Spécialités sans composition (attendu : quelques-unes, ex. produits de contraste) :'
SELECT count(*) FROM ref_specialite s
WHERE NOT EXISTS (SELECT 1 FROM ref_specialite_composition c WHERE c.cis = s.cis);

\echo 'Groupes génériques sans princeps (attendu : peu, princeps retiré du marché) :'
SELECT count(*) FROM ref_groupe_generique g
WHERE NOT EXISTS (SELECT 1 FROM ref_specialite s WHERE s.groupe_generique_id = g.id AND s.type_generique = 'PRINCEPS');

\echo 'Exemple de substitution : substituts de la première spécialité commercialisée du premier groupe :'
SELECT * FROM v_ref_substitut
WHERE cis = (SELECT min(cis) FROM ref_specialite WHERE groupe_generique_id = (SELECT min(id) FROM ref_groupe_generique))
LIMIT 5;
