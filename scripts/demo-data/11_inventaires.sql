-- ============================================================================
-- 11_inventaires.sql — Inventaires : un clôturé, un en cours, un planning
--
-- Sans ce script, les trois onglets de l'écran « Inventaires » s'ouvrent vides
-- et donnent à croire que la fonction n'existe pas. Or l'inventaire est le
-- point de contact entre le stock théorique et le stock réel : il n'a de sens,
-- dans une démonstration comme dans un manuel, que s'il montre des ÉCARTS.
--
-- Trois objets, un par état de l'écran :
--   * un inventaire CLÔTURÉ, magasin entier, il y a 45 jours, avec ses écarts
--     qualifiés (casse, vol, erreur de saisie…) — onglet « Clôturés » ;
--   * un inventaire EN COURS sur un rayon, partiellement compté — onglet
--     « En cours », et c'est lui qui porte la barre de progression ;
--   * un planning d'inventaire tournant mensuel par rayon — onglet
--     « Tournant ».
--
-- PIÈGES DU MODÈLE, vérifiés sur la base :
--   * store_inventory_line porte une contrainte d'unicité sur
--     (produit_id, store_inventory_id) ET sur (produit_id, store_inventory_id,
--     storage_id) : un produit n'apparaît qu'UNE fois par inventaire, tous
--     emplacements confondus. On choisit donc un emplacement par produit.
--   * `updated` (booléen) distingue la ligne COMPTÉE de la ligne encore
--     vierge. C'est lui, et non `quantity_on_hand`, que l'écran interroge pour
--     calculer la progression : une ligne comptée à zéro reste une ligne
--     comptée.
--   * inventory_gap_analysis a une contrainte d'unicité sur la ligne : une
--     seule cause par écart, pas d'historique de requalification.
-- ============================================================================

\i _header.sql

\echo '>> 11_inventaires : inventaires clôturé, en cours et planning tournant'

-- ---------------------------------------------------------------------------
-- 1. Périmètre : les produits réellement en stock au rayon
--
-- Un inventaire ne porte que sur ce qui est censé être là. Partir du stock
-- évite des lignes à zéro partout, qui ne montreraient aucun écart.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_inv_produits AS
-- DISTINCT ON (produit_id) : « Stock rayon » et « Stock dépôt » sont tous deux
-- de type PRINCIPAL, et un produit a une ligne de stock dans chacun. Sans cela,
-- il apparaîtrait deux fois dans le même inventaire — ce que la contrainte
-- d'unicité (produit_id, store_inventory_id) refuse.
SELECT DISTINCT ON (sp.produit_id)
    sp.produit_id,
    sp.storage_id,
    sp.qty_stock                                   AS quantite_theorique,
    COALESCE(fp.prix_achat, 0)                     AS prix_achat,
    COALESCE(fp.prix_uni, 0)                       AS prix_vente,
    row_number() OVER (ORDER BY sp.produit_id)     AS rang
FROM stock_produit sp
JOIN storage s ON s.id = sp.storage_id AND s.storage_type = 'PRINCIPAL'
-- Les prix se lisent chez le fournisseur PRINCIPAL (parent_id IS NULL) : les
-- agences n'en portent pas. Même LATERAL que 12_destruction, pour la même
-- raison.
LEFT JOIN LATERAL (
    SELECT x.prix_achat, x.prix_uni
      FROM fournisseur_produit x
      JOIN fournisseur f ON f.id = x.fournisseur_id AND f.parent_id IS NULL
     WHERE x.produit_id = sp.produit_id
     ORDER BY x.id LIMIT 1
) fp ON true
WHERE sp.qty_stock > 0
ORDER BY sp.produit_id, sp.storage_id
LIMIT 80;

CREATE INDEX ON tmp_inv_produits (rang);

-- ---------------------------------------------------------------------------
-- 2. L'inventaire clôturé (il y a 45 jours)
--
-- Les valeurs de début et de fin sont posées à zéro puis recalculées en §5,
-- une fois les lignes connues : l'écran de valorisation les lit telles quelles.
-- ---------------------------------------------------------------------------
INSERT INTO store_inventory (
    id, category, created_at, updated_at, description,
    inventory_amount_begin, inventory_amount_after,
    inventory_value_cost_begin, inventory_value_cost_after,
    inventory_category, inventory_type, statut,
    storage_id, user_id, version
)
SELECT
    1, 'STOCK_TOTAL',
    (CURRENT_DATE - INTERVAL '45 days')::date + TIME '07:30:00',
    (CURRENT_DATE - INTERVAL '45 days')::date + TIME '19:10:00',
    'Inventaire général de l''officine — clôturé',
    0, 0, 0, 0,
    'MAGASIN', 'MANUEL', 'CLOSED',
    (SELECT id FROM storage WHERE storage_type = 'PRINCIPAL' ORDER BY id LIMIT 1),
    (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1),
    0;

-- Lignes comptées : neuf sur dix tombent juste, une sur dix porte un écart.
-- C'est l'ordre de grandeur d'une officine tenue correctement, et c'est ce qui
-- rend l'analyse des écarts lisible : une poignée de lignes, pas une liste.
INSERT INTO store_inventory_line (
    store_inventory_id, produit_id, storage_id,
    quantity_init, quantity_on_hand, quantity_sold,
    gap, inventory_value_cost, last_unit_price,
    updated, updated_at, counted_by_id, version
)
SELECT
    1,
    p.produit_id,
    p.storage_id,
    p.quantite_theorique,
    -- L'écart alterne en signe : la démarque domine (casse, vol), mais un
    -- inventaire fait aussi remonter des produits oubliés en réserve.
    CASE
      WHEN p.rang % 10 = 0 THEN GREATEST(p.quantite_theorique - (1 + p.rang % 4), 0)
      WHEN p.rang % 17 = 0 THEN p.quantite_theorique + 1 + p.rang % 3
      ELSE p.quantite_theorique
    END,
    0,
    CASE
      WHEN p.rang % 10 = 0 THEN -(LEAST(1 + p.rang % 4, p.quantite_theorique))
      WHEN p.rang % 17 = 0 THEN 1 + p.rang % 3
      ELSE 0
    END,
    p.prix_achat,
    p.prix_vente,
    true,
    (CURRENT_DATE - INTERVAL '45 days')::date + TIME '18:45:00',
    (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1),
    0
FROM tmp_inv_produits p;

-- Qualification des écarts : c'est l'étape que STK-13 décrit et que l'écran
-- « Analyse des écarts » restitue. Une cause par ligne, jamais deux.
--
-- La cause est tirée d'un RANG et non de l'identifiant de la ligne : avec une
-- poignée de lignes en écart, un `id % 5` laisse au hasard une ou deux causes
-- absentes, et l'écran « Analyse des écarts » n'affiche alors que trois
-- rubriques sur cinq — sans que rien ne signale ce qui manque.
INSERT INTO inventory_gap_analysis (store_inventory_line_id, cause, quantity, commentaire, created_at)
SELECT
    l.id,
    CASE (l.rang % 5)
      WHEN 0 THEN 'CASSE'
      WHEN 1 THEN 'VOL'
      WHEN 2 THEN 'ERREUR_SAISIE'
      WHEN 3 THEN 'ERREUR_RECEPTION'
      ELSE 'PEREMPTION'
    END,
    abs(l.gap),
    CASE (l.rang % 5)
      WHEN 0 THEN 'Flacon cassé au comptoir, constaté à l''inventaire.'
      WHEN 1 THEN 'Écart non expliqué par les mouvements — démarque inconnue.'
      WHEN 2 THEN 'Quantité saisie à l''envers lors d''un ajustement.'
      WHEN 3 THEN 'Colis reçu incomplet, écart non signalé au fournisseur.'
      ELSE 'Retiré pour péremption sans passer par la sortie de stock.'
    END,
    (CURRENT_DATE - INTERVAL '45 days')::date + TIME '19:00:00'
FROM (
    SELECT x.id, x.gap, row_number() OVER (ORDER BY x.id) AS rang
      FROM store_inventory_line x
     WHERE x.store_inventory_id = 1
       AND x.gap <> 0
) l;

-- ---------------------------------------------------------------------------
-- 3. L'inventaire en cours (rayon, ouvert avant-hier)
--
-- Volontairement INCOMPLET : deux tiers des lignes comptées. Un inventaire en
-- cours entièrement compté n'aurait aucune raison de rester ouvert, et la
-- barre de progression afficherait 100 % — l'écran perdrait ce qu'il a
-- d'utile à montrer.
-- ---------------------------------------------------------------------------
INSERT INTO store_inventory (
    id, category, created_at, updated_at, description,
    inventory_amount_begin, inventory_amount_after,
    inventory_value_cost_begin, inventory_value_cost_after,
    inventory_category, inventory_type, statut,
    storage_id, rayon_id, user_id, version
)
SELECT
    2, 'STOCK_TOTAL',
    (CURRENT_DATE - INTERVAL '2 days')::date + TIME '08:15:00',
    (CURRENT_DATE - INTERVAL '1 day')::date + TIME '11:20:00',
    'Inventaire tournant — rayon ANTIBIOTIQUES',
    0, 0, 0, 0,
    'RAYON', 'PROGRAMME', 'PROCESSING',
    (SELECT id FROM storage WHERE storage_type = 'PRINCIPAL' ORDER BY id LIMIT 1),
    (SELECT id FROM rayon WHERE libelle = 'ANTIBIOTIQUES' LIMIT 1),
    (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1),
    0;

INSERT INTO store_inventory_line (
    store_inventory_id, produit_id, storage_id,
    quantity_init, quantity_on_hand, quantity_sold,
    gap, inventory_value_cost, last_unit_price,
    updated, updated_at, counted_by_id, version
)
SELECT
    2,
    p.produit_id,
    p.storage_id,
    p.quantite_theorique,
    -- Ligne non comptée : quantité à zéro ET `updated` faux. C'est le couple
    -- qui distingue « compté à zéro » de « pas encore compté ».
    CASE WHEN p.rang % 3 = 0 THEN 0 ELSE p.quantite_theorique END,
    0,
    0,
    p.prix_achat,
    p.prix_vente,
    (p.rang % 3 <> 0),
    (CURRENT_DATE - INTERVAL '1 day')::date + TIME '11:20:00',
    CASE WHEN p.rang % 3 = 0 THEN NULL
         ELSE (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1) END,
    0
FROM tmp_inv_produits p
JOIN rayon_produit rp ON rp.produit_id = p.produit_id
JOIN rayon r ON r.id = rp.rayon_id AND r.libelle = 'ANTIBIOTIQUES';

-- ---------------------------------------------------------------------------
-- 4. Le planning d'inventaire tournant
--
-- Mensuel, par rayon : à chaque échéance le planning avance d'un rayon, ce que
-- porte `critere_index_courant`. Deux exécutions déjà passées pour que
-- l'écran ait un historique à montrer plutôt qu'un planning neuf.
-- ---------------------------------------------------------------------------
INSERT INTO planning_inventaire_tournant (
    libelle, frequence, critere, storage_id, user_id,
    prochaine_execution, actif, critere_index_courant,
    nb_executions, derniere_execution, created_at, updated_at
)
SELECT
    'Rotation mensuelle des rayons',
    'MENSUEL', 'RAYON',
    (SELECT id FROM storage WHERE storage_type = 'PRINCIPAL' ORDER BY id LIMIT 1),
    (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1),
    (CURRENT_DATE + INTERVAL '12 days')::date,
    true, 2, 2,
    (CURRENT_DATE - INTERVAL '18 days')::date,
    (CURRENT_DATE - INTERVAL '80 days')::date + TIME '09:00:00',
    (CURRENT_DATE - INTERVAL '18 days')::date + TIME '09:00:00';

-- ---------------------------------------------------------------------------
-- 5. Valorisation des en-têtes, déduite des lignes
--
-- L'écran de valorisation (STK-15, STK-16) lit ces quatre colonnes sans jamais
-- recalculer : si elles ne correspondent pas aux lignes, il affiche un total
-- faux sans le moindre avertissement.
-- ---------------------------------------------------------------------------
UPDATE store_inventory si
   SET inventory_value_cost_begin = v.cout_theorique,
       inventory_value_cost_after = v.cout_compte,
       inventory_amount_begin     = v.vente_theorique,
       inventory_amount_after     = v.vente_compte,
       -- L'écart ne se calcule qu'À LA CLÔTURE. Sur un inventaire en cours, les
       -- lignes non comptées valent zéro : en déduire un écart ferait afficher
       -- un manquant colossal, qui n'est que du travail restant à faire.
       gap_cost   = CASE WHEN si.statut = 'CLOSED' THEN v.cout_compte  - v.cout_theorique  END,
       gap_amount = CASE WHEN si.statut = 'CLOSED' THEN v.vente_compte - v.vente_theorique END
  FROM (
      SELECT l.store_inventory_id,
             sum(l.quantity_init * l.inventory_value_cost)::bigint AS cout_theorique,
             sum(l.quantity_init * l.last_unit_price)::bigint      AS vente_theorique,
             -- Côté compté, seules les lignes RÉELLEMENT comptées entrent dans
             -- la valorisation, pour la même raison.
             sum(CASE WHEN l.updated THEN l.quantity_on_hand * l.inventory_value_cost ELSE 0 END)::bigint AS cout_compte,
             sum(CASE WHEN l.updated THEN l.quantity_on_hand * l.last_unit_price      ELSE 0 END)::bigint AS vente_compte
        FROM store_inventory_line l
       GROUP BY l.store_inventory_id
  ) v
 WHERE v.store_inventory_id = si.id;

-- Les séquences d'identité doivent repartir au-dessus des identifiants posés à
-- la main, sinon la première création depuis l'application viole la clé.
SELECT setval(pg_get_serial_sequence('store_inventory', 'id'),
              (SELECT max(id) FROM store_inventory));
SELECT setval(pg_get_serial_sequence('store_inventory_line', 'id'),
              (SELECT max(id) FROM store_inventory_line));

DROP TABLE tmp_inv_produits;

\echo '   inventaires chargés (1 clôturé, 1 en cours, 1 planning tournant)'
