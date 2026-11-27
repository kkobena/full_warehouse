-- Pose un pack VALIDÉ « ESSAI E2E » : PANSEMENT ADHESIF B/20 + SPARADRAP HYPOALLERGENIQUE 5M à
-- 2 000 F, en cours depuis hier et pour un mois, dans le magasin du compte administrateur (parcours
-- RFD-27). Code-barres fixe, PK99999999 : le parcours le « scanne ». Les deux articles sont de la
-- parapharmacie (TVA 18 %), achetés 800 et 700 F : le pack reste au-dessus de leur prix d'achat.
--
-- Le nettoyage est celui des autres parcours : scripts/essai-promotion/nettoyer.sql.
SET search_path = pharma_smart, public;

DELETE FROM promotion WHERE libelle LIKE 'ESSAI E2E%';

WITH promo AS (
    INSERT INTO promotion (code, libelle, type, valeur, date_debut, date_fin, statut, code_barre_pack)
    VALUES ('ESSAI-E2E', 'ESSAI E2E Pack soins', 'PROMO_PACK', 2000,
            CURRENT_DATE - 1, CURRENT_DATE + 30, 'VALIDEE', 'PK99999999')
    RETURNING id
), magasin AS (
    INSERT INTO promotion_magasin (promotion_id, magasin_id)
    SELECT promo.id, u.magasin_id FROM promo, app_user u WHERE u.login = 'admin'
    RETURNING promotion_id
)
INSERT INTO promotion_produit (promotion_id, produit_id, quantite)
SELECT magasin.promotion_id, p.id, 1
FROM magasin, produit p
WHERE p.libelle IN ('PANSEMENT ADHESIF B/20', 'SPARADRAP HYPOALLERGENIQUE 5M');

DO $$
BEGIN
    IF (SELECT count(*) FROM promotion_produit pp JOIN promotion p ON p.id = pp.promotion_id
        WHERE p.libelle LIKE 'ESSAI E2E%') <> 2 THEN
        RAISE EXCEPTION 'pack d''essai incomplet : article ou magasin introuvable';
    END IF;
END $$;
