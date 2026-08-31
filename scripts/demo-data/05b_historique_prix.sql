-- ============================================================================
-- 05b_historique_prix.sql — Historique des prix fournisseur-produit
--
-- À la réception, l'application enregistre tout changement de prix d'un couple
-- (produit, fournisseur) dans `fournisseur_produit_price_history` : ancien et
-- nouveau prix d'achat, ancien et nouveau prix de vente, le bon qui l'a
-- constaté, et qui l'a saisi.
--
-- Cet historique est ce qu'on consulte AVANT de contester une facture ou
-- d'accepter un écart : « ce produit a-t-il vraiment augmenté deux fois cette
-- année, ou est-ce une erreur de saisie ? ». Sans lui, l'écran répond « Aucun
-- changement de prix enregistré » et le contrôle de concordance (ACH-43) n'a
-- rien à quoi se comparer.
--
-- Les hausses posées ici sont modestes et datées des réceptions passées : elles
-- racontent la dérive ordinaire des tarifs grossistes, pas une anomalie.
-- ============================================================================

\i _header.sql

\echo '>> 05b_historique_prix : changements de prix constatés à la réception'

-- ---------------------------------------------------------------------------
-- Un changement de prix par couple (produit, fournisseur) déjà réceptionné.
--
-- Un seul par couple, et non un sur onze : l'historique se consulte depuis
-- N'IMPORTE QUELLE ligne d'un bon, et une fenêtre vide sur la première ligne
-- venue ne démontre rien. Le nouveau prix est celui que porte aujourd'hui la
-- fiche fournisseur-produit ; l'ancien s'en déduit par la hausse appliquée.
-- ---------------------------------------------------------------------------
INSERT INTO fournisseur_produit_price_history (
    fournisseur_produit_id,
    old_prix_achat, new_prix_achat,
    old_prix_uni, new_prix_uni,
    changed_at, changed_by_id, commande_id, receipt_reference
)
SELECT DISTINCT ON (fp.id)
    fp.id,
    -- Hausse de 3 à 8 % selon la ligne : déterministe, jamais nulle.
    greatest(1, (fp.prix_achat * (100 - (3 + ol.id % 6)) / 100)::int),
    fp.prix_achat,
    greatest(1, (fp.prix_uni * (100 - (3 + ol.id % 6)) / 100)::int),
    fp.prix_uni,
    c.receipt_date + TIME '10:20:00',
    u.id,
    c.id,
    c.receipt_reference
FROM order_line ol
JOIN commande c
  ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
CROSS JOIN LATERAL (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1) u
-- Clôturées ET en cours de réception : l'historique se consulte aussi depuis un bon en
-- cours de saisie, et les couples produit/fournisseur de ces bons-là ne se retrouvent pas
-- forcément dans une commande déjà soldée.
WHERE c.order_status IN ('CLOSED', 'RECEIVED')
  AND c.receipt_reference IS NOT NULL
ORDER BY fp.id, c.receipt_date DESC;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_nb       int;
    v_produits int;
    v_absurde  int;
BEGIN
    SELECT count(*), count(DISTINCT fournisseur_produit_id)
      INTO v_nb, v_produits
      FROM fournisseur_produit_price_history;

    IF v_nb < 20 THEN
        RAISE EXCEPTION 'Historique de prix trop maigre : % lignes', v_nb;
    END IF;

    -- Un historique dont le nouveau prix ne serait pas celui de la fiche
    -- laisserait croire à un changement postérieur jamais enregistré.
    SELECT count(*) INTO v_absurde
      FROM fournisseur_produit_price_history h
      JOIN fournisseur_produit fp ON fp.id = h.fournisseur_produit_id
     WHERE h.new_prix_achat <> fp.prix_achat
        OR h.new_prix_uni <> fp.prix_uni
        OR h.old_prix_achat >= h.new_prix_achat;
    IF v_absurde > 0 THEN
        RAISE EXCEPTION 'Historique de prix incohérent : % ligne(s)', v_absurde;
    END IF;

    RAISE NOTICE 'Historique de prix : % changements sur % couples produit/fournisseur.',
        v_nb, v_produits;
END $$;

\echo '<< 05b_historique_prix : terminé'

-- ---------------------------------------------------------------------------
-- Des lignes commandées AVANT la hausse
--
-- Le contrôle de concordance (ACH-43) compare le prix d'achat COMMANDÉ au tarif
-- actuel du couple produit/fournisseur, et signale les lignes dont l'écart
-- dépasse le seuil configuré (`APP_SEUIL_VARIATION_PRIX`, 20 % par défaut).
-- Sans aucune ligne en écart, la colonne P.A du bon n'affiche jamais son
-- avertissement et le contrôle ne se démontre pas.
--
-- On rejoue donc l'histoire dans le bon sens : une ligne sur sept des bons en
-- cours de réception a été commandée à l'ANCIEN prix — celui d'avant la hausse
-- enregistrée plus haut, minoré pour dépasser franchement le seuil.
-- ---------------------------------------------------------------------------
UPDATE order_line ol
   SET order_cost_amount = greatest(1, (h.old_prix_achat * 70) / 100)
  FROM commande c, fournisseur_produit_price_history h
 WHERE c.id = ol.commande_id AND c.order_date = ol.commande_order_date
   AND c.order_status = 'RECEIVED'
   AND h.fournisseur_produit_id = ol.fournisseur_produit_id
   AND ol.id % 7 = 0;

-- Les totaux de l'en-tête suivent les lignes : les recalculer, sinon le
-- contrôle « Total achat = somme des lignes » de 99_verification échoue.
WITH totaux AS (
    SELECT ol.commande_id, ol.commande_order_date,
           sum(CASE WHEN c.order_status = 'REQUESTED' THEN ol.quantity_requested
                    ELSE COALESCE(ol.quantity_received, 0) END * ol.order_cost_amount)::int AS gross
      FROM order_line ol
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     GROUP BY ol.commande_id, ol.commande_order_date
)
UPDATE commande c
   SET gross_amount = t.gross,
       ht_amount    = CASE WHEN c.order_status = 'REQUESTED' THEN 0 ELSE t.gross END
  FROM totaux t
 WHERE t.commande_id = c.id AND t.commande_order_date = c.order_date;

DO $$
DECLARE v_nb int;
BEGIN
    SELECT count(*) INTO v_nb
      FROM order_line ol
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
      JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
     WHERE c.order_status = 'RECEIVED'
       AND abs(fp.prix_achat - ol.order_cost_amount) * 100 > 20 * ol.order_cost_amount;
    IF v_nb = 0 THEN
        RAISE EXCEPTION 'Aucune ligne en écart de prix : le contrôle de concordance ne se démontre pas';
    END IF;
    RAISE NOTICE 'Écarts de prix au-delà du seuil : % ligne(s).', v_nb;
END $$;
