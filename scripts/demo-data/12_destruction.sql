-- ============================================================================
-- 12_destruction.sql — Produits périmés en attente de destruction
--
-- PIÈGE DE NOMMAGE, vérifié sur la base : products_to_destroy porte
--     numlot, prixachat, prixunit, dateperemption, datedestuction
-- SANS underscore — l'entité déclare ces champs sans @Column, Hibernate a mis
-- le camelCase en minuscules. Noter que « datedestuction » reprend la faute de
-- frappe du champ Java (dateDestuction) : la colonne est bien orthographiée
-- ainsi en base, et la corriger ici casserait le mapping.
--
-- Seul stock_initial garde son underscore, parce qu'il est le seul à déclarer
-- @Column(name = "stock_initial").
--
-- La table référence fournisseur_produit (obligatoire) — donc, une fois de
-- plus, un fournisseur PRINCIPAL — et magasin (obligatoire).
-- ============================================================================

\i _header.sql

\echo '>> 12_destruction : produits périmés à détruire'

-- ---------------------------------------------------------------------------
-- Alimentation depuis les lots périmés
--
-- Deux états coexistent, comme en officine :
--   * en attente  → destroyed = false, datedestuction nulle ;
--   * détruits    → destroyed = true, procès-verbal daté.
--
-- Les lots DESTROYED sont ceux périmés depuis plus de 150 jours : ils ont eu
-- le temps de passer en destruction effective. Les EXPIRED récents sont encore
-- en attente.
-- ---------------------------------------------------------------------------
INSERT INTO products_to_destroy (
    numlot, dateperemption, datedestuction,
    prixachat, prixunit, quantity, stock_initial,
    destroyed, editing,
    fournisseur_produit_id, magasin_id, user_id,
    created, updated
)
SELECT
    l.num_lot,
    l.expiry_date,
    -- Le procès-verbal de destruction intervient environ un mois après le
    -- constat de péremption.
    CASE WHEN l.statut = 'DESTROYED'
         THEN LEAST(l.expiry_date + 30, CURRENT_DATE - 1)
         ELSE NULL END,
    l.prixachat,
    l.prixunit,
    l.current_quantity,
    -- @Min(1) sur les deux : on ne met pas en destruction une quantité nulle.
    GREATEST(1, l.quantity),
    l.statut = 'DESTROYED',
    false,
    fp.id,
    1,
    u.id,
    l.expiry_date + TIME '09:00:00',
    l.expiry_date + TIME '09:00:00'
FROM lot l
JOIN produit p ON p.id = l.produit_id
-- Le fournisseur_produit retenu est celui du PRINCIPAL : la contrainte du §3.3
-- vaut ici comme partout ailleurs.
JOIN LATERAL (
    SELECT x.id FROM fournisseur_produit x
      JOIN fournisseur f ON f.id = x.fournisseur_id AND f.parent_id IS NULL
     WHERE x.produit_id = l.produit_id
     ORDER BY x.id LIMIT 1
) fp ON true
CROSS JOIN LATERAL (SELECT id FROM app_user WHERE login = 'admin' LIMIT 1) u
WHERE l.statut IN ('EXPIRED', 'DESTROYED')
  AND l.current_quantity >= 1
  AND NOT EXISTS (
      SELECT 1 FROM products_to_destroy d
       WHERE d.numlot = l.num_lot AND d.fournisseur_produit_id = fp.id
  );

-- ---------------------------------------------------------------------------
-- Contrôles immédiats
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_total int; v_attente int; v_detruits int; v_agence int; v_dates int;
BEGIN
    SELECT count(*) INTO v_total    FROM products_to_destroy;
    SELECT count(*) INTO v_attente  FROM products_to_destroy WHERE NOT destroyed;
    SELECT count(*) INTO v_detruits FROM products_to_destroy WHERE destroyed;

    SELECT count(*) INTO v_agence FROM products_to_destroy d
      JOIN fournisseur_produit fp ON fp.id = d.fournisseur_produit_id
      JOIN fournisseur f ON f.id = fp.fournisseur_id
     WHERE f.parent_id IS NOT NULL;

    -- La destruction ne peut pas précéder la péremption.
    SELECT count(*) INTO v_dates FROM products_to_destroy
     WHERE datedestuction IS NOT NULL AND datedestuction < dateperemption;

    IF v_total < 30 THEN RAISE EXCEPTION 'Produits à détruire : % (attendu >= 30)', v_total; END IF;
    IF v_attente = 0 THEN RAISE EXCEPTION 'Aucun produit en attente de destruction'; END IF;
    IF v_detruits = 0 THEN RAISE EXCEPTION 'Aucun produit effectivement détruit'; END IF;
    IF v_agence > 0 THEN RAISE EXCEPTION '% ligne(s) rattachée(s) à une agence', v_agence; END IF;
    IF v_dates > 0 THEN RAISE EXCEPTION '% destruction(s) antérieure(s) à la péremption', v_dates; END IF;

    RAISE NOTICE '% à détruire (% en attente, % détruits).', v_total, v_attente, v_detruits;
END $$;

\echo '<< 12_destruction : terminé'
