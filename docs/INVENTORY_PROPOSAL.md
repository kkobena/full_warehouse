# Propositions d'amélioration — Module Inventaire

> **Complémentaire à** `INVENTORY_ANALYSIS.md`
> **Date :** 2026-03-13 — **Révision :** 2026-03-13 (v5)

---

## Sommaire

0. [Plan d'implémentation — Priorités](#0-plan-implementation)
1. [État actuel — ce qui fonctionne déjà](#1-etat-actuel)
2. [Décisions de conception](#2-decisions-de-conception)
3. [Changements proposés — Création](#3-changements-creation)
4. [Changements proposés — Clôture](#4-changements-cloture)
5. [Changements proposés — Interface de saisie](#5-changements-interface)
6. [Annexe — Récapitulatif par type](#annexe)
7. [Propositions d'améliorations — Éditeurs de référence](#7-propositions-ameliorations)

---

## 0. Plan d'implémentation — Priorités

> **Principe :** Les inventaires sans gestion de lot d'abord. La grille existante (`inventory-lines-grid`) est quasi OK.

### Phase 1 — Fondations : `storage_id` NOT NULL (prérequis)

| # | Tâche | Fichier(s) | État |
|---|---|---|---|
| 1.1 | Migration V1.2.2 : backfill `storage_id` NULL + `SET NOT NULL` | `V1.2.2__storage_id_not_null.sql` | À faire |
| 1.2 | Corriger les 11 INSERT dans `InventaireCreationServiceImpl` pour renseigner `storage_id` | `InventaireCreationServiceImpl.java` | À faire |

### Phase 2 — Clôture corrigée (sans lots)

| # | Tâche | Fichier(s) | État |
|---|---|---|---|
| 2.1 | `proc_close_inventory_v2` : stock inventorié + réserve → 0 (sauf STORAGE) + transactions bulk | `V1.2.2__storage_id_not_null.sql` | À faire |
| 2.2 | Appel `proc_close_inventory_v2` depuis Java + `InventoryClosedEvent` | `InventaireServiceImpl.java`, `StoreInventoryLineRepository.java` | À faire |
| 2.3 | `InventoryClosedEventListener` async + requête suggestions réserve optimisée | Nouveau : `InventoryClosedEventListener.java` | À faire |

### Phase 3 — Frontend grille enrichie (sans lots)

| # | Tâche | Fichier(s) | État |
|---|---|---|---|
| 3.1 | Enrichir `StoreInventoryLineRecord` : `storageId`, `quantitySold`, `seuilMini` | `StoreInventoryLineRecord.java`, `InventaireQueryServiceImpl.java` | À faire |
| 3.2 | `@Input() extraColumns` dans la grille existante | `inventory-lines-grid.component.ts` | À faire |

### Phase 4 — Gestion des lots (après validation phases 1-3)

| # | Tâche |
|---|---|
| 4.1 | Paramètres `GESTION_LOT_INVENTAIRE` + `MODE_SAISIE_LOT_INVENTAIRE` dans `AppConfiguration` |
| 4.2 | Création des `inventory_lot` post-INSERT (requête générique) |
| 4.3 | Step 3 conditionnel dans `proc_close_inventory_v2` (`UPDATE lot.current_quantity`) |
| 4.4 | `lot-flat-grid` (mode LOT_PLAT + PERIME/ALERTE) |
| 4.5 | `lot-edit-modal` (mode MODAL) |
| 4.6 | `expansion-lot-grid` (mode EXPANSION — PrimeNG Table) |
| 4.7 | `add-lot-modal` (ajout lot manquant, partagé) |
| 4.8 | Endpoint `POST /api/store-inventory-lines/{lineId}/lots` |

### Phase 5 — Améliorations

| # | Tâche |
|---|---|
| 5.1 | `InventoryCategory.ABC` (utilise `mv_abc_pareto_analysis`) |
| 5.2 | Badge A/B/C dans les grilles |
| 5.3 | Analyse des écarts / démarque inconnue |
| 5.4 | Valorisation ventilée + rapport PDF |

---

## 1. État actuel

### Services refactorisés (implémentés)

| Service | Rôle | État |
|---------|------|------|
| `InventaireCreationServiceImpl` | Création + INSERT paramétrés par `InventoryCategory` | OK — 10 types implémentés |
| `InventaireQueryServiceImpl` | Pagination N+1 corrigé via `InventoryStockService.buildStockMap()` | OK |
| `InventoryStockServiceImpl` | Chargement bulk du stock, NPE-safe | OK |
| `InventaireProgressServiceImpl` | Indicateurs de progression | OK |

### Migration V1.2.0 (appliquée)

- `store_inventory_line.storage_id` ✓
- `inventory_lot.store_inventory_line_id` (reparentage) ✓
- Contrainte unique `(produit_id, store_inventory_id, storage_id)` ✓

### `proc_close_inventory` (legacy — à remplacer)

Fait actuellement :
- Cursor sur toutes les lignes de l'inventaire
- `UPDATE stock_produit SET qty_stock = v_quantity_on_hand, qty_ug = 0, qty_virtual = v_quantity_on_hand WHERE produit_id = ? AND storage_id = ?`
- `INSERT INTO inventory_transaction (...)` pour traçabilité

**Ne fait PAS :**
- Distinguer rayon vs réserve (utilise le `storage_id` du `store_inventory` parent, pas celui de la ligne)
- Gérer les lots (`inventory_lot.quantity_on_hand` n'est pas propagé vers `lot.current_quantity`)

### Contraintes techniques frontend

- **AG Grid Community 35.1.0** uniquement (pas Enterprise)
- Master-detail / row expansion = **fonctionnalité Enterprise**, non disponible
- Alternative retenue : **modale ngbModal** pour la saisie lot dans les grilles standard

---

## 2. Décisions de conception

### 2.1 `storage_id` NOT NULL — résolu à la création, jamais à la clôture

**Problème :** `store_inventory_line.storage_id` (V1.2.0) et `store_inventory.storage_id` peuvent tous deux être NULL. Toute tentative de COALESCE/fallback à la clôture est fragile.

**Règle :** `store_inventory_line.storage_id` est **OBLIGATOIRE**. C'est la création qui le résout.

| InventoryCategory | Source du `storage_id` |
|---|---|
| STORAGE, RAYON | Le storage cible (paramètre de création) |
| MAGASIN, FAMILLY, VENDU, INVENDU, SOUS_SEUIL, EN_RUPTURE | `stock_produit.storage_id` via JOIN — une ligne par (produit, storage PRINCIPAL) |
| PERIME, ALERTE_PEREMPTION | `stock_produit.storage_id` via JOIN — **tous storages** (un lot périmé peut être en réserve) |

**Migration requise :** après backfill des `storage_id` NULL existants :
```sql
ALTER TABLE store_inventory_line ALTER COLUMN storage_id SET NOT NULL;
```

La procédure de clôture utilise directement `sil.storage_id` — pas de COALESCE, pas de sous-requête.

### 2.2 Gestion des lots paramétrable — `GESTION_LOT_INVENTAIRE`

**Problème :** Certaines officines ne gèrent pas les lots. La saisie par lot doit être optionnelle.

**Paramètre :** `AppConfiguration`

```
name:        GESTION_LOT_INVENTAIRE
value:       false
description: Activer la saisie par lot lors des inventaires
valueType:   BOOLEAN
```

**Le paramètre s'applique à TOUS les types d'inventaire**, pas uniquement PERIME/ALERTE. Un produit multi-lot dans un inventaire MAGASIN, STORAGE ou RAYON doit aussi pouvoir être saisi par lot si l'officine le souhaite.

**Impact sur toute la chaîne :**

| Couche | `GESTION_LOT = false` | `GESTION_LOT = true` |
|---|---|---|
| **Création** | `store_inventory_line` uniquement | `store_inventory_line` + `inventory_lot` pour chaque lot actif (`current_quantity > 0`) du produit |
| **Query** | `StoreInventoryLineRecord` (comme aujourd'hui) | Enrichi : lots rattachés à chaque ligne (ou grille plate pour PERIME/ALERTE) |
| **Saisie UI** | Grille produit classique, saisie au niveau ligne | Produits sans lot → saisie normale. Produits avec lots → **modale ngbModal** de saisie par lot |
| **Réconciliation** | — | `line.quantity_on_hand = SUM(inventory_lot.quantity_on_hand)` — la quantité ligne est **calculée**, pas saisie |
| **Clôture** | `UPDATE stock_produit` uniquement | `UPDATE stock_produit` + `UPDATE lot.current_quantity` |

**Création des lots — requête générique :**

```sql
-- Exécutée APRÈS l'insertion des store_inventory_line, pour TOUS les types d'inventaire
INSERT INTO inventory_lot (lot_id, quantity_init, store_inventory_line_id, updated_at, updated, gap)
SELECT l.id, l.current_quantity, sil.id, NOW(), false, 0
FROM store_inventory_line sil
JOIN lot l ON l.produit_id = sil.produit_id
WHERE sil.store_inventory_id = :inventoryId
  AND l.current_quantity > 0
ON CONFLICT ON CONSTRAINT uq_il_lot_line DO NOTHING
```

Pour PERIME/ALERTE, un filtre supplémentaire sur `l.expiry_date` restreint aux lots concernés.

### 2.3 Lots avec `current_quantity = 0` mais physiquement présents

**Problème :** Le filtre `l.current_quantity > 0` exclut les lots épuisés en machine. Si l'agent trouve physiquement du stock pour un lot à 0, il ne peut pas le saisir.

**Solution : ajout de lot manquant**

```
Flux normal :
  Création → inventory_lot pour tous lots avec current_quantity > 0

Flux correction :
  L'agent voit la liste des lots pré-remplis dans la modale
  → Bouton "Ajouter un lot"
  → Recherche par numéro de lot (lot existant avec qty=0) ou création d'un nouveau lot
  → Crée un inventory_lot avec quantity_init = 0
```

**Backend :**

```java
// Nouvel endpoint
POST /api/store-inventory-lines/{lineId}/lots
Body: { lotId: Long }               // lot existant avec qty = 0
  ou: { numLot: String, expiryDate: LocalDate }  // lot inconnu du système

// Logique :
// 1. Chercher le lot par id ou numLot + produit
// 2. Si lot existe avec current_quantity = 0 → créer inventory_lot(quantity_init=0)
// 3. Si lot inexistant → créer Lot + inventory_lot(quantity_init=0)
// 4. Retourner l'inventory_lot créé
```

### 2.4 Gestion des réserves à la clôture

**Contexte métier :** Quand une officine fait un inventaire (MAGASIN, RAYON, FAMILLY, etc.), elle inventorie le rayon. Le stock réserve (SAFETY_STOCK) n'est pas compté physiquement. Pour éviter les incohérences, **le stock réserve doit être mis à zéro** pour les produits inventoriés, car l'inventaire vise à réaligner le stock informatique avec le stock physique global.

**Exception :** L'inventaire de type **STORAGE** cible un storage précis (qui peut être rayon OU réserve). Il ne doit toucher que le storage inventorié.

**Règle :**

| InventoryCategory | Comportement clôture |
|---|---|
| **STORAGE** | Met à jour **uniquement** le storage inventorié (`sil.storage_id`). Ne touche pas les autres storages. |
| **Tous les autres** (MAGASIN, RAYON, FAMILLY, PERIME, ALERTE, VENDU, INVENDU, SOUS_SEUIL, EN_RUPTURE) | 1. Met à jour le stock du storage inventorié (`sil.storage_id`). 2. **Met à zéro le stock réserve** (SAFETY_STOCK) pour les produits inventoriés. |

**Justification :** L'inventaire rayon est le moment de vérité. Si le stock théorique dit "10 en rayon + 5 en réserve" et que l'agent compte 10 en rayon, la réserve informatique (5) est suspecte — elle n'a pas été vérifiée. La mise à zéro force une remise en cohérence. Les produits réellement en réserve seront re-comptabilisés lors de la prochaine réception ou d'un inventaire STORAGE dédié à la réserve.

### 2.5 Suggestions de réapprovisionnement — exécution différée et optimisée

**Problème (ancienne proposition) :** `createReserveSuggestions()` en synchrone dans la clôture bloque l'utilisateur et allonge la transaction.

**Solution : event listener post-commit asynchrone**

```java
// Dans InventaireServiceImpl.close()
int nbLignes = closeItems(storeInventory.getId());
storeInventory.setStatut(InventoryStatut.CLOSED);

// Publier un événement APRÈS le commit — ne bloque pas l'utilisateur
applicationEventPublisher.publishEvent(
    new InventoryClosedEvent(storeInventory.getId(), storeInventory.getInventoryCategory())
);

return new ItemsCountRecord(nbLignes);
```

```java
@Component
public class InventoryClosedEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional
    public void onInventoryClosed(InventoryClosedEvent event) {
        if (event.category() == PERIME || event.category() == ALERTE_PEREMPTION) {
            return; // pas de suggestion pour les types lot
        }
        reserveSuggestionService.createForInventory(event.inventoryId());
    }
}
```

**Requête optimisée — un seul appel, uniquement les produits avec réserve disponible :**

```sql
SELECT sp_principal.*
FROM store_inventory_line sil
JOIN stock_produit sp_principal
    ON sp_principal.produit_id = sil.produit_id
JOIN storage s_principal
    ON s_principal.id = sp_principal.storage_id
    AND s_principal.storage_type = 'PRINCIPAL'
WHERE sil.store_inventory_id = :inventoryId
  AND sp_principal.qty_stock <= COALESCE(sp_principal.seuil_mini, 0)
  AND EXISTS (
      SELECT 1 FROM stock_produit sp_reserve
      JOIN storage s_reserve ON s_reserve.id = sp_reserve.storage_id
          AND s_reserve.storage_type = 'SAFETY_STOCK'
          AND s_reserve.magasin_id = s_principal.magasin_id
      WHERE sp_reserve.produit_id = sil.produit_id
        AND sp_reserve.qty_stock > 0
  )
```

Un seul SELECT, pas de for-loop, uniquement les produits qui ont réellement de la réserve disponible.

### 2.6 Architecture UI — grilles dédiées par type d'inventaire

**Problème :** Un composant monolithique `inventory-lines-grid` avec des conditions partout pour gérer 10 types différents n'est pas maintenable.

**Contrainte technique :** AG Grid Community 35.1.0 — pas de master-detail / row expansion natif (fonctionnalité Enterprise).

**Alternative disponible :** PrimeNG Table (`p-table`) — **déjà utilisée dans 20+ composants** du projet avec row expansion (`pRowToggler`, `ng-template #rowexpansion`), lazy loading (`[lazy]="true"`, `(onLazyLoad)`), et styles dédiés (`_expanded-row.scss`). C'est un pattern bien établi dans le codebase.

#### Choix technologique par grille

| Grille | Technologie | Justification |
|---|---|---|
| **Standard grid** (sans lots) | AG Grid Community | Navigation cellule, édition inline, auto-navigation — déjà implémenté |
| **Lot grid plate** (PERIME/ALERTE) | AG Grid Community | Même logique que standard, une ligne = un lot |
| **Grille avec expansion lot** | **PrimeNG Table** | Row expansion native gratuite, pattern déjà maîtrisé dans le projet |
| **Sold grid / Threshold grid** | AG Grid Community | Même base que standard, colonnes supplémentaires |

#### Choix du mode de saisie par l'officine (quand `GESTION_LOT = true`)

L'officine choisit son mode de saisie lot. Ce choix s'applique à **tous les types d'inventaire**. Paramètre dans `AppConfiguration` :

```
AppConfiguration:
  name:        MODE_SAISIE_LOT_INVENTAIRE
  value:       MODAL           (valeur par défaut)
  description: Mode de saisie des lots en inventaire
  valueType:   STRING
  options:     [
    { key: "MODAL",     value: "Grille produit + modale lots" },
    { key: "LOT_PLAT",  value: "Grille plate par lot (une ligne = un lot)" },
    { key: "EXPANSION", value: "Grille produit avec expansion lots" }
  ]
```

---

#### Mode 1 — MODAL (AG Grid + ngbModal)

**Principe :** Grille AG Grid standard (une ligne = un produit). Les produits ayant des lots affichent une icône cliquable → ouvre une modale ngbModal avec la liste des lots à saisir.

| CIP | Produit | Stock actuel | Qté inventoriée | Écart | Saisi | Lots |
|-----|---------|-------------|----------------|-------|-------|------|
| 3400001 | Doliprane 1000mg | 15 | [12] | -3 | ✓ | 🔗 |
| 3400002 | Amoxicilline 500mg | 8 | — | — | ✗ | 🔗 |
| 3400003 | Sérum physio (sans lot) | 20 | [20] | 0 | ✓ | — |

- Colonne **Lots** : icône cliquable uniquement si `hasLots = true`
- Clic → ouvre `lot-edit-modal` avec la liste des `inventory_lot` du produit
- Saisie `quantity_on_hand` par lot dans la modale
- Bouton "Ajouter un lot" dans la modale (lots qty=0 ou inexistants)
- À la fermeture : `line.quantity_on_hand = SUM(lots.quantity_on_hand)` (calculé, pas saisi)
- Produits sans lots → saisie directe sur la ligne comme aujourd'hui
- **Avantage :** vue produit compacte, navigation rapide entre produits, grille légère
- **Référence :** Pharmagest id./LGPI (onglet "Lots" au niveau produit)

---

#### Mode 2 — LOT_PLAT (AG Grid, une ligne = un lot)

**Principe :** Grille AG Grid plate où chaque lot est une ligne indépendante. Un produit avec 3 lots = 3 lignes dans la grille. Les produits sans lots = 1 ligne (saisie directe).

| CIP | Produit | N° Lot | Date expiration | Stock lot | Qté constatée | Écart | Saisi |
|-----|---------|--------|----------------|-----------|---------------|-------|-------|
| 3400001 | Doliprane 1000mg | LOT-A | 2026-06-15 | 10 | [8] | -2 | ✓ |
| 3400001 | Doliprane 1000mg | LOT-B | 2026-12-01 | 5 | [4] | -1 | ✓ |
| 3400002 | Amoxicilline 500mg | LOT-C | 2026-09-20 | 8 | — | — | ✗ |
| 3400003 | Sérum physio | — | — | 20 | [20] | 0 | ✓ |

- Même technologie que la grille actuelle (AG Grid Community) → navigation cellule, édition inline, auto-navigation
- Backend retourne `StoreInventoryLotLineRecord` (jointure `inventory_lot + store_inventory_line`)
- Pour les produits sans lots : une ligne avec `lotNumero = null`, `expiryDate = null` → saisie directe
- Bouton "Ajouter un lot" (toolbar ou clic droit) → ouvre `add-lot-modal`
- Tri par défaut : CIP puis date d'expiration croissante
- `line.quantity_on_hand = SUM(lots.quantity_on_hand)` pour chaque produit (calculé au save)
- **Avantage :** tout est visible d'un coup, pas de clic supplémentaire, même UX que la grille standard
- **Référence :** Smart Rx Logi-Stock mode "Comptage détaillé"

**Note :** Ce mode est **le même composant** que celui utilisé pour PERIME/ALERTE_PEREMPTION (grille plate lot). La seule différence : pour PERIME/ALERTE, seuls les lots filtrés par date d'expiration sont affichés ; pour les autres types, tous les lots actifs du produit sont affichés + les produits sans lots.

---

#### Mode 3 — EXPANSION (PrimeNG Table + pRowToggler)

**Principe :** Grille PrimeNG Table (une ligne = un produit). Les produits ayant des lots affichent un bouton d'expansion → déplie les sous-lignes lot inline.

```html
<!-- PrimeNG Table avec row expansion (pattern existant dans 20+ composants du projet) -->
<p-table [value]="inventoryLines" [lazy]="true" (onLazyLoad)="lazyLoad($event)"
         [paginator]="true" [rows]="rows" [totalRecords]="totalItems"
         dataKey="id" [rowExpandMode]="'single'">
  <ng-template #header>
    <tr>
      <th style="width: 3%"></th>
      <th>CIP</th>
      <th>Produit</th>
      <th>Stock actuel</th>
      <th>Qté inventoriée</th>
      <th>Écart</th>
      <th>Saisi</th>
    </tr>
  </ng-template>
  <ng-template #body let-line let-expanded="expanded">
    <tr>
      <td>
        @if (line.hasLots) {
          <p-button [icon]="expanded ? 'pi pi-chevron-down' : 'pi pi-chevron-right'"
                    [pRowToggler]="line" [rounded]="true" [text]="true" type="button" />
        }
      </td>
      <td>{{ line.produitCip }}</td>
      <td>{{ line.produitLibelle }}</td>
      <td class="text-right">{{ line.quantityInit }}</td>
      <td class="text-right">{{ line.quantityOnHand }}</td>
      <td class="text-right">{{ line.gap }}</td>
      <td><i [class]="line.updated ? 'pi pi-check text-success' : 'pi pi-times text-danger'"></i></td>
    </tr>
  </ng-template>
  <ng-template #rowexpansion let-line>
    <tr>
      <td [attr.colspan]="7">
        <div class="expanded-row-container">
          <p-table [value]="line.lots" class="rowexpansion-table" dataKey="lotId">
            <ng-template #header>
              <tr>
                <th>N° Lot</th>
                <th>Date expiration</th>
                <th>Stock init</th>
                <th>Qté constatée</th>
              </tr>
            </ng-template>
            <ng-template #body let-lot>
              <tr>
                <td>{{ lot.lotNumero }}</td>
                <td>{{ lot.expiryDate | date:'dd/MM/yyyy' }}</td>
                <td class="text-right">{{ lot.quantityInit }}</td>
                <td>
                  <input type="number" [min]="0" [(ngModel)]="lot.quantityOnHand"
                         (change)="onLotQuantityChange(line, lot)" />
                </td>
              </tr>
            </ng-template>
          </p-table>
          <p-button label="Ajouter un lot" icon="pi pi-plus" [text]="true"
                    (onClick)="addMissingLot(line)" size="small" />
        </div>
      </td>
    </tr>
  </ng-template>
</p-table>
```

- Produits sans lots → saisie directe sur la ligne (pas d'expansion)
- Produits avec lots → `quantityOnHand` en lecture seule (calculé depuis les sous-lignes)
- Bouton "Ajouter un lot" dans la zone expandée → ouvre `add-lot-modal`
- `line.quantity_on_hand = SUM(lots.quantity_on_hand)` (calculé en temps réel)
- Styles : réutilise `expanded-row-container` et `rowexpansion-table` existants (`_expanded-row.scss`)
- **Avantage :** vision hiérarchique produit → lots, pas de changement de contexte
- **Référence :** Winpharma (expansion inline / accordion)

---

#### Comparaison des 3 modes

| Critère | Mode 1 — MODAL | Mode 2 — LOT_PLAT | Mode 3 — EXPANSION |
|---|---|---|---|
| **Technologie** | AG Grid + ngbModal | AG Grid | PrimeNG Table |
| **Vue principale** | 1 ligne = 1 produit | 1 ligne = 1 lot | 1 ligne = 1 produit |
| **Accès aux lots** | Clic → modale | Directement dans la grille | Clic → expand inline |
| **Navigation clavier** | AG Grid native | AG Grid native | PrimeNG (à implémenter) |
| **Produits sans lots** | Saisie directe | Saisie directe (1 ligne) | Saisie directe |
| **Ajout lot manquant** | Dans la modale | Bouton toolbar/action | Dans la zone expandée |
| **Nb lignes affichées** | Compact (1 par produit) | Élevé (N par produit multi-lot) | Compact + expandable |
| **Complexité impl.** | Faible | Faible (même composant que PERIME/ALERTE) | Moyenne (PrimeNG + navigation) |
| **Référence éditeur** | Pharmagest id./LGPI | Smart Rx Logi-Stock | Winpharma |

---

#### Colonnes supplémentaires selon le type d'inventaire

Quel que soit le mode choisi, les colonnes supplémentaires sont ajoutées :

| InventoryCategory | Colonne(s) ajoutée(s) |
|---|---|
| VENDU | `Vendu (période)` = `quantity_sold` |
| SOUS_SEUIL | `Seuil mini` = `stock_produit.seuil_mini` |
| PERIME / ALERTE_PEREMPTION | `Date expiration` avec code couleur (rouge/orange/jaune) |
| Autres | Colonnes standard uniquement |

---

#### Structure des composants

```
inventory-lines-grid/                          (composant conteneur — dispatch)
  ├── standard-grid.component.ts                (AG Grid — grille produit, mode MODAL si GESTION_LOT)
  ├── lot-flat-grid.component.ts                (AG Grid — grille plate lot, mode LOT_PLAT + PERIME/ALERTE)
  ├── expansion-lot-grid.component.ts           (PrimeNG Table — mode EXPANSION, row expansion lot)
  ├── lot-edit-modal.component.ts               (ngbModal — mode MODAL, saisie lot par produit)
  └── add-lot-modal.component.ts                (ngbModal — ajout lot manquant, partagé par les 3 modes)
```

**Points clés :**
- `lot-flat-grid` est **réutilisé** pour PERIME/ALERTE (avec filtre date expiration) ET pour le mode LOT_PLAT (tous lots)
- Plus de composants `sold-grid` / `threshold-grid` séparés : les colonnes supplémentaires sont gérées par `@Input() extraColumns` sur chaque grille
- `add-lot-modal` est partagé par les 3 modes

**Dispatch dans le conteneur :**

```typescript
@if (!lotManagementEnabled) {
    <!-- GESTION_LOT = false : grille produit standard pour tous les types -->
    <app-standard-grid [inventory]="inventory" [extraColumns]="extraColumnsForType" />
} @else {
    <!-- GESTION_LOT = true : dispatch selon le mode choisi -->
    @switch (modeSaisieLot) {
        @case ('MODAL') {
            <app-standard-grid [inventory]="inventory"
                               [extraColumns]="extraColumnsForType"
                               [lotManagement]="true" />
        }
        @case ('LOT_PLAT') {
            <app-lot-flat-grid [inventory]="inventory"
                               [extraColumns]="extraColumnsForType" />
        }
        @case ('EXPANSION') {
            <app-expansion-lot-grid [inventory]="inventory"
                                    [extraColumns]="extraColumnsForType" />
        }
    }
}
```

```typescript
// Calcul des colonnes supplémentaires selon le type
get extraColumnsForType(): string[] {
    switch (this.inventoryCategory) {
        case 'VENDU': return ['quantitySold'];
        case 'SOUS_SEUIL': return ['seuilMini'];
        case 'PERIME':
        case 'ALERTE_PEREMPTION': return ['expiryDate'];
        default: return [];
    }
}
```

**Factorisation :** `InventoryGridService` extrait les comportements communs :
- Auto-navigation après saisie (AG Grid et PrimeNG)
- Sauvegarde immédiate via API
- Pagination serveur / lazy loading
- Mode aveugle (masquer `quantityInit`)
- Filtres storage/rayon
- Style de ligne (saisi / non saisi / écart)
- Réconciliation `line.quantity_on_hand = SUM(lots.quantity_on_hand)`

#### Référence éditeurs

| Éditeur | Approche UI | Correspondance Pharma-Smart |
|---|---|---|
| **Pharmagest id./LGPI** | Onglets contextuels : "Comptage" + "Lots" au niveau produit | → Mode 1 MODAL |
| **Smart Rx Logi-Stock** | Deux modes : "Comptage rapide" (par produit) et "Comptage détaillé" (par lot) | → Mode 2 LOT_PLAT |
| **Winpharma** | Grille avec expansion inline (accordion) | → Mode 3 EXPANSION |
| **Datarithm** | Dashboard-first + grille spécialisée par type | → Conteneur dispatch + extraColumns |

L'approche retenue offre les 3 modes au choix de l'officine, combinant les forces de chaque éditeur de référence.

---

## 3. Changements — Création (`InventaireCreationServiceImpl`)

### 3.1 `storage_id` dans les INSERT

**Pour MAGASIN, FAMILLY, VENDU, INVENDU, SOUS_SEUIL, EN_RUPTURE** — une ligne par (produit, storage PRINCIPAL) :

```sql
INSERT INTO store_inventory_line (produit_id, storage_id, updated_at, updated, store_inventory_id)
SELECT DISTINCT p.id, sp.storage_id, NOW(), false, :inventoryId
FROM produit p
JOIN stock_produit sp ON sp.produit_id = p.id
JOIN storage s ON s.id = sp.storage_id
WHERE p.status = 'ENABLE'
  AND s.storage_type = 'PRINCIPAL'
  AND s.magasin_id = :magasinId
```

**Pour STORAGE et RAYON** — scopés à un storage :

```sql
INSERT INTO store_inventory_line (produit_id, storage_id, updated_at, updated, store_inventory_id)
SELECT p.id, :storageId, NOW(), false, :inventoryId
FROM produit p
JOIN rayon_produit rp ON p.id = rp.produit_id
JOIN rayon r ON r.id = rp.rayon_id
JOIN storage s ON s.id = r.storage_id
WHERE p.status = 'ENABLE' AND s.id = :storageId
```

**Pour PERIME et ALERTE_PEREMPTION** — tous storages (un lot périmé peut être en réserve) :

```sql
INSERT INTO store_inventory_line (produit_id, storage_id, updated_at, updated, store_inventory_id)
SELECT DISTINCT p.id, sp.storage_id, NOW(), false, :inventoryId
FROM produit p
JOIN lot l ON l.produit_id = p.id
JOIN stock_produit sp ON sp.produit_id = p.id
JOIN storage s ON s.id = sp.storage_id
WHERE p.status = 'ENABLE'
  AND s.magasin_id = :magasinId
  AND l.expiry_date < CURRENT_DATE
  AND l.current_quantity > 0
```

> Pas de filtre `storage_type = 'PRINCIPAL'` ici.

### 3.2 Création des `inventory_lot` (si `GESTION_LOT = true`)

Exécutée **après** l'insertion des `store_inventory_line`, pour **tous les types** :

```sql
-- Générique : tous lots actifs rattachés aux produits inventoriés
INSERT INTO inventory_lot (lot_id, quantity_init, store_inventory_line_id, updated_at, updated, gap)
SELECT l.id, l.current_quantity, sil.id, NOW(), false, 0
FROM store_inventory_line sil
JOIN lot l ON l.produit_id = sil.produit_id
WHERE sil.store_inventory_id = :inventoryId
  AND l.current_quantity > 0
ON CONFLICT ON CONSTRAINT uq_il_lot_line DO NOTHING
```

Pour PERIME : filtre additionnel `AND l.expiry_date < CURRENT_DATE`.
Pour ALERTE_PEREMPTION : filtre additionnel `AND l.expiry_date BETWEEN CURRENT_DATE AND (CURRENT_DATE + :alerteJours)`.

### 3.3 VENDU — ajouter `quantity_sold`

```sql
INSERT INTO store_inventory_line (produit_id, storage_id, quantity_sold, updated_at, updated, store_inventory_id)
SELECT p.id, sp.storage_id, SUM(sl.quantity_requested),
       NOW(), false, :inventoryId
FROM sales_line sl
JOIN sales s ON s.id = sl.sales_id
JOIN stock_produit sp ON sp.produit_id = p.id
JOIN storage st ON st.id = sp.storage_id AND st.storage_type = 'PRINCIPAL'
GROUP BY p.id, sp.storage_id
```

### 3.4 SOUS_SEUIL et EN_RUPTURE — déjà corrects

Les SQL existants vérifient le stock total (rayon + réserve). Pas de changement sur la logique de sélection.

---

## 4. Changements — Clôture

### 4.1 `proc_close_inventory_v2` — met à jour le stock inventorié + écrase les réserves

```sql
CREATE OR REPLACE PROCEDURE proc_close_inventory_v2(
    IN  p_store_inventory_id BIGINT,
    IN  p_gestion_lot        BOOLEAN,
    INOUT p_nombre_ligne      INT
)
LANGUAGE plpgsql AS $$
DECLARE
    v_user_id         INT;
    v_magasin_id      INT;
    v_inventory_category TEXT;
BEGIN
    -- Récupérer user, magasin et catégorie d'inventaire
    SELECT s.user_id, u.magasin_id, s.inventory_category
    INTO v_user_id, v_magasin_id, v_inventory_category
    FROM store_inventory s
    JOIN app_user u ON s.user_id = u.id
    WHERE s.id = p_store_inventory_id;

    -- ── STEP 1 : Mettre à jour le stock du storage inventorié ──────────────
    -- storage_id est NOT NULL (garanti à la création)
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

    -- ── STEP 2 : Réserve → 0 (tous types SAUF STORAGE) ────────────────────
    -- Quand l'inventaire n'est PAS de type STORAGE, écraser le stock réserve
    -- des produits inventoriés pour forcer la remise en cohérence.
    -- Le type STORAGE cible un storage précis (qui peut être la réserve elle-même),
    -- donc il ne doit pas toucher les autres storages.
    IF v_inventory_category <> 'STORAGE' THEN
        UPDATE stock_produit sp
        SET qty_stock   = 0,
            qty_ug      = 0,
            qty_virtual = 0,
            updated_at  = NOW()
        FROM store_inventory_line sil
        JOIN storage s_inventoried ON s_inventoried.id = sil.storage_id
        WHERE sil.store_inventory_id = p_store_inventory_id
          AND sp.produit_id = sil.produit_id
          AND sp.storage_id <> sil.storage_id   -- ne pas re-toucher le storage déjà mis à jour
          AND sp.storage_id IN (
              SELECT s2.id FROM storage s2
              WHERE s2.magasin_id = s_inventoried.magasin_id
                AND s2.storage_type = 'SAFETY_STOCK'
          );
    END IF;

    -- ── STEP 3 : Lots (si gestion lot activée) ────────────────────────────
    IF p_gestion_lot THEN
        UPDATE lot l
        SET current_quantity = COALESCE(il.quantity_on_hand, il.quantity_init)
        FROM inventory_lot il
        JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
        WHERE sil.store_inventory_id = p_store_inventory_id
          AND l.id = il.lot_id;
    END IF;

    -- ── STEP 4 : Transactions d'inventaire (traçabilité) ──────────────────
    INSERT INTO inventory_transaction
        (cost_amount, created_at, entity_id, mouvement_type, quantity,
         quantity_after, quantity_befor, regular_unit_price, magasin_id,
         produit_id, user_id)
    SELECT sil.inventory_value_cost,
           sil.updated_at,
           sil.id,
           'INVENTAIRE',
           sil.quantity_on_hand,
           sil.quantity_on_hand,
           sil.quantity_init,
           sil.last_unit_price,
           v_magasin_id,
           sil.produit_id,
           v_user_id
    FROM store_inventory_line sil
    WHERE sil.store_inventory_id = p_store_inventory_id;

END;
$$;
```

**Points clés :**
- **Step 2 conditionnel** : `IF v_inventory_category <> 'STORAGE'` — seul STORAGE ne touche pas la réserve
- **`sp.storage_id <> sil.storage_id`** — évite de re-écraser le storage déjà mis à jour au step 1
- **`sil.storage_id` directement** — NOT NULL garanti, pas de COALESCE
- **Paramètre `p_gestion_lot`** — le step lots n'est exécuté que si activé
- 4 statements SET-based au lieu d'un cursor row-by-row

### 4.2 Suggestions réserve — post-commit asynchrone

```java
// InventaireServiceImpl.close()
int nbLignes = closeItems(storeInventory.getId(), gestionLotEnabled);
storeInventory.setStatut(InventoryStatut.CLOSED);
applicationEventPublisher.publishEvent(
    new InventoryClosedEvent(storeInventory.getId(), storeInventory.getInventoryCategory())
);
return new ItemsCountRecord(nbLignes);
```

```java
// InventoryClosedEventListener
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async
@Transactional
public void onInventoryClosed(InventoryClosedEvent event) {
    if (event.category() == PERIME || event.category() == ALERTE_PEREMPTION) {
        return;
    }
    reserveSuggestionService.createForInventory(event.inventoryId());
}
```

```java
// ReserveSuggestionService.createForInventory — un seul appel
public void createForInventory(Long inventoryId) {
    List<StockProduit> candidates = stockProduitRepository
        .findInventoriedProductsWithReserveUnderThreshold(inventoryId);
    suggestionReassortRepository.saveAll(
        candidates.stream().map(this::buildSuggestion).toList()
    );
}
```

```sql
-- Requête unique : produits inventoriés, sous seuil, avec réserve disponible
SELECT sp_principal.*
FROM store_inventory_line sil
JOIN stock_produit sp_principal
    ON sp_principal.produit_id = sil.produit_id
JOIN storage s_principal
    ON s_principal.id = sp_principal.storage_id
    AND s_principal.storage_type = 'PRINCIPAL'
WHERE sil.store_inventory_id = :inventoryId
  AND sp_principal.qty_stock <= COALESCE(sp_principal.seuil_mini, 0)
  AND EXISTS (
      SELECT 1 FROM stock_produit sp_reserve
      JOIN storage s_reserve ON s_reserve.id = sp_reserve.storage_id
          AND s_reserve.storage_type = 'SAFETY_STOCK'
          AND s_reserve.magasin_id = s_principal.magasin_id
      WHERE sp_reserve.produit_id = sil.produit_id
        AND sp_reserve.qty_stock > 0
  )
```

### 4.3 Résumé clôture

| Étape | Actuel (`proc_close_inventory`) | Proposé (`proc_close_inventory_v2` + event) |
|-------|--------------------------------|---------------------------------------------|
| Stock inventorié | UPDATE cursor, storage_id ambigu | UPDATE bulk, `sil.storage_id` NOT NULL |
| Stock réserve | Non traité | UPDATE bulk → 0 (tous types sauf STORAGE) |
| Lots | Non traité | UPDATE bulk conditionnel (`p_gestion_lot`) |
| Transactions | INSERT cursor | INSERT bulk |
| Suggestions | Non traité | Java post-commit async, requête unique avec EXISTS réserve |

---

## 5. Changements — Interface de saisie

> L'architecture des grilles, les 3 modes de saisie lot et le dispatch sont définis en section 2.6.
> Cette section détaille les composants, records API et endpoints.

### 5.1 `standard-grid` — AG Grid (mode MODAL ou GESTION_LOT=false)

Grille AG Grid standard, une ligne = un produit.

| CIP | Produit | [Extra cols] | Stock actuel | Qté inventoriée | Écart | Saisi | [Lots] |
|-----|---------|-------------|-------------|----------------|-------|-------|--------|

- Colonnes supplémentaires via `@Input() extraColumns` : `quantitySold` (VENDU), `seuilMini` (SOUS_SEUIL), `expiryDate` (PERIME/ALERTE si GESTION_LOT=false)
- **Colonne Storage** : visible uniquement pour type MAGASIN si multi-storage
- Si `GESTION_LOT = true` et `MODE_SAISIE = MODAL` : colonne **Lots** avec icône cliquable → ouvre `lot-edit-modal`
- Produits sans lots → saisie directe `quantityOnHand`
- Produits avec lots (mode MODAL) → `quantityOnHand` en lecture seule (calculé depuis la modale)

### 5.2 `lot-flat-grid` — AG Grid (mode LOT_PLAT + PERIME/ALERTE)

Grille AG Grid plate, une ligne = un lot. **Réutilisée** pour :
- Mode LOT_PLAT (tous types d'inventaire) : tous les lots actifs + produits sans lots
- PERIME/ALERTE (quand GESTION_LOT=true, quel que soit le mode) : lots filtrés par date d'expiration

| CIP | Produit | N° Lot | Date expiration | [Extra cols] | Stock lot | Qté constatée | Écart | Saisi |
|-----|---------|--------|----------------|-------------|-----------|---------------|-------|-------|

- Un produit avec 3 lots = 3 lignes
- Produits sans lots = 1 ligne avec `lotNumero = null`, saisie directe
- Même technologie que la grille actuelle → navigation cellule, édition inline, auto-navigation
- Tri par défaut : CIP puis date d'expiration croissante
- Bouton "Ajouter un lot" dans la toolbar → ouvre `add-lot-modal`
- `line.quantity_on_hand = SUM(lots.quantity_on_hand)` calculé au save
- Code couleur date d'expiration pour PERIME/ALERTE : rouge (périmé), orange (< 60j), jaune (< 90j)
- Colonnes supplémentaires via `@Input() extraColumns`

**Backend :** retourne `StoreInventoryLotLineRecord` (jointure `inventory_lot + store_inventory_line`). Pour les produits sans lots : `lotId = null`, `lotNumero = null`.

### 5.3 `expansion-lot-grid` — PrimeNG Table (mode EXPANSION)

Grille PrimeNG Table, une ligne = un produit, expansion inline pour les lots.

- Bouton ⊕ uniquement si `hasLots = true`
- Produits sans lots → saisie directe (pas d'expansion)
- Produits avec lots → `quantityOnHand` en lecture seule (calculé depuis les sous-lignes)
- Sous-lignes lot dans la zone expandée avec saisie `quantity_on_hand`
- Bouton "Ajouter un lot" dans la zone expandée → ouvre `add-lot-modal`
- `line.quantity_on_hand = SUM(lots.quantity_on_hand)` en temps réel
- Styles : `expanded-row-container` et `rowexpansion-table` existants
- Colonnes supplémentaires via `@Input() extraColumns`

### 5.4 `lot-edit-modal` — ngbModal (mode MODAL)

Ouverte depuis `standard-grid` quand l'utilisateur clique sur l'icône lots d'un produit.

| N° Lot | Date expiration | Stock init | Qté constatée |
|--------|----------------|-----------|---------------|
| LOT-A  | 2026-06-15     | 10        | [input]       |
| LOT-B  | 2026-12-01     | 5         | [input]       |
| **Total** |             | **15**    | **[calculé]** |

- Bouton "Ajouter un lot" → ouvre `add-lot-modal`
- À la fermeture : `line.quantity_on_hand = SUM(lots.quantity_on_hand)`
- Sauvegarde via `PUT /api/store-inventory-lines/{lineId}/lots` (batch)

### 5.5 `add-lot-modal` — ngbModal (partagée par les 3 modes)

Utilisée par `lot-edit-modal` (mode MODAL), `lot-flat-grid` (mode LOT_PLAT), `expansion-lot-grid` (mode EXPANSION).

**Flux :**
1. Champ de recherche par numéro de lot
2. Si lot trouvé avec `current_quantity = 0` → affiche les infos, bouton "Ajouter"
3. Si lot non trouvé → formulaire de création : numéro de lot + date d'expiration
4. Crée `inventory_lot` avec `quantity_init = 0` via `POST /api/store-inventory-lines/{lineId}/lots`

### 5.6 Records API

```java
// Existant — enrichi
record StoreInventoryLineRecord(
    int produitId, String produitCip, String produitEan, String produitLibelle,
    Long id, Integer gap, Integer quantityOnHand, int quantityInit,
    boolean updated, Integer prixAchat, Integer prixUni,
    Integer storageId,        // NOUVEAU
    Integer quantitySold,     // NOUVEAU (VENDU)
    Integer seuilMini,        // NOUVEAU (SOUS_SEUIL)
    boolean hasLots           // NOUVEAU — indique si la modale lot est disponible
)

// Nouveau — pour lot-grid (PERIME/ALERTE avec GESTION_LOT=true)
record StoreInventoryLotLineRecord(
    int produitId, String produitCip, String produitLibelle,
    Long lineId,
    Long inventoryLotId,
    Long lotId, String lotNumero, LocalDate expiryDate,
    int lotQuantityInit, Integer lotQuantityOnHand,
    boolean updated
)
```

---

## 6. Annexe — Récapitulatif par type

| InventoryCategory    | Changement création | Changement clôture | UI (LOT=false) | UI (LOT=true, MODAL) | UI (LOT=true, LOT_PLAT) | UI (LOT=true, EXPANSION) |
|---|---|---|---|---|---|---|
| MAGASIN       | `storage_id` + lots | Stock + réserve → 0 | standard-grid | standard-grid + modale | lot-flat-grid | expansion-lot-grid |
| STORAGE       | `storage_id` + lots | Stock **uniquement** | standard-grid | standard-grid + modale | lot-flat-grid | expansion-lot-grid |
| RAYON         | `storage_id` + lots | Stock + réserve → 0 | standard-grid | standard-grid + modale | lot-flat-grid | expansion-lot-grid |
| FAMILLY       | `storage_id` + lots | Stock + réserve → 0 | standard-grid | standard-grid + modale | lot-flat-grid | expansion-lot-grid |
| PERIME        | `storage_id` + lots | Stock + réserve → 0 + lots | standard-grid | lot-flat-grid | lot-flat-grid | lot-flat-grid |
| ALERTE_PEREMPTION | `storage_id` + lots | Stock + réserve → 0 + lots | standard-grid | lot-flat-grid | lot-flat-grid | lot-flat-grid |
| VENDU         | `storage_id` + `qty_sold` + lots | Stock + réserve → 0 | standard-grid (+col vendu) | standard-grid (+col vendu) + modale | lot-flat-grid (+col vendu) | expansion-lot-grid (+col vendu) |
| INVENDU       | `storage_id` + lots | Stock + réserve → 0 | standard-grid | standard-grid + modale | lot-flat-grid | expansion-lot-grid |
| SOUS_SEUIL    | `storage_id` + lots | Stock + réserve → 0 | standard-grid (+col seuil) | standard-grid (+col seuil) + modale | lot-flat-grid (+col seuil) | expansion-lot-grid (+col seuil) |
| EN_RUPTURE    | `storage_id` + lots | Stock + réserve → 0 | standard-grid | standard-grid + modale | lot-flat-grid | expansion-lot-grid |

> **Note :** PERIME et ALERTE_PEREMPTION utilisent toujours `lot-flat-grid` quel que soit le mode choisi — tous les éléments sont des lots par définition, les 3 modes convergent vers la même grille plate.

### Résumé clôture

```
Tous types SAUF STORAGE :
  1. proc_close_inventory_v2 :
     - UPDATE bulk stock_produit = quantity_on_hand (storage inventorié)
     - UPDATE bulk stock réserve (SAFETY_STOCK) = 0
     - IF gestion_lot : UPDATE bulk lot.current_quantity = inventory_lot.quantity_on_hand
     - INSERT bulk inventory_transaction
  2. Java post-commit async (sauf PERIME/ALERTE) :
     - Suggestions réserve : requête unique avec EXISTS réserve qty > 0

Type STORAGE :
  1. proc_close_inventory_v2 :
     - UPDATE bulk stock_produit = quantity_on_hand (storage inventorié UNIQUEMENT)
     - PAS de mise à zéro d'autres storages
     - IF gestion_lot : UPDATE bulk lot.current_quantity
     - INSERT bulk inventory_transaction
  2. Java post-commit async :
     - Suggestions réserve (si le storage inventorié est PRINCIPAL)
```

---

## 7. Propositions d'améliorations — Inspirées des éditeurs de référence

> Sources : Pharmagest id./LGPI, Smart Rx Logi-Stock, Winpharma, Datarithm, SupplyLogix,
> pratiques recommandées par le Quotidien du Pharmacien et la démarche qualité officinale.

### 7.1 Inventaire tournant (Cycle Counting)

**Référence :** Smart Rx, Pharmagest id., SupplyLogix Pinpoint Count

Actuellement le système ne supporte que des inventaires ponctuels (création manuelle). Les éditeurs de référence proposent un **inventaire tournant planifié** qui répartit le comptage sur l'année sans fermeture.

**Principe :** Chaque semaine, un sous-ensemble de produits est automatiquement proposé au comptage selon des critères configurables.

**Propositions backend :**

- Nouveau type `InventoryType.TOURNANT` (en plus de `MANUEL`)
- Entité `PlanningInventaireTournant` :
  - `frequence` (QUOTIDIEN, HEBDO, MENSUEL, TRIMESTRIEL)
  - `critere` (RAYON, FAMILLE, CLASSIFICATION_ABC, CODE_GEOGRAPHIQUE)
  - `prochaine_execution` (date)
  - `actif` (boolean)
- Job planifié (`@Scheduled` ou Quartz) : à chaque échéance, crée automatiquement un `StoreInventory` avec les produits à compter selon le critère
- Le planning est rotatif : chaque exécution avance au critère suivant (ex : rayon 1 semaine 1, rayon 2 semaine 2, etc.)

**Propositions UI :**

- Écran de configuration du planning tournant : fréquence, critère, activation
- Dashboard des inventaires tournants : prochaine exécution, rayons déjà comptés ce mois, taux de couverture
- Notification à l'ouverture de l'application si un inventaire tournant est en attente

---

### 7.2 Classification ABC — Priorisation par valeur/rotation

**Référence :** Datarithm, méthode Pareto en gestion d'officine

Les éditeurs experts classent les produits en 3 catégories :
- **A** (20% des références, 80% du CA) → comptage fréquent
- **B** (30% des références, 15% du CA) → comptage intermédiaire
- **C** (50% des références, 5% du CA) → comptage rare

#### Infrastructure existante — déjà implémentée

Le système dispose **déjà** de toute l'infrastructure ABC Pareto :

**Vue matérialisée `mv_abc_pareto_analysis`** (V1.0.5) :
- Classification A/B/C basée sur le CA cumulé (seuils 80% / 95%)
- Colonnes : `produit_id`, `classe_pareto` (A/B/C), `ca_total`, `qte_vendue`, `nb_ventes`, `contribution_pct`, `ca_cumule_pct`, `rang`
- Période : ventes de l'année glissante (`sale_date >= CURRENT_DATE - 1 year`)
- Index : `classe_pareto`, `ca_total DESC`, `rang`, `categorie`, unique sur `produit_id`

**Vue matérialisée `mv_pareto_summary`** :
- Agrégation : `total_produits`, `ca_global`, `nb_produits_a/b/c`, `ca_classe_a/b/c`, `pct_ca_classe_a/b/c`

**Service `ABCParetoReportServiceImpl`** — déjà opérationnel :
- `getAllABCParetoAnalysis()` — toutes les classifications
- `getABCParetoByClass(ClassePareto)` — filtrer par classe A/B/C
- `getABCParetoByCategory(String)` — filtrer par famille de produits
- `getABCParetoPaginated(int page, int size)` — pagination
- `getABCParetoSummary()` → `ABCParetoSummaryDTO`
- Cache Caffeine (`abcPareto`)

**Refresh automatique** (`MaterializedViewRefreshService`) :
- TIER 3 : toutes les 6h (9h, 12h, 15h, 18h)
- Refresh CONCURRENT (index unique sur `produit_id`)

#### Ce qui reste à faire pour l'inventaire

Pas besoin de recalculer la classification ni de créer un nouveau service. Il suffit de **consommer** `mv_abc_pareto_analysis` dans le module inventaire :

**1. Nouveau `InventoryCategory.ABC`** (ou critère dans le planning tournant) :

```sql
-- SQL_INSERT_ABC : produits de classe A (ou B, configurable)
INSERT INTO store_inventory_line (produit_id, storage_id, updated_at, updated, store_inventory_id)
SELECT DISTINCT p.id, sp.storage_id, NOW(), false, :inventoryId
FROM mv_abc_pareto_analysis abc
JOIN produit p ON p.id = abc.produit_id
JOIN stock_produit sp ON sp.produit_id = p.id
JOIN storage s ON s.id = sp.storage_id
WHERE p.status = 'ENABLE'
  AND abc.classe_pareto = :classePareto   -- 'A', 'B', ou 'C'
  AND s.storage_type = 'PRINCIPAL'
  AND s.magasin_id = :magasinId
```

**2. Badge A/B/C dans les grilles d'inventaire** :

```sql
-- Enrichir StoreInventoryLineRecord avec la classe Pareto
SELECT sil.*, abc.classe_pareto, abc.rang
FROM store_inventory_line sil
LEFT JOIN mv_abc_pareto_analysis abc ON abc.produit_id = sil.produit_id
WHERE sil.store_inventory_id = :inventoryId
```

**3. Lien avec le planning tournant** (section 7.1) :
- Fréquence de comptage par classe : A = mensuel, B = trimestriel, C = annuel
- Le job planifié utilise `mv_abc_pareto_analysis` pour sélectionner les produits de la classe du tour

**Propositions UI :**

- Badge A/B/C sur chaque ligne d'inventaire (informatif, couleur : A=vert, B=orange, C=gris)
- Filtre par classification dans la grille
- Rapport existant via `ABCParetoReportService` — déjà disponible, à lier à l'écran inventaire

---

### 7.3 Saisie mobile avec scan code-barres

**Référence :** Smart Rx Logi-Stock (terminal EDA 52), Pharmagest id. mobilité

Logi-Stock permet de scanner un produit en rayon ou en réserve, visualiser son stock et saisir la quantité directement sur le terminal mobile, synchronisé en temps réel avec le LGO.

**Propositions backend :**

- Endpoint REST mobile dédié : `PUT /api/mobile/inventory-lines/{lineId}` (même logique que le PUT existant mais optimisé pour payloads légers)
- Endpoint de recherche par scan : `GET /api/mobile/inventory-lines/by-barcode?inventoryId={id}&barcode={ean13}`
  - Recherche par `code_ean_labo` ou `code_cip`
  - Retourne la `StoreInventoryLine` correspondante (ou 404 si le produit n'est pas dans l'inventaire en cours)

**Propositions UI (webapp mobile ou future app Tauri mobile) :**

- Écran de saisie mobile : champ scan → affiche la fiche produit + quantité à saisir
- Scan continu : après validation d'une ligne, le focus revient sur le champ scan
- Indicateur visuel : produit trouvé (vert), produit absent de l'inventaire (rouge), déjà saisi (orange)
- Mode hors-ligne : mise en tampon des saisies, synchronisation au retour de la connexion

---

### 7.4 Gestion FEFO (First Expired, First Out)

**Référence :** Datarithm, pratiques réglementaires pharmaceutiques

Le principe FEFO impose que les produits avec la date d'expiration la plus proche soient dispensés en premier. Les éditeurs intègrent ce contrôle dans la gestion quotidienne, pas seulement à l'inventaire.

**Propositions backend :**

- Contrôle FEFO à la vente : vérifier que le lot dispensé est bien celui avec l'expiration la plus proche. Alerter si ce n'est pas le cas
- Rapport FEFO : produits dont l'ordre de stockage ne respecte pas FEFO (lot en rayon expire après le lot en réserve)
- Lien avec l'inventaire : lors de la saisie PERIME / ALERTE_PEREMPTION, afficher les lots triés par date d'expiration croissante

**Propositions UI :**

- Tri par défaut par date d'expiration dans les grilles lot
- Alerte visuelle sur les lots mal ordonnés (lot rayon expire après lot réserve)

---

### 7.5 Analyse des écarts et démarque inconnue

**Référence :** Winpharma, Quotidien du Pharmacien, pratiques de rapprochement officinal

La démarque inconnue (écart entre stock théorique et stock physique sans cause identifiée) peut représenter 6-8% du stock. Les éditeurs de référence proposent des outils d'analyse causale.

**Propositions backend :**

- Table `inventory_gap_analysis` :
  ```sql
  CREATE TABLE inventory_gap_analysis (
      id               BIGSERIAL PRIMARY KEY,
      store_inventory_line_id BIGINT REFERENCES store_inventory_line(id),
      cause            VARCHAR(30) NOT NULL,  -- CASSE, VOL, ERREUR_RECEPTION, ERREUR_SAISIE, PEREMPTION, INCONNU
      quantity         INT NOT NULL,
      commentaire      TEXT,
      created_at       TIMESTAMP DEFAULT NOW()
  );
  ```
- Lors de la clôture : pour chaque ligne avec écart, demander (ou laisser saisir ultérieurement) la cause de l'écart
- Service `GapAnalysisService` :
  - `getGapSummary(Long inventoryId)` → agrégation par cause : nb produits, quantité totale, valorisation
  - `getGapTrend(Integer magasinId, LocalDate from, LocalDate to)` → évolution de la démarque dans le temps

**Propositions UI :**

- À la clôture : modal optionnel de qualification des écarts (dropdown cause par ligne avec écart)
- Rapport post-inventaire : tableau croisé écarts par cause + par rayon
- Dashboard historique : courbe de la démarque inconnue par mois, comparaison inter-inventaires
- Objectif affiché : "Démarque actuelle : X% → cible < 2%"

---

### 7.6 Valorisation avant/après et rapport de clôture

**Référence :** Pharmagest id., obligations comptables (inventaire annuel fiscal)

Les éditeurs génèrent automatiquement un rapport de clôture avec la valorisation du stock avant et après inventaire, exigé par l'administration fiscale.

**Propositions backend :**

La table `store_inventory` a déjà les champs `inventory_value_cost_begin`, `inventory_value_cost_after`, `inventory_amount_begin`, `inventory_amount_after`, `gap_cost`, `gap_amount`. La méthode `fetchSummary()` dans `InventaireServiceImpl` les calcule.

**Améliorations :**
- Ventilation par rayon/storage/famille (pas juste un total global)
- `StoreInventorySummaryByGroupRecord(groupKey, groupLabel, costBefore, costAfter, amountBefore, amountAfter, gapCost, gapAmount)`
- Service : `List<StoreInventorySummaryByGroupRecord> fetchSummaryByGroup(Long inventoryId, GroupBy groupBy)` avec `GroupBy` = RAYON, STORAGE, FAMILLE

**Propositions UI :**

- Onglet "Synthèse" dans l'écran d'inventaire clôturé :
  - Tableau récapitulatif : valeur avant / après / écart par groupe
  - Graphique en barres comparant avant/après par rayon
  - Export PDF conforme aux exigences comptables (format inventaire fiscal)

---

### 7.7 Inventaire par code géographique

**Référence :** Smart Rx Logi-Stock, Pharmagest id.

Les éditeurs associent un **code géographique** à chaque produit (emplacement physique dans l'officine : allée, étagère, position). Cela permet de faire des inventaires par zone physique plutôt que par rayon logique.

**Propositions :**

- Si `Produit` ou `StockProduit` a un champ `code_geographique` : ajouter un filtre dans la grille d'inventaire et un critère de création dans `InventaireCreationService`
- Nouveau `InventoryCategory.ZONE_GEOGRAPHIQUE` ou critère dans le planning tournant
- L'avantage : l'agent inventorie physiquement une zone (ex : étagère 3, tiroir B) sans se soucier du rayon logique

---

### 7.8 Contrôle de cohérence post-réception

**Référence :** Winpharma winAutopilote, Smart Rx

Après réception d'une commande fournisseur, les éditeurs proposent un mini-inventaire de contrôle : vérifier que les quantités reçues correspondent aux quantités commandées et que le stock informatique est cohérent.

**Propositions :**

- Lien `StoreInventory ← Commande` (optionnel) : créer un inventaire limité aux produits d'une commande réceptionnée
- `InventoryCategory.RECEPTION` (nouveau type éventuel) — ou simplement une fonctionnalité de la réception de commande
- UI : à la réception, bouton "Vérifier le stock" → ouvre une grille de saisie rapide limitée aux produits de la commande

---

## Annexe — Priorisation recommandée

| Proposition | Impact métier | Complexité | Priorité |
|------------|--------------|-----------|----------|
| 7.5 Analyse des écarts / démarque | Fort (obligation comptable, réduction pertes) | Moyenne | 1 |
| 7.6 Valorisation ventilée + rapport PDF | Fort (obligation fiscale) | Faible | 2 |
| 7.3 Saisie mobile avec scan | Fort (productivité terrain) | Moyenne | 3 |
| 7.1 Inventaire tournant planifié | Fort (fiabilité stock continue) | Moyenne | 4 |
| 7.2 Classification ABC | Moyen (priorisation intelligente) | Faible | 5 |
| 7.4 Contrôle FEFO | Moyen (conformité, réduction casse) | Faible | 6 |
| 7.7 Code géographique | Moyen (ergonomie terrain) | Faible | 7 |
| 7.8 Contrôle post-réception | Faible (cas d'usage ponctuel) | Faible | 8 |

---

*Proposition générée le 2026-03-13 — Révision v3 2026-03-13*


 Fonctionnement concret par critère et fréquence

  Le mécanisme de base

  À chaque exécution (automatique ou manuelle) :
  1. Le job récupère critereIndexCourant (démarre à 0)
  2. Il sélectionne l'élément à la position critereIndexCourant % taille_de_la_liste
  3. Il crée l'inventaire pour cet élément
  4. Il incrémente critereIndexCourant
  5. Il calcule prochaineExecution = aujourd'hui + fréquence

  ---
  RAYON + fréquence

  Supposons une officine avec 4 rayons : OTC, Parapharmacie, Orthopédie, Diététique (triés par nom).

  ┌───────────┬───────────┬──────────────────────────────────────┬───────────┐
  │ Fréquence │ Execution │          Rayon sélectionné           │ Prochaine │
  ├───────────┼───────────┼──────────────────────────────────────┼───────────┤
  │ HEBDO     │ 14/03     │ OTC (index 0)                        │ 21/03     │
  ├───────────┼───────────┼──────────────────────────────────────┼───────────┤
  │           │ 21/03     │ Parapharmacie (index 1)              │ 28/03     │
  ├───────────┼───────────┼──────────────────────────────────────┼───────────┤
  │           │ 28/03     │ Orthopédie (index 2)                 │ 04/04     │
  ├───────────┼───────────┼──────────────────────────────────────┼───────────┤
  │           │ 04/04     │ Diététique (index 3)                 │ 11/04     │
  ├───────────┼───────────┼──────────────────────────────────────┼───────────┤
  │           │ 11/04     │ OTC (index 4 % 4 = 0) → cycle repart │ 18/04     │
  └───────────┴───────────┴──────────────────────────────────────┴───────────┘

  → Avec HEBDO + 4 rayons : chaque rayon est compté une fois par mois. Cycle naturel.

  ┌─────────────┬──────────────────────────────────────────────────────────────────┐
  │  Fréquence  │                             Résultat                             │
  ├─────────────┼──────────────────────────────────────────────────────────────────┤
  │ QUOTIDIEN   │ 1 rayon / jour — avec 7 rayons, chaque rayon toutes les semaines │
  ├─────────────┼──────────────────────────────────────────────────────────────────┤
  │ HEBDO       │ 1 rayon / semaine — avec 4 rayons, cycle d'un mois               │
  ├─────────────┼──────────────────────────────────────────────────────────────────┤
  │ MENSUEL     │ 1 rayon / mois — avec 12 rayons, cycle d'un an                   │
  ├─────────────┼──────────────────────────────────────────────────────────────────┤
  │ TRIMESTRIEL │ 1 rayon / trimestre — avec 4 rayons, cycle d'un an               │
  └─────────────┴──────────────────────────────────────────────────────────────────┘

  Point important : le nombre de rayons détermine la durée du cycle complet. Si le storage change (ajout/suppression de rayon), nbRayons change et la rotation s'adapte automatiquement au prochain tour.

  ---
  FAMILLE + fréquence

  Supposons 12 familles (Antibiotiques, Antalgiques, Antifongiques, Antiviraux...).

  ┌─────────────┬───────────────────────┬────────────────────────────────────────┐
  │  Fréquence  │   1 cycle complet =   │              Cas d'usage               │
  ├─────────────┼───────────────────────┼────────────────────────────────────────┤
  │ QUOTIDIEN   │ 12 jours              │ Non réaliste pour les familles         │
  ├─────────────┼───────────────────────┼────────────────────────────────────────┤
  │ HEBDO       │ 12 semaines (~3 mois) │ Chaque famille comptée par trimestre   │
  ├─────────────┼───────────────────────┼────────────────────────────────────────┤
  │ MENSUEL     │ 12 mois (1 an)        │ Chaque famille comptée une fois par an │
  ├─────────────┼───────────────────────┼────────────────────────────────────────┤
  │ TRIMESTRIEL │ 36 mois (3 ans)       │ Trop long — déconseillé                │
  └─────────────┴───────────────────────┴────────────────────────────────────────┘

  Scénario FAMILLE + MENSUEL :
  Janvier  → Antibiotiques    (index 0)
  Février  → Antalgiques      (index 1)
  Mars     → Antifongiques    (index 2)
  ...
  Décembre → Vitamines        (index 11)
  Janvier  → Antibiotiques    (index 12 % 12 = 0 → recommence)

  ---
  CLASSIFICATION_ABC + fréquence

  La liste est fixe : ["A", "B", "C"] — 3 éléments, cycle de 3 exécutions.

  ┌─────────────┬─────────────────────────────────────┬─────────────────────────────────────────────────────┐
  │  Fréquence  │              Rotation               │                     Cas d'usage                     │
  ├─────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────────┤
  │ HEBDO       │ A → B → C → A toutes les 3 semaines │ Très fréquent, petite officine agile                │
  ├─────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────────┤
  │ MENSUEL     │ A → B → C → A sur 3 mois            │ Recommandé — A mensuel, B bi-mensuel, C trimestriel │
  ├─────────────┼─────────────────────────────────────┼─────────────────────────────────────────────────────┤
  │ TRIMESTRIEL │ A → B → C → A sur 9 mois            │ Chaque classe une fois tous les 9 mois              │
  └─────────────┴─────────────────────────────────────┴─────────────────────────────────────────────────────┘

  Scénario CLASSIFICATION_ABC + MENSUEL :
  Mars  2026 → Inventaire Classe A (20% des références, 80% du CA)
  Avril 2026 → Inventaire Classe B (30% des références, 15% du CA)
  Mai   2026 → Inventaire Classe C (50% des références, 5% du CA)
  Juin  2026 → Inventaire Classe A (recommence)

  Cela correspond exactement à la recommandation Pareto : les produits qui font 80% du CA (classe A) sont comptés le plus souvent.

  ---
  Ce qui se passe si la machine est éteinte

  Le job vérifie prochaine_execution <= aujourd'hui. Si la machine était éteinte 3 jours :

  - ✅ Au redémarrage à 8h, le job trouve les plannings échus et crée un seul inventaire par planning (l'exécution manquée n'est pas rattrapée en double)
  - prochaineExecution est recalculée à partir de la date d'exécution réelle (pas la date théorique), ce qui peut décaler légèrement le cycle

  ---
  Recommandations pratiques

  ┌────────────────────────────────────────┬───────────────────────────────────────────┐
  │                Officine                │          Configuration suggérée           │
  ├────────────────────────────────────────┼───────────────────────────────────────────┤
  │ Petite officine (1 storage, 5 rayons)  │ RAYON + HEBDO → chaque rayon/mois         │
  ├────────────────────────────────────────┼───────────────────────────────────────────┤
  │ Officine moyenne, produits saisonniers │ FAMILLE + MENSUEL → chaque famille/an     │
  ├────────────────────────────────────────┼───────────────────────────────────────────┤
  │ Officine avec suivi CA strict          │ ABC + MENSUEL → produits A chaque mois    │
  ├────────────────────────────────────────┼───────────────────────────────────────────┤
  │ Grosse officine, plusieurs storages    │ Un planning RAYON par storage + QUOTIDIEN │


 Références du marché français

  Les principaux éditeurs (Pharmagest/SmartRx, Winpharma, Alliadis/LEO, HP Santé, LGPI) représentent ~95% des officines françaises. Tous convergent vers les mêmes
  principes fondamentaux.

  ---
  Principe 1 — Journal immuable (append-only)

  Le stock courant n'est jamais stocké directement comme valeur de vérité. Il est toujours la projection (SUM) de tous les mouvements passés. La table stock_produit
  n'est qu'un cache recalculé.

  stock_actuel = SUM(quantité) pour tous les mouvements du produit
               filtrés par storage + lot + statut VALIDATED

  Conséquence directe : on peut reconstituer le stock à n'importe quelle date passée, détecter des incohérences, rejouer un historique.

  Ton système fait l'inverse : stock_produit est la source de vérité, inventory_transaction est le journal secondaire — c'est le modèle moins robuste.

  ---
  Principe 2 — Double entrée par storage (source → destination)

  Chaque mouvement a deux jambes, comme la comptabilité en partie double :

  SALE          : rayon_storage  →  client_virtuel
  ENTREE_STOCK  : fournisseur_virtuel  →  reserve_storage
  TRANSFERT     : reserve_storage  →  rayon_storage
  INVENTAIRE    : ajustement_virtuel  →  rayon_storage  (si +)
                : rayon_storage  →  ajustement_virtuel  (si -)

  Référence open-source la plus documentée : Odoo stock.move
  stock.move
    location_id       (source)
    location_dest_id  (destination)
    product_id
    lot_id
    qty_done
    state             (DRAFT → CONFIRMED → DONE → CANCELLED)

  Le stock par location est toujours : SUM(entrants) - SUM(sortants). Aucun zeroing implicite possible. La réserve ne peut être modifiée qu'en créant explicitement un
  mouvement la concernant.

  ---
  Principe 3 — Correction d'inventaire = mouvement delta, pas remplacement absolu

  Dans tous les systèmes professionnels, la clôture d'inventaire crée un mouvement par produit par storage avec :

  quantity  = gap  (delta = compté - système)   ← JAMAIS le stock absolu
  type      = INVENTAIRE_CORRECTION
  storage   = le storage exact où la ligne a été comptée
  lot_id    = lot concerné (si gestion lot)

  Pourquoi c'est critique :
  - Un inventaire MAGASIN corrige le rayon ET la réserve séparément, chacun avec son propre delta.
  - Un inventaire RAYON ne touche pas du tout la réserve — il n'y a pas de mouvement pour elle.
  - Le zeroing implicite du STEP 2 de ta procédure n'existe pas dans ces systèmes.

  ---
  Principe 4 — Granularité storage_id obligatoire

  Structure cible, proche de ce que font Pharmagest et SAP Pharma :

  CREATE TABLE mouvement_stock (
      id              BIGSERIAL PRIMARY KEY,
      created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      validated_at    TIMESTAMPTZ,
      statut          TEXT NOT NULL DEFAULT 'VALIDATED',  -- DRAFT, VALIDATED, CANCELLED

      -- Double entrée
      storage_src_id  INT REFERENCES storage(id),   -- NULL = externe (vente, fournisseur)
      storage_dst_id  INT REFERENCES storage(id),   -- NULL = externe

      produit_id      INT NOT NULL REFERENCES produit(id),
      lot_id          INT REFERENCES lot(id),

      -- Quantités
      quantity        INT NOT NULL,                 -- toujours positif, direction = src→dst
      quantity_befor  INT NOT NULL,                 -- stock storage_dst avant mouvement
      quantity_after  INT NOT NULL,                 -- stock storage_dst après mouvement

      -- Valorisation au moment du mouvement (FIFO)
      unit_cost       NUMERIC(12,2),
      unit_price      NUMERIC(12,2),

      -- Source document (polymorphe)
      reference_type  TEXT NOT NULL,               -- 'SALE', 'ORDER', 'INVENTORY', 'ADJUSTMENT'...
      reference_id    BIGINT NOT NULL,             -- id du document source

      user_id         INT NOT NULL REFERENCES app_user(id),
      magasin_id      INT NOT NULL REFERENCES magasin(id)
  );

  CREATE INDEX ON mouvement_stock(produit_id, storage_dst_id, created_at);
  CREATE INDEX ON mouvement_stock(lot_id) WHERE lot_id IS NOT NULL;
  CREATE INDEX ON mouvement_stock(reference_type, reference_id);

  ---
  Principe 5 — FEFO pour la valorisation et la consommation des lots

  Les officines françaises utilisent FEFO (First Expired, First Out) obligatoirement pour les médicaments, pas FIFO. Chaque mouvement de vente pointe sur le lot le plus
   proche de péremption avec du stock.

  Pour l'inventaire, le unit_cost enregistré est le prix d'achat du lot au moment du mouvement, pas le prix catalogue courant — ce que ton inventory_value_cost tente de
   faire, mais sans lier au lot.

  ---
  Ce que cela implique pour ton système

  ┌──────────────────────────────────────────────┬─────────────────────────────────────────────┐
  │               Problème actuel                │               Solution cible                │
  ├──────────────────────────────────────────────┼─────────────────────────────────────────────┤
  │ quantity = absolu pour INVENTAIRE            │ quantity = gap (delta)                      │
  ├──────────────────────────────────────────────┼─────────────────────────────────────────────┤
  │ Pas de storage_id dans inventory_transaction │ Ajouter storage_id                          │
  ├──────────────────────────────────────────────┼─────────────────────────────────────────────┤
  │ STEP 2 zeroing SAFETY_STOCK sans trace       │ Créer un mouvement explicite par storage    │
  ├──────────────────────────────────────────────┼─────────────────────────────────────────────┤
  │ stock_produit = source de vérité             │ Conserver mais recalculer depuis mouvements │
  ├──────────────────────────────────────────────┼─────────────────────────────────────────────┤
  │ Pas de storage_src_id / storage_dst_id       │ Migration progressive                       │
  └──────────────────────────────────────────────┴─────────────────────────────────────────────┘

  ---
  Recommandation pragmatique (sans tout réécrire)

  Court terme — 3 changements ciblés :

  1. Ajouter storage_id à inventory_transaction (migration + STEP 4 de la proc)
  2. Changer quantity = gap dans STEP 4 (pas le stock absolu)
  3. Remplacer le zeroing implicite STEP 2 par des INSERT INTO inventory_transaction explicites avec un nouveau type INVENTAIRE_RESERVE_CORRECTION

  Moyen terme — introduire une table mouvement_stock avec double-entrée pour les nouveaux flux (transferts, déconditions, réassorts), inventory_transaction devient
  l'alias legacy pour les flux existants.
