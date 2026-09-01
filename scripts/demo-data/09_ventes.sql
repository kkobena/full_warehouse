-- ============================================================================
-- 09_ventes.sql — Ventes, lignes, consommation FEFO des lots et règlements
--
-- Le script le plus contraint du jeu. Invariants appliqués (§4.1 à §4.5) :
--
--   ligne  : sales_amount = quantity_requested × regular_unit_price
--            cost_amount  = coût UNITAIRE du produit, pas le total
--            tax_value    = taux de TVA du produit
--   vente  : sales_amount = Σ lignes
--            ht_amount    = Σ CEIL(montant_ligne / (1 + taux/100))   <- PAR LIGNE
--            tax_amount   = Σ (montant_ligne − ht_ligne)
--            net_amount   = sales_amount − discount_amount
--            amount_to_be_taken_into_account = sales_amount  (contrôle V2 de
--            AuditDeclarationCaService : il doit égaler la somme des lignes)
--
-- Le HT s'arrondit AU PLAFOND, LIGNE PAR LIGNE, puis se somme. Calculer le HT
-- globalement à partir du TTC donne un écart de quelques francs et fausse les
-- rapports de TVA.
--
-- RÈGLE DES 90 JOURS : un lot n'est vendable que si sa péremption dépasse la
-- date de vente de 90 jours (APP_NOMBRE_JOUR_AVANT_PEREMPTION). On ne retient
-- donc que les lots dont la péremption dépasse AUJOURD'HUI + 90 : toute vente
-- de l'historique, forcément antérieure, satisfait alors la règle par
-- construction.
--
-- Les prix produits étant des multiples de 5 F, l'arrondi caisse (arrondi5)
-- est neutre sur les ventes comptant : amount_to_be_paid = net_amount exactement.
--
-- Note de structure : la ventilation par payeur (third_party_sale_line) est
-- produite ICI et non dans 10_, parce que le montant des règlements en dépend.
-- 10_ ne fait plus que les répartitions TVA et les consommations mensuelles.
-- ============================================================================

\i _header.sql

\echo '>> 09_ventes : ventes, lignes, FEFO, règlements'

-- ---------------------------------------------------------------------------
-- 1. En-têtes de vente
--
-- Affluence réaliste : samedi chargé, dimanche de garde allégé, lundi fermé
-- (les caisses de 08_caisses.sql suivent le même calendrier).
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_vente AS
WITH fenetre AS (
    -- Part de la journée d'ouverture (08 h – 20 h) déjà écoulée au moment du
    -- chargement. Elle sert à la journée en cours uniquement : générer une
    -- journée pleine à 09 h du matin daterait des ventes dans le futur, ce que
    -- 99_verification.sql refuse — et qui n'aurait aucun sens à l'écran.
    -- Le volume est ensuite borné à 12 ventes pour le jour courant : même tôt le
    -- matin, la démo doit couvrir comptant, tiers payant, conseil et ordonnance.
    SELECT GREATEST(0.08, LEAST(1.0,
             EXTRACT(EPOCH FROM (LOCALTIME - TIME '08:00:00')) / (12 * 3600.0)
           ))::numeric AS part
),
jours AS (
    SELECT
        (CURRENT_DATE - (INTERVAL '1 day' * j))::date AS jour,
        j,
        extract(dow FROM (CURRENT_DATE - (INTERVAL '1 day' * j)))::int AS dow
    -- Depuis 0 et non 1 : la journée du chargement doit avoir ses ventes,
    -- sinon la caisse du jour, le tableau de bord et les états quotidiens
    -- s'ouvrent vides. 08_caisses.sql couvre déjà cette journée.
    FROM generate_series(0, 180) AS j
    -- L'officine ferme le lundi, sauf la journee du chargement : une demo
    -- lancee un lundi n'aurait sinon ni caisse ouverte, ni vente du jour, ni
    -- tableau de bord caissier -- un ecran vide un jour sur sept.
    WHERE j = 0 OR extract(dow FROM (CURRENT_DATE - (INTERVAL '1 day' * j))) <> 1
),
compte AS (
    SELECT j.jour, j.j, j.dow,
           -- Journée en cours : volume proportionnel au temps écoulé, avec un
           -- échantillon métier minimal suffisamment large pour tous les écrans.
           GREATEST(CASE WHEN j.j = 0 THEN 12 ELSE 1 END, (
               CASE j.dow WHEN 6 THEN 35 + (j.j % 11)      -- samedi
                          WHEN 0 THEN  8 + (j.j %  5)      -- dimanche de garde
                          ELSE       20 + (j.j % 11) END
               * CASE WHEN j.j = 0 THEN f.part ELSE 1 END
           )::int) AS nb,
           -- Étendue horaire, en minutes, sur laquelle étaler les ventes.
           GREATEST(30, (720 * CASE WHEN j.j = 0 THEN f.part ELSE 1 END)::int) AS minutes
    FROM jours j CROSS JOIN fenetre f
)
SELECT
    nextval('id_sale_seq') AS id,
    c.jour AS sale_date,
    row_number() OVER (ORDER BY c.jour, k) AS rang,
    row_number() OVER (PARTITION BY c.jour ORDER BY k) AS rang_jour,
    -- Amplitude 08 h – 20 h, réduite à la partie écoulée pour le jour même.
    -- Le LEAST couvre le seul cas que le plancher de 8 % laisse passer : un
    -- chargement avant l'ouverture, où même une fenêtre minimale déborderait
    -- sur le futur. Les ventes se tassent alors sur la minute courante — peu
    -- élégant, mais l'invariant « aucune date dans le futur » tient toujours.
    LEAST(
        c.jour + TIME '08:00:00' + (INTERVAL '1 minute' * ((k * 43) % c.minutes)),
        date_trunc('minute', LOCALTIMESTAMP)
    ) AS moment,
    -- Une vente sur deux est tiers-payant : c'est le chemin dominant du jeu.
    CASE WHEN k % 2 = 0 THEN 'CashSale' ELSE 'ThirdPartySales' END AS dtype,
    c.nb
FROM compte c
CROSS JOIN LATERAL generate_series(1, c.nb) AS k;

-- Rattachement du client, du vendeur et de la caisse du jour.
ALTER TABLE tmp_vente ADD COLUMN customer_id int;
ALTER TABLE tmp_vente ADD COLUMN ayant_droit_id int;
ALTER TABLE tmp_vente ADD COLUMN seller_id int;
ALTER TABLE tmp_vente ADD COLUMN caissier_id int;
ALTER TABLE tmp_vente ADD COLUMN cash_register_id int;
ALTER TABLE tmp_vente ADD COLUMN caisse_id int;

-- Assurés : uniquement ceux qui portent un contrat, sinon la vente ne pourrait
-- être ventilée sur aucun payeur.
UPDATE tmp_vente v
   SET customer_id = a.id
  FROM (SELECT c.id, row_number() OVER (ORDER BY c.id) AS rang, count(*) OVER () AS total
          FROM customer c
         WHERE c.dtype = 'AssuredCustomer' AND c.type_assure = 'PRINCIPAL'
           AND EXISTS (SELECT 1 FROM client_tiers_payant x
                        WHERE x.assured_customer_id = c.id AND x.statut = 'ACTIF')) a
 WHERE v.dtype = 'ThirdPartySales'
   AND a.rang = 1 + (v.rang % a.total);

-- Un ayant droit sur cinq ventes tiers-payant : la vente est portée par le
-- contrat du principal, mais délivrée au bénéficiaire.
UPDATE tmp_vente v
   SET ayant_droit_id = ad.id
  FROM customer ad
 WHERE v.dtype = 'ThirdPartySales'
   AND v.rang % 5 = 0
   AND ad.assure_principal_id = v.customer_id
   AND ad.type_assure = 'AYANT_DROIT';

-- Comptant : 40 % de clients identifiés, le reste anonyme.
UPDATE tmp_vente v
   SET customer_id = u.id
  FROM (SELECT c.id, row_number() OVER (ORDER BY c.id) AS rang, count(*) OVER () AS total
          FROM customer c WHERE c.dtype = 'UninsuredCustomer' AND c.status = 'ENABLE') u
 WHERE v.dtype = 'CashSale'
   AND v.rang % 5 < 2
   AND u.rang = 1 + (v.rang % u.total);

UPDATE tmp_vente v
   SET seller_id   = s.id,
       caissier_id = cr.user_id,
       cash_register_id = cr.id,
       caisse_id   = p.id
  FROM (SELECT id, row_number() OVER (ORDER BY id) AS rang, count(*) OVER () AS total
          FROM app_user WHERE login IN ('kkone','atraore','ybrou','mdiallo')) s,
       cash_register cr,
       (SELECT id, row_number() OVER (ORDER BY id) AS rang, count(*) OVER () AS total
          FROM poste) p
 WHERE s.rang = 1 + (v.rang % s.total)
   AND cr.begin_time::date = v.sale_date
   AND p.rang = 1 + (v.rang % p.total);

-- Une vente sans caisse ne pourrait pas porter de règlement.
DELETE FROM tmp_vente WHERE cash_register_id IS NULL;

-- ---------------------------------------------------------------------------
-- 2. Lignes de vente
--
-- Le stock éligible borne ce qu'on peut vendre : seuls les lots disponibles,
-- présents en rayon et dont la péremption dépasse J+90 (règle des 90 jours).
-- On ne consomme au total que 60 % de ce gisement, pour laisser du stock en
-- rayon à la fin de l'historique.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_eligible AS
SELECT
    l.produit_id,
    sum(lsl.qty)::int AS qte_eligible,
    (sum(lsl.qty) * 6 / 10)::int AS plafond,
    row_number() OVER (ORDER BY l.produit_id) AS rang,
    count(*) OVER () AS total
FROM lot l
JOIN lot_stock_location lsl ON lsl.lot_id = l.id
JOIN storage s ON s.id = lsl.storage_id AND s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1
WHERE l.statut = 'AVAILABLE'
  AND l.expiry_date > CURRENT_DATE + 90
GROUP BY l.produit_id
HAVING sum(lsl.qty) >= 10;

CREATE INDEX ON tmp_eligible (produit_id);

-- Sélection déterministe : 1 à 5 produits distincts par vente.
CREATE TEMP TABLE tmp_ligne_brute AS
SELECT
    v.id AS sales_id,
    v.sale_date,
    v.moment,
    v.rang,
    e.produit_id,
    -- Quantités faibles, comme en officine.
    1 + ((v.rang + n) % 3) AS quantity
FROM tmp_vente v
CROSS JOIN LATERAL generate_series(1, 1 + (v.rang % 5)) AS n
JOIN tmp_eligible e ON e.rang = 1 + ((v.rang * 7 + n * 149) % e.total);

-- Un produit ne peut apparaître qu'une fois par vente : contrainte d'unicité
-- (produit_id, sales_id, sale_date).
CREATE TEMP TABLE tmp_ligne AS
SELECT DISTINCT ON (sales_id, produit_id)
    sales_id, sale_date, moment, rang, produit_id, quantity
FROM tmp_ligne_brute
ORDER BY sales_id, produit_id, quantity;

DROP TABLE tmp_ligne_brute;

-- Écrêtage au plafond de stock. Les ventes du jour passent d'abord afin que
-- l'historique ne consomme pas les cas métier nécessaires aux écrans courants ;
-- le reste conserve son ordre chronologique.
DELETE FROM tmp_ligne t
USING (
    SELECT c.sales_id, c.produit_id
      FROM (
          SELECT sales_id, produit_id,
                 sum(quantity) OVER (PARTITION BY produit_id
                                     ORDER BY CASE WHEN sale_date = CURRENT_DATE THEN 0 ELSE 1 END,
                                              sale_date, sales_id
                                     ROWS UNBOUNDED PRECEDING) AS cumul
            FROM tmp_ligne
      ) c
      JOIN tmp_eligible e ON e.produit_id = c.produit_id
     WHERE c.cumul > e.plafond
) trop
WHERE t.sales_id = trop.sales_id
  AND t.produit_id = trop.produit_id;

-- Une vente sans ligne n'a pas de sens : on la retire.
DELETE FROM tmp_vente v
WHERE NOT EXISTS (SELECT 1 FROM tmp_ligne l WHERE l.sales_id = v.id);

-- ---------------------------------------------------------------------------
-- 3. Montants de ligne
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_ligne_calc AS
SELECT
    nextval('id_sale_item_seq') AS id,
    l.sales_id,
    l.sale_date,
    l.moment,
    l.rang,
    l.produit_id,
    l.quantity,
    p.regular_unit_price,
    p.cost_amount,
    t.taux AS tax_value,
    (l.quantity * p.regular_unit_price)::int AS sales_amount,
    -- HT arrondi AU PLAFOND, ligne par ligne (SaleCommonService.computeHtAmount).
    -- Il porte sur le BRUT : la TVA est due sur le prix affiche, la remise se
    -- deduit ensuite. C'est pourquoi `sales_amount` reste le montant brut et
    -- que seule la remise en est retranchee au niveau de l'en-tete.
    CASE WHEN t.taux = 0 THEN (l.quantity * p.regular_unit_price)::int
         ELSE ceil((l.quantity * p.regular_unit_price)::numeric
                   / (1 + t.taux / 100.0))::int END AS ht_amount,
    -- ── Remises accordees au comptoir ────────────────────────────────────
    --
    -- Sans elles, le rapport « Analyse des remises » s'ouvrait sur un montant
    -- nul et la politique commerciale de l'officine restait indemontrable.
    --
    -- Les taux sont ceux de la grille produit (04b) : 5, 10 et 15 %. En deca,
    -- la remise ne se voit ni sur le ticket ni dans les etats.
    --
    -- Une vente au comptant sur huit, et la remise porte alors sur TOUTES ses
    -- lignes : c'est le geste reel du comptoir — on accorde une remise sur un
    -- panier, pas sur un article isole au milieu des autres.
    --
    -- Les ventes tiers payant en sont exclues : la part payeur se calcule sur
    -- le montant conventionne, et une remise y ouvrirait un ecart entre ce que
    -- l'organisme doit et ce que le patient a paye.
    --
    -- Le reste 3 plutot que 0 : les ventes differees sont choisies plus bas sur
    -- `id % 4 = 0`, et tout multiple de 8 en est un. Prendre 0 ici aurait rendu
    -- differee CHAQUE vente remisee -- une remise que personne n'encaisse.
    CASE WHEN v.dtype = 'CashSale' AND l.sales_id % 8 = 3
         THEN (ARRAY[5, 10, 15])[1 + (l.sales_id % 3)::int]
         ELSE 0 END AS taux_remise
FROM tmp_ligne l
JOIN tmp_vente v ON v.id = l.sales_id
JOIN produit p ON p.id = l.produit_id
JOIN tva t ON t.id = p.tva_id;

-- La remise unitaire est un ENTIER de francs : un prix a la caisse ne porte pas
-- de centimes. On la calcule donc apres coup, a partir du taux et du prix.
ALTER TABLE tmp_ligne_calc ADD COLUMN discount_unit_price int;
ALTER TABLE tmp_ligne_calc ADD COLUMN discount_amount int;

UPDATE tmp_ligne_calc
   SET discount_unit_price = round(regular_unit_price * taux_remise / 100.0)::int;

UPDATE tmp_ligne_calc
   SET discount_amount = quantity * discount_unit_price;

-- ---------------------------------------------------------------------------
-- 4. Montants d'en-tête, agrégés depuis les lignes
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_vente_calc AS
SELECT
    v.id, v.sale_date, v.moment, v.rang, v.rang_jour, v.dtype,
    v.customer_id, v.ayant_droit_id, v.seller_id, v.caissier_id,
    v.cash_register_id, v.caisse_id,
    a.sales_amount,
    a.cost_amount,
    a.ht_amount,
    a.sales_amount - a.ht_amount AS tax_amount,
    a.discount_amount,
    -- Net = brut − remise, l'invariant que controle 99_verification.
    a.sales_amount - a.discount_amount AS net_amount
FROM tmp_vente v
JOIN (
    SELECT sales_id,
           sum(sales_amount)::int              AS sales_amount,
           sum(quantity * cost_amount)::int    AS cost_amount,
           sum(ht_amount)::int                 AS ht_amount,
           sum(discount_amount)::int           AS discount_amount
      FROM tmp_ligne_calc
     GROUP BY sales_id
) a ON a.sales_id = v.id;

-- ---------------------------------------------------------------------------
-- 5. Répartition tiers-payant
--
-- Les payeurs sont servis par priorité croissante, et la part de chacun est
-- BORNÉE par ce que les précédents ont laissé (TiersPayantCalculationService).
-- Appliquer « montant × taux » à chaque payeur donnerait des sommes supérieures
-- au montant de la vente.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_tp AS
WITH payeurs AS (
    SELECT
        v.id AS sales_id, v.sale_date, v.sales_amount, v.moment,
        ctp.id AS client_tiers_payant_id,
        ctp.priorite,
        ctp.taux,
        row_number() OVER (PARTITION BY v.id ORDER BY ctp.priorite) AS ordre
    FROM tmp_vente_calc v
    JOIN client_tiers_payant ctp ON ctp.assured_customer_id = v.customer_id
                                AND ctp.statut = 'ACTIF'
    WHERE v.dtype = 'ThirdPartySales'
),
cumul AS (
    SELECT p.*,
           -- Part brute du payeur, et ce qui reste après les plus prioritaires.
           sum((p.sales_amount * p.taux) / 100) OVER (
               PARTITION BY p.sales_id ORDER BY p.ordre
               ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING) AS deja_servi
      FROM payeurs p
)
SELECT
    nextval('id_sale_assurance_item_seq') AS id,
    c.sales_id, c.sale_date, c.moment, c.sales_amount,
    c.client_tiers_payant_id, c.priorite, c.taux, c.ordre,
    LEAST(
        ((c.sales_amount * c.taux) / 100)::int,
        GREATEST(0, c.sales_amount - COALESCE(c.deja_servi, 0)::int)
    ) AS montant
FROM cumul c;

-- ---------------------------------------------------------------------------
-- 6. Insertion des ventes
--
-- dtype doit être renseigné explicitement : Sales le mappe en lecture seule,
-- une insertion SQL directe qui l'oublierait produirait des ventes rattachées
-- à aucune sous-classe, invisibles des écrans.
-- ---------------------------------------------------------------------------
INSERT INTO sales (
    dtype, id, sale_date, number_transaction,
    sales_amount, ht_amount, tax_amount, cost_amount, net_amount,
    discount_amount, amount_to_be_paid, payroll_amount, rest_to_pay, monnaie,
    amount_to_be_taken_into_account,
    statut, payment_status, nature_vente, origine_vente, type_prescription, ca,
    differe, canceled, copy, imported, to_ignore,
    magasin_id, user_id, seller_id, caissier_id, customer_id,
    cash_register_id, caisse_id, lastcaisse_id,
    num_bon, ayant_droit_id, part_assure, part_tiers_payant, has_price_option,
    created_at, updated_at, effective_update_date
)
SELECT
    v.dtype, v.id, v.sale_date,
    to_char(v.sale_date, 'YYYYMMDD') || lpad(v.rang_jour::text, 3, '0'),
    v.sales_amount, v.ht_amount, v.tax_amount, v.cost_amount, v.net_amount,
    v.discount_amount,
    -- Part réglée par le client : tout au comptant, la part assuré en TP.
    CASE WHEN v.dtype = 'CashSale' THEN v.net_amount
         ELSE GREATEST(0, v.net_amount - COALESCE(tp.part_tp, 0)) END,
    CASE WHEN v.dtype = 'CashSale' THEN v.net_amount
         ELSE GREATEST(0, v.net_amount - COALESCE(tp.part_tp, 0)) END,
    0, 0,
    -- Le declarable est le NET : on ne declare pas un chiffre d'affaires qu'on
    -- n'a pas facture. Sa somme sur les lignes doit retomber dessus (controle
    -- V2 de AuditDeclarationCaService).
    v.net_amount,
    'CLOSED', 'PAYE',
    CASE WHEN v.dtype = 'CashSale' THEN 'COMPTANT' ELSE 'ASSURANCE' END,
    'DIRECT',
    CASE WHEN v.dtype = 'CashSale' AND v.rang % 3 = 0 THEN 'CONSEIL'
         ELSE 'PRESCRIPTION' END,
    'CA',
    false, false, false, false, false,
    1, v.caissier_id, v.seller_id, v.caissier_id, v.customer_id,
    v.cash_register_id, v.caisse_id, v.caisse_id,
    -- APP_SANS_NUM_BON vaut 0 : le numéro de bon est obligatoire en TP.
    CASE WHEN v.dtype = 'ThirdPartySales'
         THEN 'BON' || to_char(v.sale_date, 'YYMMDD') || lpad(v.rang_jour::text, 4, '0')
         ELSE NULL END,
    v.ayant_droit_id,
    CASE WHEN v.dtype = 'ThirdPartySales'
         THEN GREATEST(0, v.net_amount - COALESCE(tp.part_tp, 0)) ELSE NULL END,
    CASE WHEN v.dtype = 'ThirdPartySales' THEN COALESCE(tp.part_tp, 0) ELSE NULL END,
    CASE WHEN v.dtype = 'ThirdPartySales' THEN false ELSE NULL END,
    v.moment, v.moment, v.moment
FROM tmp_vente_calc v
LEFT JOIN (SELECT sales_id, sum(montant)::int AS part_tp FROM tmp_tp GROUP BY sales_id) tp
       ON tp.sales_id = v.id;

-- ---------------------------------------------------------------------------
-- 7. Insertion des lignes
--
-- init_stock / after_stock reconstituent l'état du stock au moment de la vente,
-- à rebours depuis le stock final : c'est ce que lit l'historique produit.
-- ---------------------------------------------------------------------------
INSERT INTO sales_line (
    id, sale_date, sales_id, sales_sale_date, produit_id,
    quantity_requested, quantity_sold, quantity_ug, quantity_avoir,
    regular_unit_price, net_unit_price, discount_unit_price,
    discount_amount, sales_amount, tax_value, cost_amount,
    amount_to_be_taken_into_account, taux_remise, to_ignore,
    init_stock, after_stock, lots, rates,
    created_at, updated_at, effective_update_date
)
SELECT
    l.id, l.sale_date, l.sales_id, l.sale_date, l.produit_id,
    l.quantity, l.quantity, 0, 0,
    l.regular_unit_price, l.regular_unit_price - l.discount_unit_price, l.discount_unit_price,
    l.discount_amount, l.sales_amount, l.tax_value, l.cost_amount,
    -- Contrôle V2 de l'audit CA : la somme des lignes doit égaler la vente.
    l.sales_amount - l.discount_amount, l.taux_remise, false,
    NULL, NULL,
    '[]'::jsonb, '[]'::jsonb,
    l.moment, l.moment, l.moment
FROM tmp_ligne_calc l;

-- ---------------------------------------------------------------------------
-- 8. Ventilation par payeur
-- ---------------------------------------------------------------------------
INSERT INTO third_party_sale_line (
    id, sale_date, sale_id, sale_sale_date, client_tiers_payant_id,
    num_bon, montant, montant_regle, taux, taux_vente, statut,
    repartitions, created_at, updated_at, effective_update_date
)
SELECT
    t.id, t.sale_date, t.sales_id, t.sale_date, t.client_tiers_payant_id,
    s.num_bon, t.montant, 0,
    -- taux EFFECTIF constaté, et non le taux contractuel : ils ne coïncident
    -- que sans plafond, sans prix d'option et avec un seul payeur.
    CASE WHEN t.sales_amount > 0
         THEN round(t.montant * 100.0 / t.sales_amount)::smallint
         ELSE 0::smallint END,
    t.taux::smallint,
    'ACTIF',
    '[]'::jsonb,
    t.moment, t.moment, t.moment
FROM tmp_tp t
JOIN sales s ON s.id = t.sales_id AND s.sale_date = t.sale_date
WHERE t.montant > 0;

-- ---------------------------------------------------------------------------
-- 9. Consommation FEFO des lots
--
-- Allocation par recouvrement d'intervalles : les lots d'un produit sont
-- ordonnés par péremption croissante et cumulés, les lignes de vente sont
-- ordonnées chronologiquement et cumulées ; une ligne consomme un lot quand
-- leurs intervalles se chevauchent.
--
-- C'est l'équivalent ensembliste d'une boucle chronologique, sans la boucle.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_lot_dispo AS
SELECT
    l.produit_id, l.id AS lot_id, l.num_lot, l.expiry_date, lsl.qty,
    sum(lsl.qty) OVER (PARTITION BY l.produit_id
                       ORDER BY l.expiry_date, l.id
                       ROWS UNBOUNDED PRECEDING) - lsl.qty AS deb,
    sum(lsl.qty) OVER (PARTITION BY l.produit_id
                       ORDER BY l.expiry_date, l.id
                       ROWS UNBOUNDED PRECEDING)           AS fin
FROM lot l
JOIN lot_stock_location lsl ON lsl.lot_id = l.id
JOIN storage s ON s.id = lsl.storage_id AND s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1
WHERE l.statut = 'AVAILABLE'
  AND l.expiry_date > CURRENT_DATE + 90;

CREATE TEMP TABLE tmp_ligne_pos AS
SELECT
    l.id, l.produit_id, l.quantity,
    sum(l.quantity) OVER (PARTITION BY l.produit_id
                          ORDER BY l.sale_date, l.sales_id, l.id
                          ROWS UNBOUNDED PRECEDING) - l.quantity AS deb,
    sum(l.quantity) OVER (PARTITION BY l.produit_id
                          ORDER BY l.sale_date, l.sales_id, l.id
                          ROWS UNBOUNDED PRECEDING)              AS fin
FROM tmp_ligne_calc l;

CREATE TEMP TABLE tmp_alloc AS
SELECT
    p.id AS sales_line_id,
    d.lot_id, d.num_lot, d.expiry_date,
    (LEAST(p.fin, d.fin) - GREATEST(p.deb, d.deb))::int AS qty_prise
FROM tmp_ligne_pos p
JOIN tmp_lot_dispo d ON d.produit_id = p.produit_id
                    AND p.deb < d.fin AND d.deb < p.fin;

-- Snapshot immuable porté par la ligne de vente, ordonné par péremption
-- croissante : c'est la trace exigée pour les rappels et le contrôle FMD.
UPDATE sales_line sl
   SET lots = a.lots
  FROM (
      SELECT sales_line_id,
             jsonb_agg(jsonb_build_object(
                 'id', lot_id, 'numLot', num_lot,
                 'quantity', qty_prise, 'expiryDate', expiry_date
             ) ORDER BY expiry_date, lot_id) AS lots
        FROM tmp_alloc
       WHERE qty_prise > 0
       GROUP BY sales_line_id
  ) a
 WHERE sl.id = a.sales_line_id;

-- ---------------------------------------------------------------------------
-- 10. Décrément du stock
--
-- Les trois niveaux bougent ensemble : emplacement, lot, stock produit.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_conso AS
SELECT lot_id, sum(qty_prise)::int AS qte
FROM tmp_alloc WHERE qty_prise > 0 GROUP BY lot_id;

UPDATE lot_stock_location lsl
   SET qty = lsl.qty - c.qte, updated_at = NOW()
  FROM tmp_conso c,
       storage s
 WHERE lsl.lot_id = c.lot_id
   AND s.id = lsl.storage_id AND s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1;

UPDATE lot l
   SET current_quantity = l.current_quantity - c.qte,
       updated = NOW()
  FROM tmp_conso c
 WHERE l.id = c.lot_id;

-- Le statut est asservi à la quantité restante (LotServiceImpl.updateLots).
UPDATE lot SET statut = 'SOLD' WHERE current_quantity <= 0 AND statut = 'AVAILABLE';

-- Les emplacements épuisés sont supprimés, jamais laissés à zéro.
DELETE FROM lot_stock_location WHERE qty <= 0;

-- Stock produit : les sorties viennent des lignes de vente.
UPDATE stock_produit sp
   SET qty_stock = sp.qty_stock - v.qte,
       qty_virtual = sp.qty_stock - v.qte,
       updated_at = NOW()
  FROM (SELECT produit_id, sum(quantity)::int AS qte
          FROM tmp_ligne_calc GROUP BY produit_id) v,
       storage s
 WHERE sp.produit_id = v.produit_id
   AND s.id = sp.storage_id AND s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1;

-- init_stock / after_stock, reconstitués à rebours depuis le stock final.
UPDATE sales_line sl
   SET init_stock  = f.stock_final + f.total - p.deb,
       after_stock = f.stock_final + f.total - p.fin
  FROM tmp_ligne_pos p
  JOIN (
      SELECT l.produit_id,
             sum(l.quantity)::int AS total,
             COALESCE((SELECT sp.qty_stock FROM stock_produit sp
                        JOIN storage s ON s.id = sp.storage_id
                       WHERE sp.produit_id = l.produit_id
                         AND s.storage_type = 'PRINCIPAL' AND s.magasin_id = 1), 0) AS stock_final
        FROM tmp_ligne_calc l GROUP BY l.produit_id
  ) f ON f.produit_id = p.produit_id
 WHERE sl.id = p.id;

-- ---------------------------------------------------------------------------
-- 10b. Ventes différées (crédit client)
--
-- Une vente différée est délivrée sans encaissement : le client règle plus
-- tard, au comptoir ou par versements. Elle n'a donc AUCUN payment_transaction
-- tant qu'elle n'est pas réglée — le règlement viendra de 14b_reglements.sql,
-- sous forme de DifferePayment rattaché au CLIENT et non à la vente.
--
-- Sans ces ventes, tout le domaine « différé » reste vide : l'écran des soldes
-- clients, l'historique des règlements (/api/reglements) et les relances
-- n'ont rien à afficher.
--
-- Réservé aux clients comptant IDENTIFIÉS : on ne fait pas crédit à un client
-- anonyme, et le solde doit pouvoir être rattaché à quelqu'un.
-- ---------------------------------------------------------------------------
UPDATE sales s
   SET differe        = true,
       payment_status = 'IMPAYE',
       payroll_amount = 0,
       rest_to_pay    = s.amount_to_be_paid
 WHERE s.dtype = 'CashSale'
   AND s.customer_id IS NOT NULL
   AND s.amount_to_be_paid > 0
   AND s.id % 4 = 0;

-- ---------------------------------------------------------------------------
-- 11. Règlements
--
-- PaymentTransaction.cashRegister est obligatoire. Une vente dont la part
-- client est nulle — couverture à 100 % — n'a AUCUN règlement : c'est un cas
-- normal, pas un oubli. Les ventes différées non plus, par construction.
-- ---------------------------------------------------------------------------
INSERT INTO payment_transaction (
    dtype, id, transaction_date, sale_id, sale_date,
    expected_amount, paid_amount, reel_amount, montant_verse,
    amount_to_be_taken_into_account,
    part_assure, part_tiers_payant,
    credit, categorie_ca, type_transaction, payment_mode_code,
    cash_register_id, created_at
)
SELECT
    'SalePayment', nextval('id_transaction_seq'), s.sale_date, s.id, s.sale_date,
    s.amount_to_be_paid, s.amount_to_be_paid, s.amount_to_be_paid, s.amount_to_be_paid,
    -- Contrôle V2b : l'encaissement déclaré ne dépasse pas le CA déclaré.
    LEAST(s.amount_to_be_paid, s.amount_to_be_taken_into_account),
    COALESCE(s.part_assure, s.amount_to_be_paid),
    COALESCE(s.part_tiers_payant, 0),
    false, 'CA',
    CASE WHEN s.dtype = 'CashSale' THEN 'CASH_SALE' ELSE 'CREDIT_SALE' END,
    -- Espèces majoritaires, comme en officine.
    CASE (s.id % 20)
        WHEN 0 THEN 'CB'  WHEN 1 THEN 'CB'  WHEN 2 THEN 'CB'  WHEN 3 THEN 'CB'
        WHEN 4 THEN 'CB'
        WHEN 5 THEN 'OM'  WHEN 6 THEN 'MTN' WHEN 7 THEN 'WAVE'
        WHEN 8 THEN 'CH'
        ELSE 'CASH'
    END,
    s.cash_register_id,
    s.created_at
FROM sales s
WHERE s.amount_to_be_paid > 0
  AND s.cash_register_id IS NOT NULL
  AND NOT s.differe;

-- ---------------------------------------------------------------------------
-- 12. Recalage des caisses
--
-- Le montant final d'une caisse est son fonds initial augmenté des espèces
-- encaissées : un écart ici est le premier que relève un pharmacien.
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

-- Détail par mode de règlement, tel que l'affiche le ticket Z.
INSERT INTO cash_register_item (cash_register_id, payment_mode_code, amount, type_transaction)
SELECT pt.cash_register_id, pt.payment_mode_code, sum(pt.paid_amount)::bigint, 'CASH_SALE'
FROM payment_transaction pt
GROUP BY pt.cash_register_id, pt.payment_mode_code;

DROP TABLE tmp_alloc;
DROP TABLE tmp_conso;
DROP TABLE tmp_lot_dispo;
DROP TABLE tmp_ligne_pos;
DROP TABLE tmp_ligne_calc;
DROP TABLE tmp_ligne;
DROP TABLE tmp_eligible;
DROP TABLE tmp_tp;
DROP TABLE tmp_vente_calc;
DROP TABLE tmp_vente;

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_ventes int; v_lignes int; v_paie int;
    v_ecart int; v_ht int; v_lots int; v_neg int;
    v_remisees int; v_net int;
BEGIN
    SELECT count(*) INTO v_ventes FROM sales;
    SELECT count(*) INTO v_lignes FROM sales_line;
    SELECT count(*) INTO v_paie   FROM payment_transaction;

    SELECT count(*) INTO v_ecart FROM (
        SELECT s.id FROM sales s
          JOIN sales_line sl ON sl.sales_id = s.id AND sl.sales_sale_date = s.sale_date
         GROUP BY s.id, s.sale_date, s.sales_amount
        HAVING s.sales_amount <> sum(sl.sales_amount)) x;

    SELECT count(*) INTO v_ht FROM sales WHERE ht_amount + tax_amount <> sales_amount;

    -- Le snapshot de lots doit couvrir exactement la quantité vendue.
    SELECT count(*) INTO v_lots FROM sales_line sl
     WHERE sl.quantity_sold <> COALESCE(
        (SELECT sum((e->>'quantity')::int) FROM jsonb_array_elements(sl.lots) e), 0);

    SELECT count(*) INTO v_neg FROM stock_produit WHERE qty_stock < 0;

    -- Les remises : sans elles, le rapport « Analyse des remises » est vide.
    SELECT count(*) INTO v_remisees FROM sales WHERE discount_amount > 0;
    SELECT count(*) INTO v_net FROM sales WHERE net_amount <> sales_amount - discount_amount;

    IF v_ventes < 2500 THEN RAISE EXCEPTION 'Ventes : % (attendu >= 2500)', v_ventes; END IF;
    IF v_lignes < 5000 THEN RAISE EXCEPTION 'Lignes : % (attendu >= 5000)', v_lignes; END IF;
    IF v_ecart > 0 THEN RAISE EXCEPTION '% vente(s) dont le total contredit les lignes', v_ecart; END IF;
    IF v_ht > 0 THEN RAISE EXCEPTION '% vente(s) dont HT + TVA <> TTC', v_ht; END IF;
    IF v_lots > 0 THEN RAISE EXCEPTION '% ligne(s) dont les lots ne couvrent pas la quantité vendue', v_lots; END IF;
    IF v_neg > 0 THEN RAISE EXCEPTION '% ligne(s) de stock négative(s)', v_neg; END IF;
    IF v_remisees < 100 THEN
        RAISE EXCEPTION 'Ventes remisées : % (attendu >= 100)', v_remisees;
    END IF;
    IF v_net > 0 THEN
        RAISE EXCEPTION '% vente(s) dont le net ne vaut pas brut - remise', v_net;
    END IF;

    RAISE NOTICE '% ventes (dont % remisées), % lignes, % règlements.',
                 v_ventes, v_remisees, v_lignes, v_paie;
END $$;

\echo '<< 09_ventes : terminé'
