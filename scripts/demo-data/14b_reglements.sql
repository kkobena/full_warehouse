-- ============================================================================
-- 14b_reglements.sql — Règlements différés et règlements de factures
--
-- Comble deux trous de cohérence repérés en ouvrant l'application :
--
--   1. Des ventes différées existaient sans le moindre règlement, donc l'écran
--      « Historique des règlements » (/api/reglements) restait vide alors que
--      des clients avaient un solde.
--   2. Des factures tiers-payant portaient le statut PAID ou PARTIALLY_PAID
--      avec un montant_regle non nul, sans AUCUN encaissement en face. Le
--      statut affirmait un règlement dont il n'existait aucune trace.
--
-- ATTENTION à la modélisation, source des deux méprises possibles :
--
--   * DifferePayment et InvoicePayment ne sont PAS des tables. Ce sont deux
--     sous-classes de PaymentTransaction en héritage SINGLE_TABLE : elles
--     vivent dans payment_transaction, distinguées par la colonne « dtype ».
--     Chercher une table « differe_payment » ne donne rien.
--   * Un règlement différé se rattache au CLIENT (differecustomer_id), pas à
--     la vente. Le lien vers les ventes passe par differe_payment_item. C'est
--     pourquoi les contrôles d'encaissement de 99_verification.sql, qui
--     joignent payment_transaction.sale_id, ne les voient pas — et n'ont pas
--     à les voir.
--   * Les deux colonnes de rattachement à la facture s'écrivent
--     « facture_tierspayant_id » et « facture_tierspayant_invoice_date »,
--     sans souligné entre « tiers » et « payant ».
--
-- Se place après 14_facturation.sql : les factures doivent exister.
-- ============================================================================

\i _header.sql

\echo '>> 14b_reglements : reglements differes et reglements de factures'

-- ---------------------------------------------------------------------------
-- 1. Ventes différées à solder
--
-- Deux clients sur trois viennent régler ; ce qui reste impayé n'est pas un
-- oubli, c'est la matière des écrans de créances et de relance.
-- ---------------------------------------------------------------------------
-- Chaque client qui règle le fait à UNE date, répartie sur toute la période :
-- il solde alors tout ce qu'il devait avant cette date. C'est le comportement
-- réel — on vient éponger son ardoise — et cela étale les règlements au lieu
-- de les tasser en début d'historique.
--
-- L'étalement n'est pas cosmétique : l'écran « Historique des règlements »
-- s'ouvre sur les 30 derniers jours. Des règlements tous vieux de six mois
-- donneraient un écran vide, exactement le défaut qu'on corrige ici.
CREATE TEMP TABLE tmp_differe_client AS
SELECT
    c.customer_id,
    -- Réparti sur les 120 derniers jours ; le pas premier (17) évite que les
    -- règlements ne se groupent sur quelques dates. La fenêtre est plus courte
    -- que l'historique des ventes (180 jours) pour qu'un quart des règlements
    -- tombe dans les 30 derniers jours — la vue par défaut de l'écran.
    (CURRENT_DATE - (INTERVAL '1 day' * ((c.rang * 17) % 120)))::date AS jour_reglement
FROM (
    SELECT s.customer_id,
           row_number() OVER (ORDER BY s.customer_id) AS rang
      FROM sales s
     WHERE s.differe AND s.payment_status = 'IMPAYE' AND s.amount_to_be_paid > 0
     GROUP BY s.customer_id
) c
WHERE c.customer_id % 3 <> 0;        -- deux clients sur trois règlent

-- Ce qui reste dû après cette date demeure en créance : sans créances vivantes,
-- l'écran des soldes clients et les relances n'auraient rien à montrer.
CREATE TEMP TABLE tmp_differe AS
SELECT s.id, s.sale_date, s.customer_id, s.amount_to_be_paid, c.jour_reglement
  FROM sales s
  JOIN tmp_differe_client c ON c.customer_id = s.customer_id
 WHERE s.differe
   AND s.payment_status = 'IMPAYE'
   AND s.amount_to_be_paid > 0
   AND s.sale_date < c.jour_reglement;

-- Un règlement par client, rattaché à une caisse réellement ouverte :
-- PaymentTransaction.cashRegister est obligatoire, et une caisse inventée
-- serait invisible dans les états.
CREATE TEMP TABLE tmp_differe_paiement AS
SELECT
    nextval('id_transaction_seq') AS id,
    g.customer_id,
    g.montant,
    reg.id   AS cash_register_id,
    reg.jour AS transaction_date,
    row_number() OVER (ORDER BY g.customer_id) AS rang
FROM (
    SELECT customer_id, sum(amount_to_be_paid)::int AS montant, max(jour_reglement) AS jour
      FROM tmp_differe GROUP BY customer_id
) g
CROSS JOIN LATERAL (
    -- Première caisse ouverte à partir de la date de règlement. Le repli sur
    -- « >= » garantit qu'une caisse est toujours trouvée, y compris si le jour
    -- retenu est un lundi de fermeture.
    SELECT cr.id, cr.begin_time::date AS jour
      FROM cash_register cr
     WHERE cr.begin_time::date >= g.jour
     ORDER BY cr.begin_time
     LIMIT 1
) reg;

INSERT INTO payment_transaction (
    dtype, id, transaction_date,
    expected_amount, paid_amount, reel_amount, montant_verse,
    amount_to_be_taken_into_account,
    credit, categorie_ca, type_transaction, payment_mode_code,
    cash_register_id, differecustomer_id, created_at
)
SELECT
    'DifferePayment', p.id, p.transaction_date,
    p.montant, p.montant, p.montant, p.montant,
    -- Le chiffre d'affaires a déjà été déclaré à la vente : encaisser une
    -- créance ne le crée pas une seconde fois.
    0,
    false, 'CA', 'REGLEMENT_DIFFERE',
    CASE (p.rang % 5)
        WHEN 0 THEN 'CB' WHEN 1 THEN 'OM' WHEN 2 THEN 'WAVE' WHEN 3 THEN 'CH'
        ELSE 'CASH'
    END,
    p.cash_register_id, p.customer_id,
    p.transaction_date + TIME '11:00:00'
FROM tmp_differe_paiement p;

-- differe_payment_item.id est en IDENTITY : la colonne est omise.
--
-- expected_amount vaut 0 et non le montant dû : ReglementDiffereServiceImpl le
-- renseigne APRÈS avoir ramené restToPay à zéro, c'est donc le reliquat, pas
-- l'attendu. On reproduit ce comportement plutôt qu'une lecture plus logique
-- mais divergente de l'application.
INSERT INTO differe_payment_item (
    expected_amount, paid_amount,
    differe_payment_id, differe_payment_transaction_date,
    sale_id, sale_sale_date
)
SELECT 0, d.amount_to_be_paid, p.id, p.transaction_date, d.id, d.sale_date
  FROM tmp_differe d
  JOIN tmp_differe_paiement p ON p.customer_id = d.customer_id;

-- La vente soldée bascule à PAYE ; payroll_amount enregistre l'encaissé.
UPDATE sales s
   SET payment_status = 'PAYE',
       rest_to_pay    = 0,
       payroll_amount = s.amount_to_be_paid
  FROM tmp_differe d
 WHERE s.id = d.id AND s.sale_date = d.sale_date;

-- ---------------------------------------------------------------------------
-- 2. Règlements de factures tiers-payant
--
-- Une facture PAID ou PARTIALLY_PAID porte un montant_regle : il lui faut son
-- encaissement. Le mode retenu est le virement, comme dans la réalité pour un
-- organisme.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_facture_paiement AS
SELECT
    nextval('id_transaction_seq') AS id,
    f.id   AS facture_id,
    f.invoice_date,
    f.montant_ttc,
    f.montant_regle,
    reg.id   AS cash_register_id,
    reg.jour AS transaction_date
FROM facture_tiers_payant f
CROSS JOIN LATERAL (
    SELECT cr.id, cr.begin_time::date AS jour
      FROM cash_register cr
     WHERE cr.begin_time::date >= LEAST(f.invoice_date + 20, CURRENT_DATE)
     ORDER BY cr.begin_time
     LIMIT 1
) reg
-- Les factures de GROUPE sont écartées : leur règlement n'est pas un
-- encaissement de plus mais un encaissement PARENT, qui chapeaute ceux de ses
-- filles. Il est posé plus bas. Une parente se reconnaît à son organisme nul.
WHERE f.montant_regle > 0
  AND f.tiers_payant_id IS NOT NULL;

INSERT INTO payment_transaction (
    dtype, id, transaction_date,
    expected_amount, paid_amount, reel_amount, montant_verse,
    amount_to_be_taken_into_account,
    credit, categorie_ca, type_transaction, payment_mode_code,
    cash_register_id,
    facture_tierspayant_id, facture_tierspayant_invoice_date,
    grouped, created_at
)
SELECT
    'InvoicePayment', p.id, p.transaction_date,
    p.montant_ttc, p.montant_regle, p.montant_regle, p.montant_regle,
    -- Même raison qu'au différé : le CA a été déclaré à la vente.
    0,
    false, 'CA', 'REGLEMENT_TIERS_PAYANT', 'VIREMENT',
    p.cash_register_id,
    p.facture_id, p.invoice_date,
    false,
    p.transaction_date + TIME '15:00:00'
FROM tmp_facture_paiement p;

-- Un item par bon de la facture : c'est le détail qu'affiche le suivi des
-- créances, bon par bon. montant_regle a déjà été ventilé par 14_facturation.
INSERT INTO invoice_payment_item (
    id, transaction_date, montant_attendu, montant_paye,
    invoice_payment_id, invoice_payment_transaction_date,
    third_party_sale_line_id, third_party_sale_sale_date
)
SELECT
    nextval('id_transaction_item_seq'), p.transaction_date,
    t.montant, t.montant_regle,
    p.id, p.transaction_date,
    t.id, t.sale_date
FROM tmp_facture_paiement p
JOIN third_party_sale_line t
  ON t.facture_tiers_payant_id = p.facture_id
 AND t.invoice_date = p.invoice_date
WHERE t.montant_regle > 0;

-- ---------------------------------------------------------------------------
-- 2bis. Règlement d'une facture de GROUPE
--
-- ReglementGroupeFactureService ne produit pas un encaissement de plus : il
-- produit UN encaissement parent, porté par la facture de groupe et marqué
-- « grouped », auquel se rattachent les encaissements de chaque fille par
-- (parent_id, parent_transaction_date). Le détail bon par bon reste sur les
-- filles ; la parente ne totalise que ses enfants.
--
-- Sans cette forme, l'écran « Historique des règlements » filtré sur les
-- règlements groupés restait vide, et la facture de groupe affichait un montant
-- réglé sans le moindre encaissement en face — le défaut même que la section 2
-- corrige pour les factures individuelles.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_paiement_groupe AS
SELECT
    nextval('id_transaction_seq') AS id,
    f.id   AS facture_id,
    f.invoice_date,
    f.montant_ttc,
    f.montant_regle,
    reg.id   AS cash_register_id,
    reg.jour AS transaction_date
FROM facture_tiers_payant f
CROSS JOIN LATERAL (
    -- Même règle que pour les filles : leurs encaissements tombent donc le
    -- même jour que celui de la parente, ce qui est le cas réel — un règlement
    -- groupé est un seul virement.
    SELECT cr.id, cr.begin_time::date AS jour
      FROM cash_register cr
     WHERE cr.begin_time::date >= LEAST(f.invoice_date + 20, CURRENT_DATE)
     ORDER BY cr.begin_time
     LIMIT 1
) reg
WHERE f.tiers_payant_id IS NULL
  AND f.groupe_tiers_payant_id IS NOT NULL
  AND f.montant_regle > 0;

INSERT INTO payment_transaction (
    dtype, id, transaction_date,
    expected_amount, paid_amount, reel_amount, montant_verse,
    amount_to_be_taken_into_account,
    credit, categorie_ca, type_transaction, payment_mode_code,
    cash_register_id,
    facture_tierspayant_id, facture_tierspayant_invoice_date,
    grouped, created_at
)
SELECT
    'InvoicePayment', p.id, p.transaction_date,
    p.montant_ttc, p.montant_regle, p.montant_regle, p.montant_regle,
    0,
    false, 'CA', 'REGLEMENT_TIERS_PAYANT', 'VIREMENT',
    p.cash_register_id,
    p.facture_id, p.invoice_date,
    true,
    p.transaction_date + TIME '15:00:00'
FROM tmp_paiement_groupe p;

-- Les encaissements des filles, déjà posés par la section 2, désignent
-- maintenant leur parent.
UPDATE payment_transaction fille
   SET parent_id               = p.id,
       parent_transaction_date = p.transaction_date
  FROM tmp_paiement_groupe p
  JOIN facture_tiers_payant ff
    ON ff.groupe_facture_tiers_payant_id = p.facture_id
   AND ff.groupe_facture_tiers_payant_invoice_date = p.invoice_date
 WHERE fille.dtype = 'InvoicePayment'
   AND NOT fille.grouped
   AND fille.facture_tierspayant_id = ff.id
   AND fille.facture_tierspayant_invoice_date = ff.invoice_date;

DROP TABLE tmp_paiement_groupe;
DROP TABLE tmp_facture_paiement;
DROP TABLE tmp_differe_paiement;
DROP TABLE tmp_differe;
DROP TABLE tmp_differe_client;

-- ---------------------------------------------------------------------------
-- 3. Recalage des caisses
--
-- 09_ventes.sql a déjà calé final_amount sur les espèces encaissées, mais les
-- règlements différés réglés en espèces s'ajoutent APRÈS : sans ce second
-- passage, trois caisses affichaient un total inférieur à ce qu'elles
-- contenaient. C'est le premier écart que relève un pharmacien.
--
-- Recalcul complet et non incrémental : rejouable, et indépendant de l'ordre
-- dans lequel les règlements ont été posés.
-- ---------------------------------------------------------------------------
UPDATE cash_register cr
   SET final_amount = cr.init_amount + COALESCE(e.total, 0)
  FROM (
      SELECT pt.cash_register_id, sum(pt.paid_amount)::bigint AS total
        FROM payment_transaction pt
       WHERE pt.payment_mode_code = 'CASH'
       GROUP BY pt.cash_register_id
  ) e
 WHERE e.cash_register_id = cr.id;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_dif int; v_dif_items int; v_inv int; v_inv_items int;
    v_solde int; v_ecart int; v_orphelins int; v_recents int;
    v_ecart_inv int; v_groupe int; v_ecart_groupe int;
BEGIN
    SELECT count(*) INTO v_dif FROM payment_transaction WHERE dtype = 'DifferePayment';
    SELECT count(*) INTO v_dif_items FROM differe_payment_item;
    SELECT count(*) INTO v_inv FROM payment_transaction WHERE dtype = 'InvoicePayment';
    SELECT count(*) INTO v_inv_items FROM invoice_payment_item;

    -- Il doit RESTER des créances : une démo où tout est soldé ne montre ni
    -- solde client, ni relance, ni suivi de créance.
    SELECT count(*) INTO v_solde FROM sales WHERE differe AND payment_status = 'IMPAYE';

    SELECT count(*) INTO v_recents FROM payment_transaction
     WHERE dtype = 'DifferePayment'
       AND transaction_date >= CURRENT_DATE - 30;

    -- Le règlement différé doit égaler la somme de ses items.
    SELECT count(*) INTO v_ecart FROM (
        SELECT pt.id FROM payment_transaction pt
          JOIN differe_payment_item i
            ON i.differe_payment_id = pt.id
           AND i.differe_payment_transaction_date = pt.transaction_date
         WHERE pt.dtype = 'DifferePayment'
         GROUP BY pt.id, pt.paid_amount
        HAVING sum(i.paid_amount) <> pt.paid_amount
    ) x;

    -- Le règlement d'une facture doit égaler la somme de ses items, exactement
    -- comme on l'exige du différé juste au-dessus. Ce contrôle manquait, et
    -- c'est lui qui laissait passer l'écart d'arrondi entre le montant réglé de
    -- la facture et celui de ses bons. Les encaissements groupés en sont exclus :
    -- ils n'ont pas d'item, ils ont des enfants.
    SELECT count(*) INTO v_ecart_inv FROM (
        SELECT pt.id FROM payment_transaction pt
          JOIN invoice_payment_item i
            ON i.invoice_payment_id = pt.id
           AND i.invoice_payment_transaction_date = pt.transaction_date
         WHERE pt.dtype = 'InvoicePayment' AND NOT pt.grouped
         GROUP BY pt.id, pt.paid_amount
        HAVING sum(i.montant_paye) <> pt.paid_amount
    ) x;

    -- Un encaissement groupé totalise ceux de ses filles.
    SELECT count(*) INTO v_groupe FROM payment_transaction
     WHERE dtype = 'InvoicePayment' AND grouped;

    SELECT count(*) INTO v_ecart_groupe FROM (
        SELECT p.id FROM payment_transaction p
          JOIN payment_transaction c
            ON c.parent_id = p.id AND c.parent_transaction_date = p.transaction_date
         WHERE p.dtype = 'InvoicePayment' AND p.grouped
         GROUP BY p.id, p.paid_amount
        HAVING sum(c.paid_amount) <> p.paid_amount
    ) y;

    -- Toute facture affichant un règlement doit en avoir un.
    SELECT count(*) INTO v_orphelins FROM facture_tiers_payant f
     WHERE f.montant_regle > 0
       AND NOT EXISTS (
           SELECT 1 FROM payment_transaction pt
            WHERE pt.dtype = 'InvoicePayment'
              AND pt.facture_tierspayant_id = f.id
              AND pt.facture_tierspayant_invoice_date = f.invoice_date
       );

    IF v_dif = 0 THEN RAISE EXCEPTION 'Aucun reglement differe genere'; END IF;
    IF v_dif_items = 0 THEN RAISE EXCEPTION 'Aucun item de reglement differe'; END IF;
    IF v_inv = 0 THEN RAISE EXCEPTION 'Aucun reglement de facture genere'; END IF;
    IF v_inv_items = 0 THEN RAISE EXCEPTION 'Aucun item de reglement de facture'; END IF;
    IF v_solde = 0 THEN RAISE EXCEPTION 'Plus aucune creance client : rien a afficher'; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% reglement(s) differe(s) incoherent(s)', v_ecart; END IF;
    -- Le defaut d'origine : des reglements existaient, mais tous anterieurs a
    -- la fenetre par defaut de l'ecran, qui affichait donc une liste vide.
    IF v_recents = 0 THEN
        RAISE EXCEPTION 'Aucun reglement differe dans les 30 derniers jours : ecran vide par defaut';
    END IF;
    IF v_orphelins > 0 THEN
        RAISE EXCEPTION '% facture(s) reglee(s) sans encaissement', v_orphelins;
    END IF;
    IF v_ecart_inv > 0 THEN
        RAISE EXCEPTION '% reglement(s) de facture dont le montant contredit ses items', v_ecart_inv;
    END IF;
    IF v_groupe = 0 THEN
        RAISE EXCEPTION 'Aucun reglement groupe : l''historique filtre sur les groupes reste vide';
    END IF;
    IF v_ecart_groupe > 0 THEN
        RAISE EXCEPTION '% reglement(s) groupe(s) dont le montant contredit ses filles', v_ecart_groupe;
    END IF;

    RAISE NOTICE '% reglements differes (dont % sur 30 jours, % items), % reglements de factures (dont % groupes, % items), % creance(s) restante(s).',
                 v_dif, v_recents, v_dif_items, v_inv, v_groupe, v_inv_items, v_solde;
END $$;

\echo '<< 14b_reglements : termine'
