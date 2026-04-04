# Plan d'implémentation — Workflow Retour Fournisseur depuis le module Péremptions

> **Pharma-Smart** — Angular 20 / Spring Boot 4  
> Date d'analyse : 2026-04-04  
> Dernière mise à jour : 2026-04-04  
> Auteur : GitHub Copilot  
> Contexte : `confirmRetourFournisseurDialog()` dans `lot-perimes.component.ts` appelle actuellement `retirerStock(lot)` — comportement identique au retrait de stock — ce qui est incorrect. L'entité `RetourBon` existe côté backend mais n'est pas connectée au module péremptions.

---

## Table des matières

1. [Journal d'implémentation](#0-journal-dimplémentation) ← **NOUVEAU**
2. [Constat — bug actuel](#1-constat--bug-actuel)
3. [Analyse comparative — logiciels de référence](#2-analyse-comparative--logiciels-de-référence)
4. [Contraintes architecturales du domaine existant](#3-contraintes-architecturales-du-domaine-existant)
5. [Modèles d'implémentation possibles](#4-modèles-dimplémentation-possibles)
6. [Recommandation](#5-recommandation)
7. [Plan d'implémentation détaillé](#6-plan-dimplémentation-détaillé)
8. [Schéma de flux cible](#7-schéma-de-flux-cible)
9. [Fichiers à créer / modifier](#8-fichiers-à-créer--modifier)

---

## 0. Journal d'implémentation

### ✅ 2026-04-04 — Phase 0 : Refactorisation UX du module Retour Fournisseur

Avant de brancher la passerelle péremptions → RetourBon, le module `retour-fournisseur` existant a été restructuré pour accueillir le futur onglet péremptions et pour être cohérent avec le design system du projet.

#### 0.1 Création du fichier SCSS partagé `_subtab-bar.scss`

**Fichier :** `app/shared/scss/_subtab-bar.scss` ✅ **CRÉÉ**

Toutes les classes `su-*` (`.su-parent-toolbar`, `.su-source-tabs-bar`, `.su-tab`, `.su-tab--active`, `.su-tab-badge`, `.su-header-end`, `.su-config-btn-wrap`, `.su-title`) sont centralisées dans ce fichier unique.

Avant cette création, ces styles étaient dupliqués dans :
- `suggestions-unified.component.scss` (~150 lignes de CSS)
- `retour-fournisseur.scss` (~148 lignes de CSS)

Après refactorisation :
```
suggestions-unified.component.scss → 3 lignes (@import uniquement)
retour-fournisseur.scss             → 4 lignes (@import uniquement)
```

> **Note :** `.su-action-bar` et `.su-action-bar__filters/__actions` étaient déjà définis dans `_pharma-toolbar.scss` (ligne 1079). Ils ne sont donc pas dans `_subtab-bar.scss`.

#### 0.2 Restructuration de `AppRetourFournisseurComponent` en composant à 2 onglets

**Fichier :** `features/commande/feature/retour-fournisseur/retour-fournisseur.component.ts` ✅ **MODIFIÉ**

| Signal ajouté | Type | Rôle |
|---|---|---|
| `activeTab` | `signal<'EN_ATTENTE' \| 'HISTORIQUE'>` | Onglet actif |
| `countEnAttente` | `signal<number>` | Badge sur l'onglet "En attente" |
| `enAttenteStatutOptions` | `readonly` | Options filtre statut (VALIDATED/PROCESSING uniquement) |

Méthodes ajoutées :
- `setTab(tab)` — bascule l'onglet, réinitialise les filtres, recharge
- `loadCountEnAttente()` — query silencieuse `queryByStatut(VALIDATED, {size:1})` → `X-Total-Count`

Comportement de `loadAll()` modifié :
- Onglet `EN_ATTENTE` → filtre par `selectedStatut` (VALIDATED par défaut, PROCESSING possible)
- Onglet `HISTORIQUE` → force toujours `statut = CLOSED` (indépendamment du filtre)

**Fichier :** `features/commande/feature/retour-fournisseur/retour-fournisseur.component.html` ✅ **MODIFIÉ**

Structure HTML finale :
```
<div class="pharma-toolbar su-parent-toolbar">      ← shell header
  <div class="pharma-toolbar-header">               ← titre uniquement
  <div class="su-source-tabs-bar">                  ← 2 onglets
    <button class="su-tab" ...>En attente [badge]
    <button class="su-tab" ...>Historique

<div class="su-action-bar">                         ← filtres (pattern list-bons)
  <div class="su-action-bar__filters">
    p-iconfield | p-select (EN_ATTENTE only) | 2× p-datePicker
  <div class="su-action-bar__actions">
    [Nouveau retour]  [Rechercher]

<p-table ...>                                       ← tableau unique
  EN_ATTENTE : toutes les actions (edit/delete/send/EDI/réponse)
  HISTORIQUE : impression PDF + icône ✅ clôturé
  #emptymessage : bouton [Nouveau retour] dans pharma-empty-content
```

Suppressions :
- `<p-toolbar>` retiré (remplacé par `su-action-bar`)
- `ToolbarModule` retiré des imports Angular

---

## 1. Constat — bug actuel

### Code incriminé (`lot-perimes.component.ts`)

```typescript
// ❌ ACTUEL — le retour fournisseur fait exactement la même chose que "retirer du stock"
protected confirmRetourFournisseurDialog(lot: LotPerimes): void {
  this.confirmDialog.onConfirm(
    () => this.retirerStock(lot),          // ← appel identique à confirmRetirerDialog !
    "Retour fournisseur",
    `Voulez-vous initier un retour fournisseur pour le lot "${lot.numLot}" ?`
  );
}
```

### Différences attendues entre "Retirer du stock" et "Retour fournisseur"

| Aspect | Retirer du stock | Retour fournisseur |
|---|---|---|
| **Objet métier créé** | `ProductToDestroy` (mise en file destruction) | `RetourBon` + `RetourBonItem` |
| **Mouvement de stock** | `RETRAIT_PERIME` (sortie définitive) | `RETOUR_FOURNISSEUR` (sortie avec crédit attendu) |
| **Traçabilité financière** | Perte (PA × qty) | Avoir fournisseur attendu |
| **Document généré** | PV de destruction | Bon de Retour (BR) PDF |
| **Cycle de vie** | Terminal (détruit) | VALIDATED → PROCESSING → CLOSED |
| **Interlocuteur** | Interne (pharmacien) | Externe (fournisseur / grossiste) |

---

## 2. Analyse comparative — logiciels de référence

### 2.1 Pharmagest iConcept (leader France)

**Workflow retour fournisseur pour périmés :**

1. **Sélection des lots** dans le module "Gestion des périmés → Retours"
2. **Identification automatique de la commande source** via le numéro de lot (lookup `numLot → Réception → Commande`)
3. **Formulaire de retour** pré-rempli :
   - Fournisseur déduit automatiquement
   - Quantité = stock restant du lot (modifiable)
   - Motif = liste déroulante (périmé / proche péremption / détérioration / erreur livraison)
4. **Validation et génération automatique** :
   - BR numéroté imprimable
   - Sortie de stock immédiate (mouvement `RETOUR_FOURNISSEUR`)
   - Création d'un avoir fournisseur attendu dans le module comptabilité
5. **Suivi** : onglet "En attente de réponse fournisseur" avec statuts
6. **Réponse fournisseur** : saisie de l'avoir reçu (montant, date, référence avoir)

**Points clés** :
- Le lookup `numLot → Commande` est automatique (non bloquant si non trouvé)
- Si aucune commande trouvée → retour "hors commande" possible avec saisie manuelle du fournisseur
- Fonctionne sur périmés ET sur proches péremptions (J-30, J-60)

---

### 2.2 Winpharma

**Workflow retour fournisseur :**

1. Dans "Gestion stock → Périmés → Actions → Retourner au fournisseur"
2. Un dialogue demande : **fournisseur** (auto-détecté ou sélection manuelle) + **motif**
3. Le système cherche la réception d'origine via `numLot` — si trouvée, le BR référence la réception
4. Si non trouvée : création d'un "retour libre" (sans référence de réception)
5. Le BR est généré en PDF et peut être envoyé par email intégré
6. **Gestion de l'avoir** : dans le module "Comptabilité fournisseurs → Avoirs attendus"

**Différence notable vs Pharmagest** :
- Winpharma permet le retour **sans** lier à une commande (flexibilité maximale)
- La commande source est optionnelle, pas obligatoire

---

### 2.3 LGPI (Pharma Informatique)

**Workflow retour fournisseur :**

1. Depuis "Gestion péremptions", bouton "Créer un retour" dans la ligne du lot
2. **Résolution automatique** de l'`OrderLine` source via le numéro de lot
3. Si plusieurs réceptions trouvées → dialogue de sélection
4. Formulaire : motif (obligatoire), quantité (pré-remplie, modifiable), commentaire
5. Validation → `RetourBon` créé → stock mis à jour → notification générée
6. **Tableau de suivi** : onglet dédié "Retours en cours" dans le module commandes

**Point notable** : LGPI gère les retours partiels (retourner une partie du lot, garder le reste)

---

### 2.4 Caducée (Belgique)

**Workflow retour fournisseur :**

1. Module péremptions autonome, avec onglet "Retours fournisseur" intégré
2. Clic sur un lot → choix : "Détruire" ou "Retourner au fournisseur"
3. Pour "Retourner" : saisie du fournisseur + motif + quantité
4. **Pas de lien automatique avec la commande source** (Caducée simplifie)
5. Génération d'un bordereau de retour PDF basique
6. Suivi dans le module "Fournisseurs → Avoirs"

---

### 2.5 Dispenso (Belgique/France)

**Workflow retour fournisseur :**

1. Depuis la liste des périmés, action "Retour fournisseur"
2. **Lookup automatique** `Lot.numLot → OrderLine → Commande → Fournisseur`
3. Si commande trouvée → pré-remplie + message info
4. Si non trouvée → champ fournisseur manuel + avertissement
5. Motif obligatoire (référentiel de motifs paramétrable)
6. BR généré, stock décrémenté, avoir fournisseur créé en comptabilité

---

### 2.6 Tableau comparatif synthétique

| Fonctionnalité | Pharmagest | Winpharma | LGPI | Caducée | Dispenso | **Pharma-Smart actuel** |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| Lookup auto Lot → Commande | ✅ | ✅ (opt.) | ✅ | ❌ | ✅ | ❌ |
| Retour sans commande (libre) | ❌ | ✅ | ❌ | ✅ | ✅ | N/A |
| Motif de retour obligatoire | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| BR PDF généré automatiquement | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ (backend OK) |
| Retour partiel (qty modifiable) | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| Suivi avoir fournisseur | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ (ReponseRetourBon) |
| Onglet "Retours en cours" | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ **FAIT** |
| Intégration EDI (PharmaML) | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ (backend OK) |
| Workflow distinct de "détruire" | ✅ | ✅ | ✅ | ✅ | ✅ | ⬜ en cours |

---

## 3. Contraintes architecturales du domaine existant

### 3.1 Contrainte principale : `RetourBon.commande` est `@NotNull`

```java
// RetourBon.java
@ManyToOne(optional = false)
@NotNull
private Commande commande;  // ← OBLIGATOIRE dans le domaine actuel
```

Cette contrainte signifie qu'on **ne peut pas** créer un `RetourBon` sans référencer une `Commande`.

### 3.2 Chaîne de traçabilité `Lot → OrderLine → Commande`

```java
// Lot.java — la commande source est traçable !
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumns({
    @JoinColumn(name = "order_line_id", referencedColumnName = "id"),
    @JoinColumn(name = "commande_order_date", referencedColumnName = "order_date"),
})
private OrderLine orderLine;  // ← via orderLine.getId() → Commande
```

La chaîne `Lot → OrderLine → Commande` existe dans le modèle. On peut donc **résoudre automatiquement** la `Commande` depuis un `Lot`. C'est la bonne voie.

### 3.3 `RetourBon` déjà utilisé dans le module commandes

Le module `commande/retour_fournisseur/` possède déjà :
- `RetourBonService` (Angular) — endpoints CRUD complets
- `supplier-response-modal.component` — gestion de l'avoir
- `lot-selection-dialog.component` — sélection de lots

Ces composants sont réutilisables depuis le module péremptions.

### 3.4 Manque identifié : pas de endpoint "créer RetourBon depuis un Lot périmé"

Le backend n'a pas d'endpoint spécialisé qui prend un `lotId` et résout automatiquement la `Commande`. Cette logique est à ajouter côté service.

---

## 4. Modèles d'implémentation possibles

### Modèle A — Lookup automatique `Lot → Commande` (recommandé)

**Principe** : Ajouter un endpoint `POST /api/retour-bons/from-lot` qui :
1. Prend `lotId` + `motifRetourId` + `quantity` en entrée
2. Résout `Lot → OrderLine → Commande` automatiquement
3. Crée le `RetourBon` + `RetourBonItem` + décrémente le stock
4. Retourne le `RetourBonDTO` créé

**Avantages** :
- Compatible avec la contrainte `@NotNull` sur `Commande`
- Cohérent avec Pharmagest, LGPI et Dispenso (résolution auto)
- Réutilise la logique existante de `RetourBonServiceImpl.create()`
- Aucun changement de domaine requis

**Inconvénient** :
- Bloquant si le lot n'a pas d'`OrderLine` (lots saisis manuellement sans commande)

---

### Modèle B — Rendre `Commande` optionnelle dans `RetourBon`

**Principe** : Modifier `RetourBon.commande` pour qu'il soit `nullable`, et permettre un retour "libre" avec juste le `fournisseur` directement.

**Avantages** :
- Flexible, couvre tous les cas (lots sans commande source)
- Aligné avec Winpharma et Caducée

**Inconvénients** :
- Changement de domaine + migration Flyway requise
- Casse potentiellement la logique existante dans `RetourBonServiceImpl`
- Impact sur les rapports et l'EDI PharmaML (commande = référence EDI)

---

### Modèle C — Nouvelle entité `RetourPerimesBon` (découplage total)

**Principe** : Créer une entité spécifique aux retours de périmés, indépendante du cycle commande.

**Avantages** :
- Zéro impact sur l'existant
- Modèle métier plus précis (retour-périmé ≠ retour-erreur-livraison)

**Inconvénients** :
- Duplication de logique (nouveau service, nouveau controller, nouveaux DTO)
- Surcharge de la base de données
- Non aligné avec l'industrie (aucun logiciel de référence n'a deux entités séparées)

---

## 5. Recommandation

> **Modèle A (Lookup automatique) avec fallback Modèle B pour les lots sans commande**

### Stratégie recommandée :

1. **Chemin nominal** : `Lot → OrderLine → Commande` → `RetourBon` standard  
   → 90 % des cas (lots reçus via commande)

2. **Chemin de secours** : Si `Lot.orderLine == null` → proposer à l'utilisateur de **sélectionner manuellement la commande** du fournisseur (liste filtrée par fournisseur du lot)

3. **Plus tard (Phase 2)** : Rendre `Commande` optionnelle dans `RetourBon` pour les cas extrêmes (Modèle B léger)

**Raisons** :
- Cohérent avec le standard de l'industrie (4/5 logiciels)
- Aucun changement de domaine en Phase 1
- La chaîne `Lot → OrderLine` est déjà dans le modèle
- Le backend `RetourBonService` est déjà complet — on n'ajoute qu'un endpoint bridge

---

## 6. Plan d'implémentation détaillé

### Phase 0 — Refactorisation UX (✅ TERMINÉE)

> Restructuration du module `retour-fournisseur` existant, indépendamment de la passerelle péremptions.

| Tâche | Fichier | Statut |
|---|:---:|:---:|
| Créer `_subtab-bar.scss` (styles `su-*` partagés) | `shared/scss/_subtab-bar.scss` | ✅ |
| Simplifier `suggestions-unified.component.scss` | `suggestions-unified.component.scss` | ✅ |
| Simplifier `retour-fournisseur.scss` | `retour-fournisseur.scss` | ✅ |
| Ajouter onglets EN_ATTENTE / HISTORIQUE | `retour-fournisseur.component.ts` | ✅ |
| Badge comptage sur onglet "En attente" | `retour-fournisseur.component.ts` | ✅ |
| Remplacer `<p-toolbar>` filtres → `su-action-bar` | `retour-fournisseur.component.html` | ✅ |
| Bouton "Nouveau retour" dans `su-action-bar` | `retour-fournisseur.component.html` | ✅ |
| Bouton "Nouveau retour" dans `pharma-empty-content` | `retour-fournisseur.component.html` | ✅ |
| Supprimer `ToolbarModule` des imports | `retour-fournisseur.component.ts` | ✅ |

---

### Phase 1 — Backend : endpoint bridge `Lot → RetourBon` ⬜ À FAIRE (~3j)

#### 6.1.1 Nouveau DTO de requête

**Fichier :** `service/dto/RetourBonFromLotRequest.java` _(à créer)_

```java
public class RetourBonFromLotRequest {
    @NotNull Integer lotId;          // Lot périmé source
    @NotNull Integer motifRetourId;  // Motif obligatoire (référentiel)
    @NotNull Integer quantity;       // Quantité à retourner (≤ Lot.quantity)
    Integer storageId;               // Emplacement si multi-site
    String commentaire;              // Optionnel
}
```

#### 6.1.2 Nouvelle méthode dans `RetourBonService`

**Fichier :** `service/stock/RetourBonService.java`

```java
/**
 * Crée un RetourBon depuis un lot périmé.
 * Résout automatiquement la Commande source via Lot → OrderLine.
 */
RetourBonDTO createFromExpiredLot(RetourBonFromLotRequest request);
```

#### 6.1.3 Implémentation dans `RetourBonServiceImpl`

**Logique** :
```
1. Charger le Lot (avec fetch de OrderLine + Commande)
2. Résoudre la Commande : lot.getOrderLine().getCommande()
3. Si OrderLine null → lever RetourBonCommandeNotFoundException
4. Construire un RetourBonDTO avec :
   - commandeId = commande.getId()
   - commandeOrderDate = commande.getOrderDate()
   - retourBonItems = [ RetourBonItemDTO(lotId, qtyMvt, motifRetourId, prixAchat) ]
5. Déléguer à this.create(retourBonDTO)
```

#### 6.1.4 Nouveau endpoint REST

**Fichier :** `web/rest/commande/RetourBonResource.java`

```java
// POST /api/retour-bons/from-expired-lot
@PostMapping("/retour-bons/from-expired-lot")
public ResponseEntity<RetourBonDTO> createFromExpiredLot(
    @Valid @RequestBody RetourBonFromLotRequest request
) throws URISyntaxException
```

#### 6.1.5 Exception dédiée

**Fichier :** `service/errors/RetourBonCommandeNotFoundException.java` _(à créer)_

```java
// Levée quand un lot n'a pas d'orderLine résolvable
// → permet au frontend d'afficher un message "Commande source introuvable"
// et de proposer la sélection manuelle
```

---

### Phase 1 — Frontend : modal dédiée ⬜ À FAIRE (~3j)

#### 6.2.1 Nouveau modèle TypeScript

**Fichier :** `app/entities/gestion-peremption/model/retour-fournisseur-request.ts` _(à créer)_

```typescript
export interface RetourFournisseurRequest {
  lotId: number;
  motifRetourId: number;
  quantity: number;
  storageId?: number;
  commentaire?: string;
}

export interface MotifRetour {
  id: number;
  libelle: string;
}
```

#### 6.2.2 Service HTTP pour les motifs de retour

**Fichier :** `app/entities/gestion-peremption/motif-retour.service.ts` _(à créer)_

> **Note :** `ModifRetourProduitService` existe déjà dans `app/entities/motif-retour-produit/motif-retour-produit.service.ts` — réutiliser directement plutôt que créer un nouveau service.

```typescript
// Utiliser ModifRetourProduitService (déjà injectable, providedIn: 'root')
// GET /api/motif-retour-produits → IMotifRetourProduit[]
```

#### 6.2.3 Nouveau modal `RetourFournisseurPerimeDialogComponent`

**Fichier :** `app/entities/gestion-peremption/retour-fournisseur-perime-dialog/` _(à créer)_

**Composant Angular standalone (NgbModal), template :**

```
┌──────────────────────────────────────────────────────────────┐
│  ↩ Retour fournisseur — [Nom du produit]                     │
├──────────────────────────────────────────────────────────────┤
│  📦 Lot : L2401  │  🗓 Périmé le : 15/11/2025  │  Stock: 120  │
│                                                              │
│  Fournisseur (déduit automatiquement) : [CERP Rhin ________] │
│  Commande source  : [2024-BC-001238 — 15/03/2024 ✅ trouvée] │
│     OU                                                       │
│  ⚠️ Commande source introuvable — [Sélectionner manuellement ▼]│
│                                                              │
│  Motif de retour  : [Produit périmé ▼]  (liste référentiel)  │
│  Quantité         : [   120   ] / 120 max                    │
│  Commentaire      : [____________________________]  (opt.)   │
│                                                              │
│  💰 Avoir estimé : 120 × 450 FCFA = 54 000 FCFA             │
├──────────────────────────────────────────────────────────────┤
│                         [Annuler]  [✅ Créer le retour]      │
└──────────────────────────────────────────────────────────────┘
```

**Inputs du composant :**
- `lot: LotPerimes` — le lot périmé sélectionné
- `storageId?: number` — l'emplacement résolu

**Logique interne :**
1. Au `ngOnInit` → charger les motifs via `ModifRetourProduitService` (existant)
2. Afficher les infos du lot (produit, n° lot, date péremption, stock, fournisseur)
3. Calculer l'avoir estimé = `quantity × prixAchat`
4. À la validation → appeler `POST /api/retour-bons/from-expired-lot`
5. En cas de succès → `activeModal.close({ retourBon })`
6. En cas d'erreur 404 (commande introuvable) → basculer sur sélection manuelle

#### 6.2.4 Intégration dans `lot-perimes.component.ts`

```typescript
// ✅ CIBLE — confirmRetourFournisseurDialog corrigé
protected confirmRetourFournisseurDialog(lot: LotPerimes): void {
  if (this.isMultiLocation(lot) && !this.resolveStorageId(lot)) {
    this.notificationService.error(
      'Ce lot est présent dans plusieurs emplacements. Veuillez sélectionner un emplacement avant de retourner.',
      'Emplacement requis',
    );
    return;
  }

  const modalRef = this.modalService.open(RetourFournisseurPerimeDialogComponent, {
    size: 'lg',
    backdrop: 'static',
  });
  modalRef.componentInstance.lot = lot;
  modalRef.componentInstance.storageId = this.resolveStorageId(lot);

  modalRef.closed.subscribe(() => {
    this.notificationService.success('Retour fournisseur créé avec succès', 'Succès');
    this.loadPage();
  });
}
```

---

### Phase 2 — Enrichissements ⬜ À FAIRE (~3j)

#### 6.3.1 Sélection manuelle de commande (fallback)

Quand la commande source n'est pas résolue automatiquement :
- Charger les commandes du fournisseur (filtrées par `fournisseurId`)
- Afficher un `p-select` avec les commandes (référence + date + montant)
- L'utilisateur sélectionne la commande pertinente

#### 6.3.2 Retour groupé (multi-lots sélectionnés)

- Bouton "Retour fournisseur groupé" dans la `bulk-action-bar` de `lot-perimes`
- Un seul `RetourBon` avec plusieurs `RetourBonItem` (un par lot sélectionné)
- Contrainte : tous les lots doivent être du même fournisseur (validation)

#### 6.3.3 Gestion des lots sans commande source (Modèle B léger)

Migration Flyway requise :
```sql
-- V{version}__make_retour_bon_commande_optional.sql
ALTER TABLE warehouse.retour_bon
  ALTER COLUMN id_commande DROP NOT NULL,
  ALTER COLUMN commande_order_date DROP NOT NULL;
```

Modification domaine :
```java
// RetourBon.java
@ManyToOne(optional = true)
@JoinColumn(name = "id_commande", nullable = true)
private Commande commande;

@ManyToOne
private Fournisseur fournisseur;  // si commande absente
```

---

### Phase 3 — Suivi et reporting ⬜ À FAIRE (~4j)

#### 6.4.1 Badge comptage retours dans `lot-perimes`

KPI card "Retours en cours" :
- Valeur = `RetourBon` VALIDATED + PROCESSING
- Cliquable → navigation vers `/commande/retour-fournisseur`

#### 6.4.2 Rapport "Retours par fournisseur"

- Rapport mensuel/annuel des retours liés à des péremptions
- Export PDF + Excel
- Données : nb retours, valeur totale (PA × qty), taux d'acceptation, délai moyen

---

## 7. Schéma de flux cible

```
LOT PÉRIMÉ (lot-perimes.component)
         │
         ├── [Retirer du stock] ─────────────────→ ProductToDestroy
         │   (confirmRetirerDialog)                    ↓
         │                                     InventoryTransaction
         │                                     (RETRAIT_PERIME)
         │                                         ↓
         │                                    PV Destruction PDF
         │
         └── [Retour fournisseur] ──────────────→ ①
             (confirmRetourFournisseurDialog)
                      │
                      ↓
    RetourFournisseurPerimeDialogComponent (NgbModal)
                      │
        ┌─────────────┴──────────────────────┐
        │                                    │
        ▼ Lot.orderLine trouvé               ▼ Lot.orderLine null
    Commande pré-remplie                Sélection manuelle commande
                        │              OU Phase 2 : retour libre
                        └──────────┐
                                   ▼
                  ② POST /api/retour-bons/from-expired-lot
                     { lotId, motifRetourId, quantity, commentaire }
                                   │
                    RetourBonServiceImpl.createFromExpiredLot()
                                   │
                    Lot → OrderLine → Commande (résolution auto)
                                   │
                           RetourBon créé
                           + RetourBonItem
                           + Stock décrémenté
                           + InventoryTransaction(RETOUR_FOURNISSEUR)
                                   │
                                   ▼
                          ③ RetourBon PDF (BR)
                          (GET /api/retour-bons/{id}/pdf)
                                   │
                         ┌─────────┴─────────┐
                         ▼                   ▼
                   Imprimer BR         Envoyer EDI
                                       (PharmaML)
                                   │
                         ④ Suivi avoir fournisseur
                   POST /api/retour-bons/supplier-response
                   { retourBonId, reponseRetourBonItems: [{qty}] }
                                   │
                              RetourBon → CLOSED

SUIVI (module retour-fournisseur — ✅ UX restructurée)
     ├── Onglet "En attente" [badge orange] → VALIDATED + PROCESSING
     │     actions : modifier / supprimer / marquer en cours / EDI / réponse
     └── Onglet "Historique"                → CLOSED (lecture seule)
```

---

## 8. Fichiers à créer / modifier

### Infrastructure partagée

| Fichier | Action | Statut |
|---|:---:|:---:|
| `shared/scss/_subtab-bar.scss` | ➕ Créer | ✅ |

### Module `retour-fournisseur` (refactorisation UX)

| Fichier | Action | Statut |
|---|:---:|:---:|
| `retour-fournisseur.component.ts` | ✏️ Onglets + badge | ✅ |
| `retour-fournisseur.component.html` | ✏️ Shell + su-action-bar | ✅ |
| `retour-fournisseur.scss` | ✏️ Simplifier → imports | ✅ |
| `suggestions-unified.component.scss` | ✏️ Simplifier → imports | ✅ |

### Backend (Phase 1)

| Fichier | Action | Statut |
|---|:---:|:---:|
| `service/dto/RetourBonFromLotRequest.java` | ➕ Créer | ⬜ |
| `service/errors/RetourBonCommandeNotFoundException.java` | ➕ Créer | ⬜ |
| `service/stock/RetourBonService.java` | ✏️ `createFromExpiredLot()` | ⬜ |
| `service/stock/impl/RetourBonServiceImpl.java` | ✏️ Implémenter | ⬜ |
| `web/rest/commande/RetourBonResource.java` | ✏️ Endpoint `POST /from-expired-lot` | ⬜ |

### Frontend (Phase 1)

| Fichier | Action | Statut |
|---|:---:|:---:|
| `entities/gestion-peremption/retour-fournisseur-perime-dialog/` | ➕ Composant modal NgbModal | ⬜ |
| `entities/gestion-peremption/lot-perimes/lot-perimes.component.ts` | ✏️ `confirmRetourFournisseurDialog()` | ⬜ |

### Base de données (Phase 2)

| Fichier | Action | Statut |
|---|:---:|:---:|
| `db/migration/V{N}__make_retour_bon_commande_optional.sql` | ➕ Créer | ⬜ |

---

## 9. Estimation des efforts

| Phase | Description | Effort backend | Effort frontend | Statut |
|---|---|:---:|:---:|:---:|
| **Phase 0** | Refactorisation UX module retour-fournisseur | — | 2j | ✅ **Terminé** |
| **Phase 1** | Bridge lot→RetourBon + modal péremptions | 3j | 3j | ⬜ À faire |
| **Phase 2** | Retour groupé + fallback sélection manuelle | 1j | 2j | ⬜ À faire |
| **Phase 3** | Badge KPI + rapport fournisseur | 1j | 2j | ⬜ À faire |
| **TOTAL restant** | | 5j | 7j | **12j** |

---

## 10. Priorité et prérequis

### Prérequis avant Phase 1

1. ✅ `MotifRetourProduit` — entité + `ModifRetourProduitService` existants
2. ✅ `RetourBonService.create()` — existant et fonctionnel
3. ✅ `RetourBonService.export()` — BR PDF existant
4. ✅ `NgbModal` — déjà utilisé dans le projet
5. ✅ Module `retour-fournisseur` restructuré (Phase 0 terminée)
6. ⬜ Vérifier : `Lot.orderLine` toujours renseigné pour les lots de commande

### Vérification SQL avant démarrage Phase 1

```sql
-- Vérifier que les lots ont bien une orderLine associée
SELECT
    COUNT(*) as total_lots,
    SUM(CASE WHEN order_line_id IS NOT NULL THEN 1 ELSE 0 END) as avec_order_line,
    SUM(CASE WHEN order_line_id IS NULL THEN 1 ELSE 0 END) as sans_order_line
FROM warehouse.lot
WHERE statut != 'DESTROYED';

-- Lister les motifs de retour disponibles
SELECT id, libelle FROM warehouse.motif_retour_produit ORDER BY libelle;
```

---

*Document créé le 2026-04-04 — mis à jour le 2026-04-04*  
*Phase 0 : ✅ Terminée | Phase 1 : ⬜ À planifier*
