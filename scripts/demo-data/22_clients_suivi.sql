-- ============================================================================
-- 22_clients_suivi.sql — Suivi du client : consentements, dossier de santé, traitements
--                         chroniques, crédit, retours et avoirs
--
-- Les fonctions de la fiche client (migrations V2.1.13 à V2.1.17, V2.1.26) n'avaient aucune
-- donnée : l'onglet santé, la liste des traitements à renouveler, les relances de créance,
-- la limite de crédit, les retours et les avoirs s'ouvraient vides.
--
-- Tout se construit SUR les ventes déjà posées, pour rester cohérent avec elles :
--
--   * une allergie à une molécule n'est posée que sur un client qui n'en a jamais acheté —
--     sauf quelques cas où elle a été découverte après coup, avec la dérogation de la
--     dernière délivrance (la vente a eu lieu malgré l'alerte, donc l'alerte est tracée) ;
--   * un traitement chronique est celui que le client achète réellement, régulièrement ;
--   * une relance de créance suppose un solde différé encore dû et un téléphone ;
--   * une dérogation de limite de crédit désigne une vente différée qui l'a effectivement
--     dépassée ;
--   * un retour désigne une ligne vendue et ne dépasse jamais sa quantité ; la part du
--     tiers payant est celle de la vente d'origine.
--
-- Après 21 : tout ce dont dépend ce script (ventes, différés, règlements) est définitif.
-- ============================================================================

\i _header.sql

\echo '>> 22_clients_suivi : consentements, santé, traitements, crédit, retours, avoirs'

SELECT id AS admin_id FROM app_user WHERE login = 'admin' \gset

-- ---------------------------------------------------------------------------
-- 1. Consentements aux messages
--
-- Les clients joignables ont accordé le SMS à la création de leur fiche ; un sur dix l'a
-- retiré depuis (c'est l'état le plus récent qui compte). WhatsApp et courriel sont
-- l'exception.
-- ---------------------------------------------------------------------------
INSERT INTO customer_consentement (customer_id, canal, accorde, user_id, created_at)
SELECT c.id, 'SMS', true, :admin_id, c.created_at + INTERVAL '5 minutes'
  FROM customer c WHERE c.phone IS NOT NULL;

INSERT INTO customer_consentement (customer_id, canal, accorde, user_id, created_at)
SELECT c.id, 'SMS', false, :admin_id, NOW() - (INTERVAL '1 day' * (5 + c.id % 300))
  FROM customer c WHERE c.phone IS NOT NULL AND c.id % 10 = 0;

INSERT INTO customer_consentement (customer_id, canal, accorde, user_id, created_at)
SELECT c.id, 'WHATSAPP', true, :admin_id, c.created_at + INTERVAL '6 minutes'
  FROM customer c WHERE c.phone IS NOT NULL AND c.id % 4 = 0;

INSERT INTO customer_consentement (customer_id, canal, accorde, user_id, created_at)
SELECT c.id, 'EMAIL', true, :admin_id, c.created_at + INTERVAL '7 minutes'
  FROM customer c WHERE c.email IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 2. Dossier de santé et allergies
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_ds AS
SELECT c.id AS customer_id, c.sexe, c.dat_naiss,
       row_number() OVER (ORDER BY c.id) AS rang
  FROM customer c
 WHERE c.id % 4 = 1 AND c.status = 'ENABLE';

INSERT INTO customer_dossier_sante (
    customer_id, pathologies, grossesse, date_terme, allaitement,
    poids_kg, date_pesee, note, updated_at, updated_by_id
)
SELECT d.customer_id,
       (SELECT COALESCE(jsonb_agg(p.label ORDER BY p.k), '[]'::jsonb)
          FROM (SELECT k, (ARRAY['Diabète de type 2', 'Hypertension artérielle', 'Asthme',
                                 'Drépanocytose', 'Insuffisance rénale chronique', 'Épilepsie',
                                 'Hypothyroïdie', 'Glaucome', 'Insuffisance cardiaque',
                                 'Ulcère gastrique'])[1 + (d.rang + k * 3) % 10] AS label
                  FROM generate_series(0, d.rang % 3 - 1) k) p),
       g.grossesse,
       CASE WHEN g.grossesse THEN CURRENT_DATE + (20 + d.customer_id % 220) END,
       COALESCE((NOT g.grossesse) AND d.sexe = 'F' AND d.rang % 13 = 0, false),
       (45 + (d.customer_id * 7) % 50)::numeric(5, 2),
       CURRENT_DATE - (d.customer_id % 300),
       CASE WHEN d.rang % 6 = 0 THEN 'Suivi régulier par son médecin traitant.' END,
       NOW() - (INTERVAL '1 day' * (d.customer_id % 200)),
       :admin_id
  FROM tmp_ds d
  CROSS JOIN LATERAL (SELECT COALESCE(d.sexe = 'F' AND d.rang % 9 = 0
                              AND d.dat_naiss BETWEEN CURRENT_DATE - 42 * 365 AND CURRENT_DATE - 19 * 365, false) AS grossesse) g;

-- Allergies à une molécule que le client n'a JAMAIS achetée.
CREATE TEMP TABLE tmp_allergene AS
SELECT d.id AS dci_id, row_number() OVER (ORDER BY d.libelle) - 1 AS rang
  FROM dci d
 WHERE d.libelle IN ('AMOXICILLINE', 'IBUPROFENE', 'DICLOFENAC', 'ACIDE ACETYLSALICYLIQUE',
                     'COTRIMOXAZOLE', 'METRONIDAZOLE', 'TRAMADOL', 'CIPROFLOXACINE');

INSERT INTO customer_allergie (customer_id, dci_id, reaction, created_at)
SELECT d.customer_id, a.dci_id,
       (ARRAY['Urticaire', 'Éruption cutanée', 'Œdème de Quincke', 'Choc anaphylactique',
              'Troubles digestifs sévères', 'Bronchospasme'])[1 + d.rang % 6],
       NOW() - (INTERVAL '1 day' * (30 + d.customer_id % 700))
  FROM tmp_ds d
  CROSS JOIN LATERAL (
      SELECT t.dci_id
        FROM tmp_allergene t
       WHERE NOT EXISTS (
           SELECT 1 FROM sales s
             JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
             JOIN produit_dci pd ON pd.produit_id = sl.produit_id
            WHERE s.customer_id = d.customer_id AND pd.dci_id = t.dci_id)
       ORDER BY (t.rang + d.rang) % 8
       LIMIT 1
  ) a
 WHERE d.rang % 2 = 0;

-- Une allergie sans molécule connue : texte libre.
INSERT INTO customer_allergie (customer_id, libelle, reaction, created_at)
SELECT d.customer_id,
       (ARRAY['Latex', 'Pénicillines (antécédent familial)', 'Sulfamides', 'Iode'])[1 + d.rang % 4],
       'Réaction signalée par le patient',
       NOW() - (INTERVAL '1 day' * (10 + d.customer_id % 400))
  FROM tmp_ds d
 WHERE d.rang % 11 = 0;

-- Les cas de DÉROGATION : l'allergie a été découverte après des achats de la molécule, et
-- la dernière délivrance a eu lieu malgré l'alerte. Seule cette dernière est dérogée : toute
-- vente ultérieure serait bloquée, et il n'y en a pas.
CREATE TEMP TABLE tmp_derog AS
SELECT x.*, row_number() OVER (ORDER BY x.moment) AS rang
  FROM (
      SELECT DISTINCT ON (s.customer_id)
             s.customer_id, sl.produit_id, t.dci_id, sl.created_at AS moment, s.caissier_id,
             d.libelle AS molecule
        FROM sales s
        JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
        JOIN produit_dci pd ON pd.produit_id = sl.produit_id
        JOIN tmp_allergene t ON t.dci_id = pd.dci_id
        JOIN dci d ON d.id = t.dci_id
       WHERE s.customer_id IS NOT NULL
         AND s.customer_id % 4 = 3
         AND s.sale_date >= CURRENT_DATE - 600
         AND NOT EXISTS (SELECT 1 FROM customer_allergie ca WHERE ca.customer_id = s.customer_id)
       ORDER BY s.customer_id, sl.created_at DESC
  ) x;

DELETE FROM tmp_derog WHERE rang > 8;

INSERT INTO customer_allergie (customer_id, dci_id, reaction, created_at)
SELECT customer_id, dci_id, 'Éruption cutanée', moment - INTERVAL '3 days' FROM tmp_derog;

-- Une dérogation par ligne délivrée malgré l'alerte : une même vente peut contenir plusieurs
-- produits porteurs de la molécule, et chacun a levé son alerte.
INSERT INTO alerte_sante_derogation (customer_id, produit_id, alertes, motif, user_id, autorise_par_id, created_at)
SELECT dr.customer_id, sl.produit_id,
       'Allergie connue à ' || dr.molecule || ' : ce produit contient ' || dr.molecule || '. Réaction signalée : Éruption cutanée.',
       (ARRAY['Prescription du médecin maintenue malgré l''allergie signalée',
              'Le patient confirme avoir déjà pris ce produit sans réaction',
              'Pas d''alternative disponible, accord du pharmacien'])[1 + dr.rang % 3],
       s.caissier_id, :admin_id, sl.created_at
  FROM tmp_derog dr
  JOIN customer_allergie ca ON ca.customer_id = dr.customer_id AND ca.dci_id = dr.dci_id
  JOIN sales s ON s.customer_id = dr.customer_id AND s.created_at > ca.created_at
  JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
  JOIN produit_dci pd ON pd.produit_id = sl.produit_id AND pd.dci_id = dr.dci_id;


DROP TABLE tmp_derog;
DROP TABLE tmp_allergene;
DROP TABLE tmp_ds;

-- ---------------------------------------------------------------------------
-- 3. Traitements chroniques : le produit que le client achète le plus souvent
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_tc AS
SELECT x.customer_id, x.produit_id, x.dci_id, x.derniere_vente, x.nb,
       (row_number() OVER (ORDER BY x.customer_id))::int AS rang
  FROM (
      SELECT DISTINCT ON (s.customer_id)
             s.customer_id, sl.produit_id, pd.dci_id,
             max(s.sale_date) AS derniere_vente, count(*) AS nb
        FROM sales s
        JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
        JOIN produit_dci pd ON pd.produit_id = sl.produit_id AND pd.rang = 1
       WHERE s.customer_id IS NOT NULL
         AND s.sale_date >= CURRENT_DATE - 365
       GROUP BY s.customer_id, sl.produit_id, pd.dci_id
       ORDER BY s.customer_id, count(*) DESC, sl.produit_id
  ) x
 WHERE x.nb >= 2;

INSERT INTO customer_traitement_chronique (
    customer_id, dci_id, produit_id, dosage, posologie, duree_jours,
    date_ordonnance, date_fin_ordonnance, note, actif, created_at, updated_at, updated_by_id
)
SELECT t.customer_id, t.dci_id, t.produit_id,
       (ARRAY['500 mg', '1 g', '10 mg', '5 mg', '20 mg'])[1 + t.rang % 5],
       (ARRAY['1 comprimé matin et soir', '1 comprimé par jour', '2 comprimés par jour',
              '1 comprimé le soir'])[1 + t.rang % 4],
       (ARRAY[30, 60, 90])[1 + t.rang % 3],
       t.derniere_vente - (t.rang % 12),
       t.derniere_vente - (t.rang % 12) + (ARRAY[180, 365, 90])[1 + t.rang % 3],
       CASE WHEN t.rang % 7 = 0 THEN 'Renouvellement à vérifier avec le médecin.' END,
       -- Un traitement sur douze a été arrêté.
       t.rang % 12 <> 0,
       (t.derniere_vente - (t.rang % 12))::timestamp + TIME '10:00:00',
       NOW() - (INTERVAL '1 day' * (t.rang % 60)),
       :admin_id
  FROM tmp_tc t
 WHERE t.rang <= 90;

DROP TABLE tmp_tc;

-- ---------------------------------------------------------------------------
-- 4. Relances de créance : SMS aux clients qui ont un solde différé et un téléphone
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_solde AS
SELECT s.customer_id, sum(s.rest_to_pay)::int AS solde, min(s.sale_date) AS plus_ancienne
  FROM sales s
 WHERE s.differe AND s.payment_status = 'IMPAYE' AND s.rest_to_pay > 0
 GROUP BY s.customer_id;

INSERT INTO relance_differe (customer_id, telephone, montant, message, user_id, created_at)
SELECT so.customer_id, c.phone, so.solde,
       'Bonjour ' || COALESCE(c.first_name, '') || ', ' || (SELECT name FROM magasin ORDER BY id LIMIT 1)
         || ' vous rappelle un solde de ' || replace(to_char(so.solde, 'FM999G999G999'), ',', ' ') || ' '
         || (SELECT value FROM app_configuration WHERE name = 'APP_DEVISE')
         || ' à régler. Merci de votre visite.',
       :admin_id,
       NOW() - (INTERVAL '1 day' * (3 + (so.customer_id + n) % 40)) - (INTERVAL '1 hour' * n)
  FROM tmp_solde so
  JOIN customer c ON c.id = so.customer_id
  -- Pas de relance à un client qui a retiré son consentement : le service la refuserait.
  CROSS JOIN LATERAL generate_series(1, 1 + so.customer_id % 3) n
 WHERE c.phone IS NOT NULL
   AND so.plus_ancienne < CURRENT_DATE - 20
   AND NOT EXISTS (
       SELECT 1 FROM customer_consentement cc
        WHERE cc.customer_id = c.id AND cc.canal = 'SMS' AND NOT cc.accorde
          AND cc.created_at = (SELECT max(x.created_at) FROM customer_consentement x
                                WHERE x.customer_id = c.id AND x.canal = 'SMS'));

DROP TABLE tmp_solde;

-- ---------------------------------------------------------------------------
-- 5. Limite de crédit et dérogations
--
-- La limite est unique pour l'officine. On la pose juste sous les plus gros encours observés,
-- de sorte que seules quelques ventes différées l'aient franchie — et chacune de celles-là a
-- sa dérogation. L'encours d'une vente est le solde différé du client AVANT elle : ses
-- ventes différées antérieures, moins ce qu'il avait déjà réglé à cette date.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_encours AS
SELECT s.id AS sale_id, s.sale_date, s.customer_id, s.amount_to_be_paid AS montant, s.caissier_id,
       s.created_at AS moment,
       COALESCE((
           SELECT sum(p.amount_to_be_paid) FROM sales p
            WHERE p.customer_id = s.customer_id AND p.differe
              AND (p.sale_date < s.sale_date OR (p.sale_date = s.sale_date AND p.id < s.id))
              AND NOT EXISTS (
                  SELECT 1 FROM differe_payment_item i
                   WHERE i.sale_id = p.id AND i.sale_sale_date = p.sale_date
                     AND i.differe_payment_transaction_date <= s.sale_date)
       ), 0)::int AS encours
  FROM sales s
 WHERE s.differe AND s.amount_to_be_paid > 0 AND s.customer_id IS NOT NULL;

SELECT COALESCE((SELECT (floor((encours + montant) / 50000.0) * 50000)::int
                   FROM tmp_encours ORDER BY encours + montant DESC OFFSET 9 LIMIT 1), 0) AS limite \gset

UPDATE app_configuration
   SET value = :'limite', updated = NOW()
 WHERE name = 'APP_LIMITE_CREDIT_CLIENT' AND :limite > 0;

INSERT INTO limite_credit_derogation (
    customer_id, sale_id, sale_date, montant, encours, limite, motif, user_id, autorise_par_id, created_at
)
SELECT e.customer_id, e.sale_id, e.sale_date, e.montant, e.encours, :limite,
       (ARRAY['Client de confiance, règlement prévu en fin de mois',
              'Traitement au long cours, ordonnance présentée',
              'Accord du pharmacien titulaire'])[1 + e.sale_id::int % 3],
       e.caissier_id, :admin_id, e.moment
  FROM tmp_encours e
 WHERE :limite > 0 AND e.encours + e.montant > :limite;

DROP TABLE tmp_encours;

-- ---------------------------------------------------------------------------
-- 6. Utilisation de la clé de sécurité
--
-- Une remise accordée par un caissier qui n'a pas le droit de la consentir se fait avec la
-- clé du pharmacien : l'usage est tracé. On prend une vente remisée sur cinq.
-- ---------------------------------------------------------------------------
INSERT INTO utilisation_cle_securite (
    caisse, commentaire, entity_id, entity_name, mvt_date, clesecuriteowner_id, connecteduser_id
)
SELECT p.name,
       'Remise de ' || s.discount_amount || ' accordée sur la vente ' || s.number_transaction,
       s.id, 'Sales', s.created_at, :admin_id, s.caissier_id
  FROM sales s
  JOIN cash_register cr ON cr.id = s.cash_register_id
  JOIN poste p ON p.id = s.caisse_id
 WHERE s.discount_amount > 0
   AND s.id % 40 = 3;

-- ---------------------------------------------------------------------------
-- 7. Retours clients
--
-- Une ligne vendue, une quantité de 1 (jamais plus que vendu), reprise un à vingt jours plus
-- tard. Les produits dont le statut interdit le retour (stupéfiants, psychotropes) et les
-- thermosensibles sont écartés. Toutes les lignes ont un défaut de conformité (emballage ouvert,
-- lot illisible ou péremption douteuse) : le produit part en quarantaine, il ne retourne pas
-- au stock — c'est ce que fait validerRetour pour un état non conforme, et c'est ce qui laisse
-- intacte la comptabilité des lots.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_rc AS
SELECT x.*, row_number() OVER (ORDER BY x.sale_date, x.line_id) AS rang
  FROM (
      SELECT sl.id AS line_id, sl.sale_date, s.id AS sale_id, s.number_transaction, s.customer_id,
             s.nature_vente, s.net_amount, s.part_assure, s.caissier_id, sl.produit_id,
             sl.regular_unit_price,
             LEAST(s.sale_date + 1 + (sl.id % 20)::int, CURRENT_DATE) AS jour
        FROM sales_line sl
        JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
        JOIN produit p ON p.id = sl.produit_id
       WHERE sl.id % 1700 = 11
         AND sl.quantity_sold >= 1
         AND NOT s.differe
         AND p.statut_legal NOT IN ('STUPEFIANTS', 'PSO')
         AND NOT COALESCE(p.thermosensible, false)
  ) x;

INSERT INTO retour_client (
    reference, created_at, validated_at, motif, mode_reglement, commentaire, montant_total,
    original_sale_id, original_sale_date, original_sale_ref, customer_id,
    created_by_id, validated_by_id, avec_echange, echange_sale_ref
)
SELECT to_char(r.jour, 'YYYYMMDD') || lpad(r.rang::text, 4, '0'),
       r.jour + TIME '15:10:00', r.jour + TIME '15:10:00',
       (ARRAY['ERREUR_DISPENSATION', 'PRODUIT_DEFECTUEUX', 'ERREUR_QUANTITE', 'INSATISFACTION', 'AUTRE'])[1 + r.rang % 5],
       CASE WHEN r.rang % 7 = 0 THEN 'AVOIR_CLIENT'
            ELSE (ARRAY['REMBOURSEMENT_ESPECES', 'REMBOURSEMENT_CB', 'AVOIR_CLIENT'])[1 + r.rang % 3] END,
       (ARRAY['Boîte ouverte, le client n''en a plus l''usage',
              'Le client signale un conditionnement abîmé',
              'Produit remis par erreur, ordonnance différente',
              NULL, 'Retour accepté par le pharmacien'])[1 + r.rang % 5],
       taux.montant,
       r.sale_id, r.sale_date, r.number_transaction, r.customer_id,
       r.caissier_id, r.caissier_id,
       r.rang % 11 = 0,
       CASE WHEN r.rang % 11 = 0 THEN
           (SELECT s2.number_transaction FROM sales s2
             WHERE s2.sale_date = r.jour AND s2.id <> r.sale_id ORDER BY s2.id LIMIT 1) END
  FROM tmp_rc r
 CROSS JOIN LATERAL (
     SELECT round(r.regular_unit_price * CASE WHEN r.nature_vente = 'COMPTANT' OR r.net_amount = 0 THEN 1.0
                                               ELSE r.part_assure::numeric / r.net_amount END)::int AS montant
 ) taux;

-- Les avec-échange forcent le mode « avoir client » (comme validerRetour).
UPDATE retour_client SET mode_reglement = 'AVOIR_CLIENT' WHERE avec_echange;

INSERT INTO retour_client_line (
    retour_client_id, produit_id, quantite, prix_unitaire, montant,
    original_sales_line_id, original_sales_line_date, montant_tp,
    emballage_intact, num_lot_lisible, date_peremption_valide
)
SELECT rc.id, r.produit_id, 1, r.regular_unit_price, rc.montant_total,
       r.line_id, r.sale_date, r.regular_unit_price - rc.montant_total,
       r.rang % 3 <> 0, r.rang % 3 <> 1, r.rang % 3 <> 2
  FROM tmp_rc r
  JOIN retour_client rc ON rc.reference = to_char(r.jour, 'YYYYMMDD') || lpad(r.rang::text, 4, '0');

-- ---------------------------------------------------------------------------
-- 8. Avoirs clients
--
-- Deux origines : les retours réglés par avoir (un avoir par ligne, valable 90 jours,
-- APP_DELAI_VALIDITE_AVOIR), et les produits DUS — marchandise payée au comptoir mais pas
-- remise faute de stock, que le client viendra chercher.
--
-- États : un avoir récent est OUVERT (parfois déjà entamé) ; un ancien est CLOTURE, par
-- remise du produit, bon d'avoir, compensation sur une vente ou remboursement, ou EXPIRE
-- s'il n'a jamais servi. Le détail des utilisations s'écrit avec la clôture.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_av AS
-- 8a. issus des retours réglés par avoir
SELECT rc.created_at AS cree, rc.customer_id, l.produit_id, l.quantite, l.montant,
       l.original_sales_line_id AS line_id, l.original_sales_line_date AS line_date,
       rc.created_at::date + 90 AS expiration, 'RETOUR' AS origine, rc.id AS source_id
  FROM retour_client rc
  JOIN retour_client_line l ON l.retour_client_id = rc.id
 WHERE rc.mode_reglement = 'AVOIR_CLIENT' AND l.montant > 0
UNION ALL
-- 8b. produits dus : une ligne vendue en quantité ≥ 2 à un client identifié, un cas sur 120
SELECT sl.created_at, s.customer_id, sl.produit_id, 1, sl.net_unit_price,
       sl.id, sl.sale_date, sl.sale_date + 90, 'DU', NULL::int
  FROM sales_line sl
  JOIN sales s ON s.id = sl.sales_id AND s.sale_date = sl.sales_sale_date
 WHERE sl.id % 120 = 5
   AND sl.quantity_sold >= 2 AND sl.net_unit_price > 0
   AND s.customer_id IS NOT NULL AND s.dtype = 'CashSale' AND NOT s.differe;

CREATE TEMP TABLE tmp_av2 AS
SELECT a.*, row_number() OVER (ORDER BY a.cree, a.produit_id) AS rang,
       (a.cree::date < CURRENT_DATE - 120) AS ancien
  FROM tmp_av a;

-- Statut et mode de clôture, déduits de l'ancienneté et d'un cycle déterministe.
ALTER TABLE tmp_av2 ADD COLUMN statut text;
ALTER TABLE tmp_av2 ADD COLUMN mode_cloture text;
UPDATE tmp_av2
   SET statut = CASE WHEN ancien AND origine = 'RETOUR' AND rang % 2 = 0 THEN 'EXPIRE'
                     WHEN ancien THEN 'CLOTURE'
                     WHEN rang % 13 = 0 THEN 'ANNULE'
                     ELSE 'OUVERT' END,
       mode_cloture = (ARRAY['RETOUR_PRODUIT', 'BON_AVOIR', 'COMPENSATION_VENTE',
                             'REMBOURSEMENT_ESPECES', 'REMBOURSEMENT_CB'])[1 + rang % 5];
-- Un avoir issu d'un retour se règle en argent ou en bon, jamais « produit remis ».
UPDATE tmp_av2 SET mode_cloture = 'BON_AVOIR' WHERE origine = 'RETOUR' AND mode_cloture = 'RETOUR_PRODUIT';
-- Une annulation n'a pas de mode de clôture.
UPDATE tmp_av2 SET mode_cloture = NULL WHERE statut IN ('OUVERT', 'EXPIRE', 'ANNULE');

INSERT INTO avoir_client (
    reference, created_at, cloture_le, statut, mode_cloture, quantite, montant, commentaire,
    customer_id, produit_id, sales_line_id, sales_line_date, created_by_id, closed_by_id,
    date_expiration, montant_utilise, quantite_remise
)
SELECT to_char(a.cree, 'YYYYMMDD') || lpad(a.rang::text, 4, '0'),
       a.cree,
       CASE WHEN a.statut = 'CLOTURE' THEN a.cree + INTERVAL '1 day' * (2 + a.rang % 18) END,
       a.statut, a.mode_cloture, a.quantite, a.montant,
       CASE a.origine WHEN 'RETOUR' THEN 'Avoir issu d''un retour client'
                      ELSE 'Produit réglé au comptoir mais non remis : rupture au moment de la vente' END,
       a.customer_id, a.produit_id, a.line_id, a.line_date,
       :admin_id,
       CASE WHEN a.statut = 'CLOTURE' THEN :admin_id END,
       a.expiration,
       CASE WHEN a.statut = 'CLOTURE' THEN a.montant
            WHEN a.statut = 'OUVERT' AND a.rang % 5 = 0 THEN a.montant / 2
            ELSE 0 END,
       CASE WHEN a.statut = 'CLOTURE' AND a.mode_cloture = 'RETOUR_PRODUIT' THEN a.quantite ELSE 0 END
  FROM tmp_av2 a;

-- Les utilisations : une par avoir clôturé ou entamé, du montant utilisé.
INSERT INTO avoir_client_utilisation (avoir_client_id, montant_utilise, utilise_le, commentaire, utilise_par_id)
SELECT av.id, av.montant_utilise,
       COALESCE(av.cloture_le, av.created_at + INTERVAL '3 days'),
       CASE WHEN av.mode_cloture = 'RETOUR_PRODUIT' THEN av.quantite_remise || ' unité(s) remise(s)'
            WHEN av.mode_cloture IS NOT NULL THEN 'Clôture : ' || av.mode_cloture
            ELSE 'Utilisation partielle sur une vente' END,
       :admin_id
  FROM avoir_client av
 WHERE av.montant_utilise > 0;

-- Les produits dus encore OUVERTS : la ligne de vente porte la quantité due, et l'avoir se
-- rattache à la commande en cours qui doit ramener le produit, quand il y en a une.
UPDATE sales_line sl
   SET quantity_avoir = av.quantite
  FROM avoir_client av
 WHERE av.statut = 'OUVERT' AND av.commentaire LIKE 'Produit réglé%'
   AND sl.id = av.sales_line_id AND sl.sale_date = av.sales_line_date;

UPDATE avoir_client av
   SET commande_id = x.commande_id, commande_order_date = x.order_date
  FROM (
      SELECT DISTINCT ON (ol_p.produit_id) ol_p.produit_id, c.id AS commande_id, c.order_date
        FROM order_line ol
        JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
        JOIN fournisseur_produit ol_p ON ol_p.id = ol.fournisseur_produit_id
       WHERE c.order_status = 'REQUESTED'
       ORDER BY ol_p.produit_id, c.order_date DESC
  ) x
 WHERE av.produit_id = x.produit_id
   AND av.statut = 'OUVERT' AND av.commentaire LIKE 'Produit réglé%';

-- Les remboursements en argent sortent de la caisse (AvoirClientDocumentServiceImpl) : espèces
-- ET carte, en SORTIE_CAISSE, sur la caisse du jour de la clôture. Même écriture que 14d.
CREATE TEMP TABLE tmp_av_sortie AS
SELECT nextval('id_transaction_seq') AS id, av.id AS avoir_id, av.reference, av.montant_utilise AS montant,
       av.cloture_le::date AS jour,
       CASE av.mode_cloture WHEN 'REMBOURSEMENT_ESPECES' THEN 'CASH' ELSE 'CB' END AS mode
  FROM avoir_client av
 WHERE av.statut = 'CLOTURE' AND av.mode_cloture IN ('REMBOURSEMENT_ESPECES', 'REMBOURSEMENT_CB')
   AND av.montant_utilise > 0 AND av.cloture_le::date < CURRENT_DATE;

INSERT INTO payment_transaction (
    dtype, id, transaction_date, categorie_ca, commentaire, created_at, credit,
    expected_amount, montant_verse, paid_amount, reel_amount,
    transaction_number, type_transaction, payment_mode_code,
    cash_register_id, amount_to_be_taken_into_account
)
SELECT 'DefaultPayment', t.id, reg.jour, 'CA', 'Remboursement avoir ' || t.reference,
       reg.jour + TIME '17:05:00', true,
       t.montant, t.montant, t.montant, t.montant,
       to_char(reg.jour, 'YYYYMMDD') || lpad((7000 + t.id % 900)::text, 4, '0'),
       'SORTIE_CAISSE', t.mode, reg.id, 0
  FROM tmp_av_sortie t
 CROSS JOIN LATERAL (
     SELECT cr.id, cr.begin_time::date AS jour FROM cash_register cr
      WHERE cr.statut <> 'OPEN'
      ORDER BY abs(cr.begin_time::date - t.jour), cr.begin_time LIMIT 1
 ) reg;

INSERT INTO cash_register_item (cash_register_id, payment_mode_code, amount, type_transaction)
SELECT pt.cash_register_id, pt.payment_mode_code, sum(pt.paid_amount)::bigint, pt.type_transaction
  FROM payment_transaction pt
 WHERE pt.id IN (SELECT id FROM tmp_av_sortie)
 GROUP BY pt.cash_register_id, pt.payment_mode_code, pt.type_transaction
ON CONFLICT (cash_register_id, payment_mode_code, type_transaction) DO UPDATE
   SET amount = cash_register_item.amount + EXCLUDED.amount;

UPDATE cash_register cr
   SET final_amount = cr.init_amount
       + COALESCE((SELECT sum(i.amount) FROM cash_register_item i
                    WHERE i.cash_register_id = cr.id AND i.payment_mode_code = 'CASH'
                      AND i.type_transaction NOT IN ('SORTIE_CAISSE', 'FONDS_CAISSE', 'REGLMENT_FOURNISSEUR')), 0)
       - COALESCE((SELECT sum(i.amount) FROM cash_register_item i
                    WHERE i.cash_register_id = cr.id AND i.payment_mode_code = 'CASH'
                      AND i.type_transaction IN ('SORTIE_CAISSE', 'FONDS_CAISSE', 'REGLMENT_FOURNISSEUR')), 0)
 WHERE cr.id IN (SELECT cash_register_id FROM payment_transaction WHERE id IN (SELECT id FROM tmp_av_sortie));

DROP TABLE tmp_av_sortie;
DROP TABLE tmp_av2;
DROP TABLE tmp_av;
DROP TABLE tmp_rc;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_cons int; v_dossiers int; v_all int; v_derog int; v_contre int; v_tc int; v_rel int;
    v_cle int; v_rc int; v_av int; v_depasse int; v_lim int; v_ret_qte int; v_ouvert_sans_ligne int;
    v_util int; v_z int;
BEGIN
    SELECT count(*) INTO v_cons FROM customer_consentement;
    SELECT count(*) INTO v_dossiers FROM customer_dossier_sante;
    SELECT count(*) INTO v_all FROM customer_allergie;
    SELECT count(*) INTO v_derog FROM alerte_sante_derogation;
    SELECT count(*) INTO v_tc FROM customer_traitement_chronique;
    SELECT count(*) INTO v_rel FROM relance_differe;
    SELECT count(*) INTO v_cle FROM utilisation_cle_securite;
    SELECT count(*) INTO v_rc FROM retour_client;
    SELECT count(*) INTO v_av FROM avoir_client;
    SELECT count(*) INTO v_lim FROM limite_credit_derogation;

    -- Une allergie à une molécule ne coexiste avec un achat de cette molécule APRÈS sa pose
    -- que sur une vente dérogée.
    SELECT count(*) INTO v_contre FROM customer_allergie ca
      JOIN sales s ON s.customer_id = ca.customer_id AND s.created_at > ca.created_at
      JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
      JOIN produit_dci pd ON pd.produit_id = sl.produit_id AND pd.dci_id = ca.dci_id
     WHERE ca.dci_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM alerte_sante_derogation d
                        WHERE d.customer_id = ca.customer_id AND d.produit_id = sl.produit_id
                          AND d.created_at = sl.created_at);

    -- Aucune relance sans solde ni au-delà du consentement.
    SELECT count(*) INTO v_depasse FROM relance_differe r
     WHERE NOT EXISTS (SELECT 1 FROM sales s WHERE s.customer_id = r.customer_id
                        AND s.differe AND s.payment_status = 'IMPAYE' AND s.rest_to_pay > 0);

    SELECT count(*) INTO v_ret_qte FROM retour_client_line l
      JOIN sales_line sl ON sl.id = l.original_sales_line_id AND sl.sale_date = l.original_sales_line_date
     WHERE l.quantite > sl.quantity_sold OR l.quantite <= 0;

    -- Une quantité due ouverte a sa ligne de vente marquée ; une ligne marquée a son avoir ouvert.
    SELECT count(*) INTO v_ouvert_sans_ligne FROM sales_line sl
     WHERE sl.quantity_avoir > 0
       AND NOT EXISTS (SELECT 1 FROM avoir_client a WHERE a.sales_line_id = sl.id
                        AND a.sales_line_date = sl.sale_date AND a.statut = 'OUVERT');

    SELECT count(*) INTO v_util FROM avoir_client a
     WHERE a.montant_utilise > a.montant
        OR (a.statut = 'CLOTURE' AND a.montant_utilise <> a.montant)
        OR a.montant_utilise <> COALESCE((SELECT sum(u.montant_utilise) FROM avoir_client_utilisation u
                                           WHERE u.avoir_client_id = a.id), 0);

    SELECT count(*) INTO v_z FROM cash_register cr
     WHERE cr.statut <> 'OPEN'
       AND cr.final_amount <> cr.init_amount
           + COALESCE((SELECT sum(i.amount) FROM cash_register_item i
                        WHERE i.cash_register_id = cr.id AND i.payment_mode_code = 'CASH'
                          AND i.type_transaction NOT IN ('SORTIE_CAISSE', 'FONDS_CAISSE', 'REGLMENT_FOURNISSEUR')), 0)
           - COALESCE((SELECT sum(i.amount) FROM cash_register_item i
                        WHERE i.cash_register_id = cr.id AND i.payment_mode_code = 'CASH'
                          AND i.type_transaction IN ('SORTIE_CAISSE', 'FONDS_CAISSE', 'REGLMENT_FOURNISSEUR')), 0);

    IF v_cons < 200 THEN RAISE EXCEPTION 'Consentements : % (attendu >= 200)', v_cons; END IF;
    IF v_dossiers < 50 THEN RAISE EXCEPTION 'Dossiers de santé : % (attendu >= 50)', v_dossiers; END IF;
    IF v_all < 30 THEN RAISE EXCEPTION 'Allergies : % (attendu >= 30)', v_all; END IF;
    IF v_derog = 0 THEN RAISE EXCEPTION 'Aucune dérogation d''alerte santé'; END IF;
    IF v_contre > 0 THEN RAISE EXCEPTION '% délivrance(s) malgré une allergie, sans dérogation', v_contre; END IF;
    IF v_tc < 30 THEN RAISE EXCEPTION 'Traitements chroniques : % (attendu >= 30)', v_tc; END IF;
    IF v_rel = 0 THEN RAISE EXCEPTION 'Aucune relance de créance'; END IF;
    IF v_depasse > 0 THEN RAISE EXCEPTION '% relance(s) sur un client sans solde', v_depasse; END IF;
    IF v_cle < 30 THEN RAISE EXCEPTION 'Usages de clé de sécurité : % (attendu >= 30)', v_cle; END IF;
    IF v_rc < 15 THEN RAISE EXCEPTION 'Retours clients : % (attendu >= 15)', v_rc; END IF;
    IF v_ret_qte > 0 THEN RAISE EXCEPTION '% ligne(s) de retour dépassant la quantité vendue', v_ret_qte; END IF;
    IF v_av < 15 THEN RAISE EXCEPTION 'Avoirs clients : % (attendu >= 15)', v_av; END IF;
    IF v_ouvert_sans_ligne > 0 THEN RAISE EXCEPTION '% quantité(s) due(s) sans avoir ouvert', v_ouvert_sans_ligne; END IF;
    IF v_util > 0 THEN RAISE EXCEPTION '% avoir(s) dont les utilisations contredisent le montant', v_util; END IF;
    IF v_z > 0 THEN RAISE EXCEPTION '% caisse(s) dont le fond ne ferme pas le ticket Z', v_z; END IF;

    RAISE NOTICE '% consentements, % dossiers, % allergies (% dérogées), % traitements, % relances, % clés, % retours, % avoirs, % dérogations de crédit.',
                 v_cons, v_dossiers, v_all, v_derog, v_tc, v_rel, v_cle, v_rc, v_av, v_lim;
END $$;

\echo '<< 22_clients_suivi : terminé'
