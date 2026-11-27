-- Retire les promotions « ESSAI E2E » posées par `preparer.sql` ou créées par le parcours RFD-22.
-- Une promotion d'essai laissée active changerait les prix d'une démonstration (plan des
-- promotions, §7 point 15) : les parcours rejouent ce script à la fin, puis vérifient qu'il ne
-- reste rien.
--
-- Les paniers des parcours sont abandonnés avant (l'abandon supprime la vente et ses lignes) :
-- aucune ligne de vente ne doit plus pointer vers une promotion d'essai. Si une vente clôturée en
-- portait une, la suppression échouerait sur la clé étrangère — et c'est voulu : on ne réécrit pas
-- l'historique des ventes pour ranger un essai.
--
--   docker exec -i pharma_smart_postgres psql -U postgres -d pharma_smart -v ON_ERROR_STOP=1 < scripts/essai-promotion/nettoyer.sql
SET search_path = pharma_smart, public;

-- Un parcours interrompu peut laisser un panier ouvert sur l'article : on détache la promotion de
-- ce seul panier (jamais d'une vente clôturée), remise comprise. Le panier, abandonné, sera
-- défait par le parcours suivant (`assurerPanierVide`).
UPDATE sales_line sl
SET promotion_id = NULL, promotion_libelle = NULL, quantite_promo = NULL, discount_amount = 0, taux_remise = 0
FROM sales s
WHERE s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
  AND s.statut <> 'CLOSED'
  AND sl.promotion_id IN (SELECT id FROM promotion WHERE libelle LIKE 'ESSAI E2E%');

-- promotion_magasin et promotion_produit suivent (ON DELETE CASCADE).
DELETE FROM promotion WHERE libelle LIKE 'ESSAI E2E%';
