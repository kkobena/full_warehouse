-- ============================================================================
-- 14d_mouvements_caisse.sql — Règlements fournisseurs, entrées et sorties de caisse
--
-- Jusqu'ici la caisse ne connaissait que des encaissements. Deux familles de mouvements
-- manquaient, et avec elles trois écrans :
--
--   * le RÈGLEMENT FOURNISSEUR (type REGLMENT_FOURNISSEUR, dtype PaymentFournisseur) : une
--     commande « PAID » sans règlement en face affirmait un paiement dont il n'existait
--     aucune trace — le défaut même que 14b corrigeait pour les factures tiers payant ;
--   * les SORTIES DE CAISSE (petite caisse : fournitures, livraison, avance) et les
--     ENTRÉES DE CAISSE (apport de fonds) : l'écran « Mouvements de caisse » et le ticket Z
--     n'avaient rien à totaliser ;
--   * les BANQUES, que référencent les règlements par chèque ou virement.
--
-- Ce sont des mouvements de la caisse : PaymentTransaction.cashRegister est obligatoire, on
-- les rattache donc à la première caisse ouverte à partir de leur date. Seuls les
-- mouvements EN ESPÈCES modifient le tiroir ; les chèques et virements n'y touchent pas.
-- Le sens se lit sur la catégorie du type (credit = sortie), comme le fait
-- FinancialTransactionServiceImpl.
--
-- Après 14c : les écritures de caisse de 09 et 14b sont posées, on les complète puis on
-- recale le fond de tiroir de chaque caisse sur l'ensemble de ses lignes de ticket Z.
-- ============================================================================

\i _header.sql

\echo '>> 14d_mouvements_caisse : règlements fournisseurs, entrées et sorties de caisse'

INSERT INTO banque (nom, code, beneficiaire, adresse)
VALUES ('SGBCI',        'SGBCI',  'PHARMACIE PHARMA-SMART', 'Abidjan Plateau, Avenue Joseph Anoma'),
       ('NSIA BANQUE',  'NSIA',   'PHARMACIE PHARMA-SMART', 'Abidjan Plateau, Avenue Houdaille'),
       ('ECOBANK',      'ECOCI',  'PHARMACIE PHARMA-SMART', 'Abidjan Plateau, Immeuble Alliance'),
       ('BICICI',       'BICICI', 'PHARMACIE PHARMA-SMART', 'Abidjan Plateau, Avenue Franchet d''Esperey'),
       ('SOCIETE IVOIRIENNE DE BANQUE', 'SIB', 'PHARMACIE PHARMA-SMART', 'Abidjan Plateau, Avenue Terrasson de Fougères');

-- ---------------------------------------------------------------------------
-- 1. Règlements des commandes payées
--
-- Une commande PAID est réglée une à cinq semaines après sa réception, par virement le plus
-- souvent, par chèque parfois, en espèces pour les petits bons. Jamais le jour même d'un
-- chargement : la caisse du jour est ouverte, et son ticket Z ne se recale qu'à la clôture.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_pf AS
SELECT nextval('id_transaction_seq') AS id,
       c.id AS commande_id, c.order_date, c.final_amount AS montant, c.receipt_reference,
       LEAST(c.receipt_date + 8 + (c.id % 28), CURRENT_DATE - 1) AS jour,
       CASE WHEN c.final_amount < 150000 AND c.id % 6 = 0 THEN 'CASH'
            WHEN c.id % 4 = 0 THEN 'CH'
            ELSE 'VIREMENT' END AS mode,
       (c.id % 5) AS banque_rang
  FROM commande c
 WHERE c.paiment_status = 'PAID'
   AND c.final_amount > 0
   AND c.receipt_date IS NOT NULL;

INSERT INTO payment_transaction (
    dtype, id, transaction_date, categorie_ca, commentaire, created_at, credit,
    expected_amount, montant_verse, paid_amount, reel_amount,
    transaction_number, type_transaction, payment_mode_code,
    cash_register_id, banque_id, commande_id, commande_order_date,
    amount_to_be_taken_into_account
)
SELECT 'PaymentFournisseur', p.id, reg.jour, 'CA',
       'Règlement du bon ' || p.receipt_reference,
       reg.jour + TIME '16:30:00', true,
       p.montant, p.montant, p.montant, p.montant,
       to_char(reg.jour, 'YYYYMMDD') || lpad((1 + (p.id % 9000))::text, 4, '0'),
       'REGLMENT_FOURNISSEUR', p.mode,
       reg.id,
       CASE WHEN p.mode = 'CASH' THEN NULL
            ELSE (SELECT b.id FROM (SELECT id, row_number() OVER (ORDER BY id) - 1 AS pos FROM banque) b
                   WHERE b.pos = p.banque_rang) END,
       p.commande_id, p.order_date, 0
  FROM tmp_pf p
 CROSS JOIN LATERAL (
     SELECT cr.id, cr.begin_time::date AS jour
       FROM cash_register cr
      WHERE cr.statut <> 'OPEN'
      ORDER BY abs(cr.begin_time::date - p.jour), cr.begin_time
      LIMIT 1
 ) reg;

DROP TABLE tmp_pf;

-- ---------------------------------------------------------------------------
-- 2. Petite caisse : sorties et entrées en espèces
--
-- Une caisse sur quatre porte un mouvement. Les sorties (fournitures, frais de livraison,
-- avance sur salaire, dépannage) dominent ; l'apport de fonds est plus rare.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_mc AS
SELECT nextval('id_transaction_seq') AS id,
       cr.id AS cash_register_id, cr.begin_time::date AS jour,
       row_number() OVER (ORDER BY cr.id) AS rang
  FROM cash_register cr
 WHERE cr.statut <> 'OPEN' AND cr.id % 4 = 0;

INSERT INTO payment_transaction (
    dtype, id, transaction_date, categorie_ca, commentaire, created_at, credit,
    expected_amount, montant_verse, paid_amount, reel_amount,
    transaction_number, type_transaction, payment_mode_code,
    cash_register_id, amount_to_be_taken_into_account
)
SELECT 'DefaultPayment', m.id, m.jour, 'CA',
       (ARRAY['Achat de fournitures de bureau', 'Frais de livraison', 'Avance sur salaire',
              'Dépannage collègue pharmacie voisine', 'Entretien climatisation',
              'Achat de sacs et rouleaux de caisse'])[1 + (m.rang % 6)],
       m.jour + TIME '14:20:00',
       t.sortie,
       t.montant, t.montant, t.montant, t.montant,
       to_char(m.jour, 'YYYYMMDD') || lpad((9000 + (m.rang % 900))::text, 4, '0'),
       CASE WHEN t.sortie THEN 'SORTIE_CAISSE' ELSE 'ENTREE_CAISSE' END,
       'CASH', m.cash_register_id, 0
  FROM tmp_mc m
 CROSS JOIN LATERAL (
     SELECT (m.rang % 5 <> 0) AS sortie,
            (500 * (4 + ((m.rang * 7) % 46)))::int AS montant
 ) t;

DROP TABLE tmp_mc;

-- Les entrées d'apport ne portent pas le commentaire d'une dépense.
UPDATE payment_transaction
   SET commentaire = 'Apport de fonds pour la monnaie'
 WHERE type_transaction = 'ENTREE_CAISSE';

-- ---------------------------------------------------------------------------
-- 3. Détail du ticket Z et fond de tiroir
--
-- Le triplet (caisse, mode, type) est unique : on regroupe. Puis le fond de chaque caisse
-- close se recale sur ses lignes, avec la règle que contrôle 14b : ce qui entre s'ajoute,
-- ce qui sort (SORTIE_CAISSE, FONDS_CAISSE, REGLMENT_FOURNISSEUR) se retranche.
-- ---------------------------------------------------------------------------
INSERT INTO cash_register_item (cash_register_id, payment_mode_code, amount, type_transaction)
SELECT pt.cash_register_id, pt.payment_mode_code, sum(pt.paid_amount)::bigint, pt.type_transaction
  FROM payment_transaction pt
 WHERE pt.type_transaction IN ('SORTIE_CAISSE', 'ENTREE_CAISSE', 'REGLMENT_FOURNISSEUR')
 GROUP BY pt.cash_register_id, pt.payment_mode_code, pt.type_transaction
ON CONFLICT (cash_register_id, payment_mode_code, type_transaction) DO UPDATE
   SET amount = EXCLUDED.amount;

UPDATE cash_register cr
   SET final_amount = cr.init_amount
       + COALESCE((SELECT sum(i.amount) FROM cash_register_item i
                    WHERE i.cash_register_id = cr.id AND i.payment_mode_code = 'CASH'
                      AND i.type_transaction NOT IN ('SORTIE_CAISSE', 'FONDS_CAISSE', 'REGLMENT_FOURNISSEUR')), 0)
       - COALESCE((SELECT sum(i.amount) FROM cash_register_item i
                    WHERE i.cash_register_id = cr.id AND i.payment_mode_code = 'CASH'
                      AND i.type_transaction IN ('SORTIE_CAISSE', 'FONDS_CAISSE', 'REGLMENT_FOURNISSEUR')), 0),
       updated = GREATEST(cr.updated, cr.end_time)
 WHERE cr.statut <> 'OPEN'
   AND EXISTS (SELECT 1 FROM payment_transaction pt
                WHERE pt.cash_register_id = cr.id
                  AND pt.type_transaction IN ('SORTIE_CAISSE', 'ENTREE_CAISSE', 'REGLMENT_FOURNISSEUR'));

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_pf int; v_payees int; v_ecart int; v_sorties int; v_entrees int; v_banque int;
    v_double int; v_z int;
BEGIN
    SELECT count(*) INTO v_pf FROM payment_transaction WHERE dtype = 'PaymentFournisseur';
    SELECT count(*) INTO v_sorties FROM payment_transaction WHERE type_transaction = 'SORTIE_CAISSE';
    SELECT count(*) INTO v_entrees FROM payment_transaction WHERE type_transaction = 'ENTREE_CAISSE';

    -- Toute commande payée a son règlement, au franc près ; aucune commande impayée n'en a.
    SELECT count(*) INTO v_payees FROM commande c
     WHERE c.paiment_status = 'PAID' AND c.final_amount > 0 AND c.receipt_date IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM payment_transaction p
                        WHERE p.dtype = 'PaymentFournisseur' AND p.commande_id = c.id
                          AND p.commande_order_date = c.order_date AND p.paid_amount = c.final_amount);
    SELECT count(*) INTO v_ecart FROM payment_transaction p
      JOIN commande c ON c.id = p.commande_id AND c.order_date = p.commande_order_date
     WHERE p.dtype = 'PaymentFournisseur' AND c.paiment_status <> 'PAID';
    SELECT count(*) INTO v_double FROM (
        SELECT commande_id, commande_order_date FROM payment_transaction
         WHERE dtype = 'PaymentFournisseur'
         GROUP BY commande_id, commande_order_date HAVING count(*) > 1) x;

    -- Un chèque ou un virement désigne une banque ; l'espèce, non.
    SELECT count(*) INTO v_banque FROM payment_transaction
     WHERE dtype = 'PaymentFournisseur'
       AND ((payment_mode_code = 'CASH') <> (banque_id IS NULL));

    -- Le ticket Z se ferme : le fond de tiroir égale ses lignes (même règle que 14b).
    SELECT count(*) INTO v_z FROM cash_register cr
     WHERE cr.statut <> 'OPEN'
       AND cr.final_amount <> cr.init_amount
           + COALESCE((SELECT sum(i.amount) FROM cash_register_item i
                        WHERE i.cash_register_id = cr.id AND i.payment_mode_code = 'CASH'
                          AND i.type_transaction NOT IN ('SORTIE_CAISSE', 'FONDS_CAISSE', 'REGLMENT_FOURNISSEUR')), 0)
           - COALESCE((SELECT sum(i.amount) FROM cash_register_item i
                        WHERE i.cash_register_id = cr.id AND i.payment_mode_code = 'CASH'
                          AND i.type_transaction IN ('SORTIE_CAISSE', 'FONDS_CAISSE', 'REGLMENT_FOURNISSEUR')), 0);

    IF v_pf < 300 THEN RAISE EXCEPTION 'Règlements fournisseurs : % (attendu >= 300)', v_pf; END IF;
    IF v_sorties < 50 OR v_entrees < 10 THEN
        RAISE EXCEPTION 'Mouvements de caisse : % sortie(s), % entrée(s)', v_sorties, v_entrees;
    END IF;
    IF v_payees > 0 THEN RAISE EXCEPTION '% commande(s) payée(s) sans règlement', v_payees; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% règlement(s) sur une commande non payée', v_ecart; END IF;
    IF v_double > 0 THEN RAISE EXCEPTION '% commande(s) réglée(s) plusieurs fois', v_double; END IF;
    IF v_banque > 0 THEN RAISE EXCEPTION '% règlement(s) dont la banque contredit le mode', v_banque; END IF;
    IF v_z > 0 THEN RAISE EXCEPTION '% caisse(s) dont le fond ne ferme pas le ticket Z', v_z; END IF;

    RAISE NOTICE '% règlements fournisseurs, % sorties, % entrées de caisse.', v_pf, v_sorties, v_entrees;
END $$;

\echo '<< 14d_mouvements_caisse : terminé'
