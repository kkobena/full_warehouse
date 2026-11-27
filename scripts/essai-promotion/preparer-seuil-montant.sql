-- Pose une remise de panier VALIDÉE « ESSAI E2E » : -10 % sur PANSEMENT ADHESIF B/20 et SPARADRAP
-- HYPOALLERGENIQUE 5M, dès un achat de deux pansements plus 1 F, en cours depuis hier et pour un mois,
-- dans le magasin du compte administrateur (parcours RFD-29).
--
-- Le seuil se calcule sur le prix du pansement, et non en dur : deux pansements ne l'atteignent pas,
-- un sparadrap de plus le franchit, quels que soient les prix du jeu de démo. À -10 %, les deux
-- articles restent au-dessus de leur prix d'achat (pansement vendu 1 240, acheté 800).
--
-- Le nettoyage est celui des autres parcours : scripts/essai-promotion/nettoyer.sql.
SET search_path = pharma_smart, public;

DELETE FROM promotion WHERE libelle LIKE 'ESSAI E2E%';

WITH promo AS (
    INSERT INTO promotion (code, libelle, type, valeur, seuil_montant, date_debut, date_fin, statut)
    SELECT 'ESSAI-E2E', 'ESSAI E2E Soins -10 % dès 2 pansements', 'PROMO_SEUIL_MONTANT', 10,
           2 * p.regular_unit_price + 1, CURRENT_DATE - 1, CURRENT_DATE + 30, 'VALIDEE'
      FROM produit p
     WHERE p.libelle = 'PANSEMENT ADHESIF B/20'
    RETURNING id
), magasin AS (
    INSERT INTO promotion_magasin (promotion_id, magasin_id)
    SELECT promo.id, u.magasin_id FROM promo, app_user u WHERE u.login = 'admin'
    RETURNING promotion_id
)
INSERT INTO promotion_produit (promotion_id, produit_id)
SELECT magasin.promotion_id, p.id
FROM magasin, produit p
WHERE p.libelle IN ('PANSEMENT ADHESIF B/20', 'SPARADRAP HYPOALLERGENIQUE 5M');

DO $$
BEGIN
    IF (SELECT count(*) FROM promotion_produit pp JOIN promotion p ON p.id = pp.promotion_id
        WHERE p.libelle LIKE 'ESSAI E2E%') <> 2 THEN
        RAISE EXCEPTION 'remise de panier d''essai incomplète : article ou magasin introuvable';
    END IF;
END $$;
