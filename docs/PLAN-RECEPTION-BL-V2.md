# Plan d'implémentation — Reception-Home

> Statut : **Planification**
> Priorité : Haute
> Référence : Analyse comparative réception BL (avril 2026)

---

## Contexte et motivation

La réception actuelle (`commande-received`) est fonctionnelle mais souffre de frictions de saisie.
L'analyse comparative avec Pharmos, LEA et WinPharma a identifié la friction principale : le mode
grille impose de localiser chaque ligne visuellement — coûteux sur 60 à 200 lignes.

---

## Décisions architecturales (arrêtées)

### 1. Pas de nouveau menu — tab dans list-bons

`reception-home` n'est pas un menu séparé. Il s'intègre comme une **tab supplémentaire dans
`list-bons.component.html`** via le système de tabs custom déjà utilisé dans le module commande.

Structure actuelle de `list-bons` (états `@if / @else if`) :

```
editingReceived()         → commande-received plein écran
selectedClosed()          → consultation BL clôturé
retourWorkspaceBon()      → retour par ligne
reconciliationWorkspace() → rapprochement facture
(else)                    → liste des BLs
```

Cible : le bloc `editingReceived()` reste mais la réception devient une **tab** au même niveau
que la liste, avec navigation tabs en haut :

```
[ Bons de livraison ]  [ Réception en cours • OCP / BL-2026-04112 ]
```

La tab "Réception" s'active quand `editingReceived()` est défini. Cliquer sur "Bons de livraison"
revient à la liste sans fermer la réception (état conservé).

---

### 2. Réutiliser commande-received.component — toggle dans le header

Pas de nouveau composant racine. Le toggle séquentiel/grille est un **bouton dans le header
de `commande-received`** :

```
[ ◄ Retour ]  OCP Réunion · BL-xxx · 27/04/2026   [ ☰ Grille | ≡ Séquentiel* ]  [ actions… ]
```

`commande-received.component.ts` reçoit un signal `viewMode = signal<'grid' | 'sequential'>('sequential')`.
Le template bascule entre les deux zones selon ce signal.
Toute la logique métier, les endpoints, les services restent **identiques et inchangés**.

---

### 3. Saisie UG — Tab flow (arrêté)

Le champ UG est **toujours visible** dans le step 'qty', positionné juste après "Reçu" dans
l'ordre de tabulation.

| Geste clavier | Résultat |
|---|---|
| Tape qty → **Entrée** | Valide directement (0 UG) |
| Tape qty → **Tab** → tape UG → **Entrée** | Valide avec UG |
| Tape qty → **Tab** → laisse vide → **Entrée** | Valide (0 UG) |

**Pourquoi pas un toggle :** un champ conditionnel (`@if`) sort du tab order — Tab saute
par-dessus et le comportement devient imprévisible. UG toujours dans le DOM = Tab toujours
prévisible (référence : WCAG 2.1 §3.2.1, pattern Excel / Sage Gestion Commerciale).

**Option C (UG uniquement au lot) rejetée** : les produits sans gestion de lot n'auraient
aucun endroit pour saisir les UG. Crée deux comportements différents selon le produit.

**Distribution UG au prorata en step 'lot' :** `resetLotDraft()` pré-remplit `draftLotUg`
avec `remainingLotUg()`. Si plusieurs lots, la part de chaque lot est naturellement ajustée
par le restant décroissant — pas de calcul explicite requis côté utilisateur.

---

### 4. Séquentiel = mode par défaut

| Logiciel | Mode principal | Mode secondaire |
|---|---|---|
| Pharmos | Séquentiel | Grille |
| LEA | Séquentiel | Grille |
| WinPharma | Grille | Séquentiel option |
| Cégid | Séquentiel | — |
| commande-received (actuel) | Grille uniquement | — |

Le pharmacien est debout face au BL papier. Une ligne à la fois est le flux naturel.
→ `viewMode` initialisé à `'sequential'`, préférence sauvegardable en localStorage.

---

### 5. Panneau gauche et filtres en mode séquentiel

**LEFT PANEL (concordance + résumé)** : inutile en mode séquentiel.

- Le résumé BL monte dans le **header compact** du `commande-received` (déjà présent en partie)
- La concordance reste accessible via un bouton `[ Vue d'ensemble ]` → modale `ReceptionConcordanceComponent`
- En mode grille : panneau gauche conservé tel quel (comportement inchangé)

**FILTRES su-action-bar** : recomposés selon le mode :

| Élément | Mode séquentiel | Mode grille |
|---|---|---|
| Champ scan douchette | ✅ Proéminent | ✅ inchangé |
| Recherche texte | ✅ Sauter à une ligne | ✅ inchangé |
| Tri (UPDATE, alpha…) | ❌ Remplacé par auto-avance | ✅ inchangé |
| Filtre statut (ALL, NOT_EQUAL…) | ❌ Remplacé par toggle | ✅ inchangé |
| Toggle `Ignorer lignes complètes` | ✅ Actif par défaut | — |
| Toggle `[ ☰ Grille / ≡ Séquentiel ]` | ✅ | ✅ |

---

### 6. EDI import existant — à ne pas dupliquer

Le bouton `[ Importer réponse ]` avec tooltip `"Importer la réponse EDI / fichier du grossiste"`
**existe déjà** dans `commande-received.component.html`. Aucun travail requis sur ce point.

---

## Charte graphique — règles

Le composant hérite des imports SCSS existants de `commande-received` sans modification :

```scss
@import 'app/shared/scss/pharma-nav';
@import 'app/features/commande/commande-shared';
@import 'app/shared/scss/pharma-toolbar';
@import 'app/shared/scss/table-common';
```

### Classes et tokens à réutiliser tels quels

| Usage | Classe / token |
|---|---|
| Header | `.view-panel__header--detail`, `.view-panel__title`, `.view-panel__meta` |
| Actions header | `.cr-header-actions` |
| Barre filtres | `.su-action-bar`, `.su-action-bar__filters` |
| Scan | `.cr-scan-field`, `.cr-scan-badge`, `.cr-fmd-badge` |
| Footer totaux | `.cr-grid-footer` |
| Barre lots | `.cr-lot-progress`, `.cr-lot-bar`, `.cr-lot-bar__fill` |
| Badges | `.commande-badge[data-severity='…']` |
| Carte résumé BL | `.reception-summary-card` |
| Row AG Grid | `.pharma-row-danger`, `.pharma-row-warning`, `.pharma-row-provisional` |
| Couleurs | `$pharma-primary`, `$pharma-success`, `$pharma-danger`, `$gray-*` |
| Fond lot editor | `#f0f9ff` + bordure `#7dd3fc` |

### Nouveaux éléments CSS

Préfixe `rh-seq-*` (reception-home sequential) pour les éléments propres au mode séquentiel.
Couleurs toujours via variables SCSS, jamais en dur.

---

## Améliorations par priorité

### P0 — Déjà corrigé (patch sur commande-received)

- [x] Format date péremption `MM/AAAA` dans l'éditeur lot inline
- [x] UG-only lot entry (qty=0, ug>0) quand quantité couverte mais UG restantes
- [x] Bugs stock : double-comptage UG dans qtyStock, annulation asymétrique
- [x] Lot inflation : auto-merge des lots dupliqués à la réception

---

### P1 — Travaux reception-home

#### 1. Tab dans list-bons

**Fichiers modifiés :** `list-bons.component.html`, `list-bons.component.ts`, `list-bons.scss`

**HTML — tab bar en haut du `@else` (liste) et du `@if editingReceived()` :**

```html
<!-- Tab bar — visible dans les deux états liste et réception -->
<div class="lb-tab-bar">
  <button class="lb-tab" [class.lb-tab--active]="!editingReceived()" (click)="onRetourSaisie()">
    <i class="pi pi-inbox"></i> Bons de livraison
  </button>
  @if (editingReceived()) {
    <button class="lb-tab lb-tab--active">
      <i class="pi pi-truck"></i>
      {{ editingReceived()!.fournisseur?.libelle }} · {{ editingReceived()!.receiptReference }}
      <i class="pi pi-times ms-1" (click)="onRetourSaisie()"></i>
    </button>
  }
</div>
```

**CSS (dans `list-bons.scss`) :**
```scss
.lb-tab-bar {
  display: flex;
  align-items: stretch;
  gap: 2px;
  border-bottom: 2px solid $gray-200;
  background: $gray-50;
  padding: 0 1rem;
  flex-shrink: 0;
}

.lb-tab {
  padding: 0.45rem 1rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: $gray-500;
  border: none;
  background: transparent;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  transition: color 0.15s, border-color 0.15s;

  &:hover { color: $pharma-primary; }

  &--active {
    color: $pharma-primary;
    border-bottom-color: $pharma-primary;
    background: $pharma-white;
  }
}
```

---

#### 2. Toggle séquentiel / grille dans commande-received

**Fichier modifié :** `commande-received.component.ts` + `.html`

**Style — segment control identique au dashboard** (classes `custom-segment-control` /
`segment-track` / `segment-slider` / `segment-option`). Comme `dashboard-common.scss` ne peut
pas être importé tel quel (son `:host { display:block }` entrerait en conflit avec le
`:host { display:flex }` de `commande-received`), les règles du segment control sont copiées
dans `commande-received.component.scss` en substituant `$primary-blue` → `$pharma-primary`
et `$gray-border` → `$gray-200`. Rendu visuel identique, zéro conflit.

**TS — signal viewMode (✅ implémenté) :**
```typescript
protected readonly viewMode = signal<'grid' | 'sequential'>(
  (localStorage.getItem('reception-view-mode') as 'grid' | 'sequential') ?? 'sequential'
);

protected setViewMode(mode: 'grid' | 'sequential'): void {
  this.viewMode.set(mode);
  localStorage.setItem('reception-view-mode', mode);
}
```

**HTML — toggle dans `.cr-header-actions` (✅ implémenté) :**
```html
<div class="custom-segment-control">
  <div class="segment-track">
    <div class="segment-slider" [class.active-right]="viewMode() === 'grid'"></div>
    <button type="button" class="segment-option" [class.active]="viewMode() === 'sequential'"
            (click)="setViewMode('sequential')"
            pTooltip="Saisie ligne par ligne — mode nominal" tooltipPosition="bottom">
      <i class="pi pi-list"></i>
      <span>Séquentiel</span>
    </button>
    <button type="button" class="segment-option" [class.active]="viewMode() === 'grid'"
            (click)="setViewMode('grid')"
            pTooltip="Vue tableau complète" tooltipPosition="bottom">
      <i class="pi pi-table"></i>
      <span>Grille</span>
    </button>
  </div>
</div>
```

**HTML — zone principale conditionnelle :**
```html
<div class="cr-content">
  @if (viewMode() === 'sequential') {
    <!-- Panneau gauche : concordance en modale uniquement -->
    <app-reception-sequential
      [orderLines]="orderLines"
      [commande]="currentCommande"
      [showLotBtn]="showLotBtn"
      (lineValidated)="onLineValidated($event)"
      (scanRequest)="onScanReception()"
    />
  } @else {
    <!-- Mode grille existant — code actuel inchangé -->
    @if (showLeftPanel) {
      <div class="cr-panel-left"> … </div>
    }
    <div class="cr-panel-right">
      <ag-grid-angular … />
      <div class="cr-grid-footer"> … </div>
    </div>
  }
</div>
```

---

#### 3. Composant reception-sequential

**Fichier à créer :**
`features/commande/feature/commande-received/sequential/reception-sequential.component.ts`

C'est le seul composant véritablement nouveau. Il reçoit `orderLines` et `commande` en input,
délègue toute la persistance aux mêmes services (`DeliveryService`, `LotService`, `CommandeService`).

**Interface :**

```
┌─ rh-seq-body ─────────────────────────────────────────────────────────────────┐
│ [← Préc]  Ligne 15 / 67  [Suivante →]   [⚠ 3 lot(s) manquant(s)]            │
│ ───────────────────────────────────────────────────────────────────────────── │
│  DOLIPRANE 1000mg cp ×8              CIP 3400937091008    [⚠ PCB : 6]         │
│  ─────────────────────────────────────────────────────────────────────────    │
│  Stock actuel : 23   Commandé : 10   Après réception : 33                     │
│                                                                                │
│  Qté reçue  [ 10 ]    UG  [ 0 ]    Prix achat  [ 452 ]                       │
│  Nouveau PMP : 452 F  (actuel : 452 F — aucune variation)                     │
│  ─────────────────────────────────────────────────────────────────────────    │
│  N° Lot  [ ABC123  ]   Péremption  [ 06/2028 ]   [ + Ajouter ]               │
│  Lots : ████████░░ 8/10  ·  commande-badge--warn  2 restant(s)               │
│  ─────────────────────────────────────────────────────────────────────────    │
│                      [ Valider et passer à la suivante → ]                    │
└───────────────────────────────────────────────────────────────────────────────┘
```

**Comportement clavier :**
- Step 'qty' — champ **Reçu** : `Entrée` valide (0 UG), `Tab` va sur **UG**
- Step 'qty' — champ **UG** : `Entrée` valide, `Shift+Tab` revient sur Reçu
- `F8` / `F9` : ligne précédente / suivante
- `F12` : action primaire du step courant (Valider qty ou Ajouter lot)
- Toggle `Ignorer lignes complètes` (actif par défaut) : saute les lignes OK
- Scan douchette : même logique que le mode grille (`onScanReception()` partagé)

**Inputs / Outputs :**
```typescript
orderLines   = input.required<IOrderLine[]>();
commande     = input.required<ICommande>();
showLotBtn   = input<boolean>(false);
lineValidated = output<IOrderLine>();
scanRequest   = output<void>();
```

---

#### 4. Lot commun — propagation vers toutes les lignes

**Bouton `[ Appliquer à toutes les lignes sans lot ]`** visible dans le formulaire lot quand
un `numLot` + `expiry` sont saisis. Fonctionne dans les deux modes (séquentiel et grille).

**Implémentation :**
```typescript
// forkJoin des addLot() pour chaque ligne sans lot
forkJoin(lignesSansLot.map(l => this.lotService.addLot({
  numLot, expiryDate,
  quantityReceived: l.quantityReceivedTmp ?? l.quantityRequested,
  ugQuantityReceived: l.freeQty ?? 0,
  receiptItemId: l.orderLineId
}))).subscribe(…)
```

**Ajout backend (seul nouvel endpoint) :** `POST /api/commandes/{id}/lots/batch`
```json
[{ "orderLineId": 1, "numLot": "ABC123", "expiryDate": "2028-06-30",
   "quantityReceived": 10, "ugQuantityReceived": 1 }]
```

---

#### 5. Navigation clavier continue en mode grille

Après fermeture de l'éditeur lot d'une ligne complète, focus automatique sur la ligne suivante
nécessitant un lot (sans souris).

```typescript
// Dans onCollapseRow(), ajouter le paramètre autoAdvance
const currentIdx = this.orderLines.findIndex(l => l.id === line.id);
const next = this.orderLines.slice(currentIdx + 1).find(l => (l.lots?.length ?? 0) === 0);
if (next) setTimeout(() => this.onToggleLotExpand(next), 100);
```

---

### P2 — Améliorations productivité (post-mise en service)

#### 6. Pré-remplissage lot depuis scan DataMatrix

Quand un DataMatrix est scanné et `lotAutoCreated = false`, le formulaire lot s'ouvre pré-rempli
avec le N° lot (AI 10) et la date (AI 17) extraits du DataMatrix.

**Plan :**
1. Ajouter `lotNumero` et `lotExpiry` dans `IReceptionScanResult`
2. Backend : remplir ces champs dans `scanReception()` si DataMatrix décodé
3. Frontend : passer ces valeurs au formulaire lot via le contexte lors de l'auto-ouverture

---

#### 7. Aperçu PMP en temps réel

`PMP_new = (initStock × pmpActuel + qteRecue × prixAchat) / (initStock + qteRecue)`

Calcul purement frontend (aucun appel API). Affiché dans la carte séquentielle et en colonne AG
Grid. Nécessite d'exposer `initPmp` dans `IOrderLine` (DTO backend + requête).

---

#### 8. Format date `MM/AAAA` dans les autres formulaires

Appliqué dans l'éditeur lot inline (P0). À propager dans :
- `form-lot.component.ts` (modal lot existant)
- `list-lot.component.ts` (édition inline liste lots)

---

## Architecture — fichiers impactés

### Fichiers modifiés

| Fichier | Modification |
|---|---|
| `list-bons.component.html` | Tab bar + restructuration `@if/else if` |
| `list-bons.component.ts` | Pas de changement logique, ajout classes tab |
| `list-bons.scss` | Classes `.lb-tab-bar`, `.lb-tab` |
| `commande-received.component.ts` | Signal `viewMode`, méthode `toggleViewMode()` |
| `commande-received.component.html` | Bouton toggle, `@if viewMode()` sur la zone principale |

### Fichier créé

| Fichier | Rôle |
|---|---|
| `commande-received/sequential/reception-sequential.component.ts` | Mode séquentiel |

### Fichiers inchangés (réutilisation)

- `LotService`, `DeliveryService`, `CommandeService` — zéro modification
- `ScanDetectorService`, `ReceptionConcordanceComponent`, `PrixHistoriqueComponent`
- `LotInlineEditorComponent`, `CommandeReceivedActionsComponent`, `CommandeReceivedStatutComponent`
- Tous les SCSS partagés (`commande-shared`, `pharma-nav`, `pharma-toolbar`)

**Seul ajout backend :** `POST /api/commandes/{id}/lots/batch` (P1 item 4).

---

## Roadmap

| Phase | Contenu | Effort |
|---|---|---|
| **P0 — fait** | Date MMAA, UG-only lot, corrections stock | — |
| **Sprint 1** | Tab list-bons + toggle, squelette reception-sequential | 3 j |
| **Sprint 2** | Mode séquentiel complet (navigation, lot, scan) | 3 j |
| **Sprint 3** | Lot commun + endpoint batch, navigation clavier grille | 2 j |
| **Sprint 4** | PMP preview, pré-remplissage scan, MMAA dans form-lot | 2 j |

---

## Notes de décision

- Pas de nouveau menu — tab dans `list-bons` uniquement.
- Réutilisation de `commande-received.component.html` : le toggle est un bouton dans le header
  existant. Le template délègue à `reception-sequential` ou à l'AG Grid selon le signal.
- L'EDI import existe déjà dans `commande-received` — aucun travail requis.
- La préférence grille/séquentiel est persistée en `localStorage` par utilisateur.
- En mode séquentiel le panneau gauche est supprimé (280px libérés pour la carte produit).
  La concordance reste accessible via modale à la demande.
