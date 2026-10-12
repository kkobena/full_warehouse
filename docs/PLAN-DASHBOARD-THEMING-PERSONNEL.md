# Dashboard — Theming et tableau personnel

> Analyse et décisions — 2026-10-11.

## 1. Problèmes de theming

### 1.1 Charts non réactifs au changement de thème ✅ corrigé

**Symptôme** : après un changement de thème (`ThemeService.setTheme`), les graphiques Chart.js
conservent les couleurs du thème au chargement — texte des axes, grille et fond de surface restent
figés.

**Cause** : dans `features/dashboard/models/chart-config.ts`, la fonction `theme()` était appelée
une seule fois au moment où `buildChart()` était invoqué, et lisait `window.matchMedia(…)` — la
préférence OS, pas le jeton `data-theme` posé par `ThemeService`. Le signal `ThemeService.theme`
n'était pas déclaré comme dépendance du `computed` qui appelle `buildChart`.

**Correction apportée** :

1. `chart-config.ts` — `theme()` lit désormais les jetons Bootstrap via
   `getComputedStyle(document.documentElement)` :

   | Propriété | Jeton CSS lu |
   |---|---|
   | `text` | `--bs-body-color` |
   | `muted` | `--bs-secondary-color` |
   | `grid` | `--bs-border-color-translucent` |
   | `surface` | `--bs-body-bg` |

   Ces jetons sont surchargés par chaque thème pharma (`_pharma-themes.scss`). La valeur dure
   n'est plus qu'un repli pour les environnements SSR / test sans DOM.

2. `data-widget.component.ts` — `ThemeService` est injecté et `themeService.theme()` est
   consommé dans le `computed chart` comme dépendance de signal :

   ```typescript
   protected readonly chart = computed(() => {
     this.themeService.theme(); // déclenche un recalcul à chaque changement de thème
     …
     return series ? buildChart(viz, series) : null;
   });
   ```

   Quand le thème change, le `computed` se réévalue, `buildChart` relit les jetons CSS courants
   via `getComputedStyle`, et `ChartComponent` reçoit de nouveaux `data`/`options` — ce qui
   déclenche son `effect` interne et appelle `chart.update()`.

**Fichiers modifiés** :
- `features/dashboard/models/chart-config.ts`
- `features/dashboard/widgets/data-widget/data-widget.component.ts`

---

### 1.2 GridStack — handles non themés

**Symptôme** : en mode édition, les poignées de redimensionnement et les bordures de
`grid-stack-item` conservent des couleurs hardcodées de la bibliothèque (blanc ou gris clair) qui
ne s'adaptent pas aux thèmes foncés.

**Cause** : la feuille CSS de GridStack est injectée globalement et ne connaît pas les jetons
Bootstrap ni `data-theme`.

**Correction à faire** : ajouter dans `dashboard-grid.component.ts` des surcharges CSS ciblées
sur les classes GridStack :

```scss
// dans les styles du composant, avec ::ng-deep pour traverser l'encapsulation
::ng-deep .grid-stack-item > .grid-stack-item-content {
  border: 1px solid var(--bs-border-color);
}
::ng-deep .ui-resizable-handle {
  background-color: var(--bs-secondary-bg);
  border-color: var(--bs-border-color);
}
```

À tester à l'écran sous chaque thème avant de fermer. **Non encore implémenté.**

---

### 1.3 PrimeIcons dans les templates du dashboard

**Symptôme** : les icônes `pi pi-*` utilisées dans `dashboard-editor.component.html` et
`widget-tile.component.ts` ne s'affichent que si `primeicons.css` est chargé globalement.

**État** : PrimeNG a été retiré des dépendances, mais PrimeIcons est chargé séparément en tant
qu'icône seule (feuille CSS autonome). Vérifier que l'entrée dans `angular.json` (section `styles`)
est bien présente et ne sera pas retirée par mégarde lors d'une prochaine mise à jour.

```json
// angular.json — à maintenir
"styles": [
  "node_modules/primeicons/primeicons.css",
  …
]
```

**Aucune action corrective requise pour l'instant** ; surveiller en cas de montée de version.

---

## 2. Tableau personnel vs pilotage

### 2.1 Ce que le pilotage couvre déjà

L'onglet **Clients & équipe** (`features/pilotage/feature/onglet-clients-equipe/section-equipe`)
propose déjà :

- Tableau par vendeur : 8 métriques (CA, nb ventes, panier moyen, articles/vente, taux marge, taux
  remise, annulations, avoirs, ordonnances) avec variation vs référence et ligne d'équipe en pied.
- Carte de chaleur CA × vendeur × heure (`app-croise-analyse`).
- Granularité configurable (jour / semaine / mois / trimestre / année) et comparaison au choix.

Ce tableau est **analytique** — il demande de choisir une période, d'aller sur l'onglet et de lire
un tableau dense. Il est fait pour des décisions de gestion.

### 2.2 Ce que le dashboard « personnel » apporterait

Un widget dashboard dédié à l'équipe n'est pertinent que s'il répond à un besoin **opérationnel**
différent : coup d'œil au comptoir, sans naviguer vers le pilotage.

| | Pilotage section-équipe | Widget dashboard équipe |
|---|---|---|
| **Cible** | Pharmacien/manager en analyse | Superviseur au comptoir |
| **Granularité** | Période configurable | Aujourd'hui uniquement |
| **Métriques** | 8+ avec variations | 3-4 essentiels |
| **Navigation** | Drill-down, fil de descente | Lecture seule |
| **Pertinence** | Décision stratégique | État du jour en un coup d'œil |

### 2.3 Recommandation : widget `vendeurs-du-jour`

Ajouter un widget **`vendeurs-du-jour`** dans la catégorie `CAISSE` (ou une nouvelle `EQUIPE`)
avec les caractéristiques suivantes :

- **Données temps réel du jour uniquement** (`usesPeriod: false` — pas d'entrée date) : il
  reflète toujours le jour courant sans réglage.
- **Colonnes** : vendeur, nb ventes, CA TTC, panier moyen. Pas de taux de marge ni de remises
  (données sensibles, réservées au pilotage analytique).
- **Droit** : `widget.vendeurs-du-jour` accordé uniquement aux rôles ADMIN et PHARMACIEN ; le
  caissier ne voit pas les chiffres de ses collègues.
- **Lien en pied de tuile** vers `/pilotage` (onglet Clients & équipe) pour approfondir.
- **Taille par défaut** : `[6, 4]`.

Déclaration front à ajouter dans `widget-registry.ts` :

```typescript
data(
  'vendeurs-du-jour',
  "Équipe aujourd'hui",
  "Ventes du jour par vendeur, mise à jour en temps réel",
  'CAISSE',
  'pi pi-users',
  [6, 4],
  table(false),
),
```

Côté backend : un `WidgetDataProvider` qui délègue à `DashboardCAService` (ou un nouveau
`EquipeJourService`) en filtrant sur la date du jour, sans passer de plage de dates.

### 2.4 Ce qu'il ne faut pas faire

- Ne pas dupliquer les 8 métriques du pilotage dans le widget dashboard : cela rend le widget
  aussi lourd que la vue analytique, sans apporter de valeur ajoutée.
- Ne pas afficher les taux de marge ni les remises par vendeur dans le dashboard : ces informations
  sont sensibles et leur contexte (comparaison historique, nuance) est essentiel — le pilotage est
  le bon endroit.
- Ne pas supprimer la section équipe du pilotage au profit du widget : les deux n'ont pas le même
  niveau de lecture.

### 2.5 Priorité proposée pour le widget équipe

Ce widget est de **phase 3** (lot 2 du plan `PLAN-DASHBOARD-PERSONNALISABLE.md`, § 6.4) : fort
intérêt opérationnel, faible effort (le backend dispose déjà de `DashboardCAService`).

---

## 3. Enrichissement des tuiles KPI ✅ implémenté

> Décision 2026-10-11 : chaque tuile KPI doit afficher le maximum d'information en un coup d'œil.
> Source de données unifiée avec le pilotage (même `VenteRecord` via `DashboardService.getPeridiqueCa`).

### 3.1 Modèle étendu

**Backend** (`WidgetData.Kpi`) — deux nouveaux champs :

| Champ | Type | Rôle |
|---|---|---|
| `secondary` | `List<Secondary>` | Métriques secondaires structurées (label + valeur + unité) |
| `objective` | `Number` | Objectif de période (repère visuel) |

`Secondary` est un record imbriqué : `String label, Number value, String unit`.

**Frontend** (`KpiData`) — mêmes champs + `KpiSecondary` interface :
```typescript
export interface KpiSecondary { label: string; value: number | null; unit: string | null; }
```

### 3.2 Widgets enrichis

| Widget | Métriques secondaires désormais incluses |
|---|---|
| `ca-net` | Ventes (nb), Taux marge (%), Panier moyen (FCFA) |
| `marge-brute` | Taux de marge (%), Coût d'achat (FCFA) |
| `panier-moyen` | Ventes (nb), CA net (FCFA) |

Chaque tuile remplace ainsi les trois KPI précédents séparés : la tuile CA net seule couvre toutes
les métriques essentielles d'une période.

### 3.3 Source de données unifiée

Les trois widgets partagent le même appel `dashboardService.getPeridiqueCa(period.toVenteParam())`
— identique à ce que le pilotage utilise en amont — pour garantir une cohérence parfaite entre les
chiffres affichés dans le dashboard et ceux de l'onglet pilotage.

### 3.4 Rendu visuel

La tuile KPI est redessinée :

```
┌─ CA net ──────────────────────────── [↗] ─┐
│  12 450 FCFA                               │
│  ▲ +5,2 %  vs période précédente         │
│  ─────────────────────────────────────    │
│  Ventes     Taux marge    Panier          │
│    54          12,5 %     230 FCFA        │
└────────────────────────────────────────────┘
```

Trois métriques en grille (`kpi-secondaries-sm`) quand il y en a 3+, deux en liste sinon.
La règle de formatage est centralisée dans `formatSecondaryValue` (`widget-data.ts`).

### 3.5 Footers de tableaux

Widgets tableau mis à jour avec un pied de page :

| Widget | Pied ajouté |
|---|---|
| `alertes-officine` | « X alerte(s) en cours » ou « Aucune alerte » |
| `achats-par-fournisseur` | « X fournisseur(s) · total Y FCFA » |
| `stock-dormant` | Déjà présent (total immobilisé) |
| `peremptions` | Déjà présent (valeur totale) |

### 3.6 Fichiers modifiés

- `service/dashboard/widget/WidgetData.java` — `Kpi` record étendu
- `service/dashboard/widget/provider/VentesWidgets.java` — 3 widgets enrichis
- `service/dashboard/widget/provider/StockAchatsWidgets.java` — 2 footers ajoutés
- `features/dashboard/models/dashboard.model.ts` — `KpiSecondary` + `KpiData`
- `features/dashboard/models/widget-data.ts` — `formatSecondaryValue`
- `features/dashboard/widgets/data-widget/data-widget.component.*` — template + styles
