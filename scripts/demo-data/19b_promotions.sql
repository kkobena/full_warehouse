\i _header.sql

-- ============================================================================
-- 19b_promotions.sql — Une promotion passée, et les ventes qu'elle a remisées
--
-- Le journal des remises (Gestion Courante ▸ Journal des remises, et l'onglet
-- Journal du module Promotions) s'ouvrait sans aucune ligne promue : le
-- chargeur de ventes pose des remises de grille, jamais de promotion. Les
-- onglets « Synthèse » et « Promotions » ne montraient donc que des zéros, et
-- la répartition d'une remise cumulée — la raison d'être du journal — restait
-- indémontrable.
--
-- CE QUE LE MODELE ATTEND, vérifié sur la base :
--
--   * `sales_line.promotion_id` désigne la promotion que le moteur a appliquée,
--     `promotion_libelle` en recopie le nom au jour de la vente et
--     `quantite_promo` compte les unités remisées.
--   * `sales_line.remise_promotion` (V2.1.68) est la PART de la promotion dans
--     `discount_amount` ; le reste revient à la grille du produit. Une ligne
--     promue où elle vaut zéro est lue comme entièrement promotionnelle : c'est
--     la règle des lignes antérieures à la colonne.
--   * L'état « en cours » ou « terminée » se lit sur les dates : le statut ne
--     porte que la décision humaine (VALIDEE).
--
-- LE CHOIX FAIT ICI : aucun montant ne bouge. On ne fabrique pas de remise, on
-- en ATTRIBUE une partie à la promotion, sur des lignes déjà remisées au
-- comptoir par la grille (09_ventes.sql : 5, 10 ou 15 %). Ventes, lignes,
-- encaissements et états restent donc exactement ceux du chargement :
--
--   * une ligne remisée à 10 % devient entièrement promotionnelle (−10 %) ;
--   * une ligne remisée à 15 % devient CUMULÉE : 10 % pour la promotion, les
--     5 % restants pour la grille ;
--   * une ligne à 5 % reste une remise produit seule.
--
-- La promotion s'est terminée la veille du chargement : elle ne change AUCUN
-- prix au comptoir, et un parcours qui vend n'en est pas affecté.
--
-- Rejouable : la promotion de démonstration est retirée, et ses lignes
-- rendues à la grille, avant d'être reposée.
-- ============================================================================

\echo '>> 19b_promotions : une promotion passée et ses lignes'

SET search_path = :schema, public;

-- Repartir propre.
UPDATE sales_line
   SET promotion_id = NULL, promotion_libelle = NULL, quantite_promo = NULL, remise_promotion = 0
 WHERE promotion_id IN (SELECT id FROM promotion WHERE code = 'DEMO-SOINS');
DELETE FROM promotion WHERE code = 'DEMO-SOINS';

-- Les lignes candidates : remisées à 10 % ou plus, sur une vente au comptant
-- clôturée du chiffre d'affaires, des quatre dernières semaines, d'un article à
-- prix libre (TVA 18 %, comme l'exigerait le formulaire d'une promotion).
CREATE TEMP TABLE tmp_promo_ligne ON COMMIT DROP AS
SELECT sl.id, sl.sale_date, sl.produit_id, s.magasin_id,
       sl.discount_amount::numeric / sl.sales_amount AS taux
  FROM sales_line sl
  JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
  JOIN produit p ON p.id = sl.produit_id
  JOIN tva t ON t.id = p.tva_id
 WHERE s.dtype = 'CashSale'
   AND s.statut = 'CLOSED'
   AND NOT s.canceled
   AND s.ca = 'CA'
   AND s.sale_date BETWEEN CURRENT_DATE - 28 AND CURRENT_DATE - 1
   AND s.magasin_id = (SELECT magasin_id FROM app_user WHERE login = 'admin')
   AND t.taux = 18
   AND sl.promotion_id IS NULL
   AND sl.sales_amount > 0
   AND sl.discount_amount::numeric / sl.sales_amount >= 0.095;

-- La liste d'articles d'une promotion est courte : les douze articles les plus
-- remisés sur la période.
CREATE TEMP TABLE tmp_promo_article ON COMMIT DROP AS
SELECT produit_id
  FROM tmp_promo_ligne
 GROUP BY produit_id
 ORDER BY count(*) DESC, produit_id
 LIMIT 12;

WITH promo AS (
    INSERT INTO promotion (code, libelle, type, valeur, date_debut, date_fin, statut, cumulable,
                           created_by_id, created_at, updated_at)
    SELECT 'DEMO-SOINS', 'Opération soins −10 %', 'PROMO_POURCENTAGE', 10,
           CURRENT_DATE - 28, CURRENT_DATE - 1, 'VALIDEE', true,
           u.id, now() - interval '30 days', now() - interval '30 days'
      FROM app_user u WHERE u.login = 'admin'
    RETURNING id
)
INSERT INTO promotion_magasin (promotion_id, magasin_id)
SELECT promo.id, u.magasin_id FROM promo, app_user u WHERE u.login = 'admin';

INSERT INTO promotion_produit (promotion_id, produit_id)
SELECT p.id, a.produit_id
  FROM promotion p, tmp_promo_article a
 WHERE p.code = 'DEMO-SOINS';

UPDATE sales_line sl
   SET promotion_id = p.id,
       promotion_libelle = p.libelle,
       quantite_promo = sl.quantity_sold,
       remise_promotion = CASE
           -- Remisée à 10 % : toute la remise est celle de la promotion.
           WHEN c.taux < 0.125 THEN sl.discount_amount
           -- Remisée à 15 % : 10 % par unité pour la promotion, le reste à la grille.
           ELSE sl.quantity_sold * round(sl.regular_unit_price * 0.10)::int
       END
  FROM tmp_promo_ligne c
  JOIN tmp_promo_article a ON a.produit_id = c.produit_id
  JOIN promotion p ON p.code = 'DEMO-SOINS'
 WHERE sl.id = c.id AND sl.sale_date = c.sale_date;

DO $$
DECLARE
    v_promues int;
    v_cumulees int;
BEGIN
    SELECT count(*), count(*) FILTER (WHERE sl.remise_promotion < sl.discount_amount)
      INTO v_promues, v_cumulees
      FROM sales_line sl JOIN promotion p ON p.id = sl.promotion_id
     WHERE p.code = 'DEMO-SOINS';
    RAISE NOTICE '   % ligne(s) promue(s), dont % cumulée(s) avec la grille', v_promues, v_cumulees;
END $$;

\echo '<< 19b_promotions : terminé'
