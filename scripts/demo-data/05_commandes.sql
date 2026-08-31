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
-- Répartition : 80 CLOSED, 20 RECEIVED, 20 REQUESTED, ENTRELACÉES sur les 180
-- jours plutôt qu'empilées par ancienneté.
--
-- L'entrelacement n'est pas cosmétique. Les statuts étaient auparavant rangés
-- par blocs — les 20 commandes les plus récentes toutes en REQUESTED — si bien
-- qu'AUCUNE réception ne tombait dans les trente derniers jours. Tous les
-- indicateurs « achats 30 jours » (tableau de bord d'accueil, performance
-- fournisseurs) restaient donc à zéro, et l'on croyait l'application en faute.
-- Une officine réelle reçoit chaque semaine : c'est cela qu'il faut simuler.
--
-- Les montants sont posés à zéro ici puis recalculés depuis les lignes (§3).
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_cmd AS
WITH base AS (
    SELECT
        i,
        -- Étalement sur 180 jours : les commandes les plus anciennes portent
        -- les rangs les plus élevés.
        -- Le rang 120 tombe sur AUJOURD'HUI : le tableau de bord d'accueil s'ouvre sur
        -- la période « Auj. » et affichait un indicateur « Achats fournisseurs » vide,
        -- faute d'une seule commande du jour.
        (CURRENT_DATE - (INTERVAL '1 day' * (((120 - i) * 179) / 120)))::date AS order_date,
        -- Un cycle de 6 : une commande en attente, une en cours de réception,
        -- quatre soldées. Les proportions d'ensemble sont conservées, mais
        -- chaque tranche de temps porte les trois statuts.
        --
        -- Les QUINZE DERNIÈRES commandes échappent à ce cycle et sont pour
        -- l'essentiel en cours de réception. La liste des réceptions s'ouvre en
        -- effet sur une fenêtre de dates de l'ordre du mois : avec un bon livré
        -- toutes les six commandes, elle n'en montrait que trois, quand la
        -- saisie, la validation en masse, le reliquat et la finalisation en
        -- réclament chacun un — et qu'un parcours qui saisit consomme le sien.
        -- Le rang 120 tombe sur aujourd'hui et reste CLÔTURÉ : le tableau de bord
        -- d'accueil ouvre sur la période « Auj. » et a besoin d'un achat du jour.
        CASE WHEN i = 120 THEN 'CLOSED'
             WHEN i >= 105 THEN
                  CASE WHEN i % 3 = 0 THEN 'REQUESTED' ELSE 'RECEIVED' END
             WHEN i % 6 = 1 THEN 'REQUESTED'
             WHEN i % 6 = 2 THEN 'RECEIVED'
             ELSE 'CLOSED' END AS order_status
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
    -- Le délai de livraison ne doit pas projeter la réception dans le futur : pour les
    -- commandes des derniers jours, elle est ramenée à aujourd'hui.
    CASE WHEN b.order_status = 'REQUESTED' THEN NULL
         ELSE LEAST((b.order_date + (INTERVAL '1 day' * (1 + b.i % 4)))::date, CURRENT_DATE) END AS receipt_date,
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
    s.i,
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
        c.i,
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
    --
    -- Les bons livrés se répartissent en TROIS ÉTATS, par rotation sur le rang de
    -- génération :
    --
    --     i % 12 ∈ (4, 5, 8) → compté, servi en entier (taux de service 100 %)
    --     i % 12 ∈ (1, 7)    → compté, partiellement servi (taux intermédiaire)
    --     sinon              → à saisir : rien n'a été compté (taux 0 %)
    --
    -- Les commandes antérieures gardent en outre une réception partielle sur une commande
    -- sur cinq : l'historique doit porter des reliquats, pas seulement le mois courant.
    --
    -- Les trois doivent exister, et surtout coexister DANS LES BONS RÉCENTS : la liste des
    -- réceptions s'ouvre sur une fenêtre de dates, et un état qu'on ne trouve que six mois
    -- en arrière est un état qu'aucun écran ne montre.
    --
    -- Le manquant d'un bon partiel ne porte que sur UNE LIGNE SUR TROIS : un grossiste sert
    -- l'essentiel et rate quelques références. Une commande courte sur chacune de ses lignes
    -- donnerait un taux de service de 0 %, indistinguable d'un bon jamais compté.
    CASE WHEN o.order_status = 'REQUESTED' THEN NULL
         WHEN (o.i % 12 IN (1, 7) OR (o.i < 105 AND o.i % 5 = 0)) AND o.rang % 3 = 0
              THEN greatest(1, (o.quantity_requested * 3) / 4)
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
    false,
    -- `is_updated` marque une ligne dont la quantité reçue a été SAISIE au comptoir. Il
    -- faut les deux états dans le jeu de données, et pas au hasard :
    --
    --   • une commande CLÔTURÉE a forcément été saisie ;
    --   • un bon PARTIELLEMENT servi est saisi lui aussi — c'est en comptant le carton
    --     qu'on découvre le manquant. Il est donc finalisable tel quel, et c'est lui qui
    --     donne le reliquat ;
    --   • un bon SERVI EN ENTIER et déjà compté : la réception sans histoire, celle qui se
    --     finalise directement ;
    --   • les autres restent À SAISIR : ce sont les bons « en attente » sur lesquels
    --     s'exercent la saisie ligne à ligne et « Tout valider ».
    --
    -- La distinction n'est pas cosmétique : « Tout valider » remonte les quantités au
    -- niveau COMMANDÉ, alors que les lots sont construits sur les quantités REÇUES. Le
    -- passer sur un bon partiel laisserait des lignes que les lots ne couvrent plus, et la
    -- finalisation serait refusée (contrôle `lotManquant`).
    -- Deux des trois états de bon livré sont COMPTÉS (cf. la rotation ci-dessus) ; le
    -- troisième reste à saisir. L'alternance porte sur le rang de génération, jamais sur
    -- l'identifiant : les identifiants des bons livrés sont tous congrus modulo 6, et un
    -- modulo sur eux les prendrait tous, ou aucun.
    o.order_status = 'CLOSED'
      OR (o.order_status = 'RECEIVED' AND o.i % 12 IN (1, 4, 5, 7, 8)),
    -- Péremption connue seulement à la réception.
    CASE WHEN o.order_status = 'REQUESTED' THEN NULL
         ELSE (o.order_date + (INTERVAL '1 day' * (400 + (o.id * 37) % 500)))::date END,
    NULL,
    o.order_date + TIME '08:35:00',
    o.order_date + TIME '08:35:00'
FROM tmp_ol o
JOIN tva t ON t.id = o.tva_id;

-- ---------------------------------------------------------------------------
-- Stock avant / après réception
--
-- L'application enregistre sur chaque ligne le stock du produit au moment de la
-- réception (`init_stock`) et celui qui en résulte (`final_stock` =
-- avant + reçu + gratuit, StockEntryServiceImpl.saveItem). Une commande
-- réceptionnée dont `final_stock` est vide n'existe pas dans l'application :
-- l'écran du bon affiche alors un « stock après » vide.
--
-- Le stock d'avant réception n'est plus reconstituable a posteriori — la
-- réception est passée. On pose donc une valeur plausible et déterministe, et
-- surtout cohérente avec l'arithmétique de la ligne, qui est ce que le lecteur
-- vérifie.
-- ---------------------------------------------------------------------------
UPDATE order_line ol
   SET init_stock  = 5 + (ol.id * 17) % 60,
       final_stock = CASE
                       WHEN c.order_status = 'REQUESTED' THEN NULL
                       ELSE 5 + (ol.id * 17) % 60
                            + COALESCE(ol.quantity_received, 0) + COALESCE(ol.free_qty, 0)
                     END
  FROM commande c
 WHERE c.id = ol.commande_id AND c.order_date = ol.commande_order_date;

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

-- ---------------------------------------------------------------------------
-- Une commande DÉJÀ TRANSMISE au grossiste par PharmaML
--
-- Le canal PharmaML se démontre en trois temps : l'envoi (ACH-28), le statut de
-- l'envoi (ACH-29), l'historique des envois d'une commande (ACH-31). Les deux
-- derniers n'ont rien à montrer tant qu'aucune commande n'a été transmise — et
-- transmettre pour de vrai n'est pas une option : le message part CHEZ le
-- grossiste. On pose donc ici une commande transmise et son envoi, tels que
-- `PharmaMlServiceImpl` les aurait écrits après une réponse complète.
--
-- La commande retenue n'est PAS la plus récente, et c'est délibéré : une
-- commande transmise est VERROUILLÉE (« Soumise via PharmaML — modifications
-- désactivées »). En tête de liste, elle happerait tous les parcours d'édition,
-- qui ouvrent la première commande venue. On prend donc la troisième.
-- ---------------------------------------------------------------------------
WITH cible AS (
    SELECT c.id, c.order_date, c.fournisseur_id,
           (SELECT count(*) FROM order_line ol
             WHERE ol.commande_id = c.id AND ol.commande_order_date = c.order_date)::int AS lignes
      FROM commande c
     WHERE c.order_status = 'REQUESTED'
     ORDER BY c.order_date DESC, c.id DESC
     OFFSET 2 LIMIT 1
)
INSERT INTO pharmaml_envoi (
    commande_id, commande_date, fournisseur_id, statut, ref_message,
    tentatives, derniere_tentative, total_lignes, lignes_acceptees, lignes_rupture,
    created_at
)
SELECT
    cible.id, cible.order_date, cible.fournisseur_id, 'SUBMITTED',
    upper(substr(md5(cible.id::text), 1, 14)),
    1, cible.order_date + TIME '09:15:00',
    cible.lignes, cible.lignes, 0,
    cible.order_date + TIME '09:15:00'
FROM cible;

UPDATE commande c
   SET has_been_submitted_to_pharmaml = true
  FROM pharmaml_envoi e
 WHERE e.commande_id = c.id AND e.commande_date = c.order_date;

-- ---------------------------------------------------------------------------
-- Substitutions proposees par le grossiste
--
-- En rupture sur une reference, le repartiteur propose un equivalent. Le
-- protocole distingue trois cas, et l'ecran les traite differemment :
--
--   EP  Equivalent Propose  -> l'officine doit ACCEPTER ou REFUSER
--   EL  Equivalent Livre    -> deja substitue, le carton est parti
--   RL  Remplacant Livre    -> idem
--
-- Sans substitution en base, l'ecran « Substitutions proposees par le
-- grossiste » ne s'ouvre sur rien et l'arbitrage EP — le seul qui demande une
-- decision — ne se demontre nulle part.
--
-- Le CIP propose est celui d'un AUTRE produit du catalogue : un substitut qui
-- n'existe pas dans le referentiel ne pourrait pas entrer en stock.
-- ---------------------------------------------------------------------------
INSERT INTO substitution_proposee (
    commande_id, commande_date, commande_order_date, order_line_id,
    fournisseur_id, cip_propose, designation, type_codification,
    quantite, statut, created_at
)
SELECT
    ol.commande_id,
    ol.commande_order_date,
    ol.commande_order_date,
    ol.id,
    c.fournisseur_id,
    fp_sub.code_cip,
    p_sub.libelle,
    v.type,
    greatest(ol.quantity_requested / 2, 1),
    v.statut,
    c.order_date + TIME '09:30:00'
  FROM (VALUES (0, 'EP', 'EN_ATTENTE'), (1, 'EL', 'ACCEPTEE'), (2, 'RL', 'ACCEPTEE')) AS v(rang, type, statut)
  CROSS JOIN LATERAL (
      -- Les lignes de la commande TRANSMISE : une substitution vient d'une reponse du
      -- grossiste, elle n'a de sens que sur un bon qui lui a ete envoye. C'est aussi la
      -- seule commande dont l'ecran affiche le bloc PharmaML.
      SELECT ol2.id, ol2.commande_id, ol2.commande_order_date, ol2.quantity_requested,
             ol2.fournisseur_produit_id
        FROM order_line ol2
        JOIN commande c2 ON c2.id = ol2.commande_id AND c2.order_date = ol2.commande_order_date
       WHERE c2.has_been_submitted_to_pharmaml
       ORDER BY ol2.id
       OFFSET v.rang LIMIT 1
  ) ol
  JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
  CROSS JOIN LATERAL (
      -- Un substitut credible, pris dans le catalogue du fournisseur PRINCIPAL.
      --
      -- Surtout pas `fp.fournisseur_id = c.fournisseur_id` : la commande part chez une
      -- AGENCE, qui ne porte aucun code produit — les codes et les prix vivent sur le
      -- principal. La chercher sur l'agence ne ramene rien, et l'insertion reste vide sans
      -- que rien ne le signale.
      SELECT fp.code_cip, fp.produit_id
        FROM fournisseur_produit fp
       WHERE fp.id <> ol.fournisseur_produit_id
         AND fp.code_cip IS NOT NULL
       ORDER BY fp.id
       OFFSET v.rang LIMIT 1
  ) fp_sub
  JOIN produit p_sub ON p_sub.id = fp_sub.produit_id;

DO $$
DECLARE v_n INTEGER;
BEGIN
    SELECT count(*) INTO v_n FROM substitution_proposee;
    IF v_n = 0 THEN
        RAISE EXCEPTION 'Aucune substitution proposee : l ecran resterait vide';
    END IF;
    SELECT count(*) INTO v_n FROM substitution_proposee
     WHERE type_codification = 'EP' AND statut = 'EN_ATTENTE';
    IF v_n = 0 THEN
        RAISE EXCEPTION 'Aucune substitution EP en attente : l arbitrage ne se demontre pas';
    END IF;
    RAISE NOTICE '% substitution(s) proposee(s).', (SELECT count(*) FROM substitution_proposee);
END $$;
