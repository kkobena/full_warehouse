# PLAN — Lazy-loading des dashboards d'accueil et des onglets de facturation

> Statut : **proposition**
> Origine : chunks de build anormalement lourds constatés en production —
> `home-home-component` (1.15 MB brut / 252.27 kB gzip) et
> `facturation-layout-facturation-layout-component` (1.04 MB brut / 58.21 kB gzip).
> Périmètre : `pharmaSmart-app/src/main/webapp` — aucun changement backend.

---

## 1. Constat

`HomeComponent` et `FacturationLayoutComponent` sont des **coquilles** : à l'écran, un seul
sous-composant est visible à la fois (un dashboard selon le rôle résolu, ou l'onglet actif d'un
`ngb-nav`). Mais les deux déclarent tous leurs enfants dans leur tableau Angular `imports: [...]`,
ce qui les fait tous embarquer **statiquement** dans le même chunk lazy-loadé — y compris ceux qui
ne seront jamais affichés pour l'utilisateur courant.

Le rendu conditionnel (`@switch`, `ngbNavContent`) contrôle déjà correctement ce qui s'affiche
dans le DOM. Le problème n'est pas le rendu, c'est le **bundling** : Angular n'a aucun moyen de
savoir qu'un import n'est utile qu'à l'intérieur d'une branche `@case` ou d'un onglet tant qu'il
n'est pas explicitement placé dans un bloc `@defer`.

### 1.1 Confirmation du builder et absence de garde-fou existant

- Builder : `@angular-builders/custom-webpack:browser` (webpack, pas le builder `application`
  esbuild) — `angular.json:21`. `webpack.custom.js` ne redéfinit aucun `splitChunks`/
  `optimization` : le découpage standard d'Angular CLI s'applique, `@defer` doit donc produire des
  chunks séparés sans configuration webpack supplémentaire à ajouter.
- `angular.json` ne définit un budget que sur le bundle **initial** (3–4 Mo) et sur les styles de
  composant (60 kb) — aucun budget n'existe aujourd'hui sur la taille des chunks lazy, ce qui
  explique que la dérive soit passée inaperçue.
- Aucun `@defer` n'est utilisé nulle part dans le code actuel (`grep -rl "@defer" app --include=*.html`
  ne retourne rien). Ce chantier **introduit** le pattern, il n'en généralise pas un existant — la
  section 3 fixe donc la convention à suivre pour la suite du projet.

---

## 2. Périmètre détaillé

### 2.1 `app/home/home.component.ts` (1.15 MB)

Rendu actuel (`home.component.html`) : un `@switch (resolvedLayout()?.componentKey)` avec 4
branches, une seule active à la fois selon le rôle résolu par `DashboardResolverService`.

| Enfant | Poids constaté | Dépendances lourdes |
|---|---|---|
| `HomeBaseComponent` (dashboard Pharmacien, cas par défaut) | Élevé | `chart.js` (via `app/shared/chart/chart.component.ts`), `DataTableComponent`/`SelectComponent`/`SkeletonComponent`, et **5 services de rapport** avec leurs DTO (`MargeReportService`, `DashboardCAService`, `StockValuationReportService`, `TiersPayantReportService`, `SupplierPerformanceReportService`) |
| `CaissierDashboardComponent` | Modéré | `NgbModal`, `FormTransactionComponent` (modale), `TauriPrinterService`, `RecapitulatifCaisseService` |
| `CommandeHomeComponent` | Élevé — **anti-pattern de second niveau**, voir 2.1.1 | — |
| `DefaultDashboardComponent` | Faible | uniquement Angular core/router/account |

`HomeBaseComponent` sert deux branches (`PHARMACIEN` et le cas `@default` sans `layoutConfig`) :
il reste donc chargé dans la majorité des cas, mais `CaissierDashboardComponent`,
`CommandeHomeComponent` et `DefaultDashboardComponent` ne concernent chacun qu'un rôle — pour un
pharmacien connecté, les trois autres pèsent dans le bundle initial de la route `/` pour rien.

#### 2.1.1 Effet de bord : `CommandeHomeComponent` reproduit le même anti-pattern

`app/features/commande/feature/commande-home/commande-home.component.ts` est lui-même un shell à
onglets (`ngb-nav`, 5 onglets : Tableau de bord Appro, Commandes & Réceptions, Répartition &
Transferts, Retours fournisseurs, Bons d'Entrée Diverses) qui importe **statiquement**
`ApproUnifiedDashboardComponent`, `SuggestionsUnifiedComponent`, `AppRepartitionStockComponent`,
`AppRetourFournisseurComponent` et `BedHomeComponent`. Comme ce composant est lui-même l'un des
enfants de `HomeComponent`, cette deuxième couche gonfle `home-home-component` même pour un
utilisateur qui n'a jamais le rôle `COMMANDE` par défaut — et gonflera aussi, une fois traité,
l'écran ordinaire du module Achats qui réutilise le même composant (cf.
`cahier-recette.model.ts`, fonctionnalité "Tableau de bord du module Achats"). Elle doit être
traitée dans le **même chantier**, avec le même pattern, sans quoi le gain sur `home.component`
reste partiel.

### 2.2 `app/features/facturation/feature/facturation-layout/facturation-layout.component.ts` (1.04 MB)

Rendu actuel (`facturation-layout.component.html`) : `ngbNav` vertical avec 9 onglets, chacun
conditionné par un signal d'habilitation (`showEdition()`, `showFactures()`...) et un
`<ng-template ngbNavContent>` — le **DOM** d'un onglet n'est instancié qu'à son activation
(comportement standard de `ngbNavContent`), mais les **9 classes de composants** sont importées de
façon statique donc bundlées ensemble.

| Onglet / composant | Lignes | Dépendances notables |
|---|---|---|
| Édition — `FacturationEditionComponent` | 299 | `PharmaDatePickerComponent`, `NgbConfirmDialogService` |
| Factures — `FacturationHomeComponent` | 230 | `PharmaDatePickerComponent`, `shared/ui` |
| Historique règlements — `HistoriqueReglementsComponent` | 258 | `TauriPrinterService`, `NgbConfirmDialogService` |
| Rapprochement — `RapprochementComponent` | 312 | `TiersPayantService`, `RapprochementApiService` |
| Récapitulatif — `RecapitulatifComponent` | 258 | `RecapitulatifApiService`, `RecapitulatifKpiBannerComponent` |
| Comptes fournisseurs — `ComptesFournisseursComponent` | 452 | `DataTableComponent`, `BlobDownloadService`, `FournisseurApApiService` |
| Remises & RFA — `RemisesRfaComponent` | 44 | `DataTableComponent`, `ToolbarComponent` |
| Avoirs TP — `AvoirComponent` | 368 | `TiersPayantService`, `AvoirApiService` |
| Automatisation — `PlanificationComponent` | 28 | léger |

Aucun de ces 9 écrans n'utilise `chart.js`/`ag-grid`/GridStack : le poids vient du **cumul** de 9
écrans métier complets (~2250 lignes de TS + leurs templates/services), pas d'une seule dépendance
lourde isolée — ce qui confirme que le vrai gain vient du découpage, pas d'un remplacement de
librairie.

---

## 3. Solution retenue : `@defer` par branche/onglet

Angular code-split automatiquement tout composant dont l'unique point d'usage dans un template est
un bloc `@defer` — sans configuration webpack supplémentaire (cf. 1.1). Le composant reste déclaré
dans `imports: [...]` (nécessaire pour la vérification de type du template), mais son import JS
devient un `import()` dynamique généré par le compilateur.

### 3.1 `home.component.html`

Chaque branche `@case`/`@default` est déjà mutuellement exclusive : il suffit d'entourer le
contenu de chaque branche d'un `@defer (on immediate)` (chargement démarré dès que la branche est
sélectionnée, pas d'attente d'une interaction utilisateur — équivalent au comportement actuel,
mais en chunk séparé) avec un `@loading` reprenant le skeleton déjà utilisé pour la résolution du
layout (`app-skeleton`, déjà importé) :

```html
@case ('CAISSIER') {
  @defer (on immediate) {
    <app-caissier-dashboard></app-caissier-dashboard>
  } @loading {
    <app-skeleton width="100%" height="160px" />
  }
}
```

À répéter pour les 4 branches. `HomeBaseComponent` étant partagé entre deux branches, un seul bloc
`@defer` suffit si on restructure légèrement le `@switch` (ou on duplique le bloc — impact
négligeable, Angular dédoublonne le chunk généré pour le même composant).

### 3.2 `commande-home.component.html`

Même traitement sur les 5 `ng-template ngbNavContent` : englober le composant de chaque onglet
dans `@defer (on immediate)`, `@loading` avec un skeleton ou spinner cohérent avec le reste de
l'écran (à vérifier dans le template actuel).

### 3.3 `facturation-layout.component.html`

Même traitement sur les 9 `ng-template ngbNavContent`. Ici `ngbNavContent` retarde déjà
l'instanciation du template à l'activation de l'onglet, donc `@defer (on immediate)` à l'intérieur
démarre le téléchargement du chunk **au moment de l'activation de l'onglet**, pas avant — c'est le
comportement souhaité (pas de préchargement des 9 écrans à l'ouverture de la facturation) :

```html
<ng-template ngbNavContent>
  @defer (on immediate) {
    <app-facturation-edition />
  } @loading {
    <app-skeleton width="100%" height="240px" />
  }
</ng-template>
```

### 3.4 Pourquoi pas `@defer (on viewport)` ou un autre déclencheur

`on viewport`/`on interaction` n'apportent rien ici : le composant n'est instancié par Angular
qu'au moment exact où il doit être visible (branche `@switch` sélectionnée, onglet activé). Le
seul objectif est de transformer un import statique en import dynamique **à ce moment précis** —
`on immediate` (qui signifie "dès que ce bloc est atteint", pas "dès le chargement de la page")
est le déclencheur correct.

---

## 4. Étapes

| # | Étape | Fichiers |
|---|---|---|
| 1 | Convertir `home.component.html` (4 branches) | `app/home/home.component.html` |
| 2 | Convertir `commande-home.component.html` (5 onglets) | `app/features/commande/feature/commande-home/commande-home.component.html` |
| 3 | Convertir `facturation-layout.component.html` (9 onglets) | `app/features/facturation/feature/facturation-layout/facturation-layout.component.html` |
| 4 | Build prod + comparer les tailles de chunks avant/après (`npm run webapp:prod`, relevé des tailles dans le rapport de build) | — |
| 5 | Vérifier manuellement chaque branche/onglet dans le navigateur (pas de régression visuelle, pas d'erreur de chargement différé) | — |
| 6 | Ajouter un budget de taille sur les chunks lazy dans `angular.json` (`type: "anyComponentStyle"` existe déjà pour les styles ; ajouter un budget `initial` réajusté ou documenter le seuil constaté pour éviter une nouvelle dérive silencieuse) | `angular.json` |

Aucune étape ne touche au backend, aux migrations, ni à un contrat d'API — risque limité au
comportement de chargement front (voir section 5).

---

## 5. Risques

| Risque | Probabilité | Impact | Mitigation |
|---|---|---|---|
| Flash de contenu (`@loading`) perceptible sur un réseau lent, aucun n'existait avant | Moyenne | Faible | Réutiliser `app-skeleton`, déjà présent dans le codebase pour ce cas d'usage |
| `ngbNavContent` + `@defer` interagissent mal (double lazy) | Faible | Moyen | Vérifier sur un onglet isolé avant de généraliser aux 9 ; `@defer` est un simple wrapper de template, sans dépendance à `ngbNavContent` |
| `HomeBaseComponent` dupliqué dans deux `@defer` génère deux chunks au lieu d'un | Faible | Faible | Angular dédoublonne par référence de composant ; à vérifier sur le rapport de build (étape 4) |
| `CommandeHomeComponent` réutilisé ailleurs (écran ordinaire du module Achats) et déjà converti en interne — pas de régression attendue mais à tester aux deux points d'entrée | Faible | Moyen | Tester l'écran depuis `home` (rôle COMMANDE) **et** depuis la navigation normale du module Achats |

---

## 6. Hors périmètre

- Remplacement de `chart.js` ou d'une autre librairie — le poids de `HomeBaseComponent` vient de
  son usage réel (graphiques du tableau de bord), pas d'un choix de librairie à remettre en
  question ici.
- Conversion en routes lazy à la place de `@defer` — les onglets `ngb-nav` et le `@switch` de rôle
  ne sont pas des routes distinctes aujourd'hui, et migrer vers un routing dédié serait un chantier
  de navigation séparé, plus risqué, pour un gain équivalent.
- Le `TODO Phase 4` du dashboard personnalisable GridStack (`home.component.html:27-31`) — non lié
  à ce chantier, à traiter indépendamment quand il sera implémenté.
