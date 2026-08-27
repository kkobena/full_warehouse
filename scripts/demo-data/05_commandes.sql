-- ============================================================================
-- 05_commandes.sql — Commandes fournisseurs et leurs lignes
--
-- Rappels du modèle (§3.4 et §4.7 du plan) :
--   * commande et order_line ont une PK COMPOSITE (id, order_date) et des
--     identifiants ASSIGNÉS À LA MAIN, tirés de id_commande_seq et
--     id_order_line_seq — pas de colonne identity ;
--   * la FK de order_line porte sur les deux colonnes :
--     (commande_id, commande_order_date) → (commande.id, commande.order_date) ;
--   * order_line.order_amount et gross_amount N'EXISTENT PAS en base : ce sont
--     des @Formula, calculées à la lecture. Vérifié sur la base.
--
-- RÈGLE DES AGENCES (§3.3) :
--   commande.fournisseur_id  → l'AGENCE quand le principal en a
--   order_line.fournisseur_produit_id → un FP du PRINCIPAL de cette agence
--   La jointure commande → ligne traverse donc le lien parent.
--
-- Les lots ne sont PAS créés ici : ils relèvent de l'étape 4, avec les
-- lot_reception et lot_stock_location qui vont de pair.
--
-- Note : commande.agence_id existe en base (V1.7.0) mais n'est mappée par
-- aucune entité. Colonne morte, laissée à NULL.
-- ============================================================================

\i _header.sql

\echo '>> 05_commandes : commandes fournisseurs et lignes'

-- ---------------------------------------------------------------------------
-- 1. En-têtes de commande (120 sur 180 jours)
--
-- Répartition : 75 CLOSED, 25 RECEIVED, 20 REQUESTED.
-- Les plus anciennes sont soldées, les plus récentes encore en cours — c'est
-- l'ordre naturel d'une officine, et il rend les écrans de suivi crédibles.
--
-- Les montants sont posés à zéro ici puis recalculés depuis les lignes (§3).
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_cmd AS
WITH base AS (
    SELECT
        i,
        -- Étalement sur 180 jours : les commandes les plus anciennes portent
        -- les rangs les plus élevés.
        (CURRENT_DATE - (INTERVAL '1 day' * (3 + ((120 - i) * 179) / 120)))::date AS order_date,
        CASE WHEN i <= 75 THEN 'CLOSED'
             WHEN i <= 100 THEN 'RECEIVED'
             ELSE 'REQUESTED' END AS order_status
    FROM generate_series(1, 120) AS i
)
SELECT
    nextval('id_commande_seq')::int AS id,
    b.i,
    b.order_date,
    b.order_status,
    -- Le fournisseur de la commande : une agence si le principal en a, le
    -- principal lui-même sinon. C'est l'invariant métier du §3.3.
    f.id AS fournisseur_id,
    f.parent_id,
    -- Le principal, chez qui vivent les fournisseur_produit.
    COALESCE(f.parent_id, f.id) AS principal_id,
    CASE WHEN b.order_status = 'REQUESTED' THEN NULL
         ELSE (b.order_date + (INTERVAL '1 day' * (1 + b.i % 4)))::date END AS receipt_date,
    CASE WHEN b.order_status = 'REQUESTED' THEN NULL
         ELSE 'BL' || to_char(b.order_date, 'YYMM') || lpad(b.i::text, 5, '0') END AS receipt_reference
FROM base b
CROSS JOIN LATERAL (
    -- Rotation déterministe sur l'ensemble « agences + principaux sans agence ».
    SELECT x.id, x.parent_id
      FROM (
          SELECT fo.id, fo.parent_id,
                 row_number() OVER (ORDER BY fo.odre, fo.id) AS rang,
                 count(*)     OVER ()                        AS total
            FROM fournisseur fo
           WHERE fo.parent_id IS NOT NULL          -- les agences
              OR NOT EXISTS (                      -- ou un principal sans agence
                  SELECT 1 FROM fournisseur a WHERE a.parent_id = fo.id
              )
      ) x
     WHERE x.rang = 1 + (b.i % x.total)
) f;

INSERT INTO commande (
    id, order_date, order_reference, receipt_reference, receipt_date,
    order_status, paiment_status, receipt_type,
    gross_amount, order_amount, final_amount, ht_amount, tax_amount, discount_amount,
    fournisseur_id, user_id, has_been_submitted_to_pharmaml,
    created_at, updated_at
)
SELECT
    c.id, c.order_date,
    'PO' || to_char(c.order_date, 'YYYYMMDD') || lpad(c.i::text, 3, '0'),
    c.receipt_reference, c.receipt_date,
    c.order_status,
    -- Le statut de paiement est indépendant du statut de commande : une
    -- commande reçue peut rester à régler.
    CASE WHEN c.order_status = 'CLOSED' AND c.i % 3 <> 0 THEN 'PAID' ELSE 'UNPAID' END,
    'ORDER',
    0, 0, 0, 0, 0, 0,
    c.fournisseur_id, u.id, false,
    c.order_date + TIME '08:30:00',
    c.order_date + TIME '08:30:00'
FROM tmp_cmd c
CROSS JOIN LATERAL (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1) u;

-- ---------------------------------------------------------------------------
-- 2. Lignes de commande (10 à 25 par commande)
--
-- Le fournisseur_produit retenu appartient au PRINCIPAL de l'agence commandée.
-- Sans ce filtre, on obtiendrait des lignes rattachées à un grossiste qui ne
-- référence pas le produit — incohérence que la production ne peut pas créer.
--
-- Contrainte d'unicité (commande_id, fournisseur_produit_id, order_date) :
-- le DISTINCT ON garantit qu'un même FP n'apparaît qu'une fois par commande.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_ol AS
SELECT
    nextval('id_order_line_seq')::int AS id,
    s.commande_id,
    s.order_date,
    s.order_status,
    s.fournisseur_produit_id,
    s.produit_id,
    s.tva_id,
    s.prix_achat,
    s.prix_uni,
    s.rang,
    -- Quantité commandée : multiple du colisage, entre 5 et 60.
    (5 + ((s.commande_id * 7 + s.rang * 13) % 56)) AS quantity_requested
FROM (
    SELECT DISTINCT ON (c.id, fp.id)
        c.id       AS commande_id,
        c.order_date,
        c.order_status,
        fp.id      AS fournisseur_produit_id,
        fp.produit_id,
        p.tva_id,
        fp.prix_achat,
        fp.prix_uni,
        row_number() OVER (PARTITION BY c.id ORDER BY fp.id) AS rang
    FROM tmp_cmd c
    JOIN fournisseur_produit fp ON fp.fournisseur_id = c.principal_id
    JOIN produit p ON p.id = fp.produit_id
    -- Sélection déterministe d'un sous-ensemble de produits par commande.
    WHERE (fp.produit_id + c.i) % 7 = 0
) s
WHERE s.rang <= 10 + (s.commande_id % 16);

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
SELECT
    o.id, o.order_date, o.commande_id, o.order_date,
    o.fournisseur_produit_id, o.tva_id,
    o.quantity_requested,
    -- Rien de reçu tant que la commande n'est pas réceptionnée.
    -- Une commande sur cinq est partiellement servie : c'est le cas réel des
    -- reliquats, et il doit exister dans le jeu de données.
    CASE WHEN o.order_status = 'REQUESTED' THEN NULL
         WHEN o.commande_id % 5 = 0 THEN greatest(1, (o.quantity_requested * 3) / 4)
         ELSE o.quantity_requested END,
    0,
    -- Unités gratuites : une ligne sur onze.
    CASE WHEN o.rang % 11 = 0 THEN greatest(1, o.quantity_requested / 10) ELSE 0 END,
    0, NULL,
    o.prix_uni,
    o.prix_achat,
    0, 0,
    -- Taxe de la ligne, calculée sur le montant d'achat au taux du produit.
    CASE WHEN o.order_status = 'REQUESTED' THEN 0
         ELSE (o.quantity_requested * o.prix_achat
               - ceil((o.quantity_requested * o.prix_achat)::numeric
                      / (1 + t.taux / 100.0)))::int END,
    false, false,
    -- Péremption connue seulement à la réception.
    CASE WHEN o.order_status = 'REQUESTED' THEN NULL
         ELSE (o.order_date + (INTERVAL '1 day' * (400 + (o.id * 37) % 500)))::date END,
    NULL,
    o.order_date + TIME '08:35:00',
    o.order_date + TIME '08:35:00'
FROM tmp_ol o
JOIN tva t ON t.id = o.tva_id;

-- ---------------------------------------------------------------------------
-- 3. Totaux de l'en-tête, recalculés depuis les lignes
--
-- La base de calcul dépend du statut (§4.7) :
--     REQUESTED → quantité COMMANDÉE
--     sinon     → quantité REÇUE
--
-- Une commande partiellement servie a donc un gross_amount inférieur à la
-- somme de ses lignes commandées : c'est le comportement de l'application, pas
-- une incohérence.
-- ---------------------------------------------------------------------------
WITH totaux AS (
    SELECT
        ol.commande_id,
        ol.commande_order_date,
        sum(CASE WHEN c.order_status = 'REQUESTED' THEN ol.quantity_requested
                 ELSE COALESCE(ol.quantity_received, 0) END * ol.order_cost_amount)::int AS gross,
        sum(CASE WHEN c.order_status = 'REQUESTED' THEN ol.quantity_requested
                 ELSE COALESCE(ol.quantity_received, 0) END * ol.order_unit_price)::int AS ordre,
        sum(ol.tax_amount)::int AS taxe
    FROM order_line ol
    JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
    GROUP BY ol.commande_id, ol.commande_order_date
)
UPDATE commande c
   SET gross_amount = t.gross,
       order_amount = t.ordre,
       final_amount = CASE WHEN c.order_status = 'REQUESTED' THEN t.ordre ELSE t.gross END,
       -- Malgré son nom, ht_amount reçoit le MONTANT DU BON de livraison,
       -- identique à gross_amount (buildDeliveryReceipt:634). Zéro tant que la
       -- commande n'est pas réceptionnée.
       ht_amount    = CASE WHEN c.order_status = 'REQUESTED' THEN 0 ELSE t.gross END,
       tax_amount   = t.taxe
  FROM totaux t
 WHERE t.commande_id = c.id AND t.commande_order_date = c.order_date;

DROP TABLE tmp_ol;
DROP TABLE tmp_cmd;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_cmd     int;
    v_lignes  int;
    v_ecart   int;
    v_agence  int;
BEGIN
    SELECT count(*) INTO v_cmd    FROM commande;
    SELECT count(*) INTO v_lignes FROM order_line;

    -- Total de l'en-tête contre la somme des lignes.
    SELECT count(*) INTO v_ecart FROM (
        SELECT c.id FROM commande c
          JOIN order_line ol ON ol.commande_id = c.id AND ol.commande_order_date = c.order_date
         GROUP BY c.id, c.order_date, c.gross_amount, c.order_status
        HAVING c.gross_amount <> sum(
            CASE WHEN c.order_status = 'REQUESTED' THEN ol.quantity_requested
                 ELSE COALESCE(ol.quantity_received, 0) END * ol.order_cost_amount)
    ) x;

    -- Chaque ligne doit pointer un FP du principal de l'agence commandée.
    SELECT count(*) INTO v_agence FROM order_line ol
      JOIN commande c    ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
      JOIN fournisseur ag ON ag.id = c.fournisseur_id
      JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
     WHERE fp.fournisseur_id <> COALESCE(ag.parent_id, ag.id);

    IF v_cmd < 120 THEN RAISE EXCEPTION 'Commandes : % (attendu 120)', v_cmd; END IF;
    IF v_lignes < 1000 THEN RAISE EXCEPTION 'Lignes de commande : % (attendu >= 1000)', v_lignes; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% commande(s) dont le total contredit les lignes', v_ecart; END IF;
    IF v_agence > 0 THEN RAISE EXCEPTION '% ligne(s) rattachée(s) au mauvais fournisseur principal', v_agence; END IF;

    RAISE NOTICE '% commandes, % lignes.', v_cmd, v_lignes;
END $$;

\echo '<< 05_commandes : terminé'
