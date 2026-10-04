-- ============================================================================
-- 17b_favoris.sql — Grille de produits favoris du comptoir
--
-- Sans eux, la rangée de tuiles de l'écran de vente comptant n'apparaît pas : elle n'occupe aucune place tant que rien n'est épinglé.
-- Les captures et les parcours de démonstration n'auraient donc jamais rien à montrer de cette fonction
-- (docs/PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md §3).
--
-- Huit tuiles, choisies dans les données plutôt que par identifiant (les identifiants ne sont pas stables d'un chargement à l'autre) :
--   - les SEPT produits sans ordonnance (statut légal SANS_LISTE) les plus vendus au comptant : c'est ce qu'un caissier épinglerait ;
--   - UN produit sans ordonnance sans stock au rayon, en dernière position : la tuile affiche « Rupture », et un clic ouvre la
--     proposition d'équivalents (docs/…§4) — de quoi montrer ce cas sans rien casser dans les ventes.
--
-- Après 16b : le stock est stabilisé, la rupture choisie le reste. La table est vidée par 00_reset (elle n'est pas dans la liste conservée).
-- ============================================================================

\i _header.sql

\echo '>> 17b_favoris : grille de produits favoris du comptoir'

-- La table vient de la migration V2.1.24, que le backend applique à son démarrage : un chargement lancé avant lui ne doit pas
-- s'arrêter ici (ON_ERROR_STOP laisserait une base à moitié chargée), il saute simplement les favoris.
SELECT to_regclass('produit_favori') IS NOT NULL AS table_favoris \gset
\if :table_favoris
WITH frequents AS (
    SELECT sl.produit_id, count(DISTINCT s.id) AS nb_ventes
      FROM sales s
           JOIN sales_line sl ON sl.sales_id = s.id
           JOIN produit p ON p.id = sl.produit_id
     WHERE s.dtype = 'CashSale'
       AND s.statut = 'CLOSED'
       AND s.canceled = FALSE
       AND s.nature_vente = 'COMPTANT'
       AND p.status = 'ENABLE'
       AND p.statut_legal = 'SANS_LISTE'
     GROUP BY sl.produit_id
     ORDER BY nb_ventes DESC, sl.produit_id
     LIMIT 7
),
rupture AS (
    -- Un produit qui a une ligne de stock au rayon, mais vide : « en rupture », pas « jamais référencé ».
    SELECT p.id AS produit_id
      FROM produit p
     WHERE p.status = 'ENABLE'
       AND p.statut_legal = 'SANS_LISTE'
       AND EXISTS (SELECT 1
                     FROM stock_produit sp JOIN storage st ON st.id = sp.storage_id
                    WHERE sp.produit_id = p.id AND st.storage_type = 'PRINCIPAL')
       AND NOT EXISTS (SELECT 1
                         FROM stock_produit sp JOIN storage st ON st.id = sp.storage_id
                        WHERE sp.produit_id = p.id AND st.storage_type = 'PRINCIPAL' AND sp.qty_stock + sp.qty_ug > 0)
     ORDER BY p.id
     LIMIT 1
)
INSERT INTO produit_favori (magasin_id, produit_id, ordre)
SELECT (SELECT min(id) FROM magasin),
       x.produit_id,
       row_number() OVER (ORDER BY x.groupe, x.nb_ventes DESC, x.produit_id)
  FROM (SELECT produit_id, nb_ventes, 1 AS groupe FROM frequents
        UNION ALL
        SELECT produit_id, 0, 2 FROM rupture) x;

\echo '   favoris posés :'
SELECT f.ordre, p.libelle
  FROM produit_favori f JOIN produit p ON p.id = f.produit_id
 ORDER BY f.ordre;
\else
\echo '   table produit_favori absente (migration V2.1.24 non appliquée) : favoris ignorés'
\endif
