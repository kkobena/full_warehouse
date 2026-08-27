-- ============================================================================
-- En-tête commun aux scripts de données de démonstration.
-- Inclus par chaque script via \i, pour qu'ils restent exécutables isolément.
--
-- Nomenclature de la démo :
--     rôle    pharma_smart
--     base    pharma_smart_demo
--     schéma  pharma_smart
--
-- Le nom du schéma n'est jamais codé en dur dans les scripts. Il reste
-- surchargeable pour les installations historiques qui en utilisent un autre
-- (variable d'environnement PHARMA_DB_SCHEMA côté application) :
--     psql -v schema=<autre_schema> -f <script>.sql
-- ============================================================================

\if :{?schema}
\else
  \set schema pharma_smart
\endif

SET search_path TO :"schema";
