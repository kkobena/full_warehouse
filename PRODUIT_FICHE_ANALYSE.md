# Analyse — Fiche Article Produit (Gestion d'Officine)

> Référence : `src/main/webapp/app/entities/produit/produit.component.html`
> Comparaison : LGPI, Winpharma, Pharmagest, SmartRx, Alliance Healthcare
> Standards : USPO, FSPF, Nielsen Norman Group, recommandations éditeurs officinaux

---

## 1. État des lieux — Ce que fait l'actuel

### Structure
- **Toolbar** : filtre par critère, rayon, famille, champ recherche (enter seulement)
- **Table** (12 colonnes) : CIP, EAN, Libellé, Stock, Prix achat, Prix vente, Qté.Reap, Qté.Mini, Qté.Réassort, **Statut** (`EtaProduitComponent`), Actions
- **Ligne expandable** : 3 panneaux inline (Infos générales · Répartition stock · Fournisseurs)
- **Actions par ligne** : Voir, Éditer (PACKAGE/DETAIL), Prix référence, Ajouter détail, Déconditionner, Supprimer
- **Pagination** : `ngb-pagination` externe

### Colonne Statut — `EtaProduitComponent` ✅

La colonne **Statut** est rendue par `<jhi-eta-produit [etatProduit]="produit.etatProduit" [showLabel]="true">` — composant partagé (`app/shared/eta-produit/`) qui affiche une barre de badges colorés permettant au métier de **localiser instantanément le produit dans le workflow officinal** :

| Badge | Couleur | Icône | Condition `EtatProduit` | Valeur métier |
|---|---|---|---|---|
| En stock | 🟢 Vert | `pi-check-circle` | `stockPositif = true` | Stock disponible |
| Rupture | 🔴 Rouge | `pi-minus-circle` | `sockZero = true` | Stock épuisé |
| Stock négatif | 🔴 Rouge | `pi-exclamation-circle` | `stockNegatif = true` | Mouvement anormal |
| En sugg. | 🔵 Bleu | `pi-lightbulb` | `enSuggestion = true` | Produit inscrit dans une suggestion de commande |
| En commande | 🟣 Violet | `pi-shopping-cart` | `enCommande = true` | Commande fournisseur en cours |
| Reçu | 🟣 Indigo | `pi-download` | `entree = true` | Réceptionné récemment |

> **Valeur métier clé** : un pharmacien ou préparateur peut en un coup d'œil savoir si un produit en rupture est déjà en commande, déjà suggéré, ou en attente de réception — sans ouvrir aucun autre écran. C'est une fonctionnalité différenciante que peu de logiciels proposent nativement en liste article.

### Points positifs existants
- Colonne Statut avec `EtaProduitComponent` : localisation workflow complète (suggestion → commande → entrée stock)
- Coloration stock sur la cellule quantité (négatif / warning / positif) via classes CSS
- Panneau fournisseurs avec toggle fournisseur principal
- Gestion PACKAGE / DETAIL / déconditionnable
- Répartition stock rayon/réserve inline

---

## 2. Problèmes UI actuels

### 2.1 Surcharge colonnes
12 colonnes dans un seul tableau. Sur 1366px les libellés sont tronqués (`Qté.Reap`, `Qté.Mini`).
Labels non traduits dans les en-têtes (`Regular Unit Price`, `Quantity` via `jhiTranslate`).

**Recommandation** : réduire à 7 colonnes visibles par défaut, colonnes secondaires masquables via un sélecteur de colonnes (PrimeNG `p-columnFilter`/`toggleableColumns`).

### 2.2 Actions non hiérarchisées
5 à 7 boutons d'action inline par ligne, même poids visuel, sans priorisation.
L'action principale (Voir / Éditer) est noyée parmi les actions secondaires.

**Recommandation** :
```
[ Éditer ]  [ ⋮ Menu contextuel ]
              ├── Voir le détail
              ├── Ajouter prix référence
              ├── Ajouter détail
              ├── Déconditionner
              └── Désactiver / Archiver
```

### 2.3 Ligne expandable trop dense
Le panneau "Infos Générales" cumule 15 champs sans distinction de priorité ni onglets.
L'œil ne sait pas où aller. Sous-tableaux stock et fournisseurs imbriqués côte à côte.

**Recommandation** : onglets dans l'expandable, ou route dédiée `/produit/:id/view` pour le détail complet (voir §5).

### 2.4 Pagination legacy
`ngb-pagination` externe alors que `p-table` PrimeNG supporte la pagination native avec `[lazy]="true"` et `(onLazyLoad)` — déjà utilisé dans le projet (`retour-fournisseur`, `bon-en-cours`, `list-bons`).

### 2.5 Pas de tri sur les colonnes
Aucun `[sortable]` déclaré. Trier par stock croissant (ruptures en tête) ou par taux de rotation est une opération quotidienne en officine.

### 2.6 Légende stock — ✅ Déjà gérée par `EtaProduitComponent`
La colonne "Statut" utilise `<jhi-eta-produit [etatProduit]="..." [showLabel]="true">` qui affiche des badges colorés avec icône, libellé et tooltip (`En stock` · `Rupture` · `Stock négatif` · `En sugg.` · `En commande` · `Reçu`).

Le composant couvre les états stock via `EtatProduit.stockPositif / sockZero / stockNegatif` et les états workflow (`enSuggestion`, `enCommande`, `entree`).

**Ce qui manque encore dans `EtaProduitComponent`** pour la complétude officinale :
- Badge **sous seuil mini** (`stock > 0 && stock < seuilMini`) — actuellement non distingué de "En stock"
- Badge **péremption proche** (< 3 mois / < 6 mois)
- Badge **produit en veille / archivé** (état inactif du produit lui-même, distinct de l'état stock)

### 2.7 Champ recherche non persisté
`(keyup.enter)` uniquement, sans `[(ngModel)]`. L'état de recherche est perdu lors d'un changement de filtre externe (rayon, famille).

### 2.8 Pas de sélection multiple
Aucune checkbox pour actions groupées (activer/désactiver lot de produits, export sélection).

### 2.9 Suppression directe sans workflow
Le bouton "Supprimer" est accessible directement sur des produits ayant potentiellement un historique de mouvements, sans passage par une désactivation préalable.

---

## 3. Fonctionnalités absentes — Comparaison logiciels experts

### 3.1 Indicateurs économiques

| Indicateur | LGPI | Winpharma | Pharmagest | Présent |
|---|:---:|:---:|:---:|:---:|
| Marge brute / Taux de marge | ✅ | ✅ | ✅ | ❌ |
| Taux de rotation annuel | ✅ | ✅ | ✅ | ❌ |
| Nombre de jours de stock | ✅ | ✅ | ✅ | ❌ |
| Classe ABC / Pareto visible | ✅ | ✅ | ✅ | ❌ |
| Historique prix d'achat | ✅ | ❌ | ✅ | ❌ |
| Couverture stock projetée (jours avant rupture) | ✅ | ✅ | ❌ | ❌ |

> Le **taux de rotation** et les **jours de stock** sont les deux indicateurs les plus consultés quotidiennement en officine (source : formations USPO). Leur absence oblige le pharmacien à calculer manuellement.

### 3.2 Traçabilité et conformité réglementaire

| Fonctionnalité | LGPI | Winpharma | Pharmagest | Présent |
|---|:---:|:---:|:---:|:---:|
| Lots actifs cliquables depuis la liste | ✅ | ✅ | ✅ | ❌ |
| Badge liste I / liste II / stupéfiant | ✅ | ✅ | ✅ | ❌ |
| Alerte péremption colorée (< 3 mois / < 6 mois) | ✅ | ✅ | ✅ | ❌ |
| Taux de remboursement / déductibilité AMO | ✅ | ✅ | ✅ | ❌ |
| Ordonnance requise (flag prescription) | ✅ | ❌ | ✅ | ❌ |

### 3.3 Aide à la commande

| Fonctionnalité | LGPI | Winpharma | Pharmagest | Présent |
|---|:---:|:---:|:---:|:---:|
| Bouton "Commander" inline (stock < seuil) | ✅ | ✅ | ✅ | ❌ |
| Indicateur de saisonnalité | ✅ | ❌ | ✅ | ❌ |
| Suggestion de quantité à commander | ✅ | ✅ | ✅ | ❌ |

### 3.4 Consultation et navigation

| Fonctionnalité | LGPI | Winpharma | Pharmagest | Présent |
|---|:---:|:---:|:---:|:---:|
| Génériques / substituts accessibles | ✅ | ✅ | ✅ | ❌ |
| Historique des ventes (graphe 12 mois) | ✅ | ✅ | ❌ | ❌ |
| Prix de vente multiple (client / mutuelle / gros) | ✅ | ❌ | ✅ | ❌ |
| Image produit (miniature boîte) | ✅ | ❌ | ✅ | ❌ |
| Emplacement physique (casier / étagère) | ✅ | ✅ | ❌ | ❌ |
| Impression étiquette / QR code depuis liste | ✅ | ✅ | ✅ | ❌ |

---

## 4. Recommandations des experts officinaux

### 4.1 Règle des 3 indicateurs clés (USPO / FSPF)
> *"Une ligne article doit permettre une décision en moins de 3 secondes."*

Chaque ligne produit doit afficher sans déploiement :
1. **Stock actuel** avec code couleur sémantique
2. **Rotation** (mensuelle ou annuelle)
3. **Marge** (taux ou valeur absolue)

### 4.2 Code couleur sémantique standardisé

| Couleur | Signification | Condition |
|---|---|---|
| 🟢 Vert | Stock normal | `stock >= seuilMini` |
| 🟠 Orange | Sous seuil minimum | `stock > 0 && stock < seuilMini` |
| 🔴 Rouge | Rupture / négatif | `stock <= 0` |
| 🟡 Jaune | Péremption proche | Lot expirant < 6 mois |
| ⚫ Gris | Produit inactif / archivé | `etatProduit = INACTIF` |

### 4.3 Tri par défaut orienté métier
Le tri par défaut doit être **stock croissant** (ruptures en tête), pas alphabétique.
Le pharmacien doit voir immédiatement les urgences à réapprovisionner.

### 4.4 Workflow de désactivation progressive
Les experts déconseillent la suppression directe d'un produit ayant des mouvements historiques.

```
Actif
  └─[Mettre en veille]──► En veille   (caché des ventes, stock conservé, commandes bloquées)
                              └─[Archiver]──► Archivé   (lecture seule, historique accessible)
                              └─[Réactiver]──► Actif
```

La suppression physique ne doit être accessible que pour les produits **sans aucun mouvement** (jamais commandé, jamais vendu).

### 4.5 Vue liste vs vue fiche (Nielsen Norman Group)
Pour les détails complexes (> 5 champs, sous-tableaux), une **route dédiée** est recommandée plutôt que l'expandable inline :
- Expandable inline : adapté pour 3-5 champs de synthèse rapide
- Page dédiée `/produit/:id/view` : pour l'ensemble des données, onglets, historiques

---

## 5. Propositions UI concrètes

### 5.1 Nouvelle structure de la liste

```
┌─────────────────────────────────────────────────────────────────────┐
│ TOOLBAR                                                             │
│ [Critère ▼] [Rayon ▼] [Famille ▼] [🔍 Recherche...]  [Colonnes ⚙] │
│                             [Importation ▼] [+ Nouveau] [📊 Export] │
└─────────────────────────────────────────────────────────────────────┘

┌──┬──────┬──────────────────────────┬───────┬────────┬───────┬───────┬─────────────────────────────┬──────────┐
│☐ │ CIP  │ Libellé                  │ Stock │ Jours  │ Marge │ Classe│ Statut (EtaProduit)         │ Actions  │
│  │      │                          │       │ stock  │  %    │  ABC  │                             │          │
├──┼──────┼──────────────────────────┼───────┼────────┼───────┼───────┼─────────────────────────────┼──────────┤
│☐ │123456│ DOLIPRANE 1000MG         │  🔴 0 │  0 j   │ 28%   │   A   │ [Rupture] [En commande]     │[Éditer][⋮]│
│☐ │789012│ IBUPROFENE 400MG         │  🟠 3 │  2 j   │ 31%   │   B   │ [En stock] [En sugg.]       │[Éditer][⋮]│
│☐ │345678│ DOLIPRANE 500MG ENFANT   │  🟢 48│  42 j  │ 27%   │   A   │ [Reçu]                      │[Éditer][⋮]│
└──┴──────┴──────────────────────────┴───────┴────────┴───────┴───────┴─────────────────────────────┴──────────┘
```

> La colonne **Statut** via `EtaProduitComponent` est **non négociable dans les colonnes par défaut** : elle permet au métier de localiser le produit dans le workflow sans navigation (rupture déjà commandée ? déjà en suggestion ? réception en cours ?). C'est la colonne à plus haute valeur métier de la liste.

**Colonnes par défaut (8)** : ☐ · CIP · Libellé · Stock · Jours de stock · Marge % · Classe ABC · **Statut** (`EtaProduitComponent`) · Actions
**Colonnes masquables** : EAN · Prix achat · Prix vente · Qté.Mini · Qté.Réassort · Famille · Rayon · Taux rotation

### 5.2 Ligne expandable refactorisée — Onglets

```
▼ DOLIPRANE 1000MG  ─────────────────────────────────────────────────────
  [ Synthèse ] [ Stock & Répartition ] [ Fournisseurs ] [ Historique ]
  ┌──────────────────────────────────────────────────────────────────────┐
  │ SYNTHÈSE                                                             │
  │  PMP: 2,45 €    Dernière vente: 12/03/2026    Rotation: 8,4×/an     │
  │  TVA: 5,5%      Entrée stock: 05/03/2026      Jours stock: 14 j      │
  │  Famille: Antalgique    Rayon: OTC    Laboratoire: Sanofi             │
  │  Péremption: 🟡 08/2026 (5 mois)    Lots actifs: [2 lots]            │
  │  Badges: [Liste II] [Non remboursé]                                  │
  └──────────────────────────────────────────────────────────────────────┘
```

### 5.3 Menu contextuel actions (bouton `⋮`)

```
⋮ Menu contextuel
  ──────────────────
  👁  Voir le détail complet
  ✏️  Éditer le produit
  🏷️  Imprimer étiquette / QR code
  ──────────────────
  🛒  Commander (générer suggestion)
  💊  Voir les génériques / substituts
  📦  Voir les lots actifs
  ──────────────────
  ⏸️  Mettre en veille
  🗄️  Archiver
  ──────────────────
  🗑️  Supprimer  (grisé si mouvements existants)
```

### 5.4 Indicateur "Jours de stock" avec tooltip

```html
<!-- Calcul : stock actuel / (ventes 90j / 90) -->
<td>
  <span [pTooltip]="'Couverture estimée sur base des 90 derniers jours'"
        [class.days-critical]="jourStock < 7"
        [class.days-warning]="jourStock >= 7 && jourStock < 30"
        [class.days-ok]="jourStock >= 30">
    {{ jourStock }} j
  </span>
</td>
```

### 5.5 Badge réglementaire

```html
<!-- Badges à afficher sur la ligne ou dans le libellé -->
@if (produit.listeReglementaire === 'LISTE_I') {
  <span class="badge badge-liste-1" pTooltip="Médicament Liste I">L1</span>
}
@if (produit.listeReglementaire === 'LISTE_II') {
  <span class="badge badge-liste-2" pTooltip="Médicament Liste II">L2</span>
}
@if (produit.stupefiants) {
  <span class="badge badge-stup" pTooltip="Stupéfiant — traçabilité obligatoire">ST</span>
}
```

### 5.6 Alerte péremption colorée

```html
<!-- Dans la ligne expandable, section Synthèse -->
<div class="info-item"
     [class.expiry-critical]="getExpiryStatus(elRow) === 'CRITICAL'"
     [class.expiry-warning]="getExpiryStatus(elRow) === 'WARNING'">
  <span class="info-label">Péremption</span>
  <span class="info-value">
    {{ elRow?.expirationDate }}
    @if (getExpiryStatus(elRow) === 'CRITICAL') {
      <i class="pi pi-exclamation-triangle text-danger" pTooltip="Expire dans moins de 3 mois"></i>
    }
    @if (getExpiryStatus(elRow) === 'WARNING') {
      <i class="pi pi-clock text-warning" pTooltip="Expire dans moins de 6 mois"></i>
    }
  </span>
</div>
```

Logique TS :
```typescript
getExpiryStatus(produit: IProduit): 'CRITICAL' | 'WARNING' | 'OK' | null {
  if (!produit.expirationDate) return null;
  const diffMonths = differenceInMonths(new Date(produit.expirationDate), new Date());
  if (diffMonths < 3) return 'CRITICAL';
  if (diffMonths < 6) return 'WARNING';
  return 'OK';
}
```

### 5.7 Toolbar améliorée — Légende + actions groupées

```
┌─────────────────────────────────────────────────────────────────┐
│ [☐ sélect. 3]  [⏸ Mettre en veille] [🗄 Archiver] [📤 Exporter]│
│                                                                  │
│ Légende stock : 🟢 Normal  🟠 Sous seuil  🔴 Rupture  ⚫ Inactif│
└─────────────────────────────────────────────────────────────────┘
```

### 5.8 Pagination native p-table (remplacement ngb-pagination)

```html
<p-table
  [value]="produits"
  [lazy]="true"
  [paginator]="true"
  [rows]="itemsPerPage"
  [totalRecords]="totalItems"
  [rowsPerPageOptions]="[10, 20, 50, 100]"
  [sortField]="'totalQuantity'"
  [sortOrder]="1"
  (onLazyLoad)="onLazyLoad($event)"
  currentPageReportTemplate="Affichage de {first} à {last} sur {totalRecords} produits"
  [showCurrentPageReport]="true"
>
```

---

## 6. Workflow désactivation — Diagramme

```
┌──────────┐   Mettre en veille    ┌───────────┐   Archiver   ┌──────────┐
│  ACTIF   │──────────────────────►│ EN VEILLE │─────────────►│ ARCHIVÉ  │
│          │◄──────────────────────│           │              │(readonly)│
└──────────┘   Réactiver           └───────────┘              └──────────┘
     │                                   │
     │ Supprimer                         │ Supprimer
     │ (seulement si 0 mouvement)        │ (seulement si 0 mouvement)
     ▼                                   ▼
  [SUPPRIMÉ]                          [SUPPRIMÉ]
```

**Règles métier** :
- `ACTIF` → visible dans les ventes, les commandes, les inventaires
- `EN VEILLE` → masqué des ventes, commandes bloquées, stock conservé, historique accessible
- `ARCHIVÉ` → lecture seule, tout historique consultable, aucune opération possible
- `SUPPRIMÉ` → uniquement si `totalMouvements = 0` (jamais commandé, jamais vendu, jamais inventorié)

---

## 7. Synthèse des axes d'amélioration — Priorités

| Priorité | Axe | Impact métier | Effort |
|---|---|---|---|
| 🔴 Critique | Indicateurs rotation + jours de stock | Décision de commande quotidienne | Moyen |
| 🔴 Critique | Tri par colonnes (`[sortable]`) | Ergonomie de base attendue | Faible |
| 🔴 Critique | Workflow désactivation vs suppression directe | Intégrité données | Moyen |
| 🔴 Critique | Pagination native `p-table` | Cohérence projet + perf | Faible |
| 🟠 Important | Classe ABC visible sur la liste | Pilotage stock | Faible (données existent) |
| 🟠 Important | Alerte péremption colorée | Conformité réglementaire | Faible |
| 🟠 Important | Menu contextuel `⋮` pour actions secondaires | Lisibilité / UX | Faible |
| 🟠 Important | Légende code couleur stock | Onboarding utilisateur | Très faible |
| 🟡 Utile | Badges réglementaires (liste I/II/stupéfiant) | Conformité officinale | Moyen |
| 🟡 Utile | Onglets dans l'expandable | Clarté information | Faible |
| 🟡 Utile | Bouton Commander inline (stock < seuil) | Productivité | Moyen |
| 🟡 Utile | Historique ventes sparkline | Analyse tendance | Élevé |
| 🔵 Nice-to-have | Image produit | Identification visuelle | Élevé |
| 🔵 Nice-to-have | Emplacement physique (casier) | Logistique interne | Moyen |
| 🔵 Nice-to-have | Génériques / substituts | Conseil officinal | Élevé |

---

---

## 8. Proposition de redesign — Architecture Split Panel

### 8.1 Choix architectural

**Pattern retenu : Split Panel (liste 35% / fiche 65%)** — inspiré de Winpharma, adapté au workflow debout officinal.

Raisons du choix vs alternatives :
- **vs Page dédiée (LGPI)** : navigation plus lente, perte du contexte liste, plus adaptée aux ERP bureau
- **vs Expandable inline (actuel)** : trop dense au-delà de 5 champs, sous-tableaux côte à côte illisibles
- **vs Modale plein écran (Pharmagest)** : coupe la vision liste, inadapté au multi-consultation rapide

Le split panel permet la consultation d'un produit sans quitter la liste — vitesse clé en officine.

### 8.2 Layout cible

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ PRODUIT HOME                                                                   │
│ [Critère ▼] [Rayon ▼] [Famille ▼] [🔍 Recherche...]  [⚙ Colonnes] [+ Nouveau]│
├────────────────────────────┬───────────────────────────────────────────────────┤
│ LISTE COMPACTE (35%)        │ FICHE DÉTAIL (65%)                               │
│                             │                                                   │
│ CIP  │ Libellé    │ Stock   │ DOLIPRANE 1000MG    CIP: 3400936...  [Éditer][⋮] │
│ ──── │ ────────── │ ─────── │ ────────────────────────────────────────────────│
│ ►    │ DOLIPRANE  │ 🔴 0    │ [🔴 Rupture][🟣 En commande]  ← EtaProduitComp. │
│      │ IBUPRO...  │ 🟠 3    │ ────────────────────────────────────────────────│
│      │ ASPIRI...  │ 🟢 48   │ Stock: 0 u   Seuil mini: 5   Jours stock: 0 j  │
│      │ AMOXIC...  │ 🟢 12   │ CMM: 8,3 u/m   Rotation: 8,4×/an   Classe: A  │
│      │ ...        │         │ Marge: 28%   PA: 2,45 €   PV: 3,40 €           │
│                             │ ────────────────────────────────────────────────│
│                             │ [ Synthèse ] [ Stock ] [ Fournisseurs ] [Histo] │
│ 243 produits                │ ← onglets du panneau                             │
└────────────────────────────┴───────────────────────────────────────────────────┘
```

### 8.3 Structure Angular recommandée

```
features/produit/
├── produit.routes.ts
├── feature/
│   ├── produit-home/
│   │   ├── produit-home.component.ts       ← page principale (split panel)
│   │   ├── produit-home.component.html
│   │   └── produit-home.component.scss
│   └── produit-edit/
│       └── produit-edit.component.ts       ← route /produit/:id/edit (formulaire complet)
└── ui/
    ├── produit-list/                        ← colonne gauche, p-table compact
    ├── produit-detail-panel/                ← colonne droite, onglets
    ├── produit-synthese-tab/                ← onglet 1 : KPIs, identificants
    ├── produit-stock-tab/                   ← onglet 2 : répartition, mouvements
    ├── produit-fournisseurs-tab/            ← onglet 3 : fournisseurs, prix
    └── produit-historique-tab/              ← onglet 4 : sparkline 12 mois (Chart.js)
```

### 8.4 Gestion de l'état (Signals)

```typescript
// produit-home.component.ts
export class ProduitHomeComponent {
  selectedProduit  = signal<ProduitDTO | null>(null);
  panelOpen        = computed(() => this.selectedProduit() !== null);
  detailLoading    = signal(false);

  onRowSelect(produit: ProduitDTO): void {
    this.selectedProduit.set(produit);
    // lazy-load des données enrichies (rotation, historique)
  }
}
```

### 8.5 Colonnes de la liste compacte (8 colonnes par défaut)

| # | Colonne | Source DTO | Tri | Masquable |
|---|---|---|:---:|:---:|
| 1 | ☐ Sélection | — | — | ❌ |
| 2 | CIP | `codeCip` | ✅ | ✅ |
| 3 | Libellé | `libelle` | ✅ | ❌ |
| 4 | Stock | `totalQuantity` + code couleur | ✅ | ❌ |
| 5 | Jours stock | `couvertureStockJours` | ✅ | ✅ |
| 6 | Marge % | calculé `(PV-PA)/PV*100` | ✅ | ✅ |
| 7 | Classe ABC | `classeCriticite` | ✅ | ✅ |
| 8 | **Statut** | `EtaProduitComponent` | ❌ | ❌ non négociable |
| 9 | Actions | `[Éditer][⋮]` | — | ❌ |

Colonnes masquables additionnelles : EAN · Prix achat · Prix vente · Qté.Mini · Qté.Réassort · Famille · Rayon · CMM · CA 30j

### 8.6 Panneau détail — Onglet Synthèse

```
DOLIPRANE 1000MG — SANOFI          [Éditer] [Commander] [Étiquette] [⋮]
CIP: 3400936...  EAN: 346789...   DCI: Paracétamol    [A_PLUS]  [Méd. Essentiel]
──────────────────────────────────────────────────────────────────────────────
[🔴 Rupture][🟣 En commande]      ← EtaProduitComponent, plein format, en haut

INDICATEURS CLÉS
  Stock: 0 u           Seuil mini: 5 u        Jours de stock: 0 j   ← critique
  CMM: 8,3 u/mois      Rotation: 8,4 ×/an     Couverture: 0 j
  CA 30j: 280 €        CA 12m: 3 360 €        Marge: 28%  (0,95 € / boite)

IDENTIFICATION
  Famille: Antalgique · Rayon: OTC · Forme: Comprimé · TVA: 5,5%
  Dernière vente: 22/03/2026 · Dernière commande: 20/03/2026
```

### 8.7 Panneau détail — Onglet Historique (sparkline)

Données : `GET /api/produits/{id}/ventes-mensuelles?nbMois=12`
Source : `VentesMensuellesAgregeesRepository.findLastNMonthsByProduit(id, 12)`

Affichage : graphe bar Chart.js — quantités vendues par mois sur 12 mois glissants.

```typescript
// Données pour Chart.js
interface VenteMoisDTO {
  anneeMois: string;       // "2026-03"
  quantiteVendue: number;
  montantCa: number;
  nombreVentes: number;
}
```

---

## 9. Inventaire des données disponibles — Analyse de collecte

### 9.1 Données déjà dans `ProduitDTO` (renvoyées par `/api/produits`)

| Champ DTO | Utilisation liste | Utilisation panneau |
|---|:---:|:---:|
| `id`, `libelle`, `codeCip`, `codeEan` | ✅ | ✅ |
| `totalQuantity` | ✅ colonne Stock | ✅ |
| `seuilMini`, `stockReassort`, `qtyAppro` | — | ✅ onglet Synthèse |
| `costAmount` (PA), `regularUnitPrice` (PV) | masquable | ✅ calcul marge |
| `etatProduit` → `EtaProduitComponent` | ✅ colonne Statut | ✅ hero panel |
| `lastDateOfSale`, `lastOrderDate` | — | ✅ onglet Synthèse |
| `familleLibelle`, `rayonLibelle`, `formeLibelle` | masquable | ✅ onglet Synthèse |
| `laboratoireLibelle`, `gammeLibelle` | — | ✅ onglet Synthèse |
| `tvaTaux`, `deconditionnable`, `typeProduit` | — | ✅ onglet Synthèse |
| `stockProduits` | — | ✅ onglet Stock |
| `fournisseurProduits` | — | ✅ onglet Fournisseurs |
| `categorie` | masquable | ✅ |

**Champs ProduitDTO existants mais NON exploités dans la liste actuelle :**
- `lastDateOfSale` — pertinent pour détecter les produits sans mouvement
- `seuilMini` — utilisé pour la coloration stock (présent dans le DTO mais pas affiché)
- `categorie` — doublon partiel de `familleLibelle`

### 9.2 Données ABSENTES de `ProduitDTO` mais disponibles en BDD

#### A. `classeCriticite` — dans `Produit` entité, absent du DTO

```java
// domain/Produit.java — ligne 181-182
@Enumerated(EnumType.STRING)
@Column(name = "classe_criticite", length = 10)
private ClasseCriticite classeCriticite = ClasseCriticite.B;
```

**Action** : Ajouter `private ClasseCriticite classeCriticite;` dans `ProduitDTO` + le setter/getter + remplissage dans le mapper.
**Effort** : Très faible — champ déjà en BDD, déjà dans l'entité.

#### B. `estMedicamentEssentiel`, `estProduitGarde` — ajoutés en V1.3.3

```sql
-- V1.3.3__mv_abc_pareto_analysis.sql
ALTER TABLE produit
  ADD COLUMN IF NOT EXISTS est_medicament_essentiel BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS est_produit_garde BOOLEAN NOT NULL DEFAULT false;
```

**Action** : Mapper ces champs dans `Produit.java` (si pas encore fait) et les exposer dans `ProduitDTO`.
**Effort** : Faible.

#### C. Indicateurs de rotation — dans `v_stock_rotation`

La vue `v_stock_rotation` calcule (en temps réel, JOIN sur `stock_produit`) :

| Colonne vue | Description | Exposé actuellement |
|---|---|:---:|
| `cmm` | Consommation mensuelle moyenne (qte12m / 12) | ❌ rapport seul |
| `rotation_annuelle_qte` | qte_12m / stock_actuel | ❌ rapport seul |
| `couverture_stock_jours` | stock / CMM * 30 | ❌ rapport seul |
| `ca_30_jours` | CA des 30 derniers jours | ❌ rapport seul |
| `ca_12_mois` | CA des 12 derniers mois | ❌ rapport seul |
| `qte_vendue_12_mois` | Quantité vendue 12 mois | ❌ rapport seul |

**Problème actuel** : `/api/stock/rotation` retourne TOUTE la liste sans filtre ni pagination — c'est un endpoint de rapport PDF, pas adapté à la liste produit.

**Solution recommandée** : Créer un endpoint dédié au panneau détail :

```
GET /api/produits/{id}/indicateurs
→ ProduitIndicateursDTO {
    classeCriticite,       // depuis produit.classe_criticite
    cmm,                   // depuis v_stock_rotation
    rotationAnnuelleQte,   // depuis v_stock_rotation
    couvertureStockJours,  // depuis v_stock_rotation
    ca30Jours,             // depuis v_stock_rotation
    ca12Mois,              // depuis v_stock_rotation
    qteVendue12Mois,       // depuis v_stock_rotation
    tauxMarge              // calculé : (PV - PA) / PV * 100
  }
```

Ce DTO est appelé uniquement à la sélection d'un produit dans le panneau (lazy load), pas pour la liste.

**Alternative pour la liste** : Enrichir directement la requête de listing avec un LEFT JOIN sur `v_stock_rotation` pour avoir `couverture_stock_jours` et `classe_criticite` dans la liste. Impact à évaluer sur les performances (vue non matérialisée).

#### D. Historique mensuel ventes — dans `ventes_mensuelles_agregees`

```java
// VentesMensuellesAgregeesRepository
List<VentesMensuellesAgregees> findLastNMonthsByProduit(Integer produitId, int nbMois);
```

Disponible, non exposé via REST. Nouveau endpoint à créer :

```
GET /api/produits/{id}/ventes-mensuelles?nbMois=12
→ List<VenteMoisDTO> { anneeMois, quantiteVendue, montantCa, nombreVentes }
```

Source : `VentesMensuellesAgregeesRepository.findLastNMonthsByProduit(id, 12)`.
Données gelées (is_frozen) → stable, cacheable.

#### E. Score Pareto par produit — dans `v_abc_pareto_analysis` via `ParetoAnalysisRepository`

```java
// ParetoAnalysisRepository — retourne 7 colonnes :
// produit_id, ca_cumule_pct, rang, ca_total, frequence_mois, qte_vendue, stock_actuel
Optional<Object[]> findByProduitId(Integer produitId);
```

Données utiles pour le panneau détail :
- `rang` → position relative dans le catalogue (ex : "Rang 3 / 1 243 produits")
- `ca_cumule_pct` → contribution cumulée au CA total
- `frequence_mois` → nombre de mois distincts avec au moins 1 vente (indicateur de régularité)

### 9.3 Calculs frontend sans appel API

Les champs suivants peuvent être calculés côté Angular à partir des données déjà dans `ProduitDTO` :

| Indicateur | Calcul | Champs requis |
|---|---|---|
| **Taux de marge brute** | `(PV - PA) / PV * 100` | `regularUnitPrice`, `costAmount` |
| **Marge absolue** | `PV - PA` | idem |
| **Sous seuil mini** | `totalQuantity > 0 && totalQuantity < seuilMini` | `totalQuantity`, `seuilMini` |
| **Rupture** | `totalQuantity <= 0` | `totalQuantity` |

Ces calculs évitent des appels API et enrichissent `EtaProduitComponent` (badge manquant "Sous seuil mini").

### 9.4 Données réglementaires ABSENTES — gap à analyser

Les champs suivants n'existent pas dans le modèle actuel et nécessiteraient une migration :

| Donnée | Table cible | Migration |
|---|---|---|
| Liste I / Liste II / Stupéfiant | `produit.liste_reglementaire` (enum) | Nouvelle colonne + migration |
| Taux de remboursement AMO | `produit.taux_remboursement` (integer %) | Nouvelle colonne |
| Ordonnance obligatoire | `produit.ordonnance_requise` (boolean) | Nouvelle colonne |

**Priorité** : Faible pour la phase 1 du redesign — à considérer pour la conformité réglementaire officinale (CNAM/AMO).

### 9.5 Récapitulatif — Ce qui est prêt vs ce qui manque

| Indicateur | Disponible BDD | Dans DTO | Exposé API | Action |
|---|:---:|:---:|:---:|---|
| Stock actuel | ✅ | ✅ `totalQuantity` | ✅ | **Rien** |
| Seuil mini | ✅ | ✅ `seuilMini` | ✅ | **Rien** |
| Statut workflow | ✅ | ✅ `etatProduit` | ✅ | **Rien** |
| Prix achat / Prix vente | ✅ | ✅ | ✅ | **Rien** |
| Taux de marge | ✅ | calculable | ✅ | **Calcul Angular** |
| Classe ABC | ✅ `produit.classe_criticite` | ❌ | ❌ | Ajouter dans DTO |
| Méd. essentiel / garde | ✅ (V1.3.3) | ❌ | ❌ | Ajouter dans DTO |
| CMM | ✅ `v_stock_rotation.cmm` | ❌ | ❌ rapport seul | Endpoint `/indicateurs` |
| Rotation annuelle | ✅ `v_stock_rotation` | ❌ | ❌ | Endpoint `/indicateurs` |
| Jours de stock | ✅ `v_stock_rotation` | ❌ | ❌ | Endpoint `/indicateurs` |
| CA 30j / 12m | ✅ `v_stock_rotation` | ❌ | ❌ | Endpoint `/indicateurs` |
| Historique mensuel | ✅ `ventes_mensuelles_agregees` | ❌ | ❌ | Endpoint `/ventes-mensuelles` |
| Rang Pareto | ✅ `v_abc_pareto_analysis` | ❌ | ❌ | Endpoint `/indicateurs` |
| Badges réglementaires | ❌ | ❌ | ❌ | Nouvelle migration + DTO |

### 9.6 Plan de mise en œuvre — Phases backend

#### Phase 1 — Aucune migration requise (enrichissement DTO + nouveau endpoint)
1. Ajouter `classeCriticite`, `estMedicamentEssentiel`, `estProduitGarde` dans `ProduitDTO` + mapper
2. Créer `ProduitIndicateursDTO` (record Java)
3. Créer `GET /api/produits/{id}/indicateurs` dans `ProduitResource` → JOIN `v_stock_rotation` + `v_abc_pareto_analysis` par produit_id
4. Créer `GET /api/produits/{id}/ventes-mensuelles` → `VentesMensuellesAgregeesRepository.findLastNMonthsByProduit`

#### Phase 2 — Migration schema (réglementaire)
1. `V1.4.x__produit_reglementaire.sql` : ajouter `liste_reglementaire`, `taux_remboursement`, `ordonnance_requise`
2. Mapper dans `ProduitDTO` et exposer

---

*Analyse mise à jour le 2026-03-23 — Pharma-Smart v1.x*
