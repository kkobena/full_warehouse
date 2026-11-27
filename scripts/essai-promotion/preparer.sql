-- Pose une promotion VALIDÉE « ESSAI E2E » sur PANSEMENT ADHESIF B/20 (-20 %), en cours depuis
-- hier et pour un mois, dans le magasin du compte administrateur. Sert de point de départ aux
-- parcours RFD-23 à RFD-25, qui n'ont pas à rejouer la création (RFD-22).
--
-- Directement en base : c'est un jeu d'essai, pas un geste à démontrer. L'article est de la
-- parapharmacie (TVA 18 %, marge d'au moins 45 % dans le jeu de démonstration) : -20 % reste
-- au-dessus du prix d'achat, comme le service l'exigerait.
--
--   docker exec -i pharma_smart_postgres psql -U postgres -d pharma_smart -v ON_ERROR_STOP=1 < scripts/essai-promotion/preparer.sql
SET search_path = pharma_smart, public;

-- Repartir propre : le même nettoyage que nettoyer.sql (le script passe par l'entrée standard de
-- psql, où un \i relatif ne trouverait pas son fichier).
DELETE FROM promotion WHERE libelle LIKE 'ESSAI E2E%';

WITH promo AS (
    INSERT INTO promotion (code, libelle, type, valeur, date_debut, date_fin, statut)
    VALUES ('ESSAI-E2E', 'ESSAI E2E Pansements -20 %', 'PROMO_POURCENTAGE', 20,
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
        RAISE EXCEPTION 'promotion d''essai incomplète : article ou magasin introuvable';
    END IF;
END $$;
