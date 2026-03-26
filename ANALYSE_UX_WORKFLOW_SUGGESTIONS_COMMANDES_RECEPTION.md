# Analyse UX — Workflow Suggestions → Commandes → Réception
## Pharma-Smart vs Logiciels de référence officine
## Date : Mars 2026

---

## 1. Cartographie de l'existant Pharma-Smart

### 1.1 Structure de navigation actuelle

```
Menu "Gestion commandes" (sidebar vertical — commande-home.component.html)
│
├── 📊 Tableau de bord          → ApproUnifiedDashboardComponent   (statique, pas d'action)
├── 🛒 Commandes en cours       → CommandeEnCoursComponent         (REQUESTED)
├── 🚚 Réceptions               → ReceptionHubComponent
│       ├── Bons en cours       → AppBonEnCoursComponent           (RECEIVED)
│       └── Historique          → AppListBonsComponent             (CLOSED)
├── 💡 Suggestions              → SuggestionsUnifiedComponent
│       ├── Par fournisseur     → SuggestionHomeComponent          (GENEREE/VALIDEE)
│       └── SEMOIS              → SemoisSuggestionsComponent       (vue MV, lecture seule)
├── ↕  Pilotage des stocks      → AppRepartitionStockComponent
└── ↩  Retours fournisseur      → AppRetourFournisseurComponent
```

### 1.2 Cycle de vie des objets — statuts actuels

**`Commande` (OrderStatut) :**
```
REQUESTED → RECEIVED → CLOSED → (ARCHIVED si clone hors-date)
```

**`Suggestion` (StatutSuggession) :**
```
GENEREE → EN_ATTENTE_VALIDATION → VALIDEE → COMMANDEE
                                          → (delete après commander())
```

### 1.3 Flux actuel entre les modules (tel qu'il existe)

```
[SUGGESTIONS]                [COMMANDES EN COURS]           [RÉCEPTIONS]
SuggestionHome               CommandeEnCours                ReceptionHub
     │                             │                              │
     │ commander(id)               │                              │
     │ → crée Commande(REQUESTED)  │                              │
     │ → delete Suggestion         │                              │
     │                             │                              │
     └─────────────────────────────▶ Commande apparaît ici       │
                                   │                              │
                                   │ [Démarrer réception]         │
                                   │ → navigue vers               │
                                   │   /commande-update/:id       │
                                   │ → statut → RECEIVED          │
                                   │                              │
                                   └──────────────────────────────▶ Bon apparaît ici
                                                                  │
                                                                  │ finalizeSaisie()
                                                                  │ → statut → CLOSED
```

### 1.4 Points de rupture dans le flux actuel 🔴

| Point de rupture | Description | Impact utilisateur |
|---|---|---|
| **Rupture 1** | Après `commander()` dans SuggestionHome, aucune navigation automatique vers "Commandes en cours" | Utilisateur ne sait pas que la commande a été créée |
| **Rupture 2** | "Commandes en cours" n'a pas de bouton "Réceptionner" contextuel par ligne | Utilisateur doit mémoriser le n° de commande, aller dans Réceptions, chercher manuellement |
| **Rupture 3** | "Bons en cours" (RECEIVED) et "Commandes en cours" (REQUESTED) sont dans des sections séparées de la sidebar | L'utilisateur ne voit pas la continuité : commande envoyée → livraison arrivée |
| **Rupture 4** | Tab SEMOIS dans Suggestions = lecture seule, pas de commander possible | Données SEMOIS visibles mais inutilisables directement |
| **Rupture 5** | `previousState()` = `window.history.back()` → retour imprévisible après réception | Désoriente l'utilisateur après une action |
| **Rupture 6** | Dashboard = KPI uniquement, aucune action contextuelle | Utilisateur doit quitter le dashboard pour agir |

---

## 2. Ce que font les logiciels de référence — Analyse comparative

### 2.1 Winpharma

#### Navigation
```
Menu principal : "Approvisionnement" (un seul menu, pas de sous-menus séparés)
│
├── Vue principale : Réapprovisionnement
│     ├── Gauche  : Liste fournisseurs avec compteur produits urgents (badge rouge)
│     └── Droite  : Suggestions par fournisseur (VMM, stock, qté suggérée, éditable)
│     └── Actions : [Valider tout] [Mettre au panier] [Générer bon de commande]
│
├── Commandes fournisseurs  ← onglet interne (même écran)
│     Filtrable par : En attente | Reçu partiel | Clôturé
│     Actions contextuelles : [Réceptionner] [Imprimer] [Envoyer EDI]
│
└── Réceptions (bons de livraison)
      Mode rapide : [Scan CIP] [Qté] [Valider ligne]
      Mode détaillé : toggle pour voir les prix
```

#### Workflow clé
1. Suggestions → Bon de commande : **1 clic** ("Générer bon de commande SEMOIS")
2. Commande → Réception : **1 clic** depuis la ligne de commande ("Réceptionner")
3. Réception → Clôture : **1 écran de synthèse** (pas de modales chaînées)
4. Navigation retour : breadcrumb `Approvisionnement > Commandes > Bon #42`

#### Principes UX distinctifs
- **Pas de navigation entre modules** : tout est dans "Approvisionnement" avec des onglets contextuels
- **Continuité visuelle** : une commande créée depuis les suggestions est immédiatement visible dans l'onglet commandes sans quitter la page
- **Actions contextuelles** sur chaque ligne (icônes inline, pas dans une toolbar globale)

---

### 2.2 Pharmagest Interactive (iSoft)

#### Navigation
```
Menu : "Achats & Stocks"
│
├── Propositions d'achat   ← calcul SEMOIS/P2
│     Table paginée avec : Fournisseur | Produit | Stock | VMM | Qté proposée | Couverture (jours)
│     Multi-sélection + "Créer commande" → génère Commande(BROUILLON) par fournisseur
│
├── Commandes fournisseurs
│     Filtres : Statut [Brouillon | Envoyée | Partiellement reçue | Clôturée]
│     Badge rouge sur "Commandes fournisseurs" si nb commandes en attente > 0
│     Actions ligne : [Voir] [Modifier] [Réceptionner ▶] [Annuler]
│     "Réceptionner ▶" ouvre directement le formulaire réception pré-rempli
│
└── Réceptions
      Mode standard : 7 colonnes (Produit | Qté cmd | Qté reçue | Écart | Statut | P.A | Action)
      Mode comptable : +4 colonnes prix (toggle)
      Mode scanner    : champ CIP + quantité uniquement
      Synthèse finale : 1 seul écran (anomalies + putaway + impression)
```

#### Workflow clé
1. Proposition → Commande : sélection multi-fournisseur + 1 bouton → plusieurs commandes créées simultanément
2. Commande → Réception : bouton "Réceptionner ▶" dans la liste = **0 navigation supplémentaire**
3. Réception → Clôture : **1 écran synthèse** avec résumé financier, anomalies et choix impression
4. Retour : breadcrumb sticky + bouton "Retour à la liste" toujours visible

#### Principes UX distinctifs
- **Statut always-visible** : chaque commande affiche son statut avec couleur (vert/orange/rouge)
- **Badge alerte** sur le menu "Commandes" si des réceptions sont en attente
- **Mode scanner** dédié pour la réception rapide (opérateur debout avec douchette)
- **Proposition non destructive** : une proposition peut être modifiée après avoir généré la commande

---

### 2.3 LGPI

#### Navigation
```
Menu : "Réapprovisionnement"
│
├── Analyse des besoins       ← vue analytique (lecture seule)
│     Indicateurs : Couverture | VMM | Stock objectif | Classe A/B/C
│
├── Suggestions de commande   ← panier de travail
│     Groupées par fournisseur
│     Inline edit quantités
│     [Transformer en commande] → Commande(DEMANDEE)
│
├── Commandes en cours        ← après transformation
│     Statuts : DEMANDEE | ENVOYEE | PARTIELLEMENT_RECUE | CLOTUREE
│     Accès direct réception depuis chaque ligne
│
└── Bons de livraison         ← historique
      Total financier visible en pied de tableau
      Filtre statut étendu (tous statuts)
```

#### Principes UX distinctifs
- **Séparation claire** entre "analyse" (lecture) et "suggestion" (travail) — inspirant pour Pharma-Smart
- **Workflow linéaire** : chaque étape a un seul bouton "étape suivante" visible
- **Total financier** en bas de chaque liste (commandes + BL)
- **Aucune pagination** dans le formulaire de réception (virtual scroll)

---

### 2.4 Alliadis (ADP / Winpharma Cloud)

#### Navigation
```
Menu : "Gestion des achats"
│
├── Tableau de réassort       ← SEMOIS + alertes combinées
│     [Ajouter au panier]     → crée Panier par fournisseur
│
├── Mes paniers               ← suggestions éditables (= Suggestion dans Pharma-Smart)
│     Paniers actifs par fournisseur (badge nb produits)
│     [Passer la commande]    → Commande(EN_ATTENTE)
│
├── Commandes à passer        ← validées, pas encore envoyées
│     [Envoyer] (EDI/email)   → Commande(ENVOYEE)
│
├── Commandes envoyées        ← attendant livraison
│     [Réceptionner]         → formulaire réception
│
└── Réceptions                ← BL en cours + historique
```

#### Principes UX distinctifs
- **5 étapes explicites** dans le menu = le pharmacien sait exactement à quelle étape il en est
- **Paniers** = concept familier (e-commerce) → adoption rapide
- **Commandes à passer** = onglet dédié pour les validées non encore envoyées (= notre futur `COMMANDES_A_PASSER`)
- **Chaque onglet menu = 1 statut** → compréhension immédiate

---

### 2.5 SurOrdonnance (moderne, cloud)

#### Navigation
```
Tableau de bord unifié
│
├── Widget "Ruptures" (cliquable → filtre direct)
├── Widget "À commander" (cliquable → liste suggestions)
├── Widget "En attente de livraison" (cliquable → commandes)
└── Widget "À réceptionner" (cliquable → réceptions)
```

**Pas de navigation sidebar** : tout part du dashboard. Chaque widget est un CTA.

---

## 3. Tableau comparatif — Interactions entre les 3 modules

| Critère UX | Winpharma | Pharmagest | LGPI | Alliadis | Pharma-Smart actuel |
|---|---|---|---|---|---|
| **Navigation entre modules** | Un seul onglet | 1 clic depuis commande | 1 clic depuis commande | Menu multi-étapes | ❌ Navigation sidebar manuelle |
| **Suggestion → Commande** | Auto-groupé par fournisseur | Multi-sélection + 1 bouton | "Transformer" | "Passer la commande" | ✅ commander() mais sans nav |
| **Commande → Réception** | Bouton "Réceptionner" inline | Bouton "Réceptionner ▶" inline | Accès direct liste | "Réceptionner" inline | ❌ Naviguer manuellement |
| **Réception → Clôture** | 1 écran synthèse | 1 écran synthèse | 1 page | Modal unique | ❌ 3 modales chaînées |
| **Badge alertes sur menu** | ✅ | ✅ | ✅ | ✅ | ❌ aucun badge |
| **Retour contextuel** | Breadcrumb | Breadcrumb sticky | Bouton retour fixe | Breadcrumb | ❌ window.history.back() |
| **Total financier liste BL** | ✅ | ✅ | ✅ | ✅ | ❌ absent |
| **Mode scanner réception** | ✅ | ✅ | ❌ | ✅ | ❌ absent |
| **Dashboard actionnable** | ✅ (cliquable) | ✅ (cliquable) | ✅ | ✅ | ❌ lecture seule |
| **Statut commande visible** | ✅ couleur | ✅ couleur + texte | ✅ | ✅ | 🟡 partiel |
| **Commandes à passer (VALIDEE)** | "Panier" | "Brouillon" | Étape séparée | Onglet dédié | ❌ absent (prévu v12) |
| **BL avec tous statuts** | ✅ | ✅ | ✅ | ✅ | ❌ CLOSED seulement |

---

## 4. Analyse des patterns de menu — Conception UX

### 4.1 Pattern 1 : Menu linéaire par étapes (Alliadis, LGPI)

```
[Analyse] → [Suggestions] → [Commandes à passer] → [Commandes envoyées] → [Réceptions]
```

**Forces :**
- Le pharmacien comprend d'un coup d'œil à quelle étape se trouve chaque article
- Chaque élément de menu = 1 statut = 1 action possible
- Apprentissage rapide (onboarding)

**Faiblesses :**
- Plus de clics pour passer d'une étape à l'autre
- Les urgences (ruptures) nécessitent de consulter plusieurs onglets

**Applicable à Pharma-Smart :** Partiellement — la décision v12 (3 onglets dans Suggestions) s'inspire de ce pattern.

---

### 4.2 Pattern 2 : Module unifié avec onglets contextuels (Winpharma)

```
[Approvisionnement] contient tout :
  Onglets internes : Suggestions | Commandes | Réceptions
  + panel latéral "alertes" toujours visible
```

**Forces :**
- 0 navigation entre modules → fluidité maximale
- Contexte préservé entre les étapes
- Idéal pour les petites équipes (1 pharmacien fait tout)

**Faiblesses :**
- Densité d'information élevée
- Interface moins lisible pour les débutants

**Applicable à Pharma-Smart :** L'ajout d'un bouton "Voir les commandes" après `commander()` + navigation auto = 80% du bénéfice sans refonte totale.

---

### 4.3 Pattern 3 : Dashboard actionnable comme hub (SurOrdonnance, Pharmagest)

```
Dashboard unique avec widgets cliquables → chaque widget filtre directement la vue cible
```

**Forces :**
- Vue d'ensemble immédiate
- Les urgences sont visibles sans naviguer
- Idéal pour les utilisateurs avancés (gestion multi-officines)

**Faiblesses :**
- Nécessite une bonne donnée temps réel
- Dashboard complexe à maintenir

**Applicable à Pharma-Smart :** L'`ApproUnifiedDashboardComponent` existe mais est en lecture seule. Rendre chaque KPI cliquable = quick win.

---

## 5. Gaps prioritaires — Ce qui manque dans Pharma-Smart

### 5.1 Gap critique (🔴) — Rupture Suggestion → Commande

**Problème :** Après `commander()`, l'utilisateur ne sait pas que la commande a été créée.
Aucune navigation automatique, aucun toast avec lien.

**Ce que font les autres :** Après création de commande, navigation automatique ou lien
"Voir la commande #42" dans le toast de succès.

**Correction minimale :**
```typescript
// SuggestionHomeComponent — après commander()
this.commandCommonService.updateCommandPreviousActiveNav('REQUESTED');
this.notificationService.success('Commande créée', 'Voir la commande', () =>
  this.commandCommonService.updateCommandPreviousActiveNav('REQUESTED')
);
```

---

### 5.2 Gap critique (🔴) — Pas de "Réceptionner" depuis Commandes en cours

**Problème :** Le pharmacien doit mémoriser le n° de commande, aller dans "Réceptions", puis
chercher dans la liste. En officine avec 20 commandes actives, c'est une source d'erreur réelle.

**Ce que font les autres :** Bouton "Réceptionner ▶" dans chaque ligne de la liste `commande-en-cours`.

**Correction minimale dans `commande-en-cours.component.html` :**
```html
<!-- Ajouter dans la colonne Actions -->
<p-button
  icon="pi pi-truck"
  label="Réceptionner"
  size="small"
  severity="success"
  pTooltip="Démarrer la réception de cette commande"
  (onClick)="demarrerReception(commande)"
/>
```
```typescript
demarrerReception(commande: ICommande): void {
  this.router.navigate(['/commande', commande.id, 'reception']);
}
```

---

### 5.3 Gap fort (🟠) — Badges alertes sur le menu sidebar

**Problème :** Le menu sidebar n'a aucun indicateur visuel de charge.
Un pharmacien qui démarre sa journée ne sait pas combien de commandes sont à réceptionner
sans naviguer dans chaque menu.

**Ce que font les autres :** Badges rouges sur chaque élément de menu avec le compteur.

**Correction dans `commande-home.component.html` :**
```html
<span>Commandes en cours</span>
@if (nbCommandesEnAttente() > 0) {
  <span class="pharma-nav-badge">{{ nbCommandesEnAttente() }}</span>
}
```

Données depuis : `commandeService.count({ orderStatuts: ['REQUESTED'] })`

---

### 5.4 Gap fort (🟠) — Dashboard actionnable

**Problème :** `ApproUnifiedDashboardComponent` affiche des KPI mais aucun n'est cliquable.

**Ce que font les autres :** Cliquer sur "3 commandes à réceptionner" navigue vers
Réceptions filtrées sur RECEIVED.

**Correction :** Ajouter `(click)` sur chaque tuile KPI avec `updateCommandPreviousActiveNav()`.

---

### 5.5 Gap moyen (🟡) — Onglet "Commandes à passer" manquant

**Problème :** Les suggestions validées (`statut=VALIDEE`) ne sont pas visibles dans un onglet dédié.
Alliadis, LGPI et Pharmagest ont tous cet intermédiaire.

**Correction :** Déjà planifié en Sprint 2 du plan v12 de `ANALYSE_MODULE_SUGGESTIONS.md`.

---

### 5.6 Gap moyen (🟡) — Bons de livraison : statut CLOSED hardcodé

**Problème :** `list-bons` ne montre que les bons `CLOSED`. Les bons `RECEIVED` (en cours de
saisie) et `REQUESTED` (attendus) ne sont jamais visibles dans l'historique.

**Correction :** Déjà identifiée dans `ANALYSE_UX_COMMANDE_RECEPTION.md` §3.1.

---

## 6. Proposition de navigation cible — v12+

### 6.1 Sidebar avec badges et 5 entrées réorganisées

```
Menu "Gestion commandes"
│
├── 📊 Tableau de bord          [actionnable — chaque KPI navigue vers le module]
│       Widgets cliquables :
│       • "X à réceptionner" → navigue vers Réceptions (filtre RECEIVED)
│       • "X commandes en cours" → navigue vers Commandes en cours
│       • "X urgences stock" → navigue vers Suggestions > Analyse des stocks
│
├── 💡 Suggestions              [badge = nb fournisseurs GENEREE + VALIDEE]
│       Tab "Réapprovisionnement"  → GENEREE (batch créé)
│       Tab "Commandes à passer"   → VALIDEE (à envoyer)
│       Tab "Analyse des stocks"   → MV lecture seule
│
├── 🛒 Commandes en cours       [badge = nb commandes REQUESTED]
│       Bouton "Réceptionner ▶" sur chaque ligne
│       Navigation auto depuis "Commandes à passer" après commander()
│
├── 🚚 Réceptions               [badge = nb bons RECEIVED]
│       Tab "En cours"    → RECEIVED (saisie en cours)
│       Tab "Historique"  → CLOSED + RECEIVED + REQUESTED (filtre statut)
│
├── ↕  Pilotage des stocks
└── ↩  Retours fournisseur
```

### 6.2 Workflow sans rupture — Flux complet cible

```
[SUGGESTIONS — Tab "Réapprovisionnement"]
  Batch a créé les Suggestion(SEMOIS, GENEREE) automatiquement
  Pharmacien : ajuste quantités → Valider → statut = VALIDEE

              ↓ (badge onglet "Commandes à passer" s'incrémente)

[SUGGESTIONS — Tab "Commandes à passer"]
  Pharmacien : [🛒 Commander]
  → crée Commande(REQUESTED), supprime Suggestion
  → NAVIGATION AUTOMATIQUE vers "Commandes en cours"   ← 🆕

              ↓ (badge sidebar "Commandes en cours" s'incrémente)

[COMMANDES EN COURS]
  Commande visible avec statut "En attente"
  Pharmacien : [🚚 Réceptionner ▶] sur la ligne de commande   ← 🆕
  → navigue vers formulaire réception pré-rempli

              ↓ (badge sidebar "Réceptions" s'incrémente)

[RÉCEPTIONS — Tab "En cours"]
  Saisie des quantités reçues
  Finalisation : 1 écran de synthèse (pas 3 modales)   ← 🔧 existant à corriger
  → statut CLOSED, stock mis à jour

              ↓ Fin du workflow, stock corrigé
```

---

## 7. Priorisation des corrections — Workflow complet

| Priorité | Action | Fichier(s) | Effort | Impact workflow |
|---|---|---|---|---|
| 🔴 **W0.1** | Navigation auto après `commander()` → tab "Commandes en cours" | `suggestion-home.component.ts` | 🟢 30 min | Supprime Rupture 1 |
| 🔴 **W0.2** | Bouton "Réceptionner ▶" dans `commande-en-cours` | `commande-en-cours.component.html/.ts` | 🟢 45 min | Supprime Rupture 2 |
| 🟠 **W1.1** | Badges compteurs sur sidebar (REQUESTED, RECEIVED) | `commande-home.component.ts/.html` | 🟡 1h30 | Supprime Rupture 6 |
| 🟠 **W1.2** | Dashboard KPI cliquables → navigation contextuelle | `appro-unified-dashboard.component` | 🟡 1h | Supprime Rupture 6 |
| 🟠 **W1.3** | `list-bons` : supprimer `CLOSED` hardcodé + filtre statut | `list-bons.component.ts` | 🟢 30 min | Supprime Rupture 4 |
| 🟠 **W1.4** | Onglet "Commandes à passer" dans `suggestions-unified` | cf. Plan v12 Sprint 2 | 🟡 2h | Supprime Rupture 3 |
| 🟡 **W2.1** | Fusionner 3 modales réception → 1 écran synthèse | `commande-received.component` | 🟠 Moyen | Fluidité réception |
| 🟡 **W2.2** | Remplacer `window.history.back()` par router navigation | `commande-received`, `commande-requested` | 🟢 30 min | Supprime Rupture 5 |
| 🟡 **W2.3** | État vide dans `list-bons` | `list-bons.component.html` | 🟢 15 min | UX basique |
| 🟢 **W3.1** | Total financier en pied de `list-bons` | `list-bons.component` | 🟡 1h | Réconciliation comptable |
| 🟢 **W3.2** | "Recréer commande similaire" depuis `list-bons` | `list-bons.component` | 🟠 Moyen | Gain de temps |

---

## 8. Conclusion

### Ce qui distingue les meilleurs logiciels

1. **Continuité du flux** : Winpharma et Pharmagest ne forcent jamais l'utilisateur à mémoriser
   un contexte pour le retrouver dans un autre menu. Une action dans l'étape N navigue automatiquement
   vers l'étape N+1 avec le contexte pré-chargé.

2. **Menu = statut** (Alliadis) : Chaque entrée de menu correspond exactement à un statut d'objet.
   L'utilisateur comprend d'un coup d'œil où en sont ses commandes.

3. **Dashboard actionnable** : Un KPI qui ne mène nulle part est une occasion manquée. Tous les
   logiciels modernes ont des dashboards avec des CTA contextuels.

4. **Réception opérationnelle** : La réception est une opération terrain (livreur présent, urgence).
   Les meilleurs logiciels proposent un "mode rapide" (scanner + quantité) distinct du "mode comptable".

### Pharma-Smart — Forces à conserver
- Split-panel Suggestions (fournisseur + produits) : meilleure ergonomie que la plupart des concurrents
- StatusBar dans la réception : aucun concurrent ne l'a aussi bien
- PharmaML intégré : fonctionnalité différenciante unique
- SEMOIS batch : plus robuste que les suggestions post-vente des concurrents

### Les 3 corrections à faire immédiatement (< 2h total)

1. **W0.1** — Navigation auto après `commander()` → sidebar "Commandes en cours"
2. **W0.2** — Bouton "Réceptionner ▶" dans chaque ligne de `commande-en-cours`
3. **W1.3** — Supprimer `CLOSED` hardcodé dans `list-bons` + ajouter filtre statut

Ces 3 corrections transforment un flux à 5 ruptures en flux quasi-continu,
alignant Pharma-Smart avec le standard Pharmagest/LGPI pour un effort < 2 heures.

---

*Analyse basée sur : code source réel (`commande-home.component.html`, `commande-en-cours.component.ts`,
`reception-hub.component.ts`, `list-bons.component.ts`, `suggestion-home.component.ts`),
croisé avec les benchmarks Winpharma, Pharmagest Interactive, LGPI, Alliadis, SurOrdonnance.*

---

## 9. Question architecturale — Tout le cycle de vie dans un seul menu à tabs ?

> **Question :** Serait-il plus pertinent de regrouper Suggestions + Commandes + Réceptions
> dans un seul menu avec des onglets (comme l'actuel menu Suggestions), plutôt que 3 entrées
> séparées dans la sidebar ?

---

### 9.1 Observation préalable — Pharma-Smart fait déjà cela partiellement

Avant tout, une observation importante sur le code actuel :

```typescript
// command-common.service.ts — signal de navigation cross-composants DÉJÀ PRÉSENT
commandPreviousActiveNav: WritableSignal<string> = signal<string>('DASHBOARD');

navigateToSemoisSuggestions(): void {
  this.suggestionsActiveSource.set('SEMOIS');
  this.commandPreviousActiveNav.set('SUGGESTIONS');  // ← navigue vers un tab
}
```

```html
<!-- commande-home.component.html — la sidebar EST déjà un nav à tabs (NgbNav pills) -->
<div ngbNav [(activeId)]="active" orientation="vertical" class="nav flex-column nav-pills">
  <ng-container ngbNavItem="DASHBOARD"> ... </ng-container>
  <ng-container ngbNavItem="REQUESTED"> ... </ng-container>
  <ng-container ngbNavItem="RECEPTIONS"> ... </ng-container>
  <ng-container ngbNavItem="SUGGESTIONS"> ... </ng-container>
</div>
```

**La sidebar actuelle est fonctionnellement identique à des onglets verticaux.**
`CommandCommonService.commandPreviousActiveNav` gère déjà la navigation cross-composants.
La question n'est donc pas "tabs vs sidebar" — c'est déjà des tabs — mais :

> **"Faut-il réorganiser ces tabs pour que le cycle de vie (Suggestions → Commandes → Réceptions)
> soit visible comme une séquence linéaire dans le même niveau de navigation ?"**

---

### 9.2 Ce que font les logiciels de référence

| Logiciel | Architecture | Structure de navigation |
|---|---|---|
| **Winpharma** | Tout-en-un | 1 menu "Approvisionnement" avec onglets internes (3–4 tabs) |
| **Pharmagest** | Séparé mais lié | Menu séparé pour chaque étape **+ boutons contextuels** entre elles |
| **LGPI** | Linéaire numéroté | 4 entrées sidebar nommées "1 — Analyse", "2 — Suggestions", "3 — Commandes", "4 — BL" |
| **Alliadis** | Statut = menu | 5 entrées sidebar = 5 statuts explicites du cycle |
| **SurOrdonnance** | Dashboard-centré | Pas de menu : tout part de widgets cliquables sur le dashboard |
| **SAP Fiori / Odoo** | Worklist | Dashboard central avec tuiles par statut ; chaque tuile = liste filtrée |

**Winpharma est le seul** à avoir tout dans un seul module avec tabs.
**LGPI et Alliadis** le font "à moitié" : sidebar séparée mais nommage séquentiel (1, 2, 3, 4).
**Pharmagest** reste séparé mais compense avec des boutons contextuels très bien placés.

---

### 9.3 Arguments POUR — Cycle de vie unifié dans un seul menu à tabs

#### ✅ Élimination totale des 5 ruptures de navigation

En regroupant tout dans un composant parent unique avec `commandPreviousActiveNav`,
le passage d'un tab à l'autre devient une ligne de code :

```typescript
// Après commander() — 0 navigation sidebar, juste un changement de tab
this.commandCommonService.updateCommandPreviousActiveNav('COMMANDES_EN_COURS');
```

Toutes les ruptures 1–5 identifiées disparaissent structurellement.

#### ✅ Cohérence avec l'existant — la sidebar est déjà des tabs

Le code utilise déjà `NgbNav` avec `commandPreviousActiveNav`. Transformer la sidebar
en **tabs horizontaux** dans un composant `ApproHubComponent` serait une refactorisation
mineure, pas une réécriture.

#### ✅ Meilleure représentation du workflow métier

L'officine commande selon un cycle : **analyser → suggérer → valider → envoyer → réceptionner**.
Un menu qui reflète exactement ce cycle est plus intuitif qu'un menu thématique
("Suggestions", "Commandes", "Réceptions") qui cache la continuité.

#### ✅ Contexte préservé entre les étapes

Avec des tabs dans le même composant parent, le signal `currentCommand` reste vivant
entre les étapes. Passer de "Commandes en cours" à "Réceptions" sans perdre la commande
sélectionnée devient naturel.

#### ✅ Badges compteurs plus visibles sur des tabs horizontaux

```
[💡 Réapprovisionnement 3] [📋 À commander 1] [🛒 En cours 5] [🚚 Réceptions 2] [📜 Historique] [📊 Analyse]
```
Les badges sont immédiatemment visibles sur des tabs horizontaux.
Sur la sidebar actuelle, les badges nécessitent de scanner toute la liste verticale.

---

### 9.4 Arguments CONTRE — Faiblesses importantes

#### ❌ Loi de Miller — trop de tabs simultanés

Un cycle complet comprendrait **6 tabs minimum** :

```
Réapprovisionnement | Commandes à passer | Commandes en cours | Réceptions | Historique | Analyse des stocks
```

La loi de Miller (7±2 chunks) s'applique à la mémoire de travail, pas directement aux tabs,
mais au-delà de 5 tabs la navigation par tab perd son avantage sur un menu.
Les études UX (Nielsen Norman Group, 2020) montrent qu'au-delà de **5 tabs horizontaux**,
les utilisateurs commencent à traiter la navigation comme une liste scrollable —
bénéfice de la "visibilité du tab actif" largement réduit.

**Winpharma s'en sort** parce qu'il n'a que 3–4 tabs dans "Approvisionnement".
6 tabs = limite haute du pattern.

#### ❌ Layouts incompatibles dans un seul conteneur

Les 3 modules ont des layouts radicalement différents :

| Module | Layout actuel | Contrainte |
|---|---|---|
| `SuggestionHome` | Split-panel 2 col (fournisseurs + produits) | Nécessite 100% de la largeur |
| `CommandeEnCours` | Table pleine largeur + expand rows | Nécessite 100% de la largeur |
| `CommandeRequested` | Header + 3 zones (PharmaML + table + méta produit) | Layout complexe, route dédiée |
| `CommandeReceived` | Header + concordance col-3 + table col-9 | Layout complexe, route dédiée |

`CommandeRequested` et `CommandeReceived` sont des **routes Angular dédiées** aujourd'hui
(`/commande/:id/...`). Les mettre dans un tab demanderait soit :
- Un sous-router dans le tab (complexité routing Angular)
- Ou charger le composant directement avec `@Input() commandeId` (refactorisation)

#### ❌ Perte de deep-linking et bookmarks

Avec des tabs, l'URL ne change pas entre "Commandes en cours" et "Réceptions".
Un utilisateur ne peut pas bookmarker "mes réceptions du jour" ni partager l'URL.
Avec la sidebar actuelle (qui utilise `queryParams['tab']`), l'URL reflète l'onglet actif.

#### ❌ Coût de migration vs bénéfice

La sidebar actuelle **fonctionne déjà comme des tabs** grâce à `commandPreviousActiveNav`.
Refactoriser en tabs horizontaux = ~3 jours de travail pour un résultat UX quasi-identique,
sauf sur la visibilité des badges (qui peuvent être ajoutés sur la sidebar existante — §5.3).

#### ❌ Contexte "opérationnel" vs "analytique" mélangés

La réception est une **opération urgente** (livreur présent, stock bloqué).
Les suggestions sont une **activité analytique planifiée** (le matin, calmement).
Les séparer permet à chaque interface d'être optimisée pour son usage :
- Réception → colonnes minimales, mode scanner, focus unique
- Suggestions → richesse d'information, filtres multiples, analyse

Winpharma les met ensemble mais propose un **mode scanner dédié** exactement pour cette raison.

#### ❌ Contrôle d'accès plus complexe

Certains préparateurs peuvent avoir accès à "Réceptions" mais pas à "Suggestions" ni "Commandes".
Avec des sidebar items séparés, `UserRouteAccessService` gère cela simplement.
Avec des tabs dans un seul composant, il faut gérer la visibilité conditionnelle de chaque tab.

---

### 9.5 L'approche hybride — ce qui est réellement conseillé

Ni tout unifié (6 tabs), ni tout séparé (5 sidebar items sans lien).
Les logiciels les mieux notés (Pharmagest, LGPI version récente) utilisent :

> **Sidebar pour les "domaines" (2–3 niveaux) + boutons contextuels entre les domaines.**

Appliqué à Pharma-Smart :

```
SIDEBAR (4 entrées — domaines)
│
├── 📊 Tableau de bord       [Dashboard actionnable — KPI cliquables]
│
├── 📦 Cycle d'achat         [1 entrée unique = tout le cycle]
│       ┌──────────────────────────────────────────────────────────────┐
│       │ TABS HORIZONTAUX (séquence linéaire, max 5 tabs)            │
│       │                                                              │
│       │ [💡 Réapprovisionnement 3] [📋 À passer 1] [🛒 En cours 5] │
│       │ [🚚 Réceptions 2]          [📊 Analyse]                     │
│       └──────────────────────────────────────────────────────────────┘
│       → "Commandes en cours" et "Réceptions" FUSIONNÉS ici
│       → Routes /commande/:id → modal ou panneau latéral (pas de nav)
│
├── ↕  Pilotage des stocks   [séparé — activité distincte]
│
└── ↩  Retours fournisseur   [séparé — activité distincte]
```

**Pourquoi 5 tabs (pas 3, pas 7) :**
- Tab 1 "Réapprovisionnement" = `GENEREE` → travail batch quotidien
- Tab 2 "À passer" = `VALIDEE` → décision d'envoi
- Tab 3 "En cours" = `REQUESTED` → attente livraison + bouton "Réceptionner ▶"
- Tab 4 "Réceptions" = `RECEIVED` + historique `CLOSED` (sub-tabs internes)
- Tab 5 "Analyse" = MV `mv_semois_suggestion` (lecture seule)

Les routes dédiées (`/commande/:id`) s'ouvrent dans un **panneau latéral (drawer)** ou une **modal** depuis le tab "En cours" — elles ne remplacent plus le tab.

---

### 9.6 Comparaison directe des 3 options

| | Option A — Sidebar actuelle (3 items séparés) | Option B — Tout en tabs (6 tabs) | Option C — Hybride (1 sidebar "Cycle d'achat" + 5 tabs) ✅ |
|---|---|---|---|
| **Ruptures de navigation** | ❌ 5 ruptures | ✅ 0 rupture | ✅ 0 rupture |
| **Tabs visibles** | — | ❌ 6 (trop) | ✅ 5 (optimal) |
| **Deep-linking URL** | ✅ queryParams | ❌ perdu | ✅ conservé (tab=3) |
| **Layouts compatibles** | ✅ routes dédiées | ❌ layouts conflictuels | ✅ drawer pour routes |
| **Contrôle d'accès** | ✅ simple | ⚠️ conditionnel | ✅ simple par sidebar item |
| **Coût de migration** | 🟢 0 (correction actions) | 🔴 5–7j refactoring | 🟡 2–3j |
| **Font-ils pareil ?** | LGPI, Alliadis | Winpharma | Pharmagest, SAP Fiori |
| **Apprentissage utilisateur** | ⚠️ navigation non évidente | ✅ séquence visible | ✅ séquence visible |
| **Mobile/responsive** | ✅ sidebar collapsible | ❌ 6 tabs = débordement | ✅ tabs scrollables |

---

### 9.7 Recommandation finale

> **Option C — Hybride : fusionner les 3 items sidebar en "Cycle d'achat" avec 5 tabs internes.**

C'est ce que fait **Pharmagest** (la référence la mieux notée en officine française)
et **SAP Fiori** pour les modules de procurement.

**Ce qui change concrètement dans Pharma-Smart :**

1. **`commande-home.component.html`** : remplacer les 3 `ngbNavItem`
   `(REQUESTED, RECEPTIONS, SUGGESTIONS)` par un seul `ngbNavItem="CYCLE_ACHAT"` contenant
   un `CycleAchatComponent` avec tabs horizontaux.

2. **`CycleAchatComponent`** (nouveau, ou `SuggestionsUnifiedComponent` étendu) :
   ```
   <tabs> Réapprovisionnement | À passer | En cours | Réceptions | Analyse </tabs>
   <tab-content>...</tab-content>
   ```

3. **`CommandCommonService`** : étendre `commandPreviousActiveNav` pour inclure
   les tabs du cycle (`'REAPPRO' | 'COMMANDES_A_PASSER' | 'COMMANDES_EN_COURS' | 'RECEPTIONS' | 'ANALYSE'`)
   — cela existe déjà avec `SuggestionsSource`, il suffit d'élargir le type.

4. **Routes dédiées** (`/commande/:id/reception`) : restent des routes mais s'ouvrent
   dans un drawer latéral (comme les modales NgbModal existantes) pour ne pas quitter
   l'onglet parent.

5. **Sidebar réduite** :
   ```
   Dashboard | Cycle d'achat | Pilotage des stocks | Retours fournisseur
   ```
   4 items au lieu de 6 → sidebar plus lisible et plus facile à étendre.

**Effort estimé :** 🟡 2–3 jours (la mécanique `CommandCommonService` + `NgbNav` est déjà en place).

---

### 9.8 Plan de migration vers l'Option C — Actions concrètes

| # | Action | Fichier | Effort |
|---|---|---|---|
| **C0.1** | Créer `AppCycleAchatComponent` avec 5 tabs horizontaux (shell) | `cycle-achat.component.ts/.html` | 🟢 1h |
| **C0.2** | Remplacer REQUESTED + RECEPTIONS + SUGGESTIONS dans la sidebar par `CYCLE_ACHAT` → `<app-cycle-achat>` | `commande-home.component.html` | 🟢 30 min |
| **C0.3** | Étendre `SuggestionsSource` → type `CycleAchatTab = 'REAPPRO' \| 'COMMANDES_A_PASSER' \| 'COMMANDES_EN_COURS' \| 'RECEPTIONS' \| 'ANALYSE'` dans `CommandCommonService` | `command-common.service.ts` | 🟢 20 min |
| **C0.4** | Brancher les composants existants dans leurs tabs respectifs (pas de réécriture, seulement du placement) | `cycle-achat.component.html` | 🟢 1h |
| **C1.1** | ~~Routes `/commande/:id` → drawer~~ → voir §9.9 ci-dessous | — | — |
| **C1.2** | Badges compteurs sur chaque tab (signals depuis `CommandCommonService`) | `cycle-achat.component.ts` | 🟡 1h30 |
| **C2.1** | Navigation automatique entre tabs après actions (`commander()` → tab "En cours", `réceptionner()` → tab "Réceptions") | Tous composants actions | 🟡 2h |

**Total estimé : 8–10h** — soit 1.5 journée de développement.

---

### 9.9 Correction C1.1 — "Réceptionner ▶" : drawer ou autre chose ?

> **Question directe :** Le bouton "Réceptionner ▶" dans le tab "En cours" ouvre-t-il un drawer ?

**Non — et voici pourquoi.**

#### Ce que le code révèle

`CommandeReceivedComponent` est déjà conçu pour être **embarqué**, pas seulement routé :

```typescript
// commande-received.component.ts — signaux Angular déjà en place
commande = input.required<ICommande>();        // reçoit la commande en @Input
commandeChange = output<ICommande | null>();   // émet les changements vers le parent
```

Le composant n'a **pas besoin d'une route** pour recevoir sa donnée. Il peut être instancié
directement par un composant parent qui lui passe `[commande]="commandeSelectionnee"`.

#### Pourquoi un drawer serait mal adapté

| Contrainte | Impact sur un drawer |
|---|---|
| Layout `col-3` (concordance) + `col-9` (table) | Un drawer standard (30–40% de largeur) écrase ce layout |
| Table avec 15 colonnes | Nécessite 100% de la largeur disponible, pas 50% |
| Sous-modales : `ListLotComponent`, `FormLotComponent`, `PutawayModalComponent` | NgbModal imbriqué dans NgbOffcanvas = gestion z-index complexe |
| `ReceptionConcordanceComponent` sticky | Perd son positionnement sticky dans un drawer |
| 350 lignes de template | Interface dense, conçue pour grand écran |

Un drawer plein écran (NgbOffcanvas position=`end`, width=100%) serait fonctionnellement
équivalent à une route — autant garder une navigation propre.

#### La bonne solution — Pattern Master-Detail dans le tab "Réceptions"

C'est exactement ce que fait **Pharmagest** et **LGPI** :

```
TAB "RÉCEPTIONS"
│
├── État liste (défaut) : AppBonEnCoursComponent
│     Liste des commandes RECEIVED avec [Réceptionner ▶] sur chaque ligne
│
└── État détail (après clic) : CommandeReceivedComponent
      Remplace la liste dans le même tab (plein espace disponible)
      Bouton "< Retour à la liste" en haut à gauche (remplace window.history.back())
      Breadcrumb : Réceptions › BL-2024-042 · Pharmalab
```

**Implémentation dans `AppCycleAchatComponent` :**

```typescript
// cycle-achat.component.ts
readonly receptionMode = signal<'LIST' | 'DETAIL'>('LIST');
readonly commandeEnReception = signal<ICommande | null>(null);

ouvrirReception(commande: ICommande): void {
  this.commandeEnReception.set(commande);
  this.receptionMode.set('DETAIL');
  this.activeTab.set('RECEPTIONS');    // ← bascule sur le tab Réceptions
}

retourListe(): void {
  this.commandeEnReception.set(null);
  this.receptionMode.set('LIST');
}
```

```html
<!-- cycle-achat.component.html — tab "Réceptions" -->
@case ('RECEPTIONS') {
  @if (receptionMode() === 'LIST') {
    <app-bon-en-cours (onReceptionner)="ouvrirReception($event)" />
  } @else {
    <!-- breadcrumb -->
    <div class="pharma-breadcrumb">
      <p-button icon="pi pi-arrow-left" [text]="true" label="Réceptions"
                (onClick)="retourListe()" />
      <span class="bc-separator">›</span>
      <span>{{ commandeEnReception()?.receiptReference }}</span>
    </div>
    <!-- composant déjà compatible @Input -->
    <app-commande-received
      [commande]="commandeEnReception()"
      (commandeChange)="onCommandeChange($event)"
    />
  }
}
```

#### Idem pour "En cours" → saisie d'une commande (`CommandeRequestedComponent`)

`CommandeRequestedComponent` est un composant de saisie complexe (PharmaML panel, raccourcis
clavier F9, recherche produit). Même logique :

```
TAB "EN COURS"
├── État liste : CommandeEnCoursComponent
│     [Modifier] → mode DETAIL avec CommandeRequestedComponent
│     [Réceptionner ▶] → bascule tab RECEPTIONS en mode DETAIL
└── État détail : CommandeRequestedComponent
      Breadcrumb : En cours › Commande #234 · COOPER
```

#### Résumé de la décision C1.1 — corrigée

| Approche | Verdict | Raison |
|---|---|---|
| **Drawer (NgbOffcanvas)** | ❌ Non recommandé | Layout col-3+col-9, sous-modales imbriquées, 15 colonnes |
| **Route dédiée (actuel)** | ⚠️ Acceptable mais rupture de flux | `window.history.back()` imprévisible, quitte le cycle d'achat |
| **Master-Detail dans le tab** ✅ | **Recommandé** | `input.required` déjà en place, 0 route, 0 drawer, breadcrumb propre |

**Effort réel C1.1 :** 🟡 2–3h (signal `receptionMode` + refactoring `AppBonEnCoursComponent` pour émettre `onReceptionner`, le reste est du placement).

