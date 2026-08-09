
SELECT setval(
         'id_mvt_produit_seq',
         GREATEST(
           COALESCE((SELECT MAX(id) FROM inventory_transaction), 0),
           (SELECT last_value FROM id_mvt_produit_seq)
         ),
         TRUE
       );


ALTER TABLE planning_inventaire_tournant
ALTER COLUMN classe_pareto_courante TYPE VARCHAR(10);

COMMENT ON COLUMN planning_inventaire_tournant.classe_pareto_courante IS
  'Critère CLASSIFICATION_ABC : classe Pareto courante — A_PLUS, A, B, C ou D. '
  'Valeurs identiques à v_abc_pareto_analysis.classe_pareto (A_PLUS, pas ''A+'').';

-- Un planning ABC sans classe courante (créé avant l'initialisation, ou critère modifié
-- après coup) repart au début du cycle.
UPDATE planning_inventaire_tournant
SET classe_pareto_courante = 'A_PLUS'
WHERE critere = 'CLASSIFICATION_ABC'
  AND classe_pareto_courante IS NULL;

-- L'index suit la classe, et non l'inverse : c'est la classe affichée à l'utilisateur et
-- utilisée par la dernière exécution qui fait foi.
UPDATE planning_inventaire_tournant
SET critere_index_courant = CASE classe_pareto_courante
                              WHEN 'A_PLUS' THEN 0
                              WHEN 'A' THEN 1
                              WHEN 'B' THEN 2
                              WHEN 'C' THEN 3
                              WHEN 'D' THEN 4
                              ELSE 0
  END,
    updated_at             = NOW()
WHERE critere = 'CLASSIFICATION_ABC';



CREATE OR REPLACE PROCEDURE proc_close_inventory_v2(
  IN p_store_inventory_id BIGINT,
  IN p_gestion_lot BOOLEAN,
  INOUT p_nombre_ligne INT
)
  LANGUAGE plpgsql AS
$$
DECLARE
  v_user_id            INT;
  v_magasin_id         INT;
  v_inventory_category TEXT;
BEGIN
  SELECT s.user_id, u.magasin_id, s.inventory_category
  INTO v_user_id, v_magasin_id, v_inventory_category
  FROM store_inventory s
         JOIN app_user u ON s.user_id = u.id
  WHERE s.id = p_store_inventory_id;

  -- STEP 1 : Mettre à jour le stock du storage inventorié
  --   Ligne jamais comptée et sans quantité initiale connue → stock laissé en l'état.
  UPDATE stock_produit sp
  SET qty_stock   = COALESCE(sil.quantity_on_hand, sil.quantity_init, sp.qty_stock),
      qty_ug      = 0,
      qty_virtual = COALESCE(sil.quantity_on_hand, sil.quantity_init, sp.qty_virtual),
      updated_at  = NOW()
  FROM store_inventory_line sil
  WHERE sil.store_inventory_id = p_store_inventory_id
    AND sp.produit_id = sil.produit_id
    AND sp.storage_id = sil.storage_id;

  GET DIAGNOSTICS p_nombre_ligne = ROW_COUNT;

  -- STEP 2 : Réserve → 0 (MAGASIN uniquement)
  --   Pour un inventaire MAGASIN, le pharmacien saisit le stock total consolidé
  --   (rayon + réserve) sur une seule ligne PRINCIPAL. La réserve est remise à 0
  --   car son contenu est inclus dans le chiffre PRINCIPAL compté.
  --   Pour tous les autres types (RAYON, FAMILLY, thématiques), la réserve n'est
  --   pas concernée par l'inventaire et ne doit pas être modifiée.
  IF v_inventory_category = 'MAGASIN' THEN
    UPDATE stock_produit sp
    SET qty_stock   = 0,
        qty_ug      = 0,
        qty_virtual = 0,
        updated_at  = NOW()
    FROM store_inventory_line sil
           JOIN storage s_inventoried ON s_inventoried.id = sil.storage_id
    WHERE sil.store_inventory_id = p_store_inventory_id
      AND sp.produit_id = sil.produit_id
      AND sp.storage_id <> sil.storage_id
      AND sp.storage_id IN (SELECT s2.id
                            FROM storage s2
                            WHERE s2.magasin_id = s_inventoried.magasin_id
                              AND s2.storage_type = 'SAFETY_STOCK');
  END IF;

  -- STEP 3 : Lots (si gestion lot activée)
  IF p_gestion_lot THEN
    UPDATE lot l
    SET current_quantity = COALESCE(il.quantity_on_hand, il.quantity_init, l.current_quantity)
    FROM inventory_lot il
           JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
    WHERE sil.store_inventory_id = p_store_inventory_id
      AND l.id = il.lot_id;
  END IF;

  -- STEP 4 : Journal — delta signé + storage_id
  --   `id` explicite : la colonne n'a pas de défaut, et `id_mvt_produit_seq` est la
  --   séquence déjà utilisée par le code Java sur cette même table.
  --   Toutes les valeurs numériques sont repliées : la cible est NOT NULL partout.
  INSERT INTO inventory_transaction
  (id, cost_amount, created_at, entity_id, mouvement_type,
   quantity, quantity_after, quantity_befor, regular_unit_price,
   magasin_id, storage_id, produit_id, user_id, transaction_date)
  SELECT nextval('id_mvt_produit_seq'),
         COALESCE(sil.inventory_value_cost, fp.prix_achat, p.cost_amount, 0),
         sil.updated_at,
         sil.id,
         'INVENTAIRE',
         COALESCE(sil.quantity_on_hand, sil.quantity_init, 0) - COALESCE(sil.quantity_init, 0),
         COALESCE(sil.quantity_on_hand, sil.quantity_init, 0),
         COALESCE(sil.quantity_init, 0),
         COALESCE(sil.last_unit_price, fp.prix_uni, p.regular_unit_price, 0),
         v_magasin_id,
         sil.storage_id,
         sil.produit_id,
         v_user_id,
         sil.updated_at::date
  FROM store_inventory_line sil
         JOIN produit p ON p.id = sil.produit_id
         LEFT JOIN fournisseur_produit fp ON fp.id = p.fournisseur_produit_principal_id
  WHERE sil.store_inventory_id = p_store_inventory_id;

  -- STEP 5 : Snapshot checkpoint — `id` laissé à l'identité de la table
  INSERT INTO stock_produit_snapshot
  (produit_id, storage_id, snapshot_date, qty_stock, source_type, source_inventory_id)
  SELECT sil.produit_id,
         sil.storage_id,
         NOW(),
         COALESCE(sil.quantity_on_hand, sil.quantity_init, 0),
         'INVENTAIRE_CLOTURE',
         p_store_inventory_id
  FROM store_inventory_line sil
  WHERE sil.store_inventory_id = p_store_inventory_id
  ON CONFLICT ON CONSTRAINT uq_snapshot_produit_storage_date DO NOTHING;

END;
$$;




DELETE
FROM inventory_gap_analysis a USING inventory_gap_analysis b
WHERE a.store_inventory_line_id = b.store_inventory_line_id
  AND (a.created_at < b.created_at OR (a.created_at = b.created_at AND a.id < b.id));

ALTER TABLE inventory_gap_analysis
  ADD CONSTRAINT uq_iga_store_inventory_line UNIQUE (store_inventory_line_id);

-- `idx_iga_line_id` devient redondant : la contrainte d'unicité crée son propre index
-- sur la même colonne.
DROP INDEX IF EXISTS idx_iga_line_id;

CREATE INDEX IF NOT EXISTS idx_sil_gap_analysis
  ON store_inventory_line (store_inventory_id, (ABS(gap)) DESC, id)
  WHERE updated AND gap <> 0;

COMMENT ON INDEX idx_sil_gap_analysis IS
  'Sert la page de qualification des écarts : filtre partiel sur les lignes comptées en '
    'écart, et ordre de tri (ABS(gap) DESC, id) déjà matérialisé.';


