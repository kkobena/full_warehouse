-- ============================================================================
-- 15_reference.sql — Compteurs de numérotation
--
-- La table « reference » tient un compteur PAR JOUR et PAR TYPE
-- (ReferenceService.buildReference, clé (mvt_date, d_type)).
--
-- PIÈGE : la colonne de type s'appelle « d_type », pas « type » — l'entité
-- déclare @Column(name = "d_type") sur un champ nommé « type ».
--
-- Valeurs de TypeReference (des ENTIERS, pas des chaînes) :
--     0 VENTE          1 COMMANDE        2 PREVENTE_VENTE   3 SUGGESTION
--     4 TRANSACTION    5 REASSORT        6 AVOIR_CLIENT     7 RETOUR_CLIENT
--
-- Sans ces compteurs, la PREMIÈRE saisie faite dans l'application après
-- chargement repartirait de 1 et réémettrait un numéro déjà présent.
-- ============================================================================

\i _header.sql

\echo '>> 15_reference : compteurs de numérotation'

-- ---------------------------------------------------------------------------
-- 1. Compteur des ventes (type 0)
--
-- Le compteur d'un jour est le nombre de ventes émises ce jour-là, ventes
-- dépôt comprises : elles consomment la même séquence.
-- ---------------------------------------------------------------------------
INSERT INTO reference (mvt_date, d_type, number_transac, num)
SELECT
    s.sale_date, 0, count(*)::int,
    lpad(count(*)::text, 3, '0')
FROM sales s
GROUP BY s.sale_date
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Compteur des commandes (type 1)
--
-- buildNumCommande préfixe par la date : le num stocké reflète ce format.
-- ---------------------------------------------------------------------------
INSERT INTO reference (mvt_date, d_type, number_transac, num)
SELECT
    c.order_date, 1, count(*)::int,
    to_char(c.order_date, 'YYYYMMDD') || lpad(count(*)::text, 3, '0')
FROM commande c
GROUP BY c.order_date
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. Compteur des transactions financières (type 4)
-- ---------------------------------------------------------------------------
INSERT INTO reference (mvt_date, d_type, number_transac, num)
SELECT
    pt.transaction_date, 4, count(*)::int,
    to_char(pt.transaction_date, 'YYYYMMDD') || lpad(count(*)::text, 3, '0')
FROM payment_transaction pt
GROUP BY pt.transaction_date
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_ref int; v_manque int; v_sous int;
BEGIN
    SELECT count(*) INTO v_ref FROM reference;

    -- Chaque jour de vente doit avoir son compteur.
    SELECT count(*) INTO v_manque FROM (
        SELECT DISTINCT s.sale_date FROM sales s
         WHERE NOT EXISTS (SELECT 1 FROM reference r
                            WHERE r.mvt_date = s.sale_date AND r.d_type = 0)) x;

    -- Le compteur doit couvrir les ventes réellement émises, sinon la
    -- prochaine saisie réémettrait un numéro existant.
    SELECT count(*) INTO v_sous FROM (
        SELECT s.sale_date FROM sales s
         GROUP BY s.sale_date
        HAVING count(*) > COALESCE((SELECT r.number_transac FROM reference r
                                     WHERE r.mvt_date = s.sale_date AND r.d_type = 0), 0)) x;

    IF v_ref = 0 THEN RAISE EXCEPTION 'Aucun compteur de numérotation'; END IF;
    IF v_manque > 0 THEN RAISE EXCEPTION '% jour(s) de vente sans compteur', v_manque; END IF;
    IF v_sous > 0 THEN RAISE EXCEPTION '% compteur(s) inférieur(s) aux ventes du jour', v_sous; END IF;

    RAISE NOTICE '% compteurs de numérotation.', v_ref;
END $$;

\echo '<< 15_reference : terminé'
