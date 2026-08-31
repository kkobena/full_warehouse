-- Première instruction du fichier, avant tout caractère non ASCII.
--
-- Les scripts sont en UTF-8, mais psql déduit son encodage client de la page de
-- code de la console : sous Windows c'est WIN1252. Le serveur tente alors une
-- conversion WIN1252 -> UTF8 et échoue sur tout octet indéfini en WIN1252 —
-- « le caractère dont la séquence d'octets est 0x90 [...] n'a pas d'équivalent ».
-- Les accents français, eux, ne provoquent pas d'erreur : ils se dégradent
-- silencieusement en mojibake, ce qui est plus insidieux encore.
\encoding UTF8

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
