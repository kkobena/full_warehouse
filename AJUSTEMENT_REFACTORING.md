
# Refactorisation — Gestion des Ajustements de Stock

## 1. Contexte et problème initial

### État avant refactorisation

L'`AjustementService.saveItems()` ne mettait à jour que `StockProduit.qty_stock`. Les deux autres tables de traçabilité lot restaient incohérentes :

| Table | État avant | Problème |
|---|---|---|
| `stock_produit.qty_stock` | ✅ mis à jour | — |
| `lot.current_quantity` | ❌ jamais modifié | Stock lot fantôme |
| `lot_stock_location.qty` | ❌ jamais modifié | Emplacement incohérent |

### Exemple concret (AJUSTEMENT_IN +1, stock initial = 2)

```
Avant  : stockProduit=2, lot.currentQty=2, lot_stock_location=2
Après  : stockProduit=3, lot.currentQty=2 ❌, lot_stock_location=2 ❌
Impact : unité fantôme sans lot → traçabilité ANSM incomplète, FEFO incorrect à la vente suivante
```

---

## 2. Standards et normes officine

### Réglementaire (France / Europe)

| Norme | Exigence lots |
|---|---|
| **GDP 2013/C 343/01** | Tout mouvement tracé par lot + date péremption |
| **FMD 2016/161** | Débit au scan DataMatrix unitaire (sérialisation) |
| **ANSM pharmacovigilance** | Retrouver tous patients ayant reçu un lot donné |
| **BPO / ISO 9001** | FEFO obligatoire (péremption la plus proche en premier) |

### Logiciels de référence

| Logiciel | AJUSTEMENT_OUT | AJUSTEMENT_IN |
|---|---|---|
| **Winpharma / Pharmagest** | FEFO auto ou sélection lot | Sélection lot obligatoire si gestion_lot=ON |
| **Lgpi / Alliadis** | Sélection lot + motif (casse/périmé/vol) | Dialog sélection lot obligatoire |
| **SAP (PUI)** | Batch Number obligatoire dans Movement Document | Même exigence |
| **Caducée / Pixel** | FEFO auto | Saisie manuelle num lot |

**Conclusion** : aucun logiciel sérieux ne fait d'affectation automatique de lot pour AJUSTEMENT_IN. Le standard est la **saisie obligatoire du lot** si `gestion_lot = true`.

---

## 3. Architecture refactorisée

### Flux de mise à jour par opération

| Opération | `stock_produit.qty_stock` | `lot.current_quantity` | `lot_stock_location.qty` |
|---|---|---|---|
| Réception | ✅ | ✅ `LotServiceImpl` | ✅ `credit()` |
| Vente | ✅ | ✅ `updateLots()` FEFO | ✅ `debit()` FEFO |
| Annulation vente | ✅ | ✅ `restoreLots()` | ✅ `creditFromSold()` |
| **AJUSTEMENT_OUT** | ✅ | ✅ `adjustLots()` FEFO | ✅ `debitFefo()` |
| **AJUSTEMENT_IN** | ✅ | ✅ `adjustLots()` dernier reçu | ✅ `creditLastLot()` |
| Putaway transfert | — | — | ✅ `transferFefo()` |
| Clôture inventaire | ✅ stored proc | ✅ stored proc | ✅ réconciliation SQL |

### Services modifiés

#### `LotStockLocationService` — nouvelles méthodes

```java
// Débit d'un lot précis (vente)
void debit(Lot lot, Storage storage, int qtyDelta);

// Crédit depuis snapshot LotSold (annulation vente)
void creditFromSold(List<LotSold> lots, Storage storage);

// Débit FEFO sans destination (AJUSTEMENT_OUT)
void debitFefo(Produit produit, Storage storage, int qty);

// Crédit sur le lot le plus récemment reçu (AJUSTEMENT_IN fallback)
void creditLastLot(Produit produit, Storage storage, int qty);
```

#### `LotService` — nouvelle méthode

```java
/**
 * Applique un delta sur lot.current_quantity.
 * qtyDelta < 0 → FEFO (expiry ASC)
 * qtyDelta > 0 → dernier reçu (createdDate DESC)
 */
void adjustLots(Produit produit, int qtyDelta);
```

#### `AjustementService.saveItems()` — logique enrichie

```java
if (qtyMvt < 0) {
    // AJUSTEMENT_OUT
    int effectiveDebit = Math.min(Math.abs(qtyMvt), initStock);
    lotService.adjustLots(produit, -effectiveDebit);            // lot.current_quantity FEFO
    lotStockLocationService.debitFefo(produit, storage, effectiveDebit); // lot_stock_location FEFO
} else {
    // AJUSTEMENT_IN — fallback "dernier reçu" (si gestion_lot = false)
    lotService.adjustLots(produit, qtyMvt);                     // lot.current_quantity
    lotStockLocationService.creditLastLot(produit, storage, qtyMvt); // lot_stock_location
}
```

### Réconciliation inventaire (`InventoryClosedEventListener`)

Après `proc_close_inventory_v2`, réconciliation complète de `lot_stock_location` :

```sql
-- UPSERT depuis inventory_lot saisis
INSERT INTO lot_stock_location (lot_id, storage_id, qty, updated_at)
SELECT il.lot_id, :storageId, il.quantity_on_hand, NOW()
FROM inventory_lot il
JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
WHERE sil.store_inventory_id = :inventoryId AND il.updated = true AND il.quantity_on_hand > 0
ON CONFLICT (lot_id, storage_id) DO UPDATE SET qty = EXCLUDED.qty, updated_at = NOW();

-- DELETE lots épuisés
DELETE FROM lot_stock_location WHERE storage_id = :storageId AND lot_id IN (
    SELECT il.lot_id FROM inventory_lot il ...  WHERE il.quantity_on_hand = 0
);
```

Pour inventaire MAGASIN : purge totale `lot_stock_location` réserve (cohérent avec STEP 2 de la procédure).

---

## 4. Condition de débit AJUSTEMENT_OUT

**Condition correcte** : `effectiveDebit = min(|qtyMvt|, initStock)`

- `qtyMvt < 0` identifie la direction (stock en baisse)
- `min(|qtyMvt|, initStock)` évite de débiter `lot_stock_location` sur des unités qui n'étaient pas physiquement là (force stock négatif)
- `debitFefo` s'arrête naturellement si `lot_stock_location` est épuisé

---

## 5. Limitation actuelle et évolution cible

### Limitation : AJUSTEMENT_IN sans sélection de lot

L'implémentation actuelle utilise une heuristique ("dernier reçu") acceptable si `gestion_lot = false`.

**Si `gestion_lot = true`**, le standard industrie impose :

```
Si lotId fourni dans AjustementDTO → créditer ce lot précis
Si lotId absent et gestion_lot = true → lever exception métier (bloquer)
Si lotId absent et gestion_lot = false → fallback "dernier reçu"
```

### Évolution à implémenter

1. Ajouter `lotId?: Integer` dans `AjustementDTO`
2. Ajouter dialog de sélection de lot dans l'UI (si `gestion_lot = true`)
3. Conditionner dans `AjustementService.saveItems()` :

```java
boolean gestionLot = appConfigurationService.useLot().orElse(false);
if (qtyMvt > 0 && gestionLot && ajustement.getLotId() == null) {
    throw new GenericError("Numéro de lot obligatoire pour un ajustement IN");
}
if (qtyMvt > 0 && ajustement.getLotId() != null) {
    Lot lot = lotRepository.getReferenceById(ajustement.getLotId());
    lotService.adjustLotById(lot, qtyMvt);
    lotStockLocationService.credit(lot, storage, qtyMvt);
} else {
    lotService.adjustLots(produit, qtyMvt);
    lotStockLocationService.creditLastLot(produit, storage, qtyMvt);
}
```

---

## 6. Tests recommandés

| Scénario | Assertions |
|---|---|
| AJUSTEMENT_OUT -3, stock=5, lot A=5 | lot.currentQty=2, lsl.qty=2 |
| AJUSTEMENT_OUT -3, stock=2 (force) | effectiveDebit=2, lot.currentQty=0, lsl supprimé |
| AJUSTEMENT_IN +1, gestion_lot=false | lot dernier reçu +1, lsl dernier lot +1 |
| AJUSTEMENT_IN +1, gestion_lot=true, sans lotId | Exception levée |
| Clôture inventaire, lot=3 compté | lsl.qty=3 après réconciliation |
| Inventaire MAGASIN clôturé | lsl réserve purgée |

---

*Document généré suite à l'analyse du 2025-03-23*
