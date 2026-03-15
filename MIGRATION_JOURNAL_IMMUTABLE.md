# Plan de Migration — Principe 1 : Journal Immuable (Append-Only)

> **Objectif** : Faire de `inventory_transaction` la source de vérité unique pour tous les
> mouvements de stock, reconstituable par produit **et par storage** à n'importe quel instant T,
> sans jamais modifier ou supprimer une ligne existante.

---

## Diagnostic — État actuel

### Ce qui va bien ✅

| Élément | Observation |
|---|---|
| `inventory_transaction` | Pas de `UPDATE`/`DELETE` — append-only de fait |
| `repartition_stock_produit` | Append-only, snapshots init/final présents |
| `store_inventory_line` | Immutable après clôture (`updated_at` uniquement) |
| `quantity_befor` + `quantity_after` | Déjà présents → delta calculable |
| `entity_id` | Traçabilité vers l'entité source |

### Ce qui bloque ❌

| Problème | Impact |
|---|---|
| **Pas de `storage_id` dans `inventory_transaction`** | Impossible de reconstituer le stock par storage à T |
| **`quantity` incohérent** : absolu pour INVENTAIRE, delta pour tout le reste | Agrégation `SUM(quantity)` fausse pour INVENTAIRE |
| **`repartition_stock_produit` non lié à `inventory_transaction`** | Les transferts inter-storage sont invisibles dans le journal principal |
| **`determineBeforeAfterStock()` agrège tous les storages** | Les snapshots before/after dans le journal perdent la granularité storage |
| **`proc_close_inventory_v2` STEP 4** insère sans `storage_id` | Chaque INVENTAIRE dans le journal est orphelin de son storage |
| **PK composite `(id, transaction_date)`** | Complexifie joins et requêtes, anti-pattern |
| **Contrainte unique sur `LocalDate`** pas `LocalDateTime` | Risque de collision en cas de double mouvement même jour |
| **Pas de table de snapshots** | Full-scan historique pour chaque requête stock-à-T |

---

## Architecture cible

```
inventory_transaction (journal principal — toutes opérations)
  ├── produit_id
  ├── storage_id      ← AJOUT CRITIQUE
  ├── magasin_id      ← conservé (pour requêtes agrégées magasin)
  ├── mouvement_type  ← SALE, ENTREE_STOCK, AJUSTEMENT_IN/OUT,
  │                      INVENTAIRE, DECONDTION_IN/OUT,
  │                      MOUVEMENT_STOCK_IN, MOUVEMENT_STOCK_OUT,
  │                      RETRAIT_PERIME, RETOUR_DEPOT, RETOUR_FOURNISSEUR
  ├── quantity        ← TOUJOURS un delta signé (+/-)
  ├── quantity_befor  ← stock storage avant l'événement
  ├── quantity_after  ← stock storage après l'événement
  ├── entity_id       ← FK vers l'entité source (sale_line, order_line, etc.)
  └── created_at      ← LocalDateTime UTC (pas LocalDate)

stock_produit_snapshot (table de checkpoints pour performance)
  ├── produit_id
  ├── storage_id
  ├── snapshot_date
  ├── qty_stock
  └── source_inventory_id  ← FK vers store_inventory (clôture = checkpoint naturel)
```

**Règle fondamentale :**
```
stock(produit, storage, T) =
    snapshot_le_plus_proche_avant_T.qty_stock
    + SUM(delta) des inventory_transaction entre snapshot_date et T
```

---

## Migration Step by Step

---

### STEP 1 — Ajouter `storage_id` à `inventory_transaction`

**Fichier Flyway :** `V1.2.4__inventory_transaction_add_storage_id.sql`

```sql
-- 1.1 Ajout de la colonne nullable (pour backfill sans casser la prod)
ALTER TABLE inventory_transaction
    ADD COLUMN IF NOT EXISTS storage_id INT REFERENCES storage(id);

-- 1.2 Backfill INVENTAIRE : storage_id récupéré via store_inventory_line (entity_id = sil.id)
UPDATE inventory_transaction it
SET storage_id = sil.storage_id
FROM store_inventory_line sil
WHERE it.mouvement_type = 'INVENTAIRE'
  AND it.entity_id = sil.id
  AND it.storage_id IS NULL;

-- 1.3 Backfill AJUSTEMENT : via ajustement → stock_produit → storage
UPDATE inventory_transaction it
SET storage_id = sp.storage_id
FROM ajustement a
JOIN stock_produit sp ON sp.id = a.stock_produit_id
WHERE it.mouvement_type IN ('AJUSTEMENT_IN', 'AJUSTEMENT_OUT')
  AND it.entity_id = a.id
  AND it.storage_id IS NULL;

-- 1.4 Backfill SALE : storage PRINCIPAL du magasin (approximation conservatrice)
UPDATE inventory_transaction it
SET storage_id = s.id
FROM storage s
WHERE it.mouvement_type IN ('SALE', 'CANCEL_SALE', 'DELETE_SALE')
  AND it.storage_id IS NULL
  AND s.magasin_id = it.magasin_id
  AND s.storage_type = 'PRINCIPAL';

-- 1.5 Backfill ENTREE_STOCK : storage PRINCIPAL du magasin
UPDATE inventory_transaction it
SET storage_id = s.id
FROM storage s
WHERE it.mouvement_type = 'ENTREE_STOCK'
  AND it.storage_id IS NULL
  AND s.magasin_id = it.magasin_id
  AND s.storage_type = 'PRINCIPAL';

-- 1.6 Backfill restant (fallback storage PRINCIPAL)
UPDATE inventory_transaction it
SET storage_id = s.id
FROM storage s
WHERE it.storage_id IS NULL
  AND s.magasin_id = it.magasin_id
  AND s.storage_type = 'PRINCIPAL';

-- 1.7 Contrainte NOT NULL une fois backfill terminé
ALTER TABLE inventory_transaction
    ALTER COLUMN storage_id SET NOT NULL;

-- 1.8 Index pour les requêtes historiques par storage
CREATE INDEX IF NOT EXISTS idx_inv_tx_storage_produit_date
    ON inventory_transaction (storage_id, produit_id, created_at);

CREATE INDEX IF NOT EXISTS idx_inv_tx_produit_date
    ON inventory_transaction (produit_id, created_at);
```

**Entité Java — `InventoryTransaction.java` :**

```java
// Ajouter le champ storage
@ManyToOne(optional = false, fetch = FetchType.LAZY)
@NotNull
@JoinColumn(name = "storage_id")
private Storage storage;

// Getter/setter
public Storage getStorage() { return storage; }
public InventoryTransaction setStorage(Storage storage) {
    this.storage = storage;
    return this;
}
```

---

### STEP 2 — Normaliser `quantity` : toujours un delta signé

**Fichier Flyway :** `V1.2.5__inventory_transaction_normalize_quantity.sql`

```sql
-- Pour INVENTAIRE : quantity était = quantityOnHand (valeur absolue)
-- On le remplace par le delta réel = quantity_after - quantity_befor
UPDATE inventory_transaction
SET quantity = quantity_after - quantity_befor
WHERE mouvement_type = 'INVENTAIRE'
  AND quantity = quantity_after;  -- condition de détection : l'ancienne valeur
```

> **Règle cible :**
> - `quantity` = delta signé (négatif = sortie, positif = entrée)
> - `quantity_befor` = stock storage avant
> - `quantity_after` = stock storage après
> - Invariant : `quantity_after = quantity_befor + quantity`
> - Exception naturelle : INVENTAIRE peut avoir `quantity_after != quantity_befor + delta_théorique`
>   (c'est précisément ce qui constitue l'écart d'inventaire)

**Màj `InventoryTransactionBuilder.java` — cas `StoreInventoryLine` :**

```java
// AVANT (ligne 133)
.setQuantity(storeInventoryLine.getQuantityOnHand())  // absolu ← FAUX

// APRÈS
.setQuantity(storeInventoryLine.getQuantityOnHand() - storeInventoryLine.getQuantityInit())  // delta signé
.setQuantityBefor(storeInventoryLine.getQuantityInit())
.setQuantityAfter(storeInventoryLine.getQuantityOnHand())
.setStorage(storeInventoryLine.getStorage())          // ← STEP 1 requis
```

**Màj `proc_close_inventory_v2` STEP 4 — `V1.2.5__inventory_transaction_normalize_quantity.sql` :**

```sql
CREATE OR REPLACE PROCEDURE proc_close_inventory_v2(
    IN  p_store_inventory_id BIGINT,
    IN  p_gestion_lot        BOOLEAN,
    INOUT p_nombre_ligne     INT
)
LANGUAGE plpgsql AS $$
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

    -- STEP 1 : stock storage
    UPDATE stock_produit sp
    SET qty_stock   = COALESCE(sil.quantity_on_hand, sil.quantity_init),
        qty_ug      = 0,
        qty_virtual = COALESCE(sil.quantity_on_hand, sil.quantity_init),
        updated_at  = NOW()
    FROM store_inventory_line sil
    WHERE sil.store_inventory_id = p_store_inventory_id
      AND sp.produit_id = sil.produit_id
      AND sp.storage_id = sil.storage_id;

    GET DIAGNOSTICS p_nombre_ligne = ROW_COUNT;

    -- STEP 2 : SAFETY_STOCK → 0 (sauf STORAGE)
    IF v_inventory_category <> 'STORAGE' THEN
        UPDATE stock_produit sp
        SET qty_stock = 0, qty_ug = 0, qty_virtual = 0, updated_at = NOW()
        FROM store_inventory_line sil
        JOIN storage s_inventoried ON s_inventoried.id = sil.storage_id
        WHERE sil.store_inventory_id = p_store_inventory_id
          AND sp.produit_id = sil.produit_id
          AND sp.storage_id <> sil.storage_id
          AND sp.storage_id IN (
              SELECT s2.id FROM storage s2
              WHERE s2.magasin_id = s_inventoried.magasin_id
                AND s2.storage_type = 'SAFETY_STOCK'
          );
    END IF;

    -- STEP 3 : lots
    IF p_gestion_lot THEN
        UPDATE lot l
        SET current_quantity = COALESCE(il.quantity_on_hand, il.quantity_init)
        FROM inventory_lot il
        JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
        WHERE sil.store_inventory_id = p_store_inventory_id
          AND l.id = il.lot_id;
    END IF;

    -- STEP 4 : journal — delta signé + storage_id  ← CORRIGÉ
    INSERT INTO inventory_transaction
        (cost_amount, created_at, entity_id, mouvement_type,
         quantity,                                              -- delta signé
         quantity_after, quantity_befor, regular_unit_price,
         magasin_id, storage_id,                               -- ← storage_id ajouté
         produit_id, user_id)
    SELECT sil.inventory_value_cost,
           sil.updated_at,
           sil.id,
           'INVENTAIRE',
           COALESCE(sil.quantity_on_hand, sil.quantity_init) - sil.quantity_init,  -- delta
           COALESCE(sil.quantity_on_hand, sil.quantity_init),                      -- after
           sil.quantity_init,                                                       -- before
           sil.last_unit_price,
           v_magasin_id,
           sil.storage_id,                                     -- ← storage_id
           sil.produit_id,
           v_user_id
    FROM store_inventory_line sil
    WHERE sil.store_inventory_id = p_store_inventory_id;

END;
$$;
```

---

### STEP 3 — Unifier `repartition_stock_produit` avec le journal principal

Chaque transfert génère actuellement une ligne dans `repartition_stock_produit` mais **aucune ligne** dans `inventory_transaction`. Les `MOUVEMENT_STOCK_IN/OUT` sont dans le builder mais non appelés lors de la création de `RepartitionStockProduit`.

**Fichier Flyway :** `V1.2.6__unify_repartition_in_journal.sql`

```sql
-- 1. Lier repartition_stock_produit au journal (traçabilité)
ALTER TABLE repartition_stock_produit
    ADD COLUMN IF NOT EXISTS tx_out_id BIGINT,  -- FK vers inventory_transaction (source)
    ADD COLUMN IF NOT EXISTS tx_in_id  BIGINT;  -- FK vers inventory_transaction (destination)

-- 2. Backfill : créer les entrées manquantes MOUVEMENT_STOCK_OUT (source)
-- Note : les IDs sont générés par la séquence existante mvt_produit_id_seq
INSERT INTO inventory_transaction
    (id, transaction_date, cost_amount, created_at, entity_id, mouvement_type,
     quantity, quantity_after, quantity_befor, regular_unit_price,
     magasin_id, storage_id, produit_id, user_id)
SELECT
    nextval('mvt_produit_id_seq'),
    r.created_at::date,
    fp.prix_achat,
    r.created_at,
    r.id,
    'MOUVEMENT_STOCK_OUT',
    -r.qty_mvt,                          -- delta négatif (sortie)
    r.source_final_stock,
    r.source_init_stock,
    fp.prix_uni,
    s.magasin_id,
    sp_src.storage_id,
    sp_src.produit_id,
    r.user_id
FROM repartition_stock_produit r
JOIN stock_produit sp_src ON sp_src.id = r.stock_produit_source_id
JOIN storage s ON s.id = sp_src.storage_id
JOIN fournisseur_produit fp ON fp.produit_id = sp_src.produit_id AND fp.principal = true
WHERE r.stock_produit_source_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM inventory_transaction it
      WHERE it.entity_id = r.id
        AND it.mouvement_type = 'MOUVEMENT_STOCK_OUT'
        AND it.produit_id = sp_src.produit_id
  );

-- 3. Backfill : créer les entrées manquantes MOUVEMENT_STOCK_IN (destination)
INSERT INTO inventory_transaction
    (id, transaction_date, cost_amount, created_at, entity_id, mouvement_type,
     quantity, quantity_after, quantity_befor, regular_unit_price,
     magasin_id, storage_id, produit_id, user_id)
SELECT
    nextval('mvt_produit_id_seq'),
    r.created_at::date,
    fp.prix_achat,
    r.created_at,
    r.id,
    'MOUVEMENT_STOCK_IN',
    r.qty_mvt,                           -- delta positif (entrée)
    r.dest_final_stock,
    r.dest_init_stock,
    fp.prix_uni,
    s.magasin_id,
    sp_dst.storage_id,
    sp_dst.produit_id,
    r.user_id
FROM repartition_stock_produit r
JOIN stock_produit sp_dst ON sp_dst.id = r.stock_produit_destination_id
JOIN storage s ON s.id = sp_dst.storage_id
JOIN fournisseur_produit fp ON fp.produit_id = sp_dst.produit_id AND fp.principal = true
WHERE NOT EXISTS (
    SELECT 1 FROM inventory_transaction it
    WHERE it.entity_id = r.id
      AND it.mouvement_type = 'MOUVEMENT_STOCK_IN'
      AND it.produit_id = sp_dst.produit_id
);
```

**Service Java — `RepartitionStockService` / `SuggestionReassortServiceImpl` :**

Après création d'une `RepartitionStockProduit`, appeler `inventoryTransactionService.save()` pour les deux côtés :

```java
// Après save(repartition) :

// OUT : source perd du stock
InventoryTransaction txOut = new InventoryTransaction()
    .setMouvementType(MouvementProduit.MOUVEMENT_STOCK_OUT)
    .setQuantity(-repartition.getQtyMvt())                    // delta négatif
    .setQuantityBefor(repartition.getSourceInitStock())
    .setQuantityAfter(repartition.getSourceFinalStock())
    .setStorage(repartition.getStockProduitSource().getStorage())
    .setProduit(repartition.getStockProduitSource().getProduit())
    .setMagasin(repartition.getStockProduitSource().getStorage().getMagasin())
    .setEntityId(repartition.getId().longValue())
    .setUser(repartition.getUser())
    .setCreatedAt(repartition.getCreated());

// IN : destination gagne du stock
InventoryTransaction txIn = new InventoryTransaction()
    .setMouvementType(MouvementProduit.MOUVEMENT_STOCK_IN)
    .setQuantity(repartition.getQtyMvt())                     // delta positif
    .setQuantityBefor(repartition.getDestInitStock())
    .setQuantityAfter(repartition.getDestFinalStock())
    .setStorage(repartition.getStockProduitDestination().getStorage())
    .setProduit(repartition.getStockProduitDestination().getProduit())
    .setMagasin(repartition.getStockProduitDestination().getStorage().getMagasin())
    .setEntityId(repartition.getId().longValue())
    .setUser(repartition.getUser())
    .setCreatedAt(repartition.getCreated());
```

---

### STEP 4 — Corriger `InventoryTransactionBuilder` pour propager `storage_id`

**`InventoryTransactionBuilder.java` — chaque branche :**

```java
// SalesLine : vente sur le storage du rayon (PRINCIPAL)
} else if (entity instanceof SalesLine salesLine) {
    Storage storage = salesLine.getSales().getCashRegister()... // récupérer le storage de la caisse
    // Si non disponible : storage PRINCIPAL du magasin
    inventoryTransaction = new InventoryTransaction()
        ...
        .setStorage(storage)      // ← AJOUT
        .setQuantity(-salesLine.getQuantitySold())  // delta négatif pour une vente

// OrderLine : réception sur le storage de destination de la commande
} else if (entity instanceof OrderLine orderLine) {
    Storage storage = orderLine.getCommande().getStorage(); // storage destination commande
    inventoryTransaction = new InventoryTransaction()
        ...
        .setStorage(storage)

// Ajustement : directement via stockProduit
} else if (entity instanceof Ajustement ajustement) {
    Storage storage = ajustement.getStockProduit().getStorage(); // stockProduit contient le storage
    inventoryTransaction = new InventoryTransaction()
        ...
        .setQuantityBefor(ajustement.getStockBefore())   // before du storage précis
        .setQuantityAfter(ajustement.getStockAfter())    // after du storage précis
        .setStorage(storage)
    // SUPPRIMER determineBeforeAfterStock() — on veut le stock du storage, pas du magasin

// Decondition
} else if (entity instanceof Decondition decondition) {
    .setStorage(decondition.getStorage())  // ajouter storage sur Decondition

// ProductsToDestroy (retrait périmé)
} else if (entity instanceof ProductsToDestroy productsToDestroy) {
    .setStorage(productsToDestroy.getStorage())

// StoreInventoryLine
} else if (entity instanceof StoreInventoryLine storeInventoryLine) {
    .setStorage(storeInventoryLine.getStorage())                          // ← AJOUT
    .setQuantity(storeInventoryLine.getQuantityOnHand()
                 - storeInventoryLine.getQuantityInit())                  // delta signé
```

> **Note critique** : `determineBeforeAfterStock()` agrège actuellement les stocks de tous les
> storages du magasin pour calculer les before/after. Cela doit être remplacé par le stock du
> storage précis. Cette méthode doit être supprimée.

---

### STEP 5 — Créer la table de snapshots (checkpoints)

**Fichier Flyway :** `V1.2.7__stock_produit_snapshot.sql`

```sql
CREATE TABLE IF NOT EXISTS stock_produit_snapshot (
    id                  BIGSERIAL PRIMARY KEY,
    produit_id          INT          NOT NULL REFERENCES produit(id),
    storage_id          INT          NOT NULL REFERENCES storage(id),
    snapshot_date       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    qty_stock           INT          NOT NULL,
    source_type         TEXT         NOT NULL, -- 'INVENTAIRE_CLOTURE' | 'BATCH_QUOTIDIEN'
    source_inventory_id BIGINT       REFERENCES store_inventory(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_snapshot_produit_storage_date
    ON stock_produit_snapshot (produit_id, storage_id, snapshot_date DESC);

CREATE INDEX idx_snapshot_storage_date
    ON stock_produit_snapshot (storage_id, snapshot_date);

-- Snapshot initial : état actuel de stock_produit comme point de départ
INSERT INTO stock_produit_snapshot (produit_id, storage_id, snapshot_date, qty_stock, source_type)
SELECT produit_id, storage_id, NOW(), qty_stock, 'INVENTAIRE_CLOTURE'
FROM stock_produit;
```

**Mise à jour de `proc_close_inventory_v2` — STEP 5 (nouveau) :**

```sql
-- STEP 5 : checkpoint snapshot à la clôture
INSERT INTO stock_produit_snapshot
    (produit_id, storage_id, snapshot_date, qty_stock, source_type, source_inventory_id)
SELECT sil.produit_id,
       sil.storage_id,
       NOW(),
       COALESCE(sil.quantity_on_hand, sil.quantity_init),
       'INVENTAIRE_CLOTURE',
       p_store_inventory_id
FROM store_inventory_line sil
WHERE sil.store_inventory_id = p_store_inventory_id;
```

---

### STEP 6 — Requête officielle : stock à l'instant T par storage

**Fonction PostgreSQL :** `V1.2.8__fn_stock_at_time.sql`

```sql
CREATE OR REPLACE FUNCTION fn_stock_at_time(
    p_produit_id  INT,
    p_storage_id  INT,
    p_at          TIMESTAMPTZ DEFAULT NOW()
)
RETURNS INT
LANGUAGE sql STABLE AS $$
    WITH last_snapshot AS (
        SELECT qty_stock, snapshot_date
        FROM stock_produit_snapshot
        WHERE produit_id  = p_produit_id
          AND storage_id  = p_storage_id
          AND snapshot_date <= p_at
        ORDER BY snapshot_date DESC
        LIMIT 1
    ),
    deltas AS (
        SELECT COALESCE(SUM(quantity), 0) AS total_delta
        FROM inventory_transaction it
        WHERE it.produit_id  = p_produit_id
          AND it.storage_id  = p_storage_id
          AND it.mouvement_type <> 'INVENTAIRE'   -- INVENTAIRE = reset = snapshot géré séparément
          AND it.created_at  > (SELECT snapshot_date FROM last_snapshot)
          AND it.created_at <= p_at
    )
    SELECT COALESCE(
        (SELECT qty_stock FROM last_snapshot) + (SELECT total_delta FROM deltas),
        0
    );
$$;

-- Usage :
-- SELECT fn_stock_at_time(42, 3, '2025-12-31 23:59:59+00');
```

**Vue utilitaire : stock actuel reconstitué (vs `stock_produit.qty_stock`) :**

```sql
CREATE OR REPLACE VIEW v_stock_reconstitue AS
SELECT
    sp.produit_id,
    sp.storage_id,
    sp.qty_stock                       AS qty_stock_actuel,   -- état mutable courant
    fn_stock_at_time(sp.produit_id, sp.storage_id, NOW()) AS qty_stock_journal,  -- reconstitué
    sp.qty_stock - fn_stock_at_time(sp.produit_id, sp.storage_id, NOW()) AS ecart
FROM stock_produit sp;
-- Un écart != 0 révèle une mutation de stock_produit sans journal correspondant
```

---

### STEP 7 — Corriger la PK composite de `inventory_transaction`

La PK `@IdClass(ProductMvtId)` sur `(id, transaction_date)` est un anti-pattern — `id` seul devrait suffire.

**Fichier Flyway :** `V1.2.9__inventory_transaction_simplify_pk.sql`

```sql
-- 1. Supprimer l'ancienne PK composite
ALTER TABLE inventory_transaction DROP CONSTRAINT inventory_transaction_pkey;

-- 2. Nouvelle PK simple sur id (déjà unique via le générateur)
ALTER TABLE inventory_transaction ADD PRIMARY KEY (id);

-- 3. Conserver transaction_date comme colonne ordinaire (non-PK)
--    (déjà le cas physiquement, seulement la contrainte PK change)

-- 4. Nouvel index sur transaction_date pour les requêtes temporelles
CREATE INDEX IF NOT EXISTS idx_inv_tx_date
    ON inventory_transaction (transaction_date);
```

**Entité Java — `InventoryTransaction.java` :**

```java
// AVANT
@IdClass(ProductMvtId.class)
public class InventoryTransaction implements Persistable<ProductMvtId> {
    @Id private Long id;
    @Id @Column(name = "transaction_date") private LocalDate transactionDate;

// APRÈS
public class InventoryTransaction implements Serializable {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate = LocalDate.now();

    // Supprimer @IdClass, ProductMvtId, Persistable<ProductMvtId>
    // La gestion isNew via @PrePersist/@PostLoad n'est plus nécessaire
```

> **Attention** : vérifier `InventoryTransactionRepository` — tous les `findById()` retournent
> actuellement `Optional<InventoryTransaction>` sur `ProductMvtId`. À migrer vers `Long`.

---

### STEP 8 — Corriger la contrainte unique (collision même jour)

**Fichier Flyway :** `V1.2.10__inventory_transaction_unique_constraint.sql`

```sql
-- Supprimer l'ancienne contrainte basée sur LocalDate
ALTER TABLE inventory_transaction
    DROP CONSTRAINT IF EXISTS inventory_transaction_entity_id_produit_id_mouvement_type_tran_key;

-- Nouvelle contrainte basée sur created_at (LocalDateTime) + storage_id
ALTER TABLE inventory_transaction
    ADD CONSTRAINT uq_inv_tx_entity_produit_type_storage_ts
    UNIQUE (entity_id, produit_id, mouvement_type, storage_id, created_at);
```

---

### STEP 9 — Activer le checkpoint automatique (batch)

**Service Java — `StockSnapshotService.java` (nouveau) :**

```java
@Service
@Transactional
public class StockSnapshotService {

    /**
     * Crée un checkpoint de stock pour tous les produits d'un magasin.
     * À appeler : au démarrage, chaque nuit à minuit, ou après un inventaire clôturé.
     */
    public void createDailySnapshot(Integer magasinId) {
        em.createNativeQuery("""
            INSERT INTO stock_produit_snapshot
                (produit_id, storage_id, snapshot_date, qty_stock, source_type)
            SELECT sp.produit_id, sp.storage_id, NOW(), sp.qty_stock, 'BATCH_QUOTIDIEN'
            FROM stock_produit sp
            JOIN storage s ON s.id = sp.storage_id
            WHERE s.magasin_id = :magasinId
            ON CONFLICT DO NOTHING
        """).setParameter("magasinId", magasinId).executeUpdate();
    }
}
```

**Scheduler :**

```java
@Scheduled(cron = "0 0 0 * * *")   // chaque nuit à minuit
public void nightlySnapshot() {
    magasinRepository.findAll().forEach(m -> stockSnapshotService.createDailySnapshot(m.getId()));
}
```

---

## Tableau de bord de migration

| Step | Fichier Flyway | Java à modifier | Priorité | Risque |
|---|---|---|---|---|
| 1 — `storage_id` sur `inventory_transaction` | `V1.2.4` | `InventoryTransaction`, `InventoryTransactionBuilder` toutes branches | 🔴 Critique | Faible — colonne nullable puis NOT NULL |
| 2 — `quantity` delta signé | `V1.2.5` + proc update | `InventoryTransactionBuilder` cas INVENTAIRE | 🔴 Critique | Moyen — audit des rapports existants |
| 3 — Unifier repartition | `V1.2.6` | `RepartitionStockService`, `SuggestionReassortServiceImpl` | 🟠 Élevée | Faible — backfill + nouveau code forward |
| 4 — Builder storage propagation | — | `InventoryTransactionBuilder` toutes branches | 🟠 Élevée | Moyen — supprimer `determineBeforeAfterStock()` |
| 5 — Table snapshots | `V1.2.7` + proc update | `InventoryCloseServiceImpl`, nouveau `StockSnapshotService` | 🟡 Normale | Faible |
| 6 — `fn_stock_at_time` | `V1.2.8` | — (SQL uniquement) | 🟡 Normale | Nul |
| 7 — PK simple | `V1.2.9` | `InventoryTransaction`, `InventoryTransactionRepository`, tous appels `findById` | 🟡 Normale | Élevé — refactoring repository |
| 8 — Contrainte unique | `V1.2.10` | — | 🟢 Basse | Faible |
| 9 — Snapshot batch | — | `StockSnapshotService` + `@Scheduled` | 🟢 Basse | Nul |

---

## Invariants de validation à implémenter

Après migration, ces requêtes doivent retourner 0 ligne :

```sql
-- 1. Lignes inventory_transaction sans storage_id (ne doit pas exister)
SELECT COUNT(*) FROM inventory_transaction WHERE storage_id IS NULL;

-- 2. Invariant quantity : quantity_after = quantity_befor + quantity (sauf INVENTAIRE)
SELECT COUNT(*) FROM inventory_transaction
WHERE mouvement_type <> 'INVENTAIRE'
  AND quantity_after <> quantity_befor + quantity;

-- 3. Transferts inter-storage sans double écriture (OUT sans IN correspondant)
SELECT r.id
FROM repartition_stock_produit r
WHERE NOT EXISTS (
    SELECT 1 FROM inventory_transaction it
    WHERE it.entity_id = r.id AND it.mouvement_type = 'MOUVEMENT_STOCK_IN'
) AND r.stock_produit_source_id IS NOT NULL;

-- 4. Écart entre stock_produit et journal reconstitué (tolérance = 0)
SELECT produit_id, storage_id, ecart
FROM v_stock_reconstitue
WHERE ecart <> 0;
```

---

## Impact sur les features existantes

| Feature | Impact | Action |
|---|---|---|
| `InventaireQueryServiceImpl.buildStockMap*()` | Stock courant depuis `stock_produit` — inchangé | Rien |
| `InventoryTransactionServiceIml.fetchProduitDailyTransaction()` | Requête par `magasin_id` uniquement | Ajouter filtre optionnel `storage_id` |
| `fetchMouvementProduit` JSON DB function | Agrège par magasin | Créer version `_by_storage` |
| `v_stock_reconstitue` | Vue de contrôle | Nouveau |
| Rapports PDF inventaire | Utilisent `store_inventory_line` — inchangés | Rien |
| Dashboard stock | Lit `stock_produit` courant — inchangé | Rien |
| Réassort / suggestion | Créé `RepartitionStockProduit` sans journal | Ajouter `inventoryTransactionService.save()` (STEP 3) |
