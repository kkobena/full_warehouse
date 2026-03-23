# Implémentation — Gestion Produit & Répartition/Transfert de Stock

> Suivi d'avancement de la refonte du panneau détail produit et de la gestion du stock.
> Référence legacy : `src/main/webapp/app/entities/produit/produit.component.ts`

---

## Table des matières

1. [État global](#état-global)
2. [Analyse comparative — Logiciels de référence](#analyse-comparative)
3. [Recommandations par priorité](#recommandations-par-priorité)
4. [P0 — Module Produit](#p0--module-produit-featuresproducts)
5. [P1 — Module Commande (réception)](#p1--module-commande-à-la-réception)
6. [P2 — Module Inventaire](#p2--module-inventaire-featuresinventory)
7. [Infrastructure existante](#infrastructure-existante-réutilisable)
8. [Design UX](#design-ux--tab-stock-niveau-2)

---

## État global

### Réalisé

| Phase | Sujet | Statut |
|-------|-------|--------|
| 0 | Scaffolding tabs (synthese, stock, fournisseurs, lots) | ✅ Terminé |
| 1 | Liste produits + menu contextuel ⋮ | ✅ Terminé |
| 2 | Colonnes Jours de stock + Marge % | ✅ Terminé |
| 3 | Légende code couleur stock + styling KPI synthèse | ✅ Terminé |
| 4 | Workflow désactivation (veille / réactivation) | ✅ Terminé |
| 5 | Sélection multiple + actions groupées | ✅ Terminé |
| 6 | Alerte péremption colorée (tab Synthèse) | ✅ Terminé |
| 7 | Correction vue SQL v_stock_rotation (valeurs négatives) | ✅ Terminé |

### À venir — P0 (module Produit)

| Phase | Sujet | Statut |
|-------|-------|--------|
| 8 | Tab Fournisseurs — CRUD complet | ✅ Terminé |
| 9 | Tab Stock — parité legacy + UX cartes visuelles | ✅ Terminé |
| 10 | Tab Stock — Couverture cible + seuil calculé (CMM × délai) | ✅ Terminé |
| 11 | Tab Stock — FEFO dans les transferts (tri lots péremption) | ✅ Terminé |
| 12 | Badges réglementaires L1/L2/ST | ⏸ Basse (champs backend manquants) |
| 13 | Tri colonnes liste par défaut (stock croissant) | ⏸ Basse (adaptation backend requise) |

### À venir — P1 (module Commande)

| Phase | Sujet | Statut |
|-------|-------|--------|
| C1 | Répartition automatique à la réception (règle rayon/réserve) | ✅ Terminé |
| C2 | Suggestion putaway basée sur classe ABC et stockMaxi | 🔲 À faire |

### À venir — P2 (module Inventaire)

| Phase | Sujet | Statut |
|-------|-------|--------|
| I1 | Tableau de bord répartition multi-produits | 🔲 À faire |
| I2 | Comptage cyclique par zone (inventaire partiel) | 🔲 À faire |
| I3 | Alertes push stock sous seuil (tableau de bord temps réel) | 🔲 À faire |

---

## Analyse comparative

### Logiciels de référence consultés

| Logiciel | Type | Marché |
|----------|------|--------|
| **Cegid PharmaStar / LGPI** | Officine | France (référence nationale) |
| **Pharmactuel / Kroll** | Officine | Québec / Amérique du Nord |
| **Odoo Inventory** | ERP généraliste | Multi-secteurs |
| **SAP Extended Warehouse Management** | ERP enterprise | Grands comptes |
| **Sage 100 Pharmacie** | PME | France |

---

### Tableau comparatif complet

| Fonctionnalité | PharmaStar | Odoo | SAP EWM | Sage 100 | Pertinence Pharma-Smart | Module cible |
|---|---|---|---|---|---|---|
| Transfert rayon ↔ réserve manuel | ✅ | ✅ | ✅ | ✅ | ⭐⭐⭐ Critique | P0 |
| Visualisation niveau stock (barre) | ✅ | ✅ | ✅ | ❌ | ⭐⭐⭐ Haute | P0 |
| Seuil mini calculé (CMM × délai fournisseur) | ✅ | ✅ | ✅ | ❌ | ⭐⭐⭐ Haute | P0 |
| FEFO dans les transferts (lots triés péremption) | ✅ | ✅ | ✅ | ✅ | ⭐⭐⭐ Haute (réglementaire) | P0 |
| Couverture cible (qty = X jours de stock) | ✅ | ✅ | ✅ | ❌ | ⭐⭐⭐ Haute | P0 |
| Répartition automatique à la réception | ✅ | ✅ | ✅ | ✅ | ⭐⭐⭐ Haute | P1 |
| Putaway basé sur classe ABC / famille | ❌ | ✅ | ✅ | ❌ | ⭐⭐ Moyenne | P1 |
| Tableau de bord répartition multi-produits | ✅ | ✅ | ✅ | ❌ | ⭐⭐⭐ Haute | P2 |
| Comptage cyclique par zone | ✅ | ✅ | ✅ | ❌ | ⭐⭐ Moyenne | P2 |
| Alertes push sous seuil (temps réel) | ✅ | ✅ | ✅ | ❌ | ⭐⭐ Moyenne | P2 |
| Transfert inter-pharmacies (multi-site) | ✅ | ✅ | ✅ | ❌ | ⭐ Basse (si réseau) | Backlog |
| Répartition prévisionnelle (saisonnalité) | ✅ | ❌ | ✅ | ❌ | ⭐ Basse (complexe) | Backlog |
| Règles de rangement par sous-emplacement | ❌ | ✅ | ✅ | ❌ | ⭐ Basse (overkill) | Backlog |

---

## Recommandations par priorité

### Pourquoi ces priorités ?

**P0 — Module Produit** : actions directement dans le panneau détail produit, visibilité immédiate pour le pharmacien au quotidien. Infrastructure déjà présente (modaux legacy, services existants). Effort faible, impact fort.

**P1 — Module Commande** : s'intègre dans le workflow de réception existant (`StockEntryServiceImpl`). Le moment de la réception est le point critique où le stock est distribué physiquement — c'est là que la règle rayon/réserve a le plus d'impact opérationnel.

**P2 — Module Inventaire** : fonctionnalités transversales qui concernent tous les produits en même temps (pas un produit individuel). Nécessitent une page dédiée dans `features/inventory` et potentiellement de nouveaux endpoints backend.

---

## P0 — Module Produit (`features/products`)

### Phase 8 — Tab Fournisseurs : CRUD complet

**Objectif** : parité avec le legacy `produit.component`.

| # | Action | Modal / Service | Condition |
|---|--------|-----------------|-----------|
| 8.1 | Ajouter fournisseur | `FormProduitFournisseurComponent` (NgbModal) | Toujours |
| 8.2 | Éditer fournisseur | `FormProduitFournisseurComponent` (NgbModal) | Par ligne |
| 8.3 | Supprimer fournisseur | Confirm → `produitService.deleteFournisseur(id)` | Par ligne |
| 8.4 | Toggle fournisseur principal | `produitService.updateDefaultFournisseur()` | Par ligne |

**Architecture** :
- `produit-fournisseurs-tab` injecte `NgbModal` + `ProduitService`
- Output `refreshRequested = output<void>()` → parent recharge le produit
- Mise à jour optimiste locale sur toggle (restaure si erreur backend)

**Fichiers** :
```
features/products/ui/produit-fournisseurs-tab/
  ├── produit-fournisseurs-tab.component.ts    ← +NgbModal, +ProduitService, +output
  ├── produit-fournisseurs-tab.component.html  ← toolbar + toggle switch + actions
  └── produit-fournisseurs-tab.component.scss
features/products/ui/produit-detail-panel/
  └── produit-detail-panel.component.ts        ← écouter (refreshRequested)
```

**Réutilise** : `entities/produit/form-produit-fournisseur/form-produit-fournisseur.component.ts`

---

### Phase 9 — Tab Stock : parité legacy + UX cartes visuelles

**Niveau 1 — Parité legacy**

| # | Action | Modal / Service | Condition |
|---|--------|-----------------|-----------|
| 9.1 | Ajouter stock réserve | `FormStockProduitComponent` (CREATE) | `stockProduits.length < 2` |
| 9.2 | Éditer seuils | `FormStockProduitComponent` (EDIT) | Par ligne |
| 9.3 | Transférer stock | `FormTransfertStockComponent` | Par ligne |

**Niveau 2 — UX enrichie (recommandé)**

| # | Fonctionnalité | Description |
|---|---------------|-------------|
| 9.4 | Cartes Rayon ↔ Réserve côte à côte | Barre de niveau `(qty / stockMaxi) × 100%` |
| 9.5 | Badge ⚠ sous seuil | Alerte visuelle si `qtyStock < seuilMini` |
| 9.6 | Bouton "Réapprovisionner rayon" | Pré-remplit `qty = stockMaxi − qtyRayon` depuis réserve |

**Architecture** :
- `produit-stock-tab` injecte `NgbModal` + `StockProduitService` + `RepartitionStockService`
- `isMono` lu depuis `ConfigurationService` (param `APP_GESTION_STOCK === 0`)
- Output `refreshRequested = output<void>()`

**Réutilise** :
- `entities/produit/form-transfert-stock/form-transfert-stock.component.ts`
- `entities/produit/form-stock-produit/form-stock-produit.component.ts`

---

### Phase 10 — Couverture cible + seuil calculé

**Couverture cible dans la modale de transfert**
```
Objectif : 30 jours de couverture rayon
CMM = 10 u/mois → quantité suggérée = 10 u
[Appliquer]  [Saisir manuellement]
```
→ Ajouter un champ "Jours cibles" dans `FormTransfertStockComponent` qui calcule
`qty = (joursCibles × CMM / 30) − qtyRayonActuel`.

**Seuil mini calculé**
Dans `FormStockProduitComponent` (mode EDIT), afficher à côté du champ seuilMini :
```
Seuil suggéré : 3 u  (CMM=10 u/mois × délai fournisseur=7 j)  [Appliquer]
```
→ Lire `produit.fournisseurProduit.delaiLivraison` (si champ disponible) ou
afficher la formule avec un tooltip.

---

### Phase 11 — FEFO dans les transferts

**Contexte réglementaire** : la réglementation française impose le principe FEFO (First Expired, First Out) dans les officines — les lots les plus proches de la péremption doivent être vendus/déplacés en priorité.

**Implémentation** :
- Dans `FormTransfertStockComponent`, si le produit a `dateperemption = true`, afficher la liste des lots disponibles triés par `datePeremption ASC`
- Pré-sélectionner automatiquement le lot le plus proche de la péremption
- Permettre de saisir la quantité lot par lot si nécessaire

**Dépendance** : endpoint `/api/lot?produitId={id}` déjà utilisé dans le panneau synthèse.

---

## P1 — Module Commande (à la réception)

> Localisation cible : `features/commande/feature/commande-received/` ou `StockEntryServiceImpl.java`

### C1 — Répartition automatique à la réception

**Problème actuel** : quand une commande est réceptionnée, tout le stock entrant va dans un seul emplacement. Le pharmacien doit ensuite transférer manuellement vers la réserve.

**Solution** : lors de la réception (`StockEntryServiceImpl.processStockEntry()`), appliquer une règle de rangement par produit :

```
Réception : 100 unités de Doliprane
Règle produit : remplir rayon jusqu'à stockMaxi, reste → réserve
→ Rayon reçoit : min(100, stockMaxi − qtyRayon) = min(100, 60−45) = 15 u
→ Réserve reçoit : 100 − 15 = 85 u
```

**UX** :
- Dans l'écran de réception (`commande-received`), afficher une prévisualisation de la répartition avant validation
- Option "Tout en rayon" / "Tout en réserve" / "Appliquer règle produit"
- Règle configurable par produit dans le tab Stock (champ "Règle de rangement")

**Backend** : modification de `StockEntryServiceImpl` + nouveau champ `putaway_rule` sur `stock_produit` ou `produit`.

---

### C2 — Suggestion putaway basée sur classe ABC

**Principe** (inspiré Odoo/PharmaStar) :
- Produits **classe A** : stock maxi rayon plus élevé (forte rotation → toujours disponible en rayon)
- Produits **classe C/D** : majoritairement en réserve (faible rotation → économise l'espace rayon)

**Implémentation** :
- Lors d'une première réception ou si `stockMaxi = 0`, suggérer un stockMaxi rayon basé sur la classe :
  - A_PLUS / A → `stockMaxi = CMM × 2` (2 mois en rayon)
  - B → `stockMaxi = CMM × 1`
  - C / D → `stockMaxi = CMM × 0.5`
- Affiché comme suggestion modifiable, pas comme valeur imposée

---

## P2 — Module Inventaire (`features/inventory`)

> Localisation cible : `src/main/webapp/app/features/inventory/`

### I1 — Tableau de bord répartition multi-produits

**Description** : vue globale de tous les produits avec leur état rayon/réserve, sans avoir à ouvrir chaque fiche individuellement.

**Fonctionnalités** :
- Tableau paginé avec colonnes : Produit, CIP, Rayon (qty/maxi), Réserve (qty), Alerte, CMM, Jours stock
- Filtres : "Sous seuil rayon", "Réserve vide", "Classe A sans réserve", "Péremption proche"
- Actions groupées : sélectionner N produits → "Réapprovisionner rayon en masse"
- Export PDF/Excel du tableau

**Relation avec l'existant** : complète et enrichit le module `repartition-stock` déjà présent dans `features/commande`. Pourrait être migré/fusionné dans `features/inventory`.

---

### I2 — Comptage cyclique par zone

**Principe** (PharmaStar, Odoo) : au lieu d'un inventaire complet annuel avec fermeture, compter un rayon / une lettre alphabétique / une famille par semaine.

**Fonctionnalités** :
- Définir des zones de comptage (par rayon, par famille, par lettre CIP)
- Planning de comptage : zone A cette semaine, zone B la semaine prochaine
- Interface de saisie simplifiée : liste de produits de la zone, saisie quantité constatée
- Écart automatique : `qty_constatée − qty_théorique` → génère un ajustement si validé
- Historique des comptages par zone

**Dépendance** : module inventaire existant (`ajustement/`), nécessite nouveau endpoint backend.

---

### I3 — Alertes stock sous seuil (tableau de bord temps réel)

**Description** : dashboard centralisé affichant en temps réel tous les produits nécessitant une attention.

**Alertes à afficher** :
| Type | Condition | Couleur | Action suggérée |
|------|-----------|---------|-----------------|
| Rupture rayon | `qtyRayon = 0` | 🔴 Rouge | Transférer depuis réserve |
| Sous seuil rayon | `qtyRayon < seuilMini` | 🟠 Orange | Réapprovisionner rayon |
| Réserve vide | `qtyReserve = 0 AND qtyRayon < seuilMini` | 🔴 Rouge | Commander |
| Péremption < 3 mois | lot proche expiration | 🔴 Rouge | Mettre en avant / retourner |
| Péremption < 6 mois | lot proche expiration | 🟡 Jaune | Surveiller |
| Sur-stockage | `qtyTotal > stockMaxi × 2` | 🔵 Bleu | Vérifier / retourner |

**UX** : widget sur le tableau de bord principal + page dédiée dans `features/inventory`.

---

## Infrastructure existante réutilisable

| Composant / Service | Localisation | Usage |
|--------------------|-------------|-------|
| `RepartitionStockService` | `features/commande/data-access/` | Transfert (processManualRepartition) |
| `StockProduitService` | `entities/produit/form-stock-produit/` | Créer/modifier stock produit |
| `FormTransfertStockComponent` | `entities/produit/form-transfert-stock/` | Modal transfert simple (P0) |
| `FormStockProduitComponent` | `entities/produit/form-stock-produit/` | Modal création/édition réserve (P0) |
| `FormProduitFournisseurComponent` | `entities/produit/form-produit-fournisseur/` | Modal fournisseur (P0) |
| `ManualRepartitionComponent` | `features/commande/feature/repartition-stock/ui/` | Référence UX transfert multi-produit (P2) |
| `SuggestionReassortComponent` | `features/commande/feature/repartition-stock/ui/` | Suggestions AG Grid (P2) |
| `RepartitionListComponent` | `features/commande/feature/repartition-stock/ui/` | Historique mouvements (P2) |
| `ProduitService` | `entities/produit/produit.service.ts` | updateDefaultFournisseur, deleteFournisseur |
| `NgbConfirmDialogService` | `shared/dialog/` | Confirmations suppression |
| `ConfigurationService` | `shared/configuration.service.ts` | isMono (APP_GESTION_STOCK) |
| `ProductsApiService` | `features/products/data-access/` | getLots (FEFO), getIndicateurs (CMM) |

---

## Design UX — Tab Stock Niveau 2

```
┌─────────────────────────────────────────────────────┐
│  [+ Ajouter réserve]              (si isMono)       │
├───────────────────────┬─────────────────────────────┤
│  📦 RAYON (Principal) │  🏭 RÉSERVE (Safety Stock)  │
│  ─────────────────    │  ─────────────────────────  │
│  Stock : 45 u    ⚠   │  Stock : 120 u              │
│  Seuil : 50 u        │  Seuil : 20 u               │
│  Maxi  : 60 u        │  Maxi  : 200 u              │
│  ████████░░ 75%       │  ████░░░░░░ 60%             │
│                       │                             │
│  [⇄ Transférer]       │  [⇄ Transférer]             │
│  [✏ Modifier seuils]  │  [✏ Modifier seuils]        │
├───────────────────────┴─────────────────────────────┤
│  [⬆ Réapprovisionner rayon]  +15 u depuis réserve   │
│  (couverture actuelle : 12 j → cible : 30 j)        │
└─────────────────────────────────────────────────────┘
```

---

## Backlog (hors scope actuel)

| Fonctionnalité | Raison du report |
|---|---|
| Transfert inter-pharmacies | Nécessite multi-site (configuration réseau) |
| Répartition prévisionnelle saisonnière | Nécessite modèle de prévision, complexité élevée |
| Règles de rangement par sous-emplacement | Overkill pour une officine standard |

---

## Références

- Legacy : `src/main/webapp/app/entities/produit/produit.component.ts`
- Répartition moderne : `src/main/webapp/app/features/commande/feature/repartition-stock/`
- Analyse initiale produit : `PRODUIT_FICHE_ANALYSE.md`
- Migration SQL stock rotation : `src/main/resources/db/migration/V1.3.5__fix_stock_rotation_negative_values.sql`
