# Analyse comparative — Module Répartition de Stock

**Pharma-Smart vs. logiciels de gestion d'officine du marché français**

> Date d'analyse : mars 2026
> Module analysé : `features/commande/feature/repartition-stock`

---

## 1. Contexte & périmètre fonctionnel analysé

Le module **Répartition de Stock** de Pharma-Smart gère le transfert de médicaments entre les
zones de stockage d'une officine — principalement **le rayon** (espace de vente, `PRINCIPAL`) et
**la réserve** (`SAFETY_STOCK`). Il repose sur quatre sous-modules :

| Onglet | Composant | Rôle |
|---|---|---|
| Traçabilité | `AppRepartitionListComponent` | Historique paginé des mouvements rayon ↔ réserve |
| Réassort suggéré – Rayon | `AppSuggestionReassortComponent (RAYON)` | Suggestions générées automatiquement pour alimenter le rayon |
| Réassort suggéré – Réserve | `AppSuggestionReassortComponent (RESERVE)` | Suggestions pour alimenter la réserve |
| Transfert manuel | `AppManualRepartitionComponent` | Saisie libre multi-produits avec validation en lot |

**Logiciels de référence pour la comparaison :**

- **Winpharma** (PharmaRef / Alliadis) — leader historique en officine française
- **Lgpi / Pharmagest** (iSPharma, Observia) — deuxième acteur du marché
- **Allwin (Ordoclic / SmartRx)** — solution cloud nouvelle génération
- **Caduciel / PharmaSuccess** — solutions régionales, forte adoption PME
- **PharmaVitale (Pharmagest)** — solution dédiée aux groupements

---

## 2. Fonctionnalités couvertes : tableau comparatif

| Fonctionnalité | Pharma-Smart | Winpharma | Lgpi/Pharmagest | Allwin/Cloud | Caduciel |
|---|:---:|:---:|:---:|:---:|:---:|
| Transfert manuel rayon ↔ réserve | ✅ | ✅ | ✅ | ✅ | ✅ |
| Suggestions automatiques de réassort (rayon) | ✅ | ✅ | ✅ | ✅ | ⚠️ partiel |
| Suggestions automatiques de réassort (réserve) | ✅ | ⚠️ partiel | ✅ | ⚠️ partiel | ❌ |
| Transfert implicite lors d'une vente (force stock) | ✅ | ✅ | ✅ | ✅ | ⚠️ config. |
| Répartition automatique à la réception commande | ✅ FEFO | ✅ FIFO | ✅ FEFO | ✅ FEFO | ⚠️ FIFO |
| Traçabilité complète (opérateur, stocks avant/après) | ✅ | ✅ | ✅ | ✅ | ⚠️ partiel |
| Export PDF de l'historique | ✅ | ✅ | ✅ | ⚠️ CSV only | ❌ |
| Seuil minimum par produit (seuilMini) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Quantité maximum par emplacement (stockMaxi) | ✅ (auto-putaway) | ✅ | ✅ | ⚠️ | ❌ |
| Modification de quantité inline (AG Grid) | ✅ | ❌ modale | ❌ modale | ✅ | ❌ |
| Saisie multi-produits en une passe | ✅ | ⚠️ 1 par 1 | ⚠️ 1 par 1 | ✅ | ❌ |
| Recherche par scan code CIP | ✅ (autocomplete + scanner) | ✅ | ✅ | ✅ | ✅ |
| Interface multi-magasins | ⚠️ (prévu, `selectedStorageId`) | ✅ | ✅ | ✅ | ❌ |
| Validation par suggestion (workflow d'approbation) | ✅ | ❌ | ✅ | ⚠️ | ❌ |
| Alertes temps réel en cas de dépassement | ✅ (toast PrimeNG) | ⚠️ message popup | ✅ | ✅ | ⚠️ |

**Légende :** ✅ implémenté · ⚠️ partiel ou configurable · ❌ absent

---

## 3. Points forts de l'implémentation Pharma-Smart

### 3.1 Automatisation intelligente — double sens

La plupart des logiciels concurrents n'automatisent le réassort que dans le sens
**réserve → rayon**. Pharma-Smart gère les **deux sens** :

- `transfertImpliciteReserveVersRayon()` — déclenché lors d'une vente avec `forceStock=true`
- `autoPutawayRayonToReserve()` — déclenché à la réception d'une commande quand
  `qtyRayon > stockMaxi`

Cette bidirectionnalité atomique (dans la même transaction JPA que la vente/réception) est un
avantage technique fort par rapport aux solutions qui nécessitent une action manuelle de
régularisation.

### 3.2 Traçabilité détaillée avec stocks avant/après

L'entité `RepartitionStockProduit` enregistre systématiquement :
- stock source initial → final
- stock destination initial → final
- opérateur
- type de répartition (`AUTO` ou `MANUEL`)

Winpharma et Caduciel tracent l'opération mais n'enregistrent pas toujours le stock avant
déplacement, rendant les audits rétrospectifs moins précis.

### 3.3 Édition inline sur AG Grid

L'onglet *Réassort suggéré* expose les lignes dans un **AG Grid éditable inline** (colonne
`Qté à Réassortir`). Le pharmacien peut corriger les quantités sans ouvrir de modale. La
validation s'effectue côté client avant l'appel HTTP (`onCellValueChanged`) avec rollback
automatique en cas d'erreur serveur.

Les solutions Winpharma et Lgpi imposent une modale ou un formulaire séparé pour chaque
modification, ralentissant la saisie lors des traitements en lot.

### 3.4 Respect du FEFO sur l'auto-putaway

L'`autoPutawayRayonToReserve` mentionne explicitement le déplacement par `LotStockLocationService`
en ordre **FEFO** (First Expired, First Out). C'est l'exigence réglementaire pour les médicaments
soumis à date de péremption. Certains logiciels (Caduciel, versions anciennes Winpharma) gèrent
uniquement FIFO.

### 3.5 Workflow de validation des suggestions

Le système de `SuggestionReassort` (statut `OPEN` / `CLOSED`) introduit un **workflow de
validation** : une suggestion peut être créée, révisée (modification des quantités, suppression
de lignes) puis validée ou annulée. Lgpi/Pharmagest propose un mécanisme similaire ; Winpharma
et Allwin appliquent le réassort directement sans étape de relecture.

---

## 4. Points d'amélioration identifiés

### 4.1 ✅ [IMPLÉMENTÉ] Filtres avancés dans l'historique

Le backend `RepartitionStockResource` accepte les paramètres `userId`, `storageId` et
`typeRepartition` comme filtres. L'interface `repartition-stock.component.html` expose désormais :
- **Filtre type de mouvement** : Tous / Automatique / Manuel (`p-select` — `typeRepartitionOptions`)
- **Filtre opérateur** : sélecteur utilisateurs (affiché si > 1 utilisateur)
- **Filtre emplacement** : sélecteur stockages (affiché si > 1 emplacement)
- **Bouton Réinitialiser** pour remettre les filtres à zéro
- **Export PDF** tenant compte des filtres actifs
- La colonne **Type** (Auto/Manuel) a été ajoutée dans `repartition-list.component.html`

> **Impact fermé** : le pharmacien peut désormais filtrer l'historique par opérateur, type de
> mouvement et emplacement. Niveau de parité atteint avec Winpharma et Lgpi.

### 4.2 Pas de réconciliation d'inventaire intégrée

Le module répartition est découplé du module inventaire. Les solutions concurrentes (notamment
Lgpi iSPharma) proposent une vue **"écart inventaire → proposition de répartition"** : un
écart constaté lors d'un inventaire peut directement générer une suggestion de réassort.

### 4.3 ✅ [IMPLÉMENTÉ] Création de réserve depuis le transfert manuel — UX complétée

Le composant `manual-repartition` :
- Affiche une carte "Nouvelle réserve" visuelle quand `createNewDestination = true`
- Permet d'annuler la création via "Annuler" (retour à la sélection normale)
- Envoie `createNewDestination: true` dans `IManualRepartitionRequest`
- Le backend crée automatiquement le `StockProduit SAFETY_STOCK` si absent, puis effectue le
  transfert FEFO dans la même transaction `@Transactional`
- `validateRow()` accepte `createNewDestination = true` comme destination valide

### 4.4 ✅ [IMPLÉMENTÉ] Coloration conditionnelle des seuils dans AG Grid

La colonne **Stock Actuel** dans `suggestion-reassort.component.ts` applique désormais :
- 🔴 Rouge (`#dc2626`) + fond rose : stock = 0 (rupture)
- 🟠 Orange foncé (`#c2410c`) + fond orangé : `stockActuel < seuilMini` (critique)
- 🟡 Ambre (`#d97706`) + fond jaune : `stockActuel ≤ seuilMini × 1.3` (faible)
- 🟢 Vert (`#16a34a`) : stock OK
- La colonne **Seuil Mini** passe en rouge si le stock est en dessous
- Tooltip contextuel sur chaque cellule (⛔ Rupture / ⚠️ Critique / ⚠️ Faible / ✅ OK)

### 4.5 Pas d'indicateur de performance (KPI) sur les transferts

Les solutions haut de gamme (Pharmagest iSPharma, Allwin) proposent des tableaux de bord :
fréquence de réassort par produit, taux de rupture rayon, ratio stock rayon/réserve. Ces KPI
sont absents du module actuel.

### 4.6 Multi-magasins non finalisé

La logique de sélection d'emplacement (`selectedStorageId`) dans le composant manuel est
implémentée, mais elle ne couvre que les storages rattachés à l'utilisateur connecté. Pour les
groupements ou officines multi-sites, les solutions concurrentes permettent de gérer des
transferts entre points de vente physiques distincts — fonctionnalité hors périmètre actuel.

---

## 5. Comparaison architecturale

| Critère | Pharma-Smart | Solutions du marché |
|---|---|---|
| **Stack frontend** | Angular 20 standalone + PrimeNG + AG Grid | WPF/WinForms (Winpharma), React (Allwin), Angular 12-15 (Lgpi cloud) |
| **Gestion des états** | Signals Angular 20 (`resource`, `signal`, `computed`) | Souvent state global/Redux (cloud) ou pas de réactivité (client lourd) |
| **Édition inline** | AG Grid Community (libre) | Grilles propriétaires ou DataTables jQuery |
| **API** | REST Spring Boot, pagination server-side | REST ou SOAP (Winpharma legacy), GraphQL (Allwin) |
| **FEFO** | Natif via `LotStockLocationService` | Natif sur solutions modernes, optionnel sur anciennes |
| **Atomicité transactions** | `@Transactional` Spring, même transaction que la vente | Variable selon éditeur |
| **Export** | PDF (Flying Saucer + Thymeleaf) | PDF, CSV, Excel selon éditeur |
| **Desktop** | Tauri (Rust + WebView2) | Electron (Allwin), client lourd natif (Winpharma), web pur (Lgpi cloud) |

---

## 6. Synthèse

### Ce que Pharma-Smart fait aussi bien ou mieux que la concurrence

- Bidirectionnalité automatique des transferts (rayon ↔ réserve) avec atomicité transactionnelle
- FEFO natif sur la réception commande
- Workflow de validation des suggestions avec révision possible
- Édition inline AG Grid pour les suggestions (ergonomie supérieure)
- Traçabilité complète stocks avant/après avec type de répartition

### Ce que la concurrence fait que Pharma-Smart ne fait pas encore

- Filtres avancés dans l'historique (opérateur, type, emplacement) branchés côté UI
- Mise en forme conditionnelle sur les seuils (alerte visuelle stock bas)
- Réconciliation inventaire → suggestion de réassort
- KPI et tableaux de bord stock
- Création de réserve depuis le transfert manuel (logique partielle)

### Positionnement global

Le module `repartition-stock` de Pharma-Smart est **fonctionnellement complet** pour une officine
standard (une zone rayon + une réserve) et se situe au niveau des solutions mid-market comme
Lgpi/Pharmagest sur les fonctionnalités core. Il dépasse Caduciel et les anciens modules Winpharma
sur l'automatisation et l'ergonomie d'édition. Pour rivaliser avec les solutions haut de gamme
(Lgpi iSPharma Premium, Pharmagest iSirius), il manque principalement les KPI, les filtres
avancés côté UI et la réconciliation avec l'inventaire.
