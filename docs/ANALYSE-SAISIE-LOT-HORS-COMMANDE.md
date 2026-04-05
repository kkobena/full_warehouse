# Analyse — Saisie de lot hors commande (depuis la fiche produit)

> **Pharma-Smart** — Angular 20 / Spring Boot 4
> Date d'analyse : 2026-04-05 — **Mise à jour : 2026-04-05**
> Auteur : GitHub Copilot
> **Périmètre :** Saisie différée de lots pour des produits **déjà en stock**, sans obligation de lier le lot à une commande fournisseur (`OrderLine`). La saisie est accessible depuis la liste des produits et depuis l'onglet Stock (section FEFO). Elle est disponible pour **tous les produits en stock**, indépendamment du flag `checkExpiryDate`.
>
> ⚠️ Ce cas est **différent** du "Bon d'Entrée Diverse (BED)" qui crée une nouvelle entrée de stock sans commande — c'est une fonctionnalité distincte documentée séparément.

---

## Décisions de conception (2026-04-05)

| Décision | Détail |
|---|---|
| **Saisie non conditionnée par `checkExpiryDate`** | Tous les produits avec stock > 0 peuvent recevoir un lot, peu importe le flag `checkExpiryDate` |
| **Accès depuis la liste produits** | Menu contextuel (⋮) → "Saisir un lot" sur chaque ligne |
| **Accès depuis l'onglet Stock (FEFO)** | Bouton "+ Saisir un lot" dans l'en-tête de la section FEFO — visible aussi quand aucun lot n'existe encore |
| **Vue "Lots à saisir" (liste globale)** | ⏸️ **Mise en attente** — priorité donnée à la saisie depuis la fiche produit |
| **Sans lien commande** | Le lot est rattaché directement au `Produit` sans `OrderLine` |
| **Quantité ≤ stock** | La quantité saisie ne peut pas dépasser le stock total du produit |
| **Champs obligatoires** | `numLot` et `expiryDate` sont obligatoires ; `manufacturingDate` est optionnelle |
| **Aucun mouvement de stock** | La saisie crée uniquement un `Lot` record — `StockProduit.qtyStock` n'est pas modifié |

---

## Table des matières

1. [Constat — état actuel dans Pharma-Smart](#1-constat--état-actuel-dans-pharma-smart)
2. [Cas d'usage — quand saisit-on un lot a posteriori ?](#2-cas-dusage--quand-saisit-on-un-lot-a-posteriori-)
3. [Analyse comparative — logiciels de référence](#3-analyse-comparative--logiciels-de-référence)
4. [Tableau comparatif synthétique](#4-tableau-comparatif-synthétique)
5. [Contraintes architecturales Pharma-Smart](#5-contraintes-architecturales-pharma-smart)
6. [Ce qui existe déjà dans Pharma-Smart](#6-ce-qui-existe-déjà-dans-pharma-smart)
7. [Lacunes et recommandations](#7-lacunes-et-recommandations)
8. [Plan d'implémentation détaillé](#8-plan-dimplémentation-détaillé)
9. [Fichiers créés / modifiés](#9-fichiers-créés--modifiés)
10. [Estimation des efforts](#10-estimation-des-efforts)

---

## 1. Constat — état actuel dans Pharma-Smart

### 1.1 Quand se produit ce cas ?

```
Scénario typique :
1. Produit en stock (via commande ou autre entrée)
2. Le pharmacien veut enregistrer le(s) numéro(s) de lot
   et date(s) de péremption pour traçabilité ou module péremptions
3. Il accède à :
   - La liste des produits → menu ⋮ → "Saisir un lot"
   - La fiche produit → onglet Stock → section FEFO → "+ Saisir un lot"
4. Il saisit numLot + expiryDate + quantité (≤ stock)
5. Un Lot est créé, rattaché directement au Produit (sans OrderLine)
   → PAS de mouvement de stock
```

### 1.2 Changement fondamental vs l'ancienne logique

| Aspect | Ancienne logique | Nouvelle logique |
|---|---|---|
| Condition d'accès | `checkExpiryDate = true` requis | Tout produit avec stock > 0 |
| Lien obligatoire | `OrderLine` requise | Aucune commande requise |
| Point d'accès | API uniquement, pas d'UX | Menu produit + onglet FEFO |
| Validation quantité | Absente | qty ≤ stock total du produit |

---

## 2. Cas d'usage — quand saisit-on un lot a posteriori ?

| # | Cas d'usage | Fréquence | Accès |
|---|---|:---:|---|
| **UC1** | Produit reçu sans lot, traçabilité à compléter | Fréquent | Menu ⋮ liste ou bouton FEFO |
| **UC2** | Réception rapide en période de rush : lots "saisis plus tard" | Fréquent | Menu ⋮ liste ou bouton FEFO |
| **UC3** | Lots reçus endommagés (étiquette illisible) | Rare | Menu ⋮ liste ou bouton FEFO |
| **UC4** | Produit migré depuis ancien système, lots à renseigner | Ponctuel | Menu ⋮ liste ou bouton FEFO |
| **UC5** | Nouveau lot arrivé physiquement sans bon de commande | Occasionnel | Menu ⋮ liste ou bouton FEFO |

---

## 3. Analyse comparative — logiciels de référence

### 3.1 Pharmagest iConcept

**Menu d'accès :**
```
Gestion du stock
  └── Produits
        └── [Fiche produit → onglet Lots]
              └── [Associer à une réception ▼]     ← accès rapide
```

**Points clés :**
- Accessible depuis la fiche produit
- Badge d'alerte dans le tableau de bord
- Aucun mouvement de stock
- Somme des quantités lots = quantité reçue (validation)

---

### 3.2 Winpharma / LGPI / Caducée / Dispenso

Tous permettent la saisie de lot depuis la fiche produit ou depuis les réceptions clôturées.
→ **Pharma-Smart adopte l'approche centrée fiche produit** (comme Caducée / Dispenso).

---

## 4. Tableau comparatif synthétique

| Critère | Pharmagest | Winpharma | LGPI | Caducée | Dispenso | **Pharma-Smart (nouveau)** |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **Saisie lot a posteriori possible** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ **Implémenté** |
| **Lot lié à la réception source** | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ (lié au produit) |
| **Aucun mouvement de stock** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Accessible depuis fiche produit** | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ **Implémenté** |
| **Conditionné par checkExpiryDate** | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ **Retiré** |
| **Badge alerte lots manquants** | ✅ | ✅ | ✅ | ✅ | ✅ | ⏸️ En attente |
| **Vue globale "Lots à saisir"** | ✅ | ✅ | ✅ | ❌ | ✅ | ⏸️ En attente |
| **Widget dashboard** | ✅ | ✅ | ✅ | ❌ | ✅ | ⏸️ En attente |

---

## 5. Contraintes architecturales Pharma-Smart

### 5.1 Ce qui existe déjà ✅

```java
// POST /api/lot/add          → addLot(LotDTO)   — lié à une OrderLine
// POST /api/lot/edit         → editLot(LotDTO)  — modifie un lot existant
// DELETE /api/lot/{id}       → supprime un lot
// GET /api/lot/produit/{id}  → lots d'un produit (AVAILABLE)
```

### 5.2 Nouveau endpoint ajouté ✅

```java
// POST /api/lot/add-sur-produit → addLotSurProduit(LotDTO)
// → Crée un Lot lié directement au Produit (sans OrderLine)
// → Validation : qty ≤ stock, numLot + expiryDate obligatoires
// → PAS de mouvement StockProduit
```

### 5.3 Flux cible (sans mouvement de stock)

```
PRODUIT EN STOCK (totalQuantity > 0)
         │
         ▼
Accès via :
  a) Menu ⋮ liste produits → "Saisir un lot"
  b) Fiche produit → onglet Stock → FEFO → "+ Saisir un lot"
         │
         ▼
Modal LotSaisieProduitModalComponent
  - numLot (obligatoire, max 20 car.)
  - expiryDate (obligatoire, ≥ aujourd'hui)
  - manufacturingDate (optionnel, ≤ aujourd'hui)
  - quantityReceived (obligatoire, 1 ≤ qty ≤ stock)
         │
         ▼
POST /api/lot/add-sur-produit
  → Crée Lot(produit=X, numLot, expiryDate, quantity)
  → PAS de mouvement StockProduit
  → Module péremptions alimenté ✅
```

---

## 6. Ce qui existe déjà dans Pharma-Smart

### 6.1 API Backend (`LotServiceImpl` + `LotResource`)

| Endpoint | Méthode | Comportement | Status |
|---|---|---|:---:|
| `POST /api/lot/add` | `addLot(LotDTO)` | Crée un lot lié à `receiptItemId` (OrderLine). | ✅ Existe |
| `POST /api/lot/add-sur-produit` | `addLotSurProduit(LotDTO)` | Crée un lot lié directement au Produit. Validation qty. | ✅ **Nouveau** |
| `POST /api/lot/edit` | `editLot(LotDTO)` | Modifie numLot, expiryDate, qty d'un lot existant | ✅ Existe |
| `DELETE /api/lot/{id}` | `remove(id)` | Supprime un lot | ✅ Existe |
| `GET /api/lot/produit/{id}` | `findByProduitId(id)` | Liste les lots d'un produit | ✅ Existe |

### 6.2 Bug NPE corrigé ✅

`buildLotPerimePage()` dans `LotServiceImpl` plantait en NPE quand `lot.getOrderLine() == null`
(cas des lots créés hors commande). **Corrigé** : null-check avec fallback sur `produit.getFournisseurProduitPrincipal()`.

---

## 7. Lacunes et recommandations

### 7.1 Lacunes résolues ✅

| # | Lacune | Statut |
|---|---|:---:|
| L5 | `buildLotPerimePage()` NPE si `lot.getOrderLine() == null` | ✅ **Corrigé** |
| L1 partiel | Pas d'UI pour saisir un lot depuis la fiche produit | ✅ **Implémenté** |

### 7.2 Lacunes restantes (en attente)

| # | Lacune | Criticité | Impact |
|---|---|:---:|---|
| L1b | Pas de vue globale "Lignes de réception sans lots" | 🟡 Moyenne | ⏸️ Mise en attente |
| L2 | Pas de badge/alerte sur les réceptions clôturées avec lots manquants | 🟡 Moyenne | ⏸️ Mise en attente |
| L3 | Pas de validation : somme qty lots ≤ qty reçue dans `addLot()` (commande) | 🟡 Moyenne | Incohérence possible |
| L4 | Pas de flag `saisieAPosteriori` sur le lot | 🟢 Faible | Traçabilité audit |
| L6 | Pas de KPI dashboard "X lignes avec lots manquants" | 🟢 Faible | ⏸️ Mise en attente |

---

## 8. Plan d'implémentation détaillé

### Phase 0 — Bug NPE corrigé ✅ (0.5j)

**Fichier :** `LotServiceImpl.java` — `buildLotPerimePage()`

```java
// AVANT — NPE si lot.getOrderLine() == null
FournisseurProduit fournisseurProduit = lot.getOrderLine().getFournisseurProduit();

// APRÈS — null-safe avec fallback fournisseur principal
FournisseurProduit fournisseurProduit;
Produit produit;
if (lot.getOrderLine() != null) {
    fournisseurProduit = lot.getOrderLine().getFournisseurProduit();
    produit = fournisseurProduit.getProduit();
} else if (lot.getProduit() != null) {
    produit = lot.getProduit();
    fournisseurProduit = produit.getFournisseurProduitPrincipal();
    if (fournisseurProduit == null) return null; // skip
} else {
    return null; // skip
}
```

---

### Phase 1 — Backend : endpoint `addLotSurProduit` ✅ (0.5j)

#### 8.1.1 `LotDTO.java` — champ `produitId` ajouté

```java
/** Id du produit — utilisé pour la saisie de lot hors commande. */
private Integer produitId;
```

#### 8.1.2 `LotService.java` — méthode `addLotSurProduit` ajoutée

#### 8.1.3 `LotServiceImpl.java` — implémentation

```java
@Override
public LotDTO addLotSurProduit(LotDTO lot) {
    // Validation numLot, expiryDate, produitId
    // Produit récupéré par id
    // Validation qty ≤ totalStock
    // Prix depuis fournisseurProduitPrincipal (fallback product costAmount)
    // Pas d'OrderLine → saisie hors commande
    return new LotDTO(this.lotRepository.saveAndFlush(lotEntity));
}
```

#### 8.1.4 `LotResource.java` — endpoint ajouté

```java
@PostMapping("/lot/add-sur-produit")
public ResponseEntity<LotDTO> addLotSurProduit(@Valid @RequestBody LotDTO lot) {
    return ResponseEntity.ok(lotService.addLotSurProduit(lot));
}
```

---

### Phase 2 — Frontend : modal de saisie + intégration ✅ (1.5j)

#### 8.2.1 Modal `LotSaisieProduitModalComponent`

**Localisation :** `features/products/ui/lot-saisie-produit-modal/`

```
Modal NgbModal :
┌─────────────────────────────────────────────────────────────────────┐
│ 🏷 Saisir un lot — Doliprane 500mg                                   │
│─────────────────────────────────────────────────────────────────────│
│ ℹ Stock disponible : 120 unité(s) — La quantité ne peut dépasser    │
│   ce stock.                                                          │
│─────────────────────────────────────────────────────────────────────│
│ ┌──────────────────────┐  ┌───────────────────────────────────────┐ │
│ │ Numéro de lot *      │  │ Quantité *                            │ │
│ │ [L-2024-001_______]  │  │ [−] [   80  ] [+]                    │ │
│ └──────────────────────┘  └───────────────────────────────────────┘ │
│ ┌──────────────────────┐  ┌───────────────────────────────────────┐ │
│ │ Date péremption *    │  │ Date fabrication (optionnel)          │ │
│ │ [pharma-date-picker] │  │ [pharma-date-picker]                  │ │
│ └──────────────────────┘  └───────────────────────────────────────┘ │
│─────────────────────────────────────────────────────────────────────│
│                          [Annuler]  [💾 Enregistrer le lot]         │
└─────────────────────────────────────────────────────────────────────┘
```

- **Date picker** : `pharma-date-picker` (ng-bootstrap `NgbDateStruct`)
- **expiryDate** : `minDate = today`
- **manufacturingDate** : `maxDate = today`
- Validation réactive : bouton désactivé si `form.invalid || qty > stock`

#### 8.2.2 Intégration dans `produit-list.component.ts`

```typescript
// Nouveau type d'action
type ProduitMenuAction = ... | 'saisir-lots';

// Menu contextuel — entrée ajoutée
{
  label: 'Saisir un lot',
  icon: 'pi pi-tag',
  disabled: (produit.totalQuantity ?? 0) <= 0,
  command: () => this.emit('saisir-lots')
}
```

#### 8.2.3 Intégration dans `produit-stock-tab.component`

```html
<!-- Bouton dans l'en-tête FEFO (lots existants) -->
<p-button icon="pi pi-plus" label="Saisir un lot" (onClick)="onSaisirLot()" />

<!-- Section quand aucun lot enregistré mais stock > 0 -->
@else if ((produit().totalQuantity ?? 0) > 0) {
  <div class="fefo-no-lots">
    Aucun lot enregistré. <p-button label="Saisir un lot" ... />
  </div>
}
```

#### 8.2.4 Handler dans `produit-home.component.ts`

```typescript
case 'saisir-lots':
  this.openSaisirLot(produit);
  break;
```

---

### Phase 3 — Vue globale "Lots à saisir" ⏸️ EN ATTENTE

> Cette phase est **mise en attente** jusqu'à validation de la Phase 2.
> Elle concernait la vue liste filtrée des `OrderLine` clôturées sans lot (conditionnée par `checkExpiryDate = true`).
> Avec la nouvelle approche, la condition `checkExpiryDate` n'est **plus utilisée** pour conditionner la saisie.

Si cette phase est reprise ultérieurement, elle devra être redéfinie ainsi :
- Vue globale des produits avec stock > 0 et sans aucun lot enregistré
- Filtrée par famille, rayon, date
- Lien direct vers le modal de saisie

---

## 9. Fichiers créés / modifiés

### Backend

| Fichier | Action | Statut |
|---|:---:|:---:|
| `service/dto/LotDTO.java` | ✏️ Ajout champ `produitId` | ✅ Fait |
| `service/stock/LotService.java` | ✏️ Ajout méthode `addLotSurProduit()` | ✅ Fait |
| `service/impl/LotServiceImpl.java` | ✏️ Implémentation `addLotSurProduit` + fix NPE `buildLotPerimePage` | ✅ Fait |
| `web/rest/stock/LotResource.java` | ✏️ Endpoint `POST /lot/add-sur-produit` | ✅ Fait |

### Frontend

| Fichier | Action | Statut |
|---|:---:|:---:|
| `shared/model/lot.model.ts` | ✏️ Ajout `produitId` dans `ILot` | ✅ Fait |
| `features/products/data-access/services/products-api.service.ts` | ✏️ Ajout `addLotHorsCommande()` | ✅ Fait |
| `features/products/ui/lot-saisie-produit-modal/` | ➕ Nouveau composant modal (`.ts`, `.html`, `.scss`) | ✅ Fait |
| `features/products/ui/produit-list/produit-list.component.ts` | ✏️ Action `saisir-lots` dans le menu contextuel | ✅ Fait |
| `features/products/ui/produit-stock-tab/produit-stock-tab.component.ts` | ✏️ Méthode `onSaisirLot()` + import modal | ✅ Fait |
| `features/products/ui/produit-stock-tab/produit-stock-tab.component.html` | ✏️ Bouton FEFO + section "aucun lot" | ✅ Fait |
| `features/products/ui/produit-stock-tab/produit-stock-tab.component.scss` | ✏️ Style `.fefo-no-lots` + ajustement `.fefo-title` | ✅ Fait |
| `features/products/feature/produit-home/produit-home.component.ts` | ✏️ Handler `saisir-lots` + import modal | ✅ Fait |

### Non réalisé (en attente)

| Fichier | Action | Statut |
|---|:---:|:---:|
| `features/commande/feature/lots-a-saisir/` | Vue globale liste des lignes sans lots | ⏸️ En attente |
| `features/commande/commande.routes.ts` | Route `lots-a-saisir` | ⏸️ En attente |
| `repository/OrderLineRepository.java` | `findClosedLinesWithMissingLots()` | ⏸️ En attente |

---

## 10. Estimation des efforts

| Phase | Description | Effort backend | Effort frontend | Statut |
|---|---|:---:|:---:|:---:|
| **Phase 0** | Correction bug NPE `buildLotPerimePage()` | 0.5j | — | ✅ Fait |
| **Phase 1** | Backend : endpoint `addLotSurProduit` + LotDTO | 0.5j | — | ✅ Fait |
| **Phase 2** | Frontend : modal + menu + FEFO + home | — | 1.5j | ✅ Fait |
| **Phase 3** | Vue globale "Lots à saisir" | 1j | 2j | ⏸️ En attente |
| **TOTAL réalisé** | | **1j** | **1.5j** | — |

---

## Annexe — Récapitulatif des deux fonctionnalités distinctes

| Fonctionnalité | Quand ? | Stock modifié ? | `OrderLine` requise ? | Document généré ? |
|---|---|:---:|:---:|:---:|
| **Saisie de lot hors commande** ← CE DOCUMENT | Produit EN stock, lot non saisi | ❌ Non | ❌ Non (hors commande) | ❌ Non |
| **Bon d'Entrée Diverse (BED)** ← AUTRE DOCUMENT | Produit PAS encore en stock, sans commande | ✅ Oui | ❌ Non | ✅ Oui (BED numéroté) |

---

*Document créé le 2026-04-05 — mis à jour le 2026-04-05*
*Statut : ✅ Phase 0, 1, 2 implémentées — Phase 3 (vue globale) en attente*
