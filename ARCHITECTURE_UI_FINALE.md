# Architecture UI Finale — Module Approvisionnement
## Pharma-Smart — Décisions actées
## Date : Mars 2026

> Document de référence synthétisant toutes les décisions issues de :
> - `ANALYSE_MODULE_SUGGESTIONS.md` (v12)
> - `ANALYSE_UX_WORKFLOW_SUGGESTIONS_COMMANDES_RECEPTION.md` (§9)
> - `ANALYSE_UX_COMMANDE_RECEPTION.md`

---

## 1. Structure de navigation finale

### 1.1 Sidebar (inchangée structurellement, 4 items au lieu de 6)

```
commande-home.component (NgbNav vertical pills)
│
├── ngbNavItem="DASHBOARD"       📊 Tableau de bord
│     └── <app-appro-unified-dashboard />     [KPI cliquables — chaque tuile navigue]
│
├── ngbNavItem="CYCLE_ACHAT"     📦 Cycle d'achat      [badge total urgences]
│     └── <app-cycle-achat />                          [NOUVEAU composant]
│
├── ngbNavItem="REPARTITION_STOCK"  ↕ Pilotage des stocks
│     └── <app-repartition-stock />
│
└── ngbNavItem="RETOUR_FOURNISSEUR" ↩ Retours fournisseur
      └── <app-retour-fournisseur />
```

**Items supprimés de la sidebar :**
- ~~`REQUESTED` — Commandes en cours~~ → intégré dans `CYCLE_ACHAT` tab 3
- ~~`RECEPTIONS` — Réceptions~~ → intégré dans `CYCLE_ACHAT` tab 4
- ~~`SUGGESTIONS` — Suggestions~~ → intégré dans `CYCLE_ACHAT` tabs 1 & 2

---

### 1.2 `AppCycleAchatComponent` — 5 tabs horizontaux (NOUVEAU)

```
app-cycle-achat.component
│
├── [💡 Réapprovisionnement  🔴N]   tab = 'REAPPRO'
├── [📋 Commandes à passer   🟠N]   tab = 'COMMANDES_A_PASSER'
├── [🛒 En cours             🔵N]   tab = 'COMMANDES_EN_COURS'
├── [🚚 Réceptions           🟢N]   tab = 'RECEPTIONS'
└── [📊 Analyse des stocks      ]   tab = 'ANALYSE'
```

**Règle badge :**
| Tab | Badge | Source |
|---|---|---|
| Réapprovisionnement | nb fournisseurs avec `Suggestion(SEMOIS, GENEREE)` | `suggestionService.countByStatut('GENEREE')` |
| Commandes à passer | nb fournisseurs avec `Suggestion(SEMOIS, VALIDEE)` | `suggestionService.countByStatut('VALIDEE')` |
| En cours | nb `Commande(REQUESTED)` | `commandeService.count({statuts:['REQUESTED']})` |
| Réceptions | nb `Commande(RECEIVED)` | `commandeService.count({statuts:['RECEIVED']})` |
| Analyse | — | — |

---

## 2. Détail de chaque tab

### Tab 1 — "Réapprovisionnement" (`REAPPRO`)

**Source de données :** `Suggestion(type=SEMOIS, statut=GENEREE)` depuis `SuggestionRepository`

**Composant :** `app-suggestion-home` (existant, enrichi)

**Layout :** Split-panel — liste fournisseurs (gauche) + panneau produits (droite)

```
┌──────────────────────────────────────────────────────────────────┐
│  Bandeau : "Généré le 26/03/2026 à 08h15"  [Actualiser]         │
├─────────────────┬────────────────────────────────────────────────┤
│ FOURNISSEURS    │ PRODUITS — Pharmalab                           │
│ ● Pharmalab 🔴3 │ Code │ Désignation │ Stock │ VMM │ Qté │ Conso │
│   COOPER    🟠1 │ ...  │ ...         │  🔒   │ ... │ ... │ ...   │
│   SANOFI       │                                                 │
│                │  [✅ Valider]  [🗑 Rejeter]                     │
└─────────────────┴────────────────────────────────────────────────┘
```

**Enrichissements vs état actuel :**
- Bandeau date de génération batch + bouton "Actualiser"
- Icône 🔒 sur les lignes `quantiteModifieeManuel = true`
- Colonne VMM (`vmm_calcule` depuis `semois_configuration`)
- Colonnes `consommationMensuelle` dynamiques (Mois enum)
- Actions : Valider (→ statut `VALIDEE`) | Rejeter (→ delete)

**Action principale :** `validerSuggestion(id)` → `statut = VALIDEE` → badge tab 2 +1

---

### Tab 2 — "Commandes à passer" (`COMMANDES_A_PASSER`)

**Source de données :** `Suggestion(type=SEMOIS, statut=VALIDEE)` depuis `SuggestionRepository`

**Composant :** `app-suggestion-home` avec `@Input() statut = 'VALIDEE'`
(même composant que tab 1, actions contextuelles différentes)

**Layout :** identique au tab 1 — split-panel fournisseurs + produits

```
┌──────────────────────────────────────────────────────────────────┐
│ FOURNISSEURS    │ PRODUITS — Pharmalab                           │
│ ● Pharmalab 🔴3 │ Code │ Désignation │ Stock │ VMM │ Qté        │
│   COOPER    🟠1 │ ...  │ ...         │       │ ... │ [  11  ]   │
│                │                                                 │
│                │  [🛒 Commander]  [📋 PDF]  [📊 CSV]  [↩ Revenir] │
└─────────────────┴────────────────────────────────────────────────┘
```

**Action principale :** `commander(id)` → crée `Commande(REQUESTED)` + supprime `Suggestion`
→ navigation automatique vers tab 3 "En cours"

---

### Tab 3 — "En cours" (`COMMANDES_EN_COURS`)

**Pattern :** Master-Detail dans le même tab

**État LIST (défaut) :**
- Composant : `jhi-commande-en-cours` (existant)
- Source : `Commande(statut=REQUESTED)`
- Action clé ajoutée : **[🚚 Réceptionner ▶]** sur chaque ligne
  → bascule en état DETAIL + switch vers tab 4 "Réceptions"
- Action [✏ Modifier] → état DETAIL avec `app-commande-requested` dans ce même tab

**État DETAIL — saisie commande :**
```
  Breadcrumb : En cours › Commande #234 · COOPER
  <app-commande-requested [commande]="commandeSelectionnee" />
  ← Retour (→ retour état LIST)
```

**Navigation depuis tab 2 :** après `commander()`, navigation auto vers tab 3 état LIST.

---

### Tab 4 — "Réceptions" (`RECEPTIONS`)

**Pattern :** Master-Detail dans le même tab (deux sous-tabs internes)

**État LIST — sous-tab "En cours" :**
- Composant : `app-bon-en-cours` (existant)
- Source : `Commande(statut=RECEIVED)`
- Action [🚚 Réceptionner ▶] → état DETAIL

**État LIST — sous-tab "Historique" :**
- Composant : `app-list-bons` (existant, corrigé)
- Source : tous statuts (plus CLOSED hardcodé) — filtre statut ajouté
- Total financier en pied de tableau

**État DETAIL — réception d'une commande :**
```
  Breadcrumb : Réceptions › BL-2024-042 · Pharmalab
  <app-commande-received [commande]="commandeEnReception()" />
  ← Retour à la liste (→ retour état LIST)
```

**Pourquoi pas un drawer :** `commande-received` a un layout `col-3 + col-9`,
15 colonnes, sous-modales (`FormLotComponent`, `PutawayModalComponent`). Nécessite 100% de largeur.
Le composant est déjà conçu pour être embarqué : `commande = input.required<ICommande>()`.

**Navigation depuis tab 3 :** clic "Réceptionner ▶" → bascule tab 4 + état DETAIL avec
la commande pré-chargée.

---

### Tab 5 — "Analyse des stocks" (`ANALYSE`)

**Source de données :** `mv_semois_suggestion` (vue matérialisée — lecture seule)

**Composant :** `app-semois-suggestions` (existant, renommé visuellement)

**Layout :** Dashboard KPI — pas de liste paginée, pas d'actions d'édition

```
┌──────────────────────────────────────────────────────────────────┐
│  🔴 3 Ruptures   🟠 12 Sous seuil   ✅ 85 OK   🔵 2 Surstock    │
│  ┌─────────────────────────────────────────────┐                 │
│  │ Classe A : ████░░ 2 ruptures / 5 produits   │                 │
│  │ Classe B : ██░░░░ 1 rupture / 10 produits   │                 │
│  └─────────────────────────────────────────────┘                 │
│  Top urgents (lecture seule, non commandable depuis ici)         │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. State management — `CommandCommonService` étendu

### 3.1 Nouveaux types

```typescript
// command-common.service.ts — ÉTAT FINAL

// Remplace SuggestionsSource ('FOURNISSEURS' | 'SEMOIS')
export type CycleAchatTab =
  | 'REAPPRO'
  | 'COMMANDES_A_PASSER'
  | 'COMMANDES_EN_COURS'
  | 'RECEPTIONS'
  | 'ANALYSE';

// Mode master-detail pour les tabs 3 et 4
export type TabViewMode = 'LIST' | 'DETAIL';
```

### 3.2 Signals

```typescript
@Injectable({ providedIn: 'root' })
export class CommandCommonService {

  // ── Navigation sidebar ──────────────────────────────────────────
  /** Tab actif dans la sidebar (DASHBOARD | CYCLE_ACHAT | ...) */
  commandPreviousActiveNav = signal<string>('DASHBOARD');

  // ── Navigation interne CycleAchat ───────────────────────────────
  /** Tab actif dans AppCycleAchatComponent */
  cycleAchatActiveTab = signal<CycleAchatTab>('REAPPRO');

  // ── Master-Detail tab "En cours" (tab 3) ────────────────────────
  commandesEnCoursMode = signal<TabViewMode>('LIST');
  commandeEnEdition    = signal<ICommande | null>(null);

  // ── Master-Detail tab "Réceptions" (tab 4) ──────────────────────
  receptionMode        = signal<TabViewMode>('LIST');
  commandeEnReception  = signal<ICommande | null>(null);

  // ── Commande courante (partagée entre tabs) ─────────────────────
  currentCommand       = signal<ICommande | null>(null);

  // ── Méthodes de navigation ──────────────────────────────────────

  /** Après commander() — bascule vers tab "En cours" */
  naviguerVersCommandesEnCours(): void {
    this.commandPreviousActiveNav.set('CYCLE_ACHAT');
    this.cycleAchatActiveTab.set('COMMANDES_EN_COURS');
  }

  /** Depuis "Réceptionner ▶" dans tab "En cours" */
  ouvrirReception(commande: ICommande): void {
    this.commandeEnReception.set(commande);
    this.receptionMode.set('DETAIL');
    this.cycleAchatActiveTab.set('RECEPTIONS');
  }

  retourListeReceptions(): void {
    this.commandeEnReception.set(null);
    this.receptionMode.set('LIST');
  }

  /** Depuis [Modifier] dans tab "En cours" */
  ouvrirSaisieCommande(commande: ICommande): void {
    this.commandeEnEdition.set(commande);
    this.commandesEnCoursMode.set('DETAIL');
  }

  retourListeCommandesEnCours(): void {
    this.commandeEnEdition.set(null);
    this.commandesEnCoursMode.set('LIST');
  }
}
```

---

## 4. Structure des composants — Arbre final

```
commande-home.component                   [sidebar NgbNav — 4 items]
│
├── app-appro-unified-dashboard           [KPI cliquables]
│
├── app-cycle-achat                       [NOUVEAU — 5 tabs horizontaux]
│   │
│   ├── [REAPPRO] app-suggestion-home
│   │     ├── app-suggestion-fournisseur-list   [gauche]
│   │     └── app-suggestion-produit-panel      [droite — AG Grid enrichi]
│   │
│   ├── [COMMANDES_A_PASSER] app-suggestion-home [statut=VALIDEE]
│   │     ├── app-suggestion-fournisseur-list
│   │     └── app-suggestion-produit-panel
│   │
│   ├── [COMMANDES_EN_COURS]
│   │   ├── [LIST]   jhi-commande-en-cours
│   │   └── [DETAIL] app-commande-requested     [input.required<ICommande>]
│   │                + breadcrumb
│   │
│   ├── [RECEPTIONS]
│   │   ├── [LIST]   sub-tabs
│   │   │     ├── app-bon-en-cours              [RECEIVED]
│   │   │     └── app-list-bons                 [tous statuts + total financier]
│   │   └── [DETAIL] app-commande-received      [input.required<ICommande>]
│   │                + breadcrumb
│   │
│   └── [ANALYSE] app-semois-suggestions        [renommé — lecture seule]
│
├── app-repartition-stock
└── app-retour-fournisseur
```

---

## 5. Flux de navigation complet — sans rupture

```
DÉMARRAGE APP
  → doRecalculateAllConfigurations() + creerSuggestionBatch()
  → Suggestion(SEMOIS, GENEREE) créées par fournisseur

     ↓

[Tab 1 — RÉAPPROVISIONNEMENT]
  Pharmacien : ajuste quantités
  → validerSuggestion(id)  →  statut = VALIDEE
  → badge tab 2 +1

     ↓ (clic tab 2)

[Tab 2 — COMMANDES À PASSER]
  Pharmacien : [🛒 Commander]
  → commander(id)  →  crée Commande(REQUESTED)
  → commandCommonService.naviguerVersCommandesEnCours()

     ↓ (navigation auto)

[Tab 3 — EN COURS]  mode LIST
  Commande visible avec statut REQUESTED
  Pharmacien : [🚚 Réceptionner ▶]
  → commandCommonService.ouvrirReception(commande)

     ↓ (navigation auto)

[Tab 4 — RÉCEPTIONS]  mode DETAIL
  app-commande-received [commande]="commandeEnReception()"
  Breadcrumb : Réceptions › BL-2024-042 · Pharmalab
  Pharmacien : saisit quantités → [✅ Valider]
  → finalizeSaisie()  →  statut = CLOSED, stock mis à jour
  → commandCommonService.retourListeReceptions()

     ↓ (retour état LIST)

[Tab 4 — RÉCEPTIONS]  mode LIST > Historique
  BL visible dans historique
  Total financier en pied de tableau
```

---

## 6. Décisions backend associées (v12)

| # | Décision | Fichier | Sprint |
|---|---|---|---|
| B1 | `TypeSuggession.SEMOIS` ajouté à l'enum | `TypeSuggession.java` | S1.1 |
| B2 | `SuggestionLine.quantiteModifieeManuel boolean` | `SuggestionLine.java` + migration `V1.3.9` | S0.2 + S1.0 |
| B3 | `SemoisBatchJobService.creerSuggestionBatch()` appelé après `doRecalculateAllConfigurations()` | `SemoisBatchJobService.java` (nouveau) | S1.2–1.4 |
| B4 | `@EventListener(ApplicationReadyEvent)` pour déclenchement au démarrage (app non 24/24) | `SemoisCalculationService.java` | S1.5 |
| B5 | `SuggestionRepository.filterByStatut(StatutSuggession)` | `SuggestionRepository.java` | S0.3 |
| B6 | `getAllSuggestion()` + paramètre `StatutSuggession` | `SuggestionProduitServiceImpl.java` | S0.3 |
| B7 | `suggestionAuto` décommissionné (`registerSynchronization` supprimé) | `SalesLineServiceImpl.java` | S0.1 |
| B8 | `createCommandesFromSemois()` supprimé | `SuggestionProduitServiceImpl.java` | S1.7 |
| B9 | `processBatch` + `BatchResult` déplacés dans `SemoisBatchJobService` | `SemoisBatchJobService.java` | S1.2 |

---

## 7. Décisions frontend à implémenter (priorités)

### Sprint 0 — Pré-requis (< 1 jour)

| # | Action | Fichier |
|---|---|---|
| F0.1 | `filterByStatut` + param `statut` dans `getAllSuggestion()` côté client | `suggestion.service.ts` |
| F0.2 | `consommationMensuelle` : ColDef AG Grid dynamiques | `suggestion-produit-panel.component.ts` |

### Sprint 1 — Shell `CycleAchat` (1 jour)

| # | Action | Fichier |
|---|---|---|
| F1.1 | Créer `AppCycleAchatComponent` (5 tabs, shell vide) | `cycle-achat/` (nouveau) |
| F1.2 | Remplacer REQUESTED + RECEPTIONS + SUGGESTIONS dans sidebar par `CYCLE_ACHAT` | `commande-home.component.html` |
| F1.3 | Étendre `CommandCommonService` avec `CycleAchatTab`, `TabViewMode`, nouveaux signals | `command-common.service.ts` |
| F1.4 | Brancher les composants existants dans leurs tabs (placement) | `cycle-achat.component.html` |

### Sprint 2 — Navigation entre tabs (1 jour)

| # | Action | Fichier |
|---|---|---|
| F2.1 | Navigation auto après `commander()` → tab 3 | `suggestion-home.component.ts` |
| F2.2 | Bouton "Réceptionner ▶" dans `commande-en-cours` → `ouvrirReception()` | `commande-en-cours.component` |
| F2.3 | Master-detail tab 4 : signal `receptionMode` + breadcrumb | `cycle-achat.component` |
| F2.4 | Master-detail tab 3 : signal `commandesEnCoursMode` + breadcrumb | `cycle-achat.component` |
| F2.5 | Badges compteurs sur les 4 tabs actifs | `cycle-achat.component.ts` |

### Sprint 3 — Enrichissements (1–2 jours)

| # | Action | Fichier |
|---|---|---|
| F3.1 | `suggestion-home` : bandeau date batch + bouton Actualiser + icône 🔒 | `suggestion-home.component` |
| F3.2 | `suggestion-home` reçoit `@Input() statut: StatutSuggession` | `suggestion-home.component.ts` |
| F3.3 | `app-semois-suggestions` renommé "Analyse des stocks", actioned'édition supprimées | `semois-suggestions.component` |
| F3.4 | `list-bons` : supprimer `CLOSED` hardcodé + filtre statut + total financier + état vide | `list-bons.component` |
| F3.5 | Dashboard KPI cliquables → `cycleAchatActiveTab` | `appro-unified-dashboard.component` |

---

## 8. Fichiers impactés — Récapitulatif

### Nouveaux fichiers

| Fichier | Rôle |
|---|---|
| `features/commande/feature/cycle-achat/cycle-achat.component.ts` | Shell 5 tabs — hub du cycle d'achat |
| `features/commande/feature/cycle-achat/cycle-achat.component.html` | Template tabs + master-detail |
| `features/commande/feature/cycle-achat/cycle-achat.component.scss` | Styles tabs horizontaux |

### Fichiers modifiés

| Fichier | Modification |
|---|---|
| `command-common.service.ts` | Nouveaux types `CycleAchatTab`, `TabViewMode`, 6 nouveaux signals |
| `commande-home.component.html` | Sidebar : 3 items → 1 item `CYCLE_ACHAT` |
| `commande-home.component.ts` | Import `AppCycleAchatComponent` |
| `suggestion-home.component.ts` | `@Input() statut`, navigation après `commander()` |
| `commande-en-cours.component.html` | Bouton "Réceptionner ▶" dans colonne Actions |
| `commande-en-cours.component.ts` | Appel `commandCommonService.ouvrirReception()` |
| `suggestion-produit-panel.component.ts` | ColDef `consommationMensuelle` dynamiques |
| `semois-suggestions.component.html` | Renommage visuel "Analyse des stocks", suppression actions |
| `list-bons.component.ts` | Filtre statut dynamique, total financier |
| `list-bons.component.html` | État vide, pied de tableau total |
| `appro-unified-dashboard.component` | KPI cliquables |

### Fichiers NON modifiés (réutilisés tels quels)

| Fichier | Réutilisé dans |
|---|---|
| `commande-received.component.ts/html` | Tab 4 mode DETAIL (déjà `input.required<ICommande>()`) |
| `commande-requested.component.ts/html` | Tab 3 mode DETAIL |
| `app-bon-en-cours.component` | Tab 4 mode LIST |
| `suggestion-fournisseur-list.component` | Tabs 1 & 2 |
| `suggestion-produit-panel.component` | Tabs 1 & 2 (enrichi colDef) |
| `reception-concordance.component` | Tab 4 mode DETAIL |
| `commande-status-bar.component` | Tabs 3 & 4 mode DETAIL |

---

## 9. Règles de conception — À respecter lors de l'implémentation

1. **`standalone: true` non requis** (Angular 21 — inutile, tous les composants sont standalone par défaut)
2. **Pas de `p-dialog`** → utiliser `NgbModal` pour les modales
3. **Pas de `*ngIf`** → utiliser `@if` (control flow Angular 17+)
4. **Pas de `styleClass`** → utiliser `class` ou `[class]`
5. **PrimeNG 20.x** → utiliser `p-button`, `p-table`, `p-tag`, etc.
6. **Breadcrumb** : toujours `<p-button [text]="true" icon="pi pi-arrow-left" />` suivi du séparateur `›`
7. **Tabs** : utiliser `NgbNav` (déjà utilisé dans tout le module) pour cohérence
8. **Badges** : classe `pharma-nav-badge` pour cohérence avec le reste de l'app
9. **`window.history.back()`** → toujours remplacer par `commandCommonService.retour...()` ou `router.navigate()`
10. **Signaux Angular** : `signal()`, `computed()`, `effect()` — pas de BehaviorSubject

---

*Document d'architecture finale — Pharma-Smart Module Approvisionnement*
*Sources : ANALYSE_MODULE_SUGGESTIONS.md (v12) + ANALYSE_UX_WORKFLOW_SUGGESTIONS_COMMANDES_RECEPTION.md (§9)*

