-- Pose une promotion VALIDÉE et CUMULABLE « ESSAI E2E » : -10 % sur PANSEMENT ADHESIF B/20, en cours
-- depuis hier et pour un mois, dans le magasin du compte administrateur (parcours RFD-30). Cumulable :
-- elle s'applique aussi après la remise produit choisie sur la vente, sur le prix déjà remisé.
-- Vendu 1 240, acheté au plus 800 : même remise produit comprise, l'article reste au-dessus de son
-- prix d'achat, et le comptoir le garantit de toute façon (plancher sur la remise cumulée).
--
-- Le nettoyage est celui des autres parcours : scripts/essai-promotion/nettoyer.sql.
SET search_path = pharma_smart, public;

DELETE FROM promotion WHERE libelle LIKE 'ESSAI E2E%';

WITH promo AS (
    INSERT INTO promotion (code, libelle, type, valeur, date_debut, date_fin, statut, cumulable)
    VALUES ('ESSAI-E2E', 'ESSAI E2E Pansements -10 % cumulable', 'PROMO_POURCENTAGE', 10,
            CURRENT_DATE - 1, CURRENT_DATE + 30, 'VALIDEE', true)
    RETURNING id
), magasin AS (
    INSERT INTO promotion_magasin (promotion_id, magasin_id)
    SELECT promo.id, u.magasin_id FROM promo, app_user u WHERE u.login = 'admin'
    RETURNING promotion_id
)
INSERT INTO promotion_produit (promotion_id, produit_id)
SELECT magasin.promotion_id, p.id
FROM magasin, produit p
WHERE p.libelle = 'PANSEMENT ADHESIF B/20';

DO $$
BEGIN
    IF (SELECT count(*) FROM promotion_produit pp JOIN promotion p ON p.id = pp.promotion_id
        WHERE p.libelle LIKE 'ESSAI E2E%') <> 1 THEN
        RAISE EXCEPTION 'promotion d''essai incomplète : article ou magasin introuvable';
    END IF;
END $$;
