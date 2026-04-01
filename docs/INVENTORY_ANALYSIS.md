# Analyse du module Inventaire — Points faibles & Pistes d'amélioration

> Fichiers analysés :
> - `InventaireServiceImpl.java` (827 lignes)
> - `StoreInventory.java`, `StoreInventoryLine.java`
> - `StoreInventoryLineFilterBuilder.java`, `StoreInventoryRecord`, DTOs associés

---

## 1. Points faibles identifiés

### 1.1 Multi-stock / Lots — Problème central

#### `getStock()` — NullPointerException & stock partiel

```java
// InventaireServiceImpl.java : 780-786
private int getStock(StoreInventory storeInventory, Integer produitId) {
    StockProduit stockProduit =
        this.stockProduitRepository.findOneByProduitIdAndStockageId(produitId,
            storeInventory.getStorage().getId());
    return Objects.nonNull(stockProduit.getQtyUG()) ? stockProduit.getQtyUG()
        + stockProduit.getQtyStock() : stockProduit.getQtyStock();
}
```

**Problèmes :**
- Si `findOneByProduitIdAndStockageId()` retourne `null` → **NPE immédiate** (non protégé).
- Ne retourne que le stock d'**un seul storage** alors qu'un produit peut avoir du stock dans plusieurs emplacements (Storage 1, Storage 2, réserve…).
- Ne tient pas compte des **lots** (`InventoryLot`) : deux lots du même produit dans le même storage ont des stocks distincts, mais sont agrégés à tort.
- Incohérence avec `getItemsByRayonId()` qui fait correctement un `.sum()` sur tous les `StockProduits`.

#### `getItemsByRayonId()` — Agrégation multi-storage incorrecte

```java
// ligne 213-214
int stockProduit = produit.getStockProduits().stream()
    .mapToInt(StockProduit::getQtyStock).sum();
```

**Problème :** Somme les stocks de **tous** les storages confondus, alors que l'inventaire est scopé à un storage/rayon précis. Le `quantityInit` affiché est faux dès qu'un produit est présent dans plusieurs storages.

---

### 1.2 `create()` — SQL natif non paramétré (risque injection SQL)

```java
// ligne 634-649
case MAGASIN -> String.format(StoreInventoryLineFilterBuilder.SQL_ALL_INSERT_ALL,
    storeInventory.getId()).replace("{famille_close}", "");
case RAYON -> String.format(StoreInventoryLineFilterBuilder.SQL_ALL_INSERT,
    storeInventory.getId()) +
    String.format(" AND r.id=%d ", storeInventory.getRayon().getId());
```

**Problèmes :**
- Les IDs sont concaténés directement dans la chaîne SQL → pas de **PreparedStatement**, pas de plan d'exécution réutilisable.
- Bien que les IDs soient des entiers, ce pattern est une mauvaise pratique d'hygiène SQL.
- La substitution `{famille_close}` via `.replace()` est fragile (chaîne littérale dans le template SQL).

---

### 1.3 `synchronizeStoreInventoryLine()` — Problème N+1 requêtes

```java
// ligne 236-242
storeInventoryLines.forEach(storeInventoryLineDTO -> {
    StoreInventoryLine storeInventoryLine = this.storeInventoryLineRepository
        .getReferenceById(storeInventoryLineDTO.getId());
    updateStoreInventoryLine(storeInventoryLineDTO, storeInventoryLine);
    this.storeInventoryLineRepository.saveAndFlush(storeInventoryLine);  // ← flush à chaque itération
});
```

**Problèmes :**
- **N `saveAndFlush()`** pour N lignes → N aller-retours base de données.
- Devrait utiliser `saveAll()` + un seul `flush()` en fin de boucle.
- `getReferenceById()` dans une boucle produit aussi N requêtes de chargement proxy.

---

### 1.4 `close()` — Race condition & types insuffisants

```java
// ligne 134-136
long count = storeInventoryLineRepository
    .countStoreInventoryLineByUpdatedIsFalseAndStoreInventoryId(id);
if (count > 0) { throw new InventoryException(); }
// ← fenêtre de vulnérabilité entre le count et le changement de statut
storeInventory.setStatut(InventoryStatut.CLOSED);
```

**Problèmes :**
- Pas de verrou pessimiste (`SELECT FOR UPDATE`) ni optimiste (`@Version`) sur `StoreInventory` → deux appels concurrents peuvent passer la vérification simultanément.
- `gapCost` et `gapAmount` sont stockés en `int` dans `StoreInventory` alors que les totaux `inventoryValueCostBegin/After` sont en `long` → **overflow possible** sur un grand inventaire.

---

### 1.5 `buildStoreInventoryLineFromProduit()` — `quantityInit` à 0 (comportement voulu)

```java
// ligne 396-405
storeInventoryLine.setQuantityInit(0);   // ← initialisé à 0 intentionnellement
storeInventoryLine.setQuantityOnHand(0);
storeInventoryLine.setGap(0);
```

**Comportement attendu :** Le stock théorique (`quantityInit`) est capturé à la **clôture** de l'inventaire, pas à la création. Les mouvements de stock (ventes, réceptions) continuent pendant l'inventaire ; figer le stock à la création donnerait un `quantityInit` obsolète. La valeur 0 est donc un placeholder correct en phase de saisie.

**Point de vigilance :** Vérifier que la procédure de clôture (`close()`) met bien à jour `quantityInit` avec le stock réel **au moment exact de la clôture** pour chaque ligne, avant de calculer le `gap` final.

---

### 1.6 `fetchSummary()` — Paramètre positionnel SQL natif

```java
// ligne 251-255
this.em.createNativeQuery(StoreInventoryLineFilterBuilder.SUMMARY_SQL, Tuple.class)
    .setParameter(1, id)   // ← paramètre positionnel, fragile
    .getSingleResult()
```

Si la requête `SUMMARY_SQL` est modifiée (ajout d'un paramètre), le positionnement des paramètres change silencieusement. Préférer les paramètres nommés (`:inventoryId`).

---

### 1.7 Duplication — `getAllByInventory()` vs `getInventoryItems()`

Les deux méthodes sont quasi-identiques (mêmes appels `getTotalCount()`, `buildItems()`). La seule différence est la vérification `CLOSED` dans `getAllByInventory()`. Violation du principe **DRY**.

---

### 1.8 `importDetail()` — Fragilité du parsing CSV

```java
// ligne 180-183
for (CSVRecord record : parser) {
    String code = record.get(0);
    codeCipQuantity.put(code, Integer.parseInt(record.get(1)));  // ← pas de try/catch par ligne
}
```

**Problèmes :**
- `Integer.parseInt()` sans protection → un fichier mal formé fait planter tout l'import silencieusement (l'exception est capturée à l'extérieur par `catch (IOException e)`).
- Pas de validation du nombre de colonnes par ligne (`record.size() >= 2`).
- Pas de feedback sur les lignes ignorées/en erreur.
- La `IOException` est loguée avec `log.debug("{0}", e)` — mauvais format de log SLF4J (doit être `log.debug("{}", e)`).

---

### 1.9 Méthodes mortes en production

```java
// ligne 800-825
// @EventListener(ApplicationReadyEvent.class)
private void updateAll() { ... }

// @EventListener(ApplicationReadyEvent.class)
public void clean() { ... }
```

Ces méthodes sont commentées mais toujours présentes. `updateAll()` boucle sur `storeInventoryId = -1` (ID impossible), `clean()` supprime des inventaires fermés depuis 4 mois. À externaliser ou supprimer.

---

### 1.10 `getStock()` appelé dans la boucle `buildItems()`

```java
// ligne 752-756 → appel de buildStoreInventoryLineRecordRecord → ligne 764
int currentStock = getStock(storeInventory, tuple.get("produitId", Integer.class));
// → stockProduitRepository.findOneByProduitIdAndStockageId() à chaque ligne
```

Pour une page de 20 articles, cela génère **20 requêtes SQL supplémentaires**. Le stock devrait être récupéré en masse avant la boucle (`findAllByStorageIdAndProduitIdIn()`).

---

## 2. Pistes d'amélioration

### 2.1 Gestion correcte du multi-stock et des lots

**Situation actuelle :** Un produit → un `StockProduit` par storage → stock unique.
**Réalité pharmacie :** Un produit peut avoir plusieurs lots (dates de péremption différentes), plusieurs emplacements de stockage, et des stocks de quarantaine.

**Proposition :**

```
StoreInventoryLine
├── produit_id
├── storage_id          ← NOUVEAU : scoper la ligne au storage exact
├── lot_id              ← NOUVEAU : une ligne par lot si mode détaillé
├── lot_numero          ← NOUVEAU : numéro de lot
├── lot_expiration_date ← NOUVEAU : date de péremption
├── quantity_init       ← stock réel par (produit, storage, lot)
├── quantity_on_hand    ← saisi par l'agent
└── gap
```

**`getStock()` corrigé :**

```java
private int getStock(StoreInventory storeInventory, Integer produitId) {
    return this.stockProduitRepository
        .findAllByProduitIdAndStorageId(produitId, storeInventory.getStorage().getId())
        .stream()
        .mapToInt(sp -> sp.getQtyStock() + Optional.ofNullable(sp.getQtyUG()).orElse(0))
        .sum();
}
```

**Pré-chargement en masse avant la boucle :**

```java
// Avant buildItems() :
Set<Integer> produitIds = tuples.stream()
    .map(t -> t.get("produitId", Integer.class))
    .collect(Collectors.toSet());
Map<Integer, Integer> stockMap = stockProduitRepository
    .findAllByStorageIdAndProduitIdIn(storageId, produitIds)
    .stream()
    .collect(Collectors.groupingBy(
        sp -> sp.getProduit().getId(),
        Collectors.summingInt(sp -> sp.getQtyStock() + Optional.ofNullable(sp.getQtyUG()).orElse(0))
    ));
```

---

### 2.2 Paramétrer les requêtes SQL natives d'insertion

**Actuel :** `String.format(SQL, id) + " AND r.id=" + rayonId`

**Proposition :** Utiliser des paramètres nommés et une seule requête avec conditions optionnelles :

```sql
INSERT INTO store_inventory_line (store_inventory_id, produit_id, quantity_init, ...)
SELECT :inventoryId, sp.produit_id, sp.qty_stock, ...
FROM stock_produit sp
JOIN produit p ON p.id = sp.produit_id
WHERE sp.storage_id = :storageId
  AND (:rayonId IS NULL OR EXISTS (
      SELECT 1 FROM rayon_produit rp WHERE rp.produit_id = p.id AND rp.rayon_id = :rayonId
  ))
  AND (:familleId IS NULL OR p.famille_id = :familleId)
  AND p.status = 'ENABLE'
```

---

### 2.3 Vérifier la mise à jour de `quantityInit` à la clôture

`quantityInit = 0` en phase de saisie est intentionnel (le stock évolue pendant l'inventaire). Il doit être mis à jour au moment exact de la clôture.

S'assurer que `close()` capture le stock instantané avant de calculer le `gap` :

```java
// Dans close() — après vérification des lignes non saisies
storeInventoryLines.forEach(line -> {
    int stockAtClose = getStock(storeInventory, line.getProduit().getId());
    line.setQuantityInit(stockAtClose);
    line.setGap(line.getQuantityOnHand() - stockAtClose);
});
storeInventoryLineRepository.saveAll(storeInventoryLines);
```

Si ce calcul est déjà présent dans la procédure SQL de clôture, s'assurer qu'il utilise le bon `storage_id` (voir point 1.1 — agrégation multi-storage).

---

### 2.4 Éliminer le N+1 dans `synchronizeStoreInventoryLine()`

```java
@Override
public void synchronizeStoreInventoryLine(List<StoreInventoryLineDTO> dtos) {
    if (CollectionUtils.isEmpty(dtos)) return;

    Set<Long> ids = dtos.stream().map(StoreInventoryLineDTO::getId).collect(Collectors.toSet());
    Map<Long, StoreInventoryLine> lines = storeInventoryLineRepository.findAllById(ids)
        .stream()
        .collect(Collectors.toMap(StoreInventoryLine::getId, l -> l));

    dtos.forEach(dto -> {
        StoreInventoryLine line = lines.get(dto.getId());
        if (line != null) updateStoreInventoryLine(dto, line);
    });

    storeInventoryLineRepository.saveAll(lines.values()); // 1 seule requête batch
}
```

---

### 2.5 Verrou optimiste sur `StoreInventory`

```java
// StoreInventory.java
@Version
@Column(name = "version")
private Long version;
```

Garantit qu'une fermeture concurrente lève une `OptimisticLockException` plutôt que de corrompre les données.

---

### 2.6 Fusionner `getAllByInventory()` et `getInventoryItems()`

```java
@Override
@Transactional(readOnly = true)
public Page<StoreInventoryLineRecord> getInventoryItems(
    StoreInventoryLineFilterRecord filter, Pageable pageable) {
    return getInventoryPage(filter, pageable, false);
}

@Override
@Transactional(readOnly = true)
public Page<StoreInventoryLineRecord> getAllByInventory(
    StoreInventoryLineFilterRecord filter, Pageable pageable) {
    return getInventoryPage(filter, pageable, true);
}

private Page<StoreInventoryLineRecord> getInventoryPage(
    StoreInventoryLineFilterRecord filter, Pageable pageable, boolean excludeClosed) {
    StoreInventory inventory = storeInventoryRepository.getReferenceById(filter.storeInventoryId());
    if (excludeClosed && inventory.getStatut() == InventoryStatut.CLOSED) {
        return Page.empty(pageable);
    }
    long count = getTotalCount(inventory, filter);
    if (count == 0) return Page.empty(pageable);
    return new PageImpl<>(buildItems(inventory, filter, pageable), pageable, count);
}
```

---

### 2.7 Robustifier l'import CSV

```java
for (CSVRecord record : parser) {
    if (record.size() < 2) {
        log.warn("Ligne CSV ignorée (format invalide) : {}", record);
        continue;
    }
    String code = record.get(0).trim();
    try {
        int qty = Integer.parseInt(record.get(1).trim());
        codeCipQuantity.put(code, qty);
    } catch (NumberFormatException e) {
        log.warn("Quantité invalide pour le code {} : {}", code, record.get(1));
    }
}
```

---

### 2.8 Paramètres nommés dans les requêtes natives

```java
// Actuel
.setParameter(1, id)

// Proposé
.setParameter("inventoryId", id)
// Et dans SUMMARY_SQL : WHERE sil.store_inventory_id = :inventoryId
```

---

### 2.9 Nettoyer les méthodes mortes

- Supprimer `updateAll()` (utilise l'ID `-1`, code de debug).
- Externaliser `clean()` dans un `@Scheduled` dédié avec configuration du délai en propriété applicative :

```java
@Scheduled(cron = "${app.inventory.cleanup-cron:0 0 2 * * SUN}")
public void scheduledCleanup() {
    int retentionMonths = cleanupRetentionMonths; // @Value("${app.inventory.retention-months:4}")
    // ...
}
```

---

### 2.10 Plan de migration DB pour le multi-lot

```sql
-- V1.0.X__inventory_lot_support.sql

ALTER TABLE store_inventory_line
    ADD COLUMN storage_id INTEGER REFERENCES storage(id),
    ADD COLUMN lot_id      BIGINT  REFERENCES inventory_lot(id),
    ADD COLUMN lot_numero  VARCHAR(50),
    ADD COLUMN lot_expiration_date DATE;

-- Index pour les recherches par lot
CREATE INDEX idx_sil_lot_id ON store_inventory_line(lot_id);
CREATE INDEX idx_sil_storage_id ON store_inventory_line(storage_id);

-- Contrainte unique : un produit/storage/lot ne peut apparaître qu'une fois par inventaire
ALTER TABLE store_inventory_line
    ADD CONSTRAINT uq_sil_produit_storage_lot
    UNIQUE (store_inventory_id, produit_id, storage_id, lot_id);
```

---

## 3. Résumé des priorités

| Priorité | Problème | Impact |
|----------|----------|--------|
| 🔴 Critique | `getStock()` — NPE si stockProduit null | Crash en production |
| 🔴 Critique | Multi-stock non agrégé correctement | Données d'inventaire fausses |
| 🟡 Moyen | `quantityInit` à la clôture — vérifier cohérence multi-storage | Gap final potentiellement erroné |
| 🟠 Élevé | N+1 dans `synchronizeStoreInventoryLine()` | Performance dégradée |
| 🟠 Élevé | N+1 dans `buildItems()` → `getStock()` par ligne | Performance dégradée |
| 🟠 Élevé | Pas de verrou optimiste sur fermeture | Corruption données concurrentes |
| 🟡 Moyen | SQL natif non paramétré dans `buildInsertQuery()` | Mauvaise pratique, maintenance |
| 🟡 Moyen | Import CSV fragile | Perte de données silencieuse |
| 🟡 Moyen | Duplication `getAllByInventory`/`getInventoryItems` | Maintenabilité |
| 🟢 Faible | Méthodes mortes `updateAll()`/`clean()` | Pollution du code |
| 🟢 Faible | Paramètres positionnels SQL natif | Fragilité maintenance |
| 🔵 Évolution | Support natif multi-lot dans `StoreInventoryLine` | Fonctionnel manquant |

---

## 4. Analyse UI Frontend — Points faibles

> Fichiers analysés :
> - `store-inventory-update.component.ts/html` (461 lignes)
> - `inventory-form.component.ts/html`
> - `en-cours.component.ts/html`
> - `store-inventory.component.html`

---

### 4.1 `store-inventory-update` — Rechargement inutile à chaque saisie

```typescript
// store-inventory-update.component.ts
onCellValueChanged(event: CellValueChangedEvent): void {
  this.storeInventoryService.updateLine(dto).subscribe({
    next: () => {
      this.goToNextPage();   // ← appelle loadPage() même si on reste sur la même page
    }
  });
}

goToNextPage(): void {
  if (this.page * this.itemsPerPage >= this.totalItems) {
    this.page = 1;
  } else {
    this.page++;             // ← page toujours incrémentée après chaque saisie
  }
  this.loadPage();           // ← reload systématique de la grille
}
```

**Problème :** Chaque validation d'une cellule force un rechargement de page. Un inventaire de 500 articles = 500 requêtes HTTP + rechargements de grille. La progression dans l'inventaire est counter-intuitive (page suivante automatique).

---

### 4.2 Bug `isCharNumeric()` — Validation quantité erronée

```typescript
// store-inventory-update.component.ts
private isCharNumeric(charStr: string): boolean {
  return !!/\d/.test(charStr);   // ← retourne true si AU MOINS un chiffre est présent
}
```

**Problème :** `isCharNumeric("abc123")` retourne `true`. La validation accepte des chaînes comme `"1a2b"` comme valeurs numériques valides. La cellule Quantité peut recevoir des données corrompues.

---

### 4.3 Absence totale d'indicateur de progression

La grille affiche les lignes de l'inventaire mais aucun élément ne montre :
- Combien de lignes ont été saisies / total (ex: `142 / 500`)
- % de complétion par zone/rayon
- Lignes non encore touchées vs validées

Un pharmacien ne sait pas où il en est sans compter manuellement.

---

### 4.4 `en-cours.component` — Tableau incomplet

Le tableau « En cours » affiche : Emplacement, Date, Opérateur.

**Champs manquants :**
- Type d'inventaire (MAGASIN / RAYON / FAMILLE)
- Description / rayon sélectionné
- Progression (X lignes saisies / Y total)
- Nombre de produits total
- Écart total prévisionnel

Sans ces informations, la liste des inventaires en cours est illisible pour le responsable.

---

### 4.5 `inventory-form` — Fermeture sans contrôle

```typescript
// en-cours.component.ts
close(id: number): void {
  this.storeInventoryService.close(id).subscribe({ ... });
}
```

La fermeture est déclenchée directement depuis la liste sans afficher :
- Le nombre de lignes non encore saisies
- Un récapitulatif des écarts
- Une demande de confirmation avec données

Le service backend lève bien une `InventoryException` si des lignes sont non-updatées, mais l'UI affiche simplement une erreur sans contexte (pas de liste des lignes en attente).

---

### 4.6 Pas de mode « saisie aveugle » (blind count)

**Principe pharmacie :** L'agent ne voit pas le stock théorique pendant la saisie pour éviter le biais de confirmation. Il saisit la quantité comptée, et seulement après validation l'écart est révélé.

Actuellement la colonne « Stock » est conditionnellement masquée (`hasRole`) mais le `quantityInit` n'est jamais vraiment caché côté UX — il est envoyé dans la réponse API pour tous les utilisateurs.

---

### 4.7 Pas de gestion visuelle des lots / dates de péremption

La grille AG Grid affiche : CIP, Libellé, Quantité, Stock, Écart.

**Absent :**
- N° de lot
- Date de péremption
- Alerte visuelle produit périmé ou proche de péremption
- Distinction visuelle entre plusieurs lots du même produit

Dans une officine, l'inventaire physique doit inclure le contrôle des dates de péremption. C'est une obligation réglementaire (DGSIMS).

---

### 4.8 Pas d'import CSV depuis l'UI

Le backend possède `importDetail()` qui lit un fichier CSV (code CIP, quantité). Mais aucun bouton « Importer CSV » n'existe dans l'interface. Les agents ne peuvent pas utiliser cette fonctionnalité sans intervention technique.

---

### 4.9 Colonne Stock masquée mais données exposées en API

```typescript
// Colonne masquée si pas le rôle :
hide: !this.hasUpdateStockRole
```

La valeur `quantityInit` est toujours envoyée dans la réponse JSON, même si la colonne est masquée. Un utilisateur avec DevTools peut lire le stock théorique. La logique de confidentialité doit être gérée côté backend (projection DTO conditionnelle).

---

### 4.10 Hauteur fixe de la grille AG Grid

```html
<ag-grid-angular style="height: 70vh; width: 100%;" ...>
```

Sur un écran 1080p standard coupé par le header de l'application, cela affiche ~15 lignes simultanément. Sur un laptop 768p, moins de 10. Pas de configuration, pas d'adaptation.

---

## 5. Propositions fonctionnelles — Inventaire en officine

### 5.1 Flux de travail en 4 phases (standard officine)

```
Phase 1 — PRÉPARATION
  ├── Création de l'inventaire (type, périmètre, date)
  ├── Génération automatique des lignes + quantityInit
  ├── Impression optionnelle des feuilles de comptage (PDF sans stock théorique)
  └── Assignation d'agents par zone/rayon

Phase 2 — COMPTAGE (saisie aveugle)
  ├── Agent saisit les quantités comptées
  ├── Pas de stock théorique visible
  ├── Mode scan code-barres (CIP/EAN)
  └── Sauvegarde automatique par cellule validée

Phase 3 — CONTRÔLE
  ├── Révélation des écarts (gap)
  ├── Re-comptage des articles en écart significatif (> seuil configurable)
  ├── Validation des écarts acceptés par le responsable
  └── Vue récapitulative : valeur perdue, valeur gagnée, taux de conformité

Phase 4 — CLÔTURE
  ├── Validation finale avec récapitulatif complet
  ├── Ajustement automatique des stocks
  ├── Génération du rapport de clôture (PDF signé)
  └── Archivage avec horodatage réglementaire
```

---

### 5.2 Dashboard inventaire en cours

Remplacer le tableau `en-cours` minimaliste par un dashboard :

```
┌────────────────────────────────────────────────────────────────────────┐
│  INVENTAIRES EN COURS                                          [Nouveau]│
├──────────────┬──────────┬──────────────┬──────────────┬────────────────┤
│  Périmètre   │  Phase   │  Progression │  Agents      │  Actions       │
├──────────────┼──────────┼──────────────┼──────────────┼────────────────┤
│  Magasin     │ COMPTAGE │  ██████░░ 78%│  A. Dupont   │ [Ouvrir] [▶]  │
│  (352 art.)  │          │  275/352     │  B. Martin   │               │
├──────────────┼──────────┼──────────────┼──────────────┼────────────────┤
│  Rayon Cardio│ CONTRÔLE │  ████████ 100│  C. Lefebvre │ [Clôturer]    │
│  (48 art.)   │          │  48/48 ✓     │              │               │
└──────────────┴──────────┴──────────────┴──────────────┴────────────────┘
```

**Données affichées :**
- Barre de progression visuelle (% lignes saisies)
- Phase actuelle (icône + couleur)
- Agents assignés
- Nombre d'articles / écarts détectés
- Boutons contextuels selon la phase

---

### 5.3 Interface de saisie repensée

**Disposition proposée :**

```
┌─────────────────────────────────────────────────────────────────────┐
│  Inventaire Magasin — Rayon Cardiologie           Progression: 42/120│
│  [Phase: COMPTAGE]  [Aveugle ●]  [Scanner ○]                        │
├─────────────────────────────────────────────────────────────────────┤
│  Filtre: [Tous ▾]  [Non saisi ▾]  [Avec écart ▾]  🔍 Recherche...   │
├──────┬────────────────────────────┬──────────┬────────┬─────────────┤
│ Statut│ Produit                   │ Lot/Pérem │ Saisi  │ Écart       │
├──────┼────────────────────────────┼──────────┼────────┼─────────────┤
│  ✓   │ Doliprane 500mg            │ L24A-0312│  120   │   +2        │
│  ✓   │ Amlodipine 5mg             │ L23B-0612│   48   │    0        │
│  ⏳  │ Bisoprolol 10mg            │ —        │   —    │   —         │
│  🔴  │ Atorvastatine 20mg [PÉRIMÉ]│ L22C-0125│   12   │   -8        │
├──────┴────────────────────────────┴──────────┴────────┴─────────────┤
│  ← Précédent              Page 2/6              Suivant →            │
│  [Sauvegarder tout]  [Importer CSV]  [Imprimer feuilles]             │
└─────────────────────────────────────────────────────────────────────┘
```

**Éléments clés :**
- Colonne **Statut** : ✓ saisi, ⏳ non saisi, ⚠ écart, 🔴 périmé
- **Mode aveugle** toggle : masque la colonne écart pendant la saisie
- **Mode scanner** : focus automatique sur champ CIP, saisie par scan code-barres
- Navigation manuelle (pas de saut automatique à la page suivante)
- **Filtre rapide** : Tous / Non saisi / Avec écart / Périmés
- Bouton **Sauvegarder tout** (sauvegarde batch toutes les lignes modifiées non encore sauvegardées)

---

### 5.4 Vue de contrôle des écarts (Phase 3)

```
┌─────────────────────────────────────────────────────────────────────┐
│  CONTRÔLE DES ÉCARTS — Inventaire Magasin                           │
├────────────────────────────────────────────────────────────────────-┤
│  Résumé :  Produits en écart: 23/352   Valeur manquante: -1 842 €  │
│            Surstock: +5 produits        Valeur excédentaire: +210 € │
├──────────────────────────────────────────────────────────────────────┤
│  [Seuil re-comptage: > 5 unités ▾]  [Masquer écarts mineurs ○]      │
├─────────┬──────────────────────┬────────┬────────┬──────┬───────────┤
│ Priorité│ Produit              │ Stock  │ Saisi  │ Écart│ Action    │
├─────────┼──────────────────────┼────────┼────────┼──────┼───────────┤
│ 🔴 -18  │ Tramadol 100mg       │   142  │   124  │  -18 │ [Recompter│
│ 🟠 -12  │ Metformine 1000mg    │    80  │    68  │  -12 │ [Recompter│
│ 🟢  +2  │ Doliprane 500mg      │   118  │   120  │   +2 │ [Valider] │
│ 🟢   0  │ Amlodipine 5mg       │    48  │    48  │    0 │ [Valider] │
└─────────┴──────────────────────┴────────┴────────┴──────┴───────────┘
│           [Valider tous les écarts ≤ 2]    [Rapport écarts PDF]      │
└──────────────────────────────────────────────────────────────────────┘
```

---

### 5.5 Saisie par scan code-barres (mode scanner)

**Flux de saisie :**
1. Agent scanne un code CIP13 ou EAN13
2. La ligne correspondante est mise en surbrillance dans la grille
3. Focus automatique sur la cellule Quantité
4. Agent saisit la quantité sur pavé numérique / pistol scanner
5. Validation → sauvegarde auto → retour au champ de scan

**Implémentation Angular :**

```typescript
@ViewChild('scanInput') scanInput: ElementRef;

onBarcodeScanned(barcode: string): void {
  const rowIndex = this.gridApi.getRowIndex(barcode);  // cherche par CIP
  if (rowIndex !== -1) {
    this.gridApi.ensureIndexVisible(rowIndex, 'middle');
    this.gridApi.startEditingCell({ rowIndex, colKey: 'quantityOnHand' });
  } else {
    this.messageService.add({ severity: 'warn', summary: `Produit ${barcode} non trouvé` });
  }
}
```

---

### 5.6 Import CSV avec feedback

```
┌─────────────────────────────────────────────────────┐
│  Import CSV — Résultat                              │
├─────────────────────────────────────────────────────┤
│  ✅  312 lignes importées avec succès               │
│  ⚠️   8 lignes ignorées (code CIP inconnu)          │
│  ❌   2 lignes rejetées (quantité non numérique)    │
├─────────────────────────────────────────────────────┤
│  Lignes rejetées :                                  │
│  Ligne 45: CIP=3400935..., qté="abc" → REJETÉE      │
│  Ligne 89: CIP=inconnu → IGNORÉE                    │
│                                   [Fermer] [Détails]│
└─────────────────────────────────────────────────────┘
```

---

### 5.7 Rapport de clôture PDF

Le rapport généré à la clôture doit inclure (obligation réglementaire officine) :
- En-tête : Nom officine, FINESS, date d'inventaire, responsable
- Tableau : Produit, Lot, Péremption, Stock théorique, Quantité comptée, Écart, Valeur
- Pied de page : Totaux, taux de conformité, signature électronique
- Annexe : Liste des produits périmés détectés

---

## 6. Propositions côté service Java — Compléments UI

### 6.1 Endpoint de progression en temps réel

```java
// Nouveau endpoint REST
@GetMapping("/store-inventories/{id}/progress")
public InventoryProgressDTO getProgress(@PathVariable Long id) {
    long total = storeInventoryLineRepository.countByStoreInventoryId(id);
    long updated = storeInventoryLineRepository
        .countByStoreInventoryIdAndUpdatedIsTrue(id);
    long withGap = storeInventoryLineRepository
        .countByStoreInventoryIdAndGapNot(id, 0);
    return new InventoryProgressDTO(id, total, updated, withGap,
        total > 0 ? (updated * 100 / total) : 0);
}
```

Consommé par le dashboard pour afficher la barre de progression sans recharger toute la grille.

---

### 6.2 Sauvegarde batch (remplacement du save ligne par ligne)

```java
// Endpoint batch existant à enrichir
@PutMapping("/store-inventory-lines/batch")
public ResponseEntity<BatchSaveResultDTO> batchUpdateLines(
    @RequestBody List<StoreInventoryLineDTO> dtos) {

    BatchSaveResultDTO result = inventaireService.synchronizeStoreInventoryLine(dtos);
    // Retourner le nombre de lignes sauvées, erreurs éventuelles
    return ResponseEntity.ok(result);
}

public record BatchSaveResultDTO(int saved, int failed, List<Long> failedIds) {}
```

L'UI envoie toutes les lignes modifiées en une seule requête au lieu d'une requête par cellule.

---

### 6.3 Projection DTO conditionnelle pour le mode aveugle

```java
// StoreInventoryLineRecord — champ conditionnel
public record StoreInventoryLineRecord(
    Long id,
    String codeCip,
    String produitLibelle,
    Integer quantityOnHand,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Integer quantityInit,    // null si l'appelant n'a pas le rôle VIEW_STOCK
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Integer gap              // null en mode aveugle actif
) {}
```

```java
// Dans le service :
private StoreInventoryLineRecord toRecord(StoreInventoryLine line, boolean blindMode) {
    Integer init = blindMode ? null : line.getQuantityInit();
    Integer gap  = blindMode ? null : line.getGap();
    return new StoreInventoryLineRecord(line.getId(), ..., init, gap);
}
```

---

### 6.4 Endpoint de re-comptage ciblé

```java
// Retourner uniquement les lignes en écart au-delà d'un seuil
@GetMapping("/store-inventories/{id}/lines/discrepancies")
public Page<StoreInventoryLineRecord> getDiscrepancies(
    @PathVariable Long id,
    @RequestParam(defaultValue = "1") int minAbsGap,
    Pageable pageable) {
    return inventaireService.getLinesWithGapAbove(id, minAbsGap, pageable);
}
```

Permet à la vue "Contrôle" de n'afficher que les produits avec écart, sans recharger l'ensemble.

---

### 6.5 Événements de domaine pour l'audit

```java
// À la clôture d'un inventaire
@DomainEvents
public List<Object> domainEvents() {
    return List.of(new InventoryClosedEvent(this.id, this.closedAt, this.closedBy));
}

// Listener pour email/notification
@EventListener
public void onInventoryClosed(InventoryClosedEvent event) {
    notificationService.sendInventoryReport(event.inventoryId());
}
```

---

## 7. Résumé des priorités — Vue complète

| # | Priorité | Problème / Proposition | Impact |
|---|----------|------------------------|--------|
| 1 | 🔴 Critique | `getStock()` NPE si stockProduit null | Crash production |
| 2 | 🔴 Critique | Multi-stock non agrégé → données fausses | Inventaire inexploitable |
| 3 | 🟡 Moyen | `quantityInit` clôture — vérifier cohérence avec multi-storage | Gap final potentiellement erroné |
| 4 | 🔴 Critique | Bug `isCharNumeric()` → validation quantité | Données corrompues |
| 5 | 🟠 Élevé | N+1 dans `synchronizeStoreInventoryLine()` | Perf dégradée |
| 6 | 🟠 Élevé | N+1 dans `buildItems()` → `getStock()` par ligne | Perf dégradée |
| 7 | 🟠 Élevé | Reload page à chaque saisie de cellule | UX dégradée, trafic inutile |
| 8 | 🟠 Élevé | Pas de verrou optimiste sur fermeture | Corruption données |
| 9 | 🟠 Élevé | Pas d'indicateur de progression | Pilotage impossible |
| 10 | 🟡 Moyen | `en-cours` — champs manquants (type, %, description) | Lisibilité faible |
| 11 | 🟡 Moyen | Fermeture sans récapitulatif ni confirmation enrichie | Clôture accidentelle |
| 12 | 🟡 Moyen | SQL natif non paramétré dans `buildInsertQuery()` | Mauvaise pratique |
| 13 | 🟡 Moyen | Import CSV fragile + absent de l'UI | Fonctionnel caché |
| 14 | 🟡 Moyen | Stock masqué UI mais envoyé par l'API | Fuite de données |
| 15 | 🟡 Moyen | Duplication `getAllByInventory`/`getInventoryItems` | Maintenabilité |
| 16 | 🟢 Faible | Hauteur grille fixe (70vh) | Affichage réduit |
| 17 | 🟢 Faible | Méthodes mortes `updateAll()`/`clean()` | Pollution code |
| 18 | 🟢 Faible | Paramètres positionnels SQL natif | Fragilité maintenance |
| 19 | 🔵 Évolution | Mode saisie aveugle (blind count) | Qualité inventaire |
| 20 | 🔵 Évolution | Saisie par scan code-barres | Productivité agents |
| 21 | 🔵 Évolution | Vue contrôle des écarts avec seuil | Standard officine |
| 22 | 🔵 Évolution | Support natif multi-lot (lot_id, expiration_date) | Réglementaire |
| 23 | 🔵 Évolution | Rapport de clôture PDF réglementaire | Obligation officine |
| 24 | 🔵 Évolution | Endpoint de progression temps réel | Dashboard enrichi |
