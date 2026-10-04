-- Suggestions pour la grille des favoris du comptoir (docs/PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md §3) :
-- les produits les plus vendus au comptoir, sans ordonnance, sur les 30 derniers jours, par magasin.
--
-- Ce n'est PAS la grille : la grille est la table produit_favori, choisie et ordonnée à la main. Cette vue ne fait que
-- proposer des produits à épingler (écran « Favoris du comptoir » du catalogue), pour ne pas partir d'une grille vide.
--
--  - comptoir : vente au comptant (CashSale, nature COMPTANT) ; l'assurance et le carnet relèvent d'une ordonnance, pas du conseil ;
--  - sans ordonnance : statut légal SANS_LISTE ; un produit au statut inconnu n'est pas proposé ;
--  - fréquence : nombre de ventes (tickets) distinctes, puis quantité vendue ; une grosse quantité sur un seul ticket
--    ne fait pas un produit fréquent.
-- On garde les 50 premiers de chaque magasin : au-delà, personne n'épingle.
-- Rafraîchie par MaterializedViewRefreshService (palier 2) ; l'index unique permet le rafraîchissement sans verrou.
CREATE MATERIALIZED VIEW mv_produits_frequents_comptoir AS
SELECT t.magasin_id, t.produit_id, t.nb_ventes, t.qte_vendue, t.derniere_vente, t.rang
FROM (SELECT s.magasin_id,
             sl.produit_id,
             count(DISTINCT s.id)                      AS nb_ventes,
             sum(sl.quantity_requested)                AS qte_vendue,
             max(s.sale_date)                          AS derniere_vente,
             row_number() OVER (PARTITION BY s.magasin_id
                                ORDER BY count(DISTINCT s.id) DESC, sum(sl.quantity_requested) DESC, sl.produit_id) AS rang
      FROM sales s
             JOIN sales_line sl ON sl.sales_id = s.id
             JOIN produit p ON p.id = sl.produit_id
      WHERE s.dtype = 'CashSale'
        AND s.statut = 'CLOSED'
        AND s.canceled = FALSE
        AND s.ca = 'CA'
        AND s.nature_vente = 'COMPTANT'
        AND s.sale_date >= CURRENT_DATE - 30
        AND p.status = 'ENABLE'
        AND p.statut_legal = 'SANS_LISTE'
      GROUP BY s.magasin_id, sl.produit_id) t
WHERE t.rang <= 50;

CREATE UNIQUE INDEX mv_produits_frequents_comptoir_uk ON mv_produits_frequents_comptoir (magasin_id, produit_id);
