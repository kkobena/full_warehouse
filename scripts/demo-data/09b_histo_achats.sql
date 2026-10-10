-- ============================================================================
-- 09b_histo_achats.sql — Achats, lots et traçabilité des ventes de l'historique
--
-- 09_ventes.sql génère trois ans de ventes, mais ne sait adosser à du stock que
-- les 180 derniers jours : seuls les lots VIVANTS (en rayon, périmés, en alerte)
-- existent à ce stade. Pour tout ce qui est plus ancien, ce script reconstitue
-- l'approvisionnement qui a rendu ces ventes possibles — en le DÉDUISANT d'elles.
--
-- Principe : pour chaque produit et chaque mois de ventes M, une ligne d'achat
-- reçue durant le mois M-1 porte exactement la demande de M. Trois conséquences :
--
--   * le stock ne passe jamais sous zéro : la marchandise est entrée avant la
--     première vente qui la consomme ;
--   * achats et ventes se recoupent, produit par produit, au franc et à l'unité
--     près — le premier rapprochement que fait un pharmacien ;
--   * la traçabilité (sales_line.lots) est triviale et exacte : la ligne vendue
--     en M prend le lot reçu en M-1, en totalité.
--
-- Les lots sont à l'état SOLD (restant nul, aucun emplacement) : ils n'ont plus
-- de stock à porter. Leur péremption est postérieure d'au moins dix-huit mois à
-- leur réception, donc largement après les ventes qui les consomment (règle des
-- 90 jours respectée par construction).
--
-- init_stock / after_stock des lignes (de vente comme d'achat) ne sont pas posés
-- ici : 24_mouvements.sql les déduit du grand livre des mouvements, qui est la
-- seule source qui puisse les rendre cohérents avec le stock courant.
-- ============================================================================

\i _header.sql

\echo '>> 09b_histo_achats : achats déduits de la demande, lots, traçabilité'

-- ---------------------------------------------------------------------------
-- 1. Demande mensuelle par produit
--
-- Les produits hors gestion de lot (parapharmacie, accessoires) ont leurs achats comme les
-- autres, mais sans lot : leur stock n'est qu'un compteur.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_h_demande AS
SELECT sl.produit_id,
       date_trunc('month', sl.sale_date)::date AS mois,
       sum(sl.quantity_sold)::int              AS qte
  FROM sales_line sl
  JOIN produit p ON p.id = sl.produit_id
 WHERE sl.sale_date < CURRENT_DATE - pg_temp.jours_recents()
   AND sl.quantity_sold > 0
 GROUP BY sl.produit_id, date_trunc('month', sl.sale_date);

-- ---------------------------------------------------------------------------
-- 2. Achats : un par (produit, mois de demande), reçu le mois précédent
--
-- Le fournisseur est le PRINCIPAL du produit (c'est lui qui porte le code CIP et
-- les prix). Quatre créneaux hebdomadaires de livraison — les 3, 10, 17 et 24 du
-- mois — répartissent les produits entre autant de bons par fournisseur.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_h_achat AS
SELECT d.produit_id,
       d.mois                                   AS mois_demande,
       d.qte,
       fp.id                                    AS fp_id,
       fp.fournisseur_id                        AS principal_id,
       fp.prix_achat,
       fp.prix_uni,
       p.tva_id,
       (d.mois - INTERVAL '1 month')::date      AS mois_achat,
       (p.id % 4)                               AS creneau
  FROM tmp_h_demande d
  JOIN produit p ON p.id = d.produit_id
  JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id;

-- Stock d'ouverture de la fenêtre récente : posé par 06_lots.sql, il n'a pas encore de réception.
-- C'est l'achat du dernier mois de l'historique — ce qui évite le creux d'achats qu'on verrait
-- sinon à la jonction, puisque aucune demande ne le justifie dans cet historique.
ALTER TABLE tmp_h_achat ADD COLUMN lot_id int;

INSERT INTO tmp_h_achat
SELECT l.produit_id, NULL::date, l.quantity,
       fp.id, fp.fournisseur_id, fp.prix_achat, fp.prix_uni, p.tva_id,
       date_trunc('month', d.jour)::date,
       LEAST(3, (extract(day FROM d.jour)::int - 1) / 7),
       l.id
  FROM lot l
  JOIN produit p ON p.id = l.produit_id
  JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
  -- Réceptions étalées sur les cinq semaines qui précèdent la fenêtre, comme le sont celles
  -- des mois ordinaires : toutes à la veille, elles gonfleraient un mois et videraient l'autre.
  CROSS JOIN LATERAL (SELECT CURRENT_DATE - pg_temp.jours_recents() - 1 - (p.id % 35) AS jour) d
 WHERE l.num_lot LIKE 'LOUV%';

-- Les commandes passent chez l'AGENCE quand le principal en a, avec une rotation
-- sur ses agences ; sinon chez le principal lui-même (règle du §3.3 du plan).
CREATE TEMP TABLE tmp_h_agence AS
SELECT a.parent_id,
       a.id,
       row_number() OVER (PARTITION BY a.parent_id ORDER BY a.odre, a.id) - 1 AS pos,
       count(*)     OVER (PARTITION BY a.parent_id)                           AS total
  FROM fournisseur a
 WHERE a.parent_id IS NOT NULL;

CREATE TEMP TABLE tmp_h_cmd AS
SELECT nextval('id_commande_seq')::int AS id,
       g.principal_id, g.mois_achat, g.creneau, g.ouverture,
       g.rang,
       g.receipt_date,
       (g.receipt_date - (1 + (g.rang % 4))::int) AS order_date,
       COALESCE(ag.id, g.principal_id) AS fournisseur_id
  FROM (
      SELECT x.principal_id, x.mois_achat, x.creneau, x.ouverture,
             row_number() OVER (ORDER BY x.mois_achat, x.principal_id, x.creneau, x.ouverture) AS rang,
             LEAST((x.mois_achat + (2 + 7 * x.creneau))::date,
                   CURRENT_DATE - pg_temp.jours_recents() - 1) AS receipt_date
        -- Un bon distinct pour le stock d'ouverture : un produit ne peut figurer qu'une fois par bon,
        -- et il peut avoir à la fois une ligne ordinaire et une ligne d'ouverture le même mois.
        FROM (SELECT DISTINCT principal_id, mois_achat, creneau, (lot_id IS NOT NULL) AS ouverture
                FROM tmp_h_achat) x
  ) g
  LEFT JOIN LATERAL (
      SELECT a.id FROM tmp_h_agence a
       WHERE a.parent_id = g.principal_id
         AND a.pos = (extract(month FROM g.mois_achat)::int + g.creneau + g.rang) % a.total
  ) ag ON true;

-- Les bons sont saisis par l'administrateur et par le responsable stock, en alternance.
INSERT INTO commande (
    id, order_date, order_reference, receipt_reference, receipt_date,
    order_status, paiment_status, receipt_type,
    gross_amount, order_amount, final_amount, ht_amount, tax_amount, discount_amount,
    fournisseur_id, user_id, has_been_submitted_to_pharmaml,
    created_at, updated_at
)
SELECT c.id, c.order_date,
       'PO' || to_char(c.order_date, 'YYYYMMDD') || 'H' || lpad(c.rang::text, 4, '0'),
       'BL' || to_char(c.order_date, 'YYMM') || 'H' || lpad(c.rang::text, 5, '0'),
       c.receipt_date,
       'CLOSED', 'PAID', 'ORDER',
       0, 0, 0, 0, 0, 0,
       c.fournisseur_id, u.id, false,
       c.order_date + TIME '08:30:00',
       c.receipt_date + TIME '10:30:00'
  FROM tmp_h_cmd c
  JOIN LATERAL (
      SELECT id FROM app_user
       WHERE login = CASE WHEN c.rang % 3 = 0 THEN 'rkouassi' ELSE 'admin' END
       LIMIT 1
  ) u ON true;

-- ---------------------------------------------------------------------------
-- 3. Lignes de commande
--
-- Quantité reçue = demande du mois suivant. Une ligne sur neuf a été commandée
-- plus large que livrée (rupture grossiste) : l'écart nourrit le taux de service
-- des fournisseurs sans jamais toucher au stock, qui ne suit que le reçu.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_h_ol AS
SELECT nextval('id_order_line_seq')::int AS id,
       c.id            AS commande_id,
       c.order_date,
       c.receipt_date,
       a.produit_id, a.mois_demande, a.fp_id, a.tva_id, a.lot_id,
       a.prix_achat, a.prix_uni,
       a.qte           AS quantity_received,
       a.qte + CASE WHEN a.produit_id % 9 = 0 THEN greatest(1, a.qte / 5) ELSE 0 END AS quantity_requested,
       row_number() OVER (PARTITION BY a.produit_id ORDER BY c.receipt_date) AS rang_produit
  FROM tmp_h_achat a
  JOIN tmp_h_cmd c ON c.principal_id = a.principal_id
                  AND c.mois_achat   = a.mois_achat
                  AND c.creneau      = a.creneau
                  AND c.ouverture    = (a.lot_id IS NOT NULL);

INSERT INTO order_line (
    id, order_date, commande_id, commande_order_date,
    fournisseur_produit_id, tva_id,
    quantity_requested, quantity_received, quantity_returned, free_qty,
    init_stock, final_stock,
    order_unit_price, order_cost_amount,
    discount_amount, net_amount, tax_amount,
    provisional_code, is_updated, date_peremption, receipt_date,
    created_at, updated_at
)
SELECT o.id, o.order_date, o.commande_id, o.order_date,
       o.fp_id, o.tva_id,
       o.quantity_requested, o.quantity_received, 0, 0,
       0, NULL,                       -- recalés par 24_mouvements.sql
       o.prix_uni, o.prix_achat,
       0, 0,
       (o.quantity_received * o.prix_achat
        - ceil((o.quantity_received * o.prix_achat)::numeric / (1 + t.taux / 100.0)))::int,
       false, true,
       -- péremption connue à la réception : dix-huit à trente-six mois (portée aussi par une ligne
       -- hors lot, comme les lignes récentes de 05).
       (o.receipt_date + (540 + (o.produit_id * 37) % 540))::date,
       o.receipt_date + TIME '10:00:00',
       o.order_date + TIME '08:35:00',
       o.receipt_date + TIME '10:30:00'
  FROM tmp_h_ol o
  JOIN tva t ON t.id = o.tva_id
  JOIN produit pr ON pr.id = o.produit_id;

-- Totaux de l'en-tête, recalculés depuis les lignes (même règle que 05_commandes.sql :
-- une commande réceptionnée se totalise sur le REÇU).
WITH totaux AS (
    SELECT ol.commande_id, ol.commande_order_date,
           sum(ol.quantity_received * ol.order_cost_amount)::int AS gross,
           sum(ol.quantity_received * ol.order_unit_price)::int   AS ordre,
           sum(ol.tax_amount)::int                               AS taxe
      FROM order_line ol
      JOIN tmp_h_cmd c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     GROUP BY ol.commande_id, ol.commande_order_date
)
UPDATE commande c
   SET gross_amount = t.gross,
       order_amount = t.ordre,
       final_amount = t.gross,
       ht_amount    = t.gross,        -- le montant du bon, malgré son nom (voir 05)
       tax_amount   = t.taxe
  FROM totaux t
 WHERE t.commande_id = c.id AND t.commande_order_date = c.order_date;

-- ---------------------------------------------------------------------------
-- 4. Lots et réceptions
-- ---------------------------------------------------------------------------
INSERT INTO lot (
    num_lot, produit_id, order_line_id, commande_order_date,
    quantity, current_quantity, quantity_received_ug,
    prixachat, prixunit,
    expiry_date, manufacturing_date, statut,
    created_date, updated
)
SELECT 'H' || to_char(o.receipt_date, 'YYMM') || lpad(o.rang_produit::text, 3, '0'),
       o.produit_id, o.id, o.order_date,
       o.quantity_received, 0, 0,
       o.prix_achat, o.prix_uni,
       (o.receipt_date + (540 + (o.produit_id * 37) % 540))::date,
       (o.receipt_date + (540 + (o.produit_id * 37) % 540))::date - 730,
       'SOLD',
       o.receipt_date + TIME '10:00:00',
       o.receipt_date + TIME '10:00:00'
  FROM tmp_h_ol o
  JOIN produit pr ON pr.id = o.produit_id AND pr.gestion_lot
 WHERE o.lot_id IS NULL;

-- Les lots d'ouverture existent déjà : on leur rattache leur ligne d'achat.
UPDATE lot l
   SET order_line_id = o.id,
       commande_order_date = o.order_date,
       created_date = o.receipt_date + TIME '10:00:00'
  FROM tmp_h_ol o
 WHERE l.id = o.lot_id;

UPDATE order_line ol
   SET date_peremption = l.expiry_date
  FROM lot l
 WHERE l.order_line_id = ol.id
   AND l.commande_order_date = ol.order_date
   AND l.num_lot LIKE 'LOUV%';

INSERT INTO lot_reception (
    lot_id, order_line_id, commande_order_date,
    quantity_received, free_qty, prix_achat, receipt_date, created_at
)
SELECT l.id, l.order_line_id, l.commande_order_date,
       l.quantity, 0, l.prixachat, o.receipt_date,
       o.receipt_date + TIME '10:00:00'
  FROM lot l
  JOIN tmp_h_ol o ON o.id = l.order_line_id AND o.order_date = l.commande_order_date;

-- ---------------------------------------------------------------------------
-- 5. Traçabilité : chaque ligne vendue en M prend, en totalité, le lot reçu en M-1
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_h_lot AS
SELECT o.produit_id, o.mois_demande, l.id AS lot_id, l.num_lot, l.expiry_date
  FROM tmp_h_ol o
  JOIN lot l ON l.order_line_id = o.id AND l.commande_order_date = o.order_date;

CREATE INDEX ON tmp_h_lot (produit_id, mois_demande);

UPDATE sales_line sl
   SET lots = jsonb_build_array(jsonb_build_object(
           'id', h.lot_id, 'numLot', h.num_lot,
           'quantity', sl.quantity_sold, 'expiryDate', h.expiry_date))
  FROM tmp_h_lot h
 WHERE sl.sale_date < CURRENT_DATE - pg_temp.jours_recents()
   AND sl.quantity_sold > 0
   AND h.produit_id = sl.produit_id
   AND h.mois_demande = date_trunc('month', sl.sale_date)::date;

DROP TABLE tmp_h_lot;
DROP TABLE tmp_h_ol;
DROP TABLE tmp_h_cmd;
DROP TABLE tmp_h_agence;
DROP TABLE tmp_h_achat;
DROP TABLE tmp_h_demande;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_cmd int; v_lignes int; v_lots int; v_sans_lot int; v_ecart int;
    v_avant int; v_total int; v_perime int;
BEGIN
    SELECT count(*) INTO v_cmd FROM commande WHERE order_reference LIKE 'PO%H%';
    -- Seules les lignes de produits suivis par lot ont un lot.
    SELECT count(*) INTO v_lignes FROM order_line ol
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
      JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
      JOIN produit pr ON pr.id = fp.produit_id AND pr.gestion_lot
     WHERE c.order_reference LIKE 'PO%H%';
    SELECT count(*) INTO v_lots FROM lot l
      JOIN order_line ol ON ol.id = l.order_line_id AND ol.order_date = l.commande_order_date
      JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
     WHERE c.order_reference LIKE 'PO%H%';

    -- Chaque ligne de l'historique doit être couverte par ses lots, à l'unité près.
    SELECT count(*) INTO v_sans_lot FROM sales_line sl
      JOIN produit p ON p.id = sl.produit_id AND p.gestion_lot
     WHERE sl.sale_date < CURRENT_DATE - pg_temp.jours_recents()
       AND sl.quantity_sold <> COALESCE(
           (SELECT sum((e->>'quantity')::int) FROM jsonb_array_elements(sl.lots) e), 0);

    -- Achats et ventes se recoupent produit par produit.
    SELECT count(*) INTO v_ecart FROM (
        SELECT l.produit_id
          FROM lot l
         WHERE l.num_lot ~ '^H[0-9]{4}[0-9]{3}$'
         GROUP BY l.produit_id
        HAVING sum(l.quantity) <> (
            SELECT COALESCE(sum(sl.quantity_sold), 0) FROM sales_line sl
             WHERE sl.produit_id = l.produit_id
               AND sl.sale_date < CURRENT_DATE - pg_temp.jours_recents())
    ) x;

    -- Aucune vente ne précède la réception du lot qu'elle consomme.
    SELECT count(*) INTO v_avant FROM sales_line sl
      CROSS JOIN LATERAL jsonb_array_elements(sl.lots) e
      JOIN lot l ON l.id = (e->>'id')::int
      JOIN lot_reception lr ON lr.lot_id = l.id
     WHERE sl.sale_date < CURRENT_DATE - pg_temp.jours_recents()
       AND sl.sale_date < lr.receipt_date;

    -- Aucune vente de l'historique ne consomme un lot qui serait périmé ou proche de l'être.
    SELECT count(*) INTO v_perime FROM sales_line sl
      CROSS JOIN LATERAL jsonb_array_elements(sl.lots) e
     WHERE sl.sale_date < CURRENT_DATE - pg_temp.jours_recents()
       AND (e->>'expiryDate')::date < sl.sale_date + 90;

    IF v_cmd < 200 THEN RAISE EXCEPTION 'Commandes historiques : % (attendu >= 200)', v_cmd; END IF;
    IF v_lignes < 3000 THEN RAISE EXCEPTION 'Lignes historiques : % (attendu >= 3000)', v_lignes; END IF;
    IF v_lots <> v_lignes THEN RAISE EXCEPTION '% lot(s) pour % ligne(s) d''achat', v_lots, v_lignes; END IF;
    IF v_sans_lot > 0 THEN RAISE EXCEPTION '% ligne(s) historique(s) dont les lots ne couvrent pas la quantité', v_sans_lot; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% produit(s) dont achats et ventes historiques ne se recoupent pas', v_ecart; END IF;
    IF v_avant > 0 THEN RAISE EXCEPTION '% vente(s) antérieure(s) à la réception de leur lot', v_avant; END IF;
    IF v_perime > 0 THEN RAISE EXCEPTION '% vente(s) historique(s) sur un lot proche de la péremption', v_perime; END IF;

    RAISE NOTICE '% commandes historiques, % lignes, % lots.', v_cmd, v_lignes, v_lots;
END $$;

\echo '<< 09b_histo_achats : terminé'
