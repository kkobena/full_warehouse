# Analyse UX — Suivi des Mouvements d'Article (Pharma-Smart)

> **Date :** Avril 2026
> **Scope analysé :**
> - `app/entities/produit/transaction/transaction.component.html` (page standalone)
> - `app/features/products/ui/produit-mouvements-tab/` (onglet dans la fiche produit — **composant de référence**)
> - `app/entities/produit/auditing/auditing.component.html`
> - `app/entities/produit/stat-sales/stat-sales.component.html`
> - `app/entities/produit/stat-delivery/stat-delivery.component.html`
> - `app/features/products/feature/produit-home/` (fiche produit — référence)

---

## Clarifications de contexte

> ✅ **En-tête produit** : déjà visible dans le toolbar du composant parent `ProduitDetailPanelComponent` — ne pas dupliquer dans l'onglet.
> ✅ **Composant de référence** : `ProduitMouvementsTabComponent` fait foi. `AuditingComponent` sera **supprimé** (pas fusionné).

---

## 1. État des lieux — Architecture actuelle

### 1.1 La page standalone (`/produits/transaction`) — À supprimer

```
TransactionComponent (sidebar verticale 4 items)
├── [Mouvements de stock]          → AuditingComponent              ← doublon de ProduitMouvementsTabComponent
├── [Récap Produits Vendus/Invendus] → RecapProduitVenduComponent   ← hors sujet (rapport global)
├── [Historique des ventes]        → StatSalesComponent
│       ├── [tab] Historique par jour
│       └── [tab] Historique par mois
└── [Historique des commandes]     → StatDeliveryComponent
        ├── [tab] Historique par jour
        └── [tab] Historique par mois
```

### 1.2 La fiche produit (`/produits`) — panel de droite (architecture cible)

```
ProduitDetailPanelComponent (onglets horizontaux)
├── Synthèse
├── Stock
├── Fournisseurs
├── Historique     → ProduitHistoriqueTabComponent (graphique Chart.js)
├── Mouvements     → ProduitMouvementsTabComponent  ✅ composant de référence
└── Déconditionnés (conditionnel)
```

---

## 2. Problèmes identifiés

### 2.1 🔴 Duplication critique

| Composant A | Composant B | Résultat |
|---|---|---|
| `AuditingComponent` | `ProduitMouvementsTabComponent` | **Tableau identique** — À supprimer |
| `StatSalesComponent` | `ProduitHistoriqueTabComponent` | Données partiellement communes |

### 2.2 🔴 Double saisie du produit (Violation UX — Nielsen H6)

`AuditingComponent`, `StatSalesComponent` et `StatDeliveryComponent` affichent chacun leur propre autocomplete produit alors que le contexte est transmis via `ProduitAuditingParamService`.

### 2.3 🔴 Navigation à 2 niveaux imbriquée (Violation — Nielsen H4)

Sidebar verticale → Onglets horizontaux internes dans stat-sales/stat-delivery.

### 2.4 🟠 Tableau de 13–14 colonnes (Nelson H8)

Colonnes avec abréviations techniques sans tooltip, scrolling horizontal inévitable sur 1366×768.

### 2.5 🟠 "Récap Produits Vendus/Invendus" mal positionné

Rapport global multi-produits inséré dans le contexte d'un article spécifique.

---

## 3. Analyse comparative — Logiciels de référence

| Critère | Pharma-Smart | Pharmagest iPharm | LGPI Winpharma | SAP B1 Pharma |
|---|---|---|---|---|
| **Localisation** | Page séparée + onglet fiche | Onglet fiche uniquement | Onglet fiche uniquement | Module dédié |
| **Raccourcis période** | ✅ Implémenté (P0) | Hier/7j/30j/1an | Ce mois/3mois/1an | Trimestre/Mois |
| **Filtre par type** | ✅ Implémenté (P0) | Cases à cocher | Liste déroulante | Multi-select |
| **Tooltips colonnes** | ✅ Implémenté (P0) | Oui | Oui | Oui |
| **Drill-down document** | ❌ Manquant | Oui | Oui | Oui |
| **Graphique stock** | Partiel | Sparkline + courbe | Courbe stock | Configurable |
| **Export** | PDF seulement | PDF + Excel + CSV | PDF + Excel | Excel natif |
| **Navigation imbriquée** | 2 niveaux | 0 (flat) | 1 niveau max | 1 niveau max |

---

## 4. Ce qui est superflu

| Élément | Justification |
|---|---|
| `TransactionComponent` en tant que page route séparée | Doublon total avec `produit-mouvements-tab` |
| Les 3 autocompletes produit dans `AuditingComponent`, `StatSalesComponent`, `StatDeliveryComponent` | Contexte produit déjà connu |
| `RecapProduitVenduComponent` dans la sidebar de suivi article | Rapport global ≠ suivi article |
| Navigation sidebar verticale de `TransactionComponent` | Double navigation inutile |
| 2 niveaux d'onglets (sidebar + sous-onglets dans stat-sales/stat-delivery) | Complexité inutile |

---

## 5. Priorités d'implémentation

### P0 — Raccourcis de période + Filtre par type de mouvement ✅ **IMPLÉMENTÉ**

> **Fichiers modifiés :**
> - `produit-mouvements-tab.component.ts` — Logique complète (shortcuts, filtre, visibilité colonnes)
> - `produit-mouvements-tab.component.html` — Barre de shortcuts, select type, `@if` sur chaque colonne
> - `produit-mouvements-tab.scss` — Styles barre de shortcuts
> - `produit-record.model.ts` — Ajout `mouvementStockIn` / `mouvementStockOut` manquants

**Raccourcis de période :**
- 5 boutons : `Hier`, `7 j`, `Ce mois`, `3 mois`, `1 an`
- Bouton actif mis en évidence (outlined primary vs text secondary)
- Réinitialisation du raccourci actif si l'utilisateur change une date manuellement

**Filtre par type de mouvement (visibilité colonnes) :**
- Select "Tous les types" + 13 options issues de `MouvementProduit.java`
- Filtre **côté client** : hide/show des colonnes via `isFieldVisible(field)`
- `outColspan` et `inColspan` calculés dynamiquement en getters
- `totalColspan` dynamique pour `emptymessage`
- Tooltips descriptifs sur chaque en-tête de colonne (accessibilité)
- Comportement : sélectionner "Vente" → ne montrer que la colonne Vente + colonnes fixes

---

### P1 — Amélioration tableau (color-coding + colonnes optionnelles) ✅ **IMPLÉMENTÉ**

> **Fichiers modifiés :**
> - `produit-mouvements-tab.component.ts` — `qty()` zéro→tiret, `rowClass()` color-coding
> - `produit-mouvements-tab.component.html` — `[class]="rowClass(row)"`, `qty()` sur toutes les cellules de mouvement, indicateur tendance dans "Stock final"
> - `produit-mouvements-tab.scss` — Classes `.mvt-row-positive/negative/inventory`, `.mvt-trend-up/down`, `.mvt-qty-zero`

**1.1 Color-coding des lignes :**
- 🟢 Fond vert léger → `afterStock > initStock` (entrées > sorties ce jour)
- 🔴 Fond rouge léger → `afterStock < initStock` (sorties > entrées ce jour)
- 🔵 Fond bleu léger → `storeInventoryQuantity != 0` (journée d'inventaire, priorité haute)

**1.2 Indicateur tendance (dans colonne Stock final) :**
- `pi pi-arrow-up` vert → stock a augmenté (avec tooltip "Stock en hausse")
- `pi pi-arrow-down` rouge → stock a diminué (avec tooltip "Stock en baisse")
- Aucun icône → stock inchangé

**1.3 Zéros masqués :**
- `qty(val)` retourne `'—'` pour 0/null (aucun mouvement ce jour)
- `[class.mvt-qty-zero]` → tiret en gris secondaire discret
- Colonnes `initStock` et `afterStock` gardent `| number` (toujours afficher le niveau de stock)

---

### P2 — Fonctionnalités manquantes

**2.1 Graphique d'évolution du stock** (Chart.js disponible dans le projet)
- Courbe du `afterStock` par date dans `ProduitHistoriqueTabComponent`
- Barres empilées : entrées (vert) / sorties (rouge)

**2.2 Export Excel**
```typescript
// Backend : Apache POI déjà disponible
// Endpoint : GET /api/produits/stat/transactions/excel
// Frontend : bouton "Excel" à côté du PDF
```

**2.3 Drill-down vers document source**
- Chaque ligne de mouvement devrait avoir un lien vers le bon de vente ou la livraison correspondante
- Nécessite que le backend expose un `referenceDocument` dans `ProduitAuditingState`

---

### P3 — Suppression / Nettoyage (dette technique)

**Objectif :** Supprimer les doublons et simplifier l'architecture.

**Actions :**
1. Supprimer les routes `/produits/transaction` et `/produits/:id/transaction` de `products.routes.ts`
2. **Supprimer** `AuditingComponent` (`entities/produit/auditing/`)
3. Retirer `RecapProduitVenduComponent` de `TransactionComponent` → déplacer vers le module Rapports
4. **Supprimer** `TransactionComponent` (`entities/produit/transaction/`)
5. Nettoyer `ProduitAuditingParamService` (devenu inutile avec la refonte)

---

## 6. Architecture cible recommandée

```
Fiche Produit (produit-home + split panel)
└── ProduitDetailPanel [en-tête produit déjà dans le toolbar]
    ├── [Synthèse]
    ├── [Stock]
    ├── [Fournisseurs]
    ├── [Mouvements]  ←── ProduitMouvementsTabComponent (✅ P0 enrichi)
    │       ├── Barre raccourcis période [✅ P0]
    │       ├── Filtre type mouvement    [✅ P0]
    │       ├── Tooltips colonnes        [✅ P0]
    │       ├── Color-coding lignes      [P1]
    │       ├── Colonne variation stock  [P1]
    │       └── Export PDF + Excel       [P2]
    ├── [Achats]      ←── StatDeliveryComponent (P3 intégration)
    └── [Déconditionnés]
```

---

## 7. Matrice de décision — Garder / Modifier / Supprimer

| Composant | Action | Statut |
|---|---|---|
| `TransactionComponent` | 🔴 **Supprimer la route** | P3 |
| `AuditingComponent` | 🔴 **Supprimer** | P3 |
| `StatSalesComponent` | 🟠 **Intégrer** comme onglet dans le panel | P3 |
| `StatDeliveryComponent` | 🟠 **Intégrer** comme onglet dans le panel | P3 |
| `RecapProduitVenduComponent` | 🟡 **Déplacer** vers module Rapports | P3 |
| `ProduitMouvementsTabComponent` | ✅ **Référence enrichie** | P0 ✅ |
| `ProduitHistoriqueTabComponent` | ✅ **Conserver + étendre** Chart.js | P2 |

---

## 8. Résumé exécutif

### Ce qui fonctionne bien ✅
- Structure "fiche produit + split panel" de `produit-home` est le bon modèle
- `ProduitMouvementsTabComponent` : patterns Angular modernes (signals, input.required)
- Totaux en footer bien implémentés
- **P0 implémenté** : raccourcis période, filtre type, tooltips colonnes, colspan dynamique

### Ce qui est superflu ❌
- La page route `/produits/transaction` entière (doublon parfait)
- Les 3 barres de recherche produit redondantes dans les sub-components
- Le composant "Récap produits vendus/invendus" dans ce contexte

### Ce qui reste à implémenter ⚠️
1. **P1** : Color-coding lignes + indicateur variation + valeurs nulles masquées
2. **P2** : Graphique stock + Export Excel + Drill-down documents
3. **P3** : Suppression `AuditingComponent`, `TransactionComponent`, routes obsolètes

### Effort estimé

| Phase | Effort | Impact | Statut |
|---|---|---|---|
| P0 — Raccourcis + Filtre type | 0.5 jour | Fort impact UX immédiat | ✅ **FAIT** |
| P1 — Color-coding + Tableau | 0.5 jour | Lisibilité | À faire |
| P2 — Fonctionnalités manquantes | 3-4 jours | Valeur métier | À faire |
| P3 — Suppression doublons | 1 jour | Réduction dette technique | À faire |
