-- Pose une offre VALIDÉE « ESSAI E2E » 2 achetés + 1 offert sur PANSEMENT ADHESIF B/20, en cours
-- depuis hier et pour un mois, dans le magasin du compte administrateur (parcours RFD-26).
-- quantite_min porte N (2), valeur M (1). Vendu 1 240, acheté au plus 800 : une unité d'un groupe
-- revient à 826, au-dessus du prix d'achat, comme le service l'exigerait.
--
-- Le nettoyage est celui des autres parcours : scripts/essai-promotion/nettoyer.sql.
SET search_path = pharma_smart, public;

DELETE FROM promotion WHERE libelle LIKE 'ESSAI E2E%';

WITH promo AS (
    INSERT INTO promotion (code, libelle, type, valeur, quantite_min, date_debut, date_fin, statut)
    VALUES ('ESSAI-E2E', 'ESSAI E2E Pansements 2+1', 'PROMO_OFFRE_NM', 1, 2,
            CURRENT_DATE - 1, CURRENT_DATE + 30, 'VALIDEE')
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
        RAISE EXCEPTION 'offre d''essai incomplète : article ou magasin introuvable';
    END IF;
END $$;
