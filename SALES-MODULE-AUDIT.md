# Audit du module Sales

> Analyse complete du module `src/main/webapp/app/features/sales/`
> Date : 2026-02-23 | ~12 800 lignes | 45 fichiers TypeScript

---

## Table des matieres

1. [Vue d'ensemble de l'architecture](#1-vue-densemble)
2. [Problemes critiques](#2-problemes-critiques)
3. [Problemes majeurs](#3-problemes-majeurs)
4. [Problemes mineurs](#4-problemes-mineurs)
5. [Pistes d'amelioration](#5-pistes-damelioration)

---

## 1. Vue d'ensemble

### Structure du module

```
sales/
  data-access/
    facades/   sales.facade.ts              (1 802 lignes)
    store/     sales.store.ts               (  568 lignes)
    services/  sales-api.service.ts          (  511 lignes)
               + 8 services specialises
    utils/     sales-line.utils.ts
  feature/
    sales-home/         (697 lignes)  Orchestrateur principal (tabs)
    sale-creation/    (1 019 lignes)  Vente COMPTANT
    sale-assurance/   (1 074 lignes)  Vente ASSURANCE
    sale-carnet/        (806 lignes)  Vente CARNET
    sale-devis/         (596 lignes)  Devis
    presale-home/        (11 lignes)  Wrapper prevente
    devis-home/          (11 lignes)  Wrapper devis
  shared/
    mixins/  6 mixins                      (1 902 lignes total)
    styles/  _sales-common.scss
  ui/        14 composants presentationnels
  models/    enumerations, barrel exports
  services/  payment-mode-manager.service.ts
```

### Flux de donnees

```
Interaction utilisateur (UI @Output)
  -> Feature Component (handler)
    -> SalesFacade (orchestration)
      -> SalesApiService (HTTP)
      -> SalesStore (patchState)
    -> Signal change detection
  -> Re-rendu composants (OnPush)
```

---

## 2. Problemes critiques

### 2.1 SalesFacade surdimensionne (1 802 lignes)

**Fichier :** `data-access/facades/sales.facade.ts`

La facade concentre TOUTE la logique metier : creation, modification, suppression, impression, tiers payants, remises, paiement, mise en attente, transformation de vente. C'est un God Object.

**Impact :** Difficile a maintenir, a tester, et a faire evoluer. Chaque modification risque des regressions.

---

### 2.2 Duplication massive entre composants feature (~60-70%)

**Fichiers :** `sale-creation.component.ts`, `sale-assurance.component.ts`, `sale-carnet.component.ts`

Ces 3 composants (2 900 lignes cumulees) dupliquent :

- Les souscriptions `ngOnInit()` aux evenements facade (standbySuccess$, productAddedSuccess$, lineUpdatedSuccess$, etc.) - ~11 souscriptions identiques par composant
- Les handlers produit (onProductSelected, onProductScanned, onAddQuantity)
- Les handlers ligne (onLineQuantityChanged, onLineRemoved)
- La logique annulation/mise en attente/prevente
- L'initialisation des mixins (meme pattern dans les 3)

**Exemple de duplication :**
```typescript
// Identique dans les 3 composants :
this.facade.standbySuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
  this.resetForNewSale();
});
this.facade.productAddedSuccess$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
  this.productHandling.resetProductSelection();
  this.focusProductSearch();
});
// ... 9 autres souscriptions identiques
```

---

### 2.3 Triplication de la logique de chargement de vente dans la facade

**Fichier :** `sales.facade.ts`

3 methodes quasi-identiques (~95% de code commun) :

| Methode | Lignes | Difference unique |
|---------|--------|-------------------|
| `loadSaleForEdit()` | 249-295 | Emet `saleReloadedSuccessSubject` |
| `loadSale()` | 358-403 | Emet `saleReloadedToEditSuccessSubject` |
| `resumePendingSale()` | 406-459 | Check `statut !== CLOSED` + `removePendingSale()` |

Toutes font : `setLoading(true)` -> `findSale()` -> `setCurrentSale()` -> `setSaleType()` -> `setSelectedCustomer()` -> `setCashier()` -> `setSeller()` -> `setLoading(false)`

---

### 2.4 Etat `loading` global partage (BUG CORRIGE)

**Statut :** Corrige dans ce commit (ajout de `pendingSalesLoading`).

Le `store.loading` etait utilise a la fois par les operations de vente ET par `loadPendingSales()`. Via le `setupSpinnerEffect` du force-stock mixin, cela declenchait `NgxSpinner.show()` sur le UI principal quand le drawer des ventes en attente chargeait ses donnees.

---

## 3. Problemes majeurs

### 3.1 Gestion d'erreur inconsistante

**Fichier :** `sales.facade.ts`

3 patterns differents dans le meme fichier :

**Pattern A** - Helper `extractApiError()` (standardise) :
```typescript
const { errorMessage, errorKey } = this.extractApiError(error, "message par defaut");
```

**Pattern B** - Extraction inline (duplique la logique du helper) :
```typescript
// updateLineQuantityRequested() lignes 954-966
let errorMessage = 'Erreur...';
let errorKey: string | undefined;
if (error?.error) {
  if (error.error.message) errorMessage = error.error.message;
  else if (error.error.detail) errorMessage = error.error.detail;
  errorKey = error.error.errorKey;
}
```

**Pattern C** - Acces direct sans extraction :
```typescript
this.store.setError(error.message || 'Erreur lors du chargement');
```

**Impact :** Si le format d'erreur backend change, il faut modifier 3 patterns dans le meme fichier.

---

### 3.2 Subscriptions manuelles sans cleanup dans la facade

**Fichier :** `sales.facade.ts`

Plusieurs methodes creent des souscriptions `.subscribe()` sans `takeUntilDestroyed` ni unsubscribe :

| Methode | Lignes | Risque |
|---------|--------|--------|
| `removeTiersPayantFromSale()` | 638-657 | Fuite memoire si navigation |
| `addTiersPayantToSale()` | 683-701 | Fuite memoire si navigation |
| `setCustomer()` | 1121-1149 | Fuite memoire si navigation |
| `removeCustomer()` | 1173-1191 | Fuite memoire si navigation |
| `doTransformCashSale()` | 1754-1776 | Fuite memoire si navigation |
| `putOnStandby()` | 1211-1230 | Fuite memoire si navigation |

**Note :** La facade est `providedIn: 'root'`, donc ces souscriptions vivent tant que l'app est ouverte. En theorie les requetes HTTP se completent rapidement, mais en cas de lenteur reseau, des souscriptions orphelines peuvent s'accumuler.

---

### 3.3 9 Subjects exposes comme event bus

**Fichier :** `sales.facade.ts`

```typescript
private readonly standbySuccessSubject = new Subject<void>();
private readonly productAddedSuccessSubject = new Subject<void>();
private readonly lineUpdatedSuccessSubject = new Subject<void>();
private readonly lineRemovedSuccessSubject = new Subject<void>();
private readonly saleReloadedSuccessSubject = new Subject<void>();
private readonly customerRemovedSuccessSubject = new Subject<void>();
private readonly cancelSaleSuccessSubject = new Subject<void>();
private readonly customerSetSuccessSubject = new Subject<void>();
private readonly remiseUpdatedSuccessSubject = new Subject<void>();
private readonly tiersPayantAddedSuccessSubject = new Subject<IClientTiersPayant>();
private readonly saleReloadedToEditSuccessSubject = new Subject<void>();
private readonly resumePendingSaleSuccessSubject = new Subject<void>();
```

**Problemes :**
- `Subject` (pas `ReplaySubject`) : un subscriber qui arrive apres l'emission rate l'evenement
- Chaque composant enfant doit souscrire manuellement a chacun de ces sujets dans `ngOnInit()`
- Couple fortement facade et composants
- Difficile a tester unitairement

---

### 3.4 Ecriture de signal dans un effect (force-stock mixin)

**Fichier :** `shared/mixins/force-stock.mixin.ts:202-225`

```typescript
effect(() => {
  const isLoading = loading();
  const previousLoading = previousLoadingState();
  // ...
  previousLoadingState.set(isLoading); // Ecrit dans un signal lu par le meme effect
});
```

Chaque changement de `loading` cause 2 executions de cet effect (l'ecriture dans `previousLoadingState` retrigger l'effect). Multiplie par le nombre de composants actifs.

---

### 3.5 Services inutilises dans data-access/services/

---

## 4. Problemes mineurs

### 4.1 clearError() vs setError(null) - usage inconsistant

Le store expose une methode `clearError()` qui fait `patchState(store, { error: null, errorDetails: null })`. Mais dans la facade, les deux patterns coexistent :

```typescript
// Parfois :
this.store.clearError();

// Parfois :
this.store.setError(null);
this.store.setLastErrorDetails(null);
```

### 4.2 Conversion de date dupliquee dans SalesApiService

Deux methodes quasi-identiques :
- `convertItemDateFromClient(salesLine)`
- `convertDateFromClient(sales)`

Les deux convertissent `createdAt` et `updatedAt` de Moment en JSON.

### 4.3 Anti-pattern : conversion signal pour contourner optionalite

Tous les composants feature font :
```typescript
private isCashRegisterOpenSignal = computed(() => this.isCashRegisterOpen() ?? false);
```
Juste pour passer un `Signal<boolean>` au lieu de `Signal<boolean | undefined>` au mixin.

### 4.4 Sidebar state duplique

`sidebarCollapsed` et `pendingSalesSidebar` sont declares comme signaux locaux dans `SalesHomeComponent` ET certains composants enfants, alors que le store possede deja `sidebarCollapsed`.

### 4.5 `executeAndReloadSale` - option `clearErrorOnSuccess` rarement utilisee

L'option existe dans l'interface `ExecuteAndReloadOptions` mais n'est presque jamais passee. Le clear d'erreur apres succes devrait etre le comportement par defaut.

---

## 5. Pistes d'amelioration

### 5.1 [CRITIQUE] Decouper SalesFacade en sous-facades

```
sales.facade.ts (1 802 lignes)
  -> sale-lifecycle.facade.ts     Creation, chargement, annulation, transformation
  -> sale-product.facade.ts       Ajout/modif/suppression produits, force stock
  -> sale-payment.facade.ts       Paiement, mise en attente, finalisation
  -> sale-customer.facade.ts      Client, tiers payants, remises
  -> sale-print.facade.ts         Impression ticket/facture
```

Chaque sous-facade injecte le store et l'API service. La facade principale reexpose les methodes ou delegue.

---

### 5.2 [CRITIQUE] Extraire un mixin `sale-lifecycle.mixin.ts`

Factoriser les ~11 souscriptions `ngOnInit()` communes aux 3 composants (creation, assurance, carnet) :

```typescript
export function createSaleLifecycleHandling(context: {
  facade: SalesFacade;
  destroyRef: DestroyRef;
  resetForNewSale: () => void;
  resetProductSelection: () => void;
  focusProductSearch: () => void;
  loadPendingSalesCount?: () => void;
}) {
  function initializeSubscriptions(): void {
    context.facade.standbySuccess$.pipe(takeUntilDestroyed(context.destroyRef))
      .subscribe(() => context.resetForNewSale());
    context.facade.productAddedSuccess$.pipe(takeUntilDestroyed(context.destroyRef))
      .subscribe(() => { context.resetProductSelection(); context.focusProductSearch(); });
    // ... les 9 autres
  }
  return { initializeSubscriptions };
}
```

**Gain estime :** ~150-200 lignes supprimees par composant (450-600 lignes total).

---

### 5.3 [MAJEUR] Factoriser les 3 methodes de chargement de vente

Remplacer `loadSaleForEdit`, `loadSale`, et `resumePendingSale` par un helper prive :

```typescript
private loadSaleAndHydrate(
  saleId: SaleId,
  options: {
    successSubject: Subject<void>;
    beforeHydrate?: (sale: ISales) => boolean; // retourne false pour abort
    afterHydrate?: (sale: ISales) => void;
  }
): void {
  // Logique commune : setLoading, findSale, setSaleType, setCustomer, setCashier, setSeller
}
```

**Gain estime :** ~80 lignes supprimees dans la facade.

---

### 5.4 [MAJEUR] Standardiser la gestion d'erreur

Utiliser `extractApiError()` partout dans la facade :

```typescript
// AVANT (inline, duplique) :
let errorMessage = '...';
if (error?.error?.message) errorMessage = error.error.message;

// APRES (standardise) :
const { errorMessage, errorKey } = this.extractApiError(error, 'message par defaut');
```

---

### 5.5 [MAJEUR] Remplacer les Subjects par des signaux computeds ou store events

Au lieu de 12 Subjects exposes comme event bus, utiliser un signal d'evenement dans le store :

```typescript
// Option A : Signal d'evenement dans le store
lastEvent: signal<{ type: 'PRODUCT_ADDED' | 'LINE_UPDATED' | 'STANDBY' | ..., payload?: any } | null>(null);

// Option B : Retourner des Observables depuis les methodes facade
putOnStandby(): Observable<boolean> {
  // Les composants souscrivent au retour de la methode, pas a un Subject global
}
```

---

### 5.6 [MOYEN] Corriger l'effect de previousLoadingState

Remplacer l'ecriture de signal dans l'effect par un `computed` :

```typescript
// AVANT (dans l'effect) :
previousLoadingState.set(isLoading);

// APRES : utiliser un simple flag boolean non-signal, ou pairwise() de RxJS
// via toObservable(loading).pipe(pairwise())
```

---

### 5.7 [MOYEN] Supprimer les services morts

Supprimer `assurance-sales.service.ts` et `carnet-sales.service.ts` qui ne sont jamais injectes, ou les integrer dans `SalesApiService` si leurs endpoints sont utilises.

---

### 5.8 [MOYEN] Adopter `customerHandling` mixin dans SaleCreationComponent et SaleDevisComponent

Actuellement, seuls SaleAssurance et SaleCarnet utilisent le mixin customer. Les 2 autres implementent la logique client manuellement. Unifier l'approche.

---

### 5.9 [BAS] Unifier clearError()

Remplacer tous les `this.store.setError(null); this.store.setLastErrorDetails(null);` par `this.store.clearError()`.

---

### 5.10 [BAS] Genericiser la conversion de date

```typescript
// Un seul helper generique :
private convertDates<T extends { createdAt?: any; updatedAt?: any }>(obj: T): T {
  const copy = { ...obj };
  if (copy.createdAt) copy.createdAt = moment(copy.createdAt).toJSON();
  if (copy.updatedAt) copy.updatedAt = moment(copy.updatedAt).toJSON();
  return copy;
}
```

---

## Resume des priorites

| # | Priorite | Action | Impact | Effort |
|---|----------|--------|--------|--------|
| 5.1 | CRITIQUE | Decouper SalesFacade en sous-facades | Maintenabilite, testabilite | Eleve |
| 5.2 | CRITIQUE | Mixin `sale-lifecycle` pour subscriptions communes | -600 lignes de duplication | Moyen |
| 5.3 | MAJEUR | Factoriser les 3 methodes de chargement | -80 lignes, moins de bugs | Faible | ok
| 5.4 | MAJEUR | Standardiser extractApiError() partout | Coherence, maintenabilite | Faible | ok
| 5.5 | MAJEUR | Remplacer Subjects par signaux/observables | Decouplage, testabilite | Eleve |
| 5.6 | MOYEN | Corriger effect previousLoadingState | Performance | Faible | ok
| 5.8 | MOYEN | Adopter customerHandling dans Creation/Devis | Coherence | Moyen |
| 5.9 | BAS | Unifier clearError() | Coherence | Tres faible | ok
| 5.10 | BAS | Genericiser conversion de date | Clarte | Tres faible |
