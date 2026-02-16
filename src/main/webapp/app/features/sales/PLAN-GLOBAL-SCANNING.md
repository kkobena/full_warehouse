# Plan d'implementation du Scan Global - Module Sales

## 1. Diagnostic : Pourquoi le scan ne fonctionne plus comme dans le legacy

### 1.1 Architecture legacy (fonctionnelle)

```
selling-home.component.ts (PARENT - toujours dans le DOM)
├── produit-search-autocomplete-scanner  ← UNE SEULE instance, HORS NgbNav
│   └── document.addEventListener('keydown', ..., capture: true)
│       → ScanDetectorService.keyPressed(key)
│       → searchByBarcode(code)
│       → scannedProduit.emit(product)
├── NgbNav
│   ├── ComptantComponent  (PAS de product-search)
│   ├── AssuranceComponent (PAS de product-search)
│   └── CarnetComponent    (PAS de product-search)
```

**Pourquoi ca marchait** :
- L'unique `produit-search-autocomplete-scanner` est TOUJOURS dans le DOM
- Son `document.addEventListener('keydown', ..., capture: true)` intercepte TOUTES les touches, quel que soit le focus
- Le scanner USB HID envoie des keydown events au `document` meme si aucun input n'est focus
- Le composant parent (`selling-home`) recoit `scannedProduit` et dispatche au tab actif

### 1.2 Architecture nouveau module (cassee pour le scan global)

```
sales-home.component.ts (PARENT)
├── NgbNav (destroyOnHide: true par defaut)
│   ├── sale-creation (ngbNavContent)
│   │   └── app-product-search  ← Instance #1 (DETRUITE quand onglet inactif)
│   │       └── document.addEventListener('keydown', ...)
│   ├── sale-assurance (ngbNavContent)
│   │   └── app-product-search  ← Instance #2 (DETRUITE quand onglet inactif)
│   └── sale-carnet (ngbNavContent)
│       └── app-product-search  ← Instance #3 (DETRUITE quand onglet inactif)
```

**Pourquoi c'est casse** :
1. **3 instances au lieu d'1** : chaque onglet a sa propre `app-product-search`
2. **NgbNav detruit les onglets inactifs** : `destroyOnHide: true` (defaut NgbNav) → seul l'onglet actif a un scanner
3. **Scanner local, pas global** : le `keydownListener` est attache/detache avec le lifecycle du composant
4. **Scanner inactif hors onglet** : le `keydownListener` est attache au lifecycle du composant enfant, donc aucun scan possible quand le composant est detruit

### 1.3 Cas d'usage identifies ou le scan echoue

| # | Scenario | Legacy | Nouveau module | Impact |
|---|----------|--------|----------------|--------|
| 1 | Focus sur champ quantite, scan code-barres | OK | OK (keydown sur document) | - |
| 2 | Focus sur champ vendeur (p-select dans header) | OK | OK (onglet actif, listener existe) | - |
| 3 | Focus sur bouton "En attente" | OK | OK | - |
| 4 | Onglet actif change (switch COMPTANT→ASSURANCE) | OK | **Delai** - destroy/recreate listener | Latence ~200ms |
| 6 | Drawer ventes en attente ouvert | OK | **KO** - focus hors du composant, scan ignore | Perte de scan |
| 7 | Scan rapide pendant chargement vente | OK | **KO** - composant pas encore monte | Perte de scan |
| 8 | Focus sur un input de numBon (InsuranceDataBar) | OK | OK mais caracteres scanner dans l'input | UX degradee |

### 1.4 Le `GlobalScannerService` existe deja

**Fichier** : `shared/global-scanner.service.ts`

Ce service est **deja implemente** mais **non cable** dans le nouveau module :
- Etend `BaseScannerService` (meme algo de detection par vitesse de frappe)
- Ajoute `enable()`/`disable()`/`toggle()` avec persistance `localStorage`
- Expose `onScan$` (Observable des codes scannes)
- Injecte dans le legacy `selling-home.component.ts` mais **jamais utilise**

**C'est la fondation de notre solution.**

---

## 2. Architecture cible : Scan global centralise dans `sales-home`

### 2.1 Principe

```
sales-home.component.ts (PARENT - toujours dans le DOM)
│
├── GlobalScannerService (singleton, keydown sur document)
│   └── onScan$.subscribe(code => this.handleGlobalScan(code))
│
├── handleGlobalScan(code):
│   1. Recherche produit par code-barres (ProduitService.search)
│   2. Si 1 resultat → dispatch au composant enfant actif
│   3. Si 0 resultat → notification "Produit non trouve : {code}"
│   4. Si N resultats → ouvre dropdown dans le product-search actif
│
├── NgbNav
│   ├── sale-creation
│   │   └── app-product-search [enableScanner]="false"  ← Scanner LOCAL desactive
│   ├── sale-assurance
│   │   └── app-product-search [enableScanner]="false"
│   └── sale-carnet
│       └── app-product-search [enableScanner]="false"
```

### 2.2 Justification de chaque choix

| Choix | Justification |
|-------|---------------|
| **Scanner au niveau parent** | Le parent est TOUJOURS dans le DOM. Il survit aux changements d'onglet, aux modales, aux drawers. Meme pattern que le legacy. |
| **`GlobalScannerService`** au lieu de `ScanDetectorService` | Le global service a deja la logique enable/disable et persiste l'etat. Le `ScanDetectorService` est concu pour un composant unique. |
| **`[enableScanner]="false"` sur les enfants** | Evite les conflits : un seul keydown listener sur `document` au lieu de N. Supprime le probleme de scans multiples. |
| **Recherche API dans le parent** | Le parent peut rechercher AVANT de savoir quel enfant recevra le resultat. Si 0 resultats, il affiche l'erreur immediatement sans delai de delegation. |
| **Delegation au composant actif** | Chaque type de vente a ses propres regles (ASSURANCE exige un client avant ajout produit). Le parent delegue, l'enfant valide. |
| **`host: { '(window:keydown)': ... }` sur sales-home** | Plus propre que `document.addEventListener` : Angular gere le lifecycle automatiquement, pas besoin de cleanup manuel. Utilise l'objet `host` du decorateur `@Component` (pas `@HostListener`). |

### 2.3 Diagramme de flux complet

```
Scanner USB HID
    │
    ▼
document 'keydown' event
    │
    ▼
sales-home.component.ts host: { '(window:keydown)': 'handleGlobalKeyboardEvent($event)' }
    │
    ├─ Si F-key ou Alt+key → handleGlobalKeyboardEvent() (raccourcis clavier)
    │
    └─ Toute autre touche → globalScannerService.processKey(event.key)
        │
        ├─ Si scan en cours (frappes rapides) → event.preventDefault()
        │   (empeche les caracteres d'apparaitre dans un input focus)
        │
        └─ Si scan complete → onScan$.next(code)
            │
            ▼
        sales-home.handleGlobalScan(code)
            │
            ▼
        ProduitService.search({ search: code, size: 5 })
            │
            ├─ 1 resultat → dispatchScannedProduct(product)
            │   │
            │   ├─ active() === 'comptant' → saleCreation().onProductScanned(product)
            │   ├─ active() === 'assurance' → saleAssurance().onProductScanned(product)
            │   └─ active() === 'carnet' → saleCarnet().onProductScanned(product)
            │
            ├─ 0 resultats → notificationService.error('Produit non trouve : {code}')
            │   + son d'erreur (optionnel)
            │
            └─ N resultats → dispatchMultipleResults(products)
                │
                └─ Focus champ produit actif + affiche dropdown avec resultats
```

---

## 3. Gestion des cas limites

### 3.1 Scan pendant le chargement d'une vente en attente

**Probleme** : Le caissier reprend une vente en attente (chargement async) et scanne pendant le chargement.

**Comportement attendu** : Le scan est traite apres le chargement.

**Implementation** : Le `handleGlobalScan()` verifie `facade.loading()`. Si `true`, stocke le code dans un signal `pendingScanCode` et un `effect()` le traite quand `loading` passe a `false`.

### 3.2 Caracteres du scanner dans un input focus

**Probleme** : Le caissier est focus sur un champ `numBon` (InsuranceDataBar) ou `commentaire` (PaymentMode). Le scanner USB envoie des keydown qui ecrivent dans cet input ET declenchent le scan.

**Comportement attendu** : Le scan est detecte et traite, l'input focus n'est PAS pollue par les caracteres du code-barres.

**Implementation** : Dans le `HostListener`, si `globalScannerService.isScanActive()` est `true`, appeler `event.preventDefault()` pour empecher l'ecriture dans l'input. Les touches sont consommees par le scanner et n'atteignent jamais le champ.

**Precaution** : Le `preventDefault()` ne doit s'appliquer que si le scan est en cours (frappes rapides < 30ms). La frappe humaine normale (> 100ms entre touches) ne sera jamais interceptee.

### 3.3 Scanner desactive temporairement

**Probleme** : Certains ecrans hors-vente ne doivent pas reagir aux scans (ex: admin, parametres).

**Comportement attendu** : Le `GlobalScannerService` peut etre desactive.

**Implementation** : `sales-home` appelle `globalScannerService.enable()` dans `ngOnInit()` et `disable()` dans `ngOnDestroy()`. L'etat est scope au composant de vente.

---

## 4. Plan d'implementation detaille

### Etape 1 : Cablage du `GlobalScannerService` dans `sales-home`

**Fichier** : `sales-home.component.ts`

```typescript
// Injection
private globalScanner = inject(GlobalScannerService);
private produitService = inject(ProduitService);
private notificationService = inject(NotificationService);

// Signal pour scan en attente (quand composant enfant pas encore pret)
private pendingScanCode = signal<string | null>(null);

ngOnInit(): void {
  // ... code existant ...

  // Activer le scanner global pour la page de vente
  this.globalScanner.enable();

  // S'abonner aux scans completes
  this.globalScanner.onScan$
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe(code => this.handleGlobalScan(code));
}

ngOnDestroy(): void {
  this.globalScanner.disable();
}
```

**Modification du handler existant** (via `host` dans le decorateur `@Component`) :

```typescript
// Dans @Component({ host: { '(window:keydown)': 'handleGlobalKeyboardEvent($event)' } })
handleGlobalKeyboardEvent(event: KeyboardEvent): void {
  // 1. Raccourcis clavier (Alt+1/2/3, F11)
  if (event.altKey && !event.ctrlKey && ['1', '2', '3'].includes(event.key)) {
    event.preventDefault();
    const tabMap = { '1': 'comptant', '2': 'assurance', '3': 'carnet' };
    this.switchToTab(tabMap[event.key]);
    return;
  }
  if (event.key === 'F11' && !this.isPresaleMode()) {
    event.preventDefault();
    this.openPendingSales();
    return;
  }

  // 2. Scanner global : alimenter le detecteur
  const result = this.globalScanner.processKey(event.key);
  if (result.isScanInProgress) {
    // Empecher les caracteres du scanner de polluer un input focus
    event.preventDefault();
  }
}
```

**Points cles** :
- Le handler existant dans `host: { '(window:keydown)': ... }` est etendu pour gerer a la fois les raccourcis ET le scanner dans un seul handler
- `event.preventDefault()` pendant le scan empeche la pollution des inputs (supprime le besoin de la boucle `requestAnimationFrame` dans `product-search`)

### Etape 2 : Implementation de `handleGlobalScan()`

**Fichier** : `sales-home.component.ts`

```typescript
private handleGlobalScan(code: string): void {
  // Si chargement en cours, stocker pour traitement ulterieur
  if (this.salesFacade.loading()) {
    this.pendingScanCode.set(code);
    return;
  }

  this.searchAndDispatch(code);
}

private searchAndDispatch(code: string): void {
  this.produitService
    .search({ page: 0, size: 5, search: code }, false)
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({
      next: res => {
        const results = res.body || [];

        if (results.length === 1) {
          this.dispatchScannedProduct(results[0]);
        } else if (results.length === 0) {
          this.notificationService.error(
            `Produit non trouve : ${code}`,
            'Scan echoue'
          );
        } else {
          // Plusieurs resultats : deleguer au product-search enfant pour afficher le dropdown
          this.dispatchMultipleResults(results, code);
        }
      },
      error: () => {
        this.notificationService.error(
          'Erreur de recherche produit',
          'Scan echoue'
        );
      },
    });
}

private dispatchScannedProduct(product: ProduitSearch): void {
  switch (this.active()) {
    case 'comptant':
      this.saleCreation()?.onProductScanned(product);
      break;
    case 'assurance':
      this.saleAssurance()?.onProductScanned(product);
      break;
    case 'carnet':
      this.saleCarnet()?.onProductScanned(product);
      break;
  }
}

private dispatchMultipleResults(products: ProduitSearch[], code: string): void {
  this.notificationService.info(
    `${products.length} produits trouves pour le code ${code}`,
    'Scan: choix requis'
  );
  // Focus le champ produit de l'onglet actif et pre-remplir
  this.focusActiveTab();
  // Le caissier devra taper le code manuellement pour avoir le dropdown
}
```

### Etape 3 : Desactiver le scanner local dans les composants enfants

**Fichiers** : Templates HTML des 3 composants

```html
<!-- sale-creation.component.html -->
<app-product-search
  #produitbox
  [autofocus]="true"
  [enableScanner]="false"
  (productSelected)="onProductSelected($event)"
  (productScanned)="onProductScanned($event)"
  (onKeyEnter)="onProductSearchEnter($event)">
</app-product-search>
```

Meme modification pour `sale-assurance.component.html` et `sale-carnet.component.html`.

**Justification** : Un seul listener `keydown` sur `document` (celui du parent). Les enfants n'attachent plus de listener local. Cela :
- Elimine les conflits multi-listeners
- Elimine la boucle `requestAnimationFrame` de nettoyage
- Elimine le `clearActiveElement()` dangereux
- Simplifie le code de chaque `product-search`

### Etape 4 : Gestion du scan en attente

**Fichier** : `sales-home.component.ts`

```typescript
constructor() {
  // ... code existant ...

  // Traiter les scans en attente quand le chargement se termine
  effect(() => {
    const code = this.pendingScanCode();
    const isLoading = this.salesFacade.loading();
    if (code && !isLoading) {
      this.pendingScanCode.set(null);
      // Delai pour laisser le composant enfant se monter
      setTimeout(() => this.searchAndDispatch(code), 100);
    }
  });
}
```

### Etape 5 : Corrections des bugs identifies dans l'analyse precedente

Ces corrections sont appliquees en parallele car elles concernent le `ProductSearchComponent` :

| # | Correction | Fichier | Action |
|---|-----------|---------|--------|
| 1 | Supprimer `clearActiveElement()` | `product-search.component.ts` | Supprimer la methode et son appel dans `onScanComplete` |
| 2 | Supprimer le handler `onHostKeydown` doublon | `product-search.component.ts` | Supprimer `onHostKeydown()` (lignes 190-202) et le binding `host` correspondant, et l'import `HostListener` |
| 3 | Supprimer la boucle RAF | `product-search.component.ts` | Supprimer `startInputClearLoop()`, `stopInputClearLoop()`, `animationFrameId`. Plus necessaire car `event.preventDefault()` dans le parent empeche deja les caracteres |
| 4 | Ajouter output `scanFailed` | `product-search.component.ts` | Non necessaire : la notification d'erreur est geree dans le parent |

### Etape 6 : Nettoyage du `ProductSearchComponent`

Apres les etapes precedentes, le composant `ProductSearchComponent` devient plus simple :

**Code supprime** (~80 lignes) :
- `setupBarcodeScanner()` / `removeBarcodeScanner()` : inutile quand `enableScanner=false`
- `onScanStart()` / `onScanComplete()` : inutile
- `clearInputValue()` / `clearActiveElement()` : inutile
- `startInputClearLoop()` / `stopInputClearLoop()` : inutile
- `searchByBarcode()` : inutile (la recherche est dans le parent)
- `scanSubscription`, `keydownListener`, `animationFrameId`, `isScanning`, `isManualSearching` : inutile
- `onHostKeydown` handler doublon : supprime

**Code conserve** :
- `searchFn()` + `loadProduits()` : recherche manuelle par autocomplete (inchange)
- `onSelect()` / `onKeyDown()` : interaction utilisateur manuelle (inchange)
- `getFocus()` / `reset()` : methodes publiques appelees par le parent (inchange)
- `produits` / `selectProduit` / `productSelected` / `onKeyEnter` : signals et outputs (inchange)

**Note** : Le code scanner local est conserve dans le composant mais desactive via `[enableScanner]="false"`. Il n'est PAS supprime pour deux raisons :
1. Le composant est reutilisable dans d'autres contextes ou le scan local est pertinent
2. L'input `enableScanner` existe deja et controle l'activation

---

## 5. Ordre d'execution

| # | Tache | Fichiers | Dependances |
|---|-------|----------|-------------|
| 1 | Cabler `GlobalScannerService` dans `sales-home` | `sales-home.component.ts` | - |
| 2 | Implementer `handleGlobalScan()` + dispatch | `sales-home.component.ts` | #1 |
| 3 | Ajouter gestion scan en attente (effect) | `sales-home.component.ts` | #2 |
| 4 | Fusionner handler `host` (raccourcis + scanner) | `sales-home.component.ts` | #1 |
| 5 | Desactiver scanner local : `[enableScanner]="false"` | 3 templates HTML | #1 |
| 6 | Supprimer `clearActiveElement()` | `product-search.component.ts` | - |
| 7 | Supprimer handler `onHostKeydown` doublon Enter | `product-search.component.ts` | - |
| 8 | Tests manuels des 8 scenarios (section 1.3) | - | #1-#7 |

---

## 6. Comparaison avant/apres

### Architecture

| Aspect | Avant | Apres |
|--------|-------|-------|
| Listeners `document.keydown` | 1 par onglet actif | 1 unique (parent) |
| Instance scanner | `ScanDetectorService` (local) | `GlobalScannerService` (global) |
| Recherche produit sur scan | Dans chaque `product-search` | Centralisee dans `sales-home` |
| Dispatch au tab actif | N/A (chaque tab gere son scan) | Parent dispatche via `viewChild` |
| Feedback scan echoue | Silencieux | Notification + code-barres affiche |
| Scan pendant chargement | Perdu | File d'attente + retry |
| Caracteres scanner dans input | RAF loop (60 FPS) | `event.preventDefault()` (1 appel) |

### Lignes de code

| Composant | Avant | Apres | Delta |
|-----------|-------|-------|-------|
| `sales-home.component.ts` | 0 lignes scanner | ~60 lignes | +60 |
| `product-search.component.ts` scanner | ~120 lignes | ~10 lignes (garde structure) | -110 (desactive) |
| `sale-creation` template | `[enableScanner]` absent | `[enableScanner]="false"` | +1 attr |
| `sale-assurance` template | `[enableScanner]` absent | `[enableScanner]="false"` | +1 attr |
| `sale-carnet` template | `[enableScanner]` absent | `[enableScanner]="false"` | +1 attr |

---

## 7. Risques et mitigations

| Risque | Probabilite | Mitigation |
|--------|-------------|------------|
| `event.preventDefault()` bloque la frappe normale | Faible (seulement si `isScanInProgress` = frappes < 30ms) | La frappe humaine est > 100ms entre touches. Le seuil 30ms est safe. |
| Scanner bluetooth avec latence > 30ms entre chars | Moyenne | Rendre `scanDelayMs` configurable via `SCANNER_CONFIG`. Tester avec scanner reel. |
| Regression sur la recherche manuelle autocomplete | Faible (non touchee) | L'autocomplete manuelle reste 100% locale dans `product-search`. Aucune modification. |
| `GlobalScannerService.enable()` appele ailleurs | Faible | Le service est `providedIn: 'root'` mais `enable()` n'est appele que dans `sales-home`. |
| Double appel `processKey` (parent + enfant local si `enableScanner` oublie) | Faible | Le `[enableScanner]="false"` empeche le listener local. Ajouter un `console.warn` si les deux sont actifs simultanément. |

---

## 8. Configuration scanner

La configuration actuelle dans `scanner.config.ts` est adaptee aux scanners USB HID standard :

```typescript
scanDelayMs: 30,      // Max 30ms entre 2 touches pour detecter un scan
scanMinLength: 6,     // Code-barres minimum 6 chars (CIP = 6 chars min, EAN13 = 13 chars)
scanMaxTime: 500,     // Scan complete en < 500ms (meme un EAN13 a 30ms/char = 390ms)
resetDelay: 150,      // Reset si pause > 150ms
endScanTimeout: 100,  // Auto-complete apres 100ms sans Enter
```

**Types de scanners supportes** :
- USB HID (clavier emule) : mode principal, detecte par vitesse de frappe
- Bluetooth HID : meme protocole, latence potentiellement plus elevee
- Scanner serie/COM (Tauri) : necessite un canal different (non couvert ici)

**Codes-barres pharmacie** :
- CIP (6 chars min) : format francais
- CIP13 / EAN13 (13 chars) : standard actuel
- Code 2D (DataMatrix) : contient CIP13 + lot + peremption (le scanner envoie le CIP13 extrait)

---

## 9. Sources et references

| Source | Principe applique |
|--------|-------------------|
| **Nielsen Norman Group - Heuristique #1 : Visibility of System Status** | Feedback obligatoire sur scan echoue (le silence est inacceptable en POS) |
| **Nielsen Norman Group - Heuristique #5 : Error Prevention** | `event.preventDefault()` empeche la pollution des inputs par les caracteres scanner |
| **Microsoft - Guidelines for Keyboard UI Design** | "The system should not lose user input" → scan en file d'attente si chargement en cours |
| **USB HID Specification** | Les scanners USB emulent un clavier : chaque caractere est un `keydown` event. Le `Enter` final termine le scan. |
| **Pattern POS : Global Capture** | Tous les terminaux POS professionnels capturent les scans au niveau application, pas au niveau champ. Le scan doit fonctionner quel que soit le focus. |
| **Angular Component Architecture** | Le parent orchestre, les enfants executent. Le parent a la visibilite globale (quel onglet est actif, quelle modale est ouverte). |
| **Legacy `selling-home.component.ts`** | Reference interne : le composant parent gerait deja le scan et le dispatchait aux enfants via `onScannedProduct()`. |


# Amélioration de la saisie manuelle produit

## Objectif
Améliorer le comportement de la **recherche lors de la saisie manuelle au clavier**.

## Constat actuel
- Après la saisie de **3 caractères**, si un seul résultat correspond, le produit est **sélectionné automatiquement**.
- Ce comportement empêche l’utilisateur de **continuer à taper** pour préciser ou corriger sa recherche.
- Risque d’erreurs ou de frustration pour l’utilisateur.

## Attendu
- Analyser spécifiquement le cas de la **saisie manuelle** (hors scan).
- Identifier les faiblesses UX liées à la sélection automatique après 3 caractères.
- Proposer une solution **fiable, robuste et user-friendly** qui :
  - Laisse le **contrôle à l’utilisateur**,
  - Évite les **sélections involontaires**,
  - Reste **rapide et efficace**,
  - Différencie le comportement entre **saisie clavier** et **scan** si nécessaire.

## Contraintes
- Ergonomie cohérente et prévisible pour l’utilisateur.
- Adaptée à un contexte applicatif métier.
- Compatible avec des saisies partielles ou complètes avant validation.

## Livrables attendus
- Recommandations argumentées et structurées pour améliorer l’expérience utilisateur de la saisie manuelle.
