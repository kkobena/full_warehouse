# Plan d'intégration Scanner USB CDC (COM) — Module Vente

> **Contexte** : Le module de réception BL (`commande-received`) dispose déjà d'une intégration
> complète du scanner USB CDC via Tauri (port série Rust → événement Angular).
> Ce plan décrit comment porter ce mécanisme au module de vente sans casser l'architecture HID existante.

---

## 1. État des lieux

### 1.1 Architecture scanner actuelle (module vente)

```
Clavier (HID)
  └── document:keydown
        └── SalesHomeComponent.handleGlobalKeyboardEvent()
              └── GlobalScannerService.processKeyEvent()
                    ├── Mode TIMING  : BaseScannerService.processKey()
                    │     └── onScanCompleted() → scannedCode$.next(code)
                    └── Mode PREFIX_SUFFIX : PrefixSuffixScannerService.processKeyEvent()
                          └── onScan$.next(code)
                                └── (merge) GlobalScannerService.onScan$
                                      └── SalesHomeComponent.enqueueScan(code)
                                            └── searchAndDispatch(code)
                                                  └── dispatchScannedProduct(product)
                                                        └── saleXxx.onProductScanned(product)
                                                              └── productHandling.onProductScanned()
                                                                    └── addProductToSale(product, 1)
```

**Services impliqués :**

| Service | Rôle |
|---|---|
| `GlobalScannerService` | Orchestrateur HID — TIMING et PREFIX_SUFFIX |
| `BaseScannerService` | Détection par timing frappe (< 30 ms entre touches) |
| `PrefixSuffixScannerService` | Détection STX (Ctrl+B) / ETX (Ctrl+C) |
| `ScanDetectorService` | Scanner composant local (`ProductSearchComponent`) |
| `ScanAudioFeedbackService` | Bips succès / erreur |

**Limitations du mode HID :**
- Pollue les `<input>` actifs (les caractères du code-barres s'y insèrent)
- Dépend des timings du driver clavier USB — variable selon les PC
- Incompatible avec certains scanners en mode USB-COM (aucune frappe clavier émise)

---

### 1.2 Architecture scanner CDC — module réception (référence)

```
Scanner USB COM (port série Rust)
  └── start_scanner_listener (Tauri invoke)
        └── thread Rust → serialport::read()
              └── flush sur CR/LF ou timeout 100 ms
                    └── app.emit("scan-reception", code)
                          └── listen<string>('scan-reception') (Angular)
                                └── scanValue = code → onScanReception()
```

**Points-clés de l'implémentation réception :**

- Stratégie de sélection scanner : `setupBarcodeScannerWithFallback(posteId)`
  1. Device actif en base (`PosteDeviceService.getActiveDevice`)
  2. Vérification connexion (`TauriDeviceDetectionService.isPortConnected`)
  3. Fallback sur tous les SCANNER du poste (batch check)
  4. **Fallback HID si aucun connecté + retry automatique toutes les 8 s (max 10 tentatives)**
- Nom événement Tauri : `'scan-reception'` (isolé du module vente)
- Signal `scannerMode : 'HID' | 'SERIAL' | null`
- **Reconnexion automatique** sur `scan-error` (déconnexion/veille) → HID + retry CDC
- **Bouton "Reconnecter scanner"** dans le header (visible si Tauri + HID + poste configuré)
- **`hidModeDestroyer$` (Subject)** : annule la souscription HID active avant d'en créer une nouvelle → évite les doubles appels lors des reconnexions
- Cleanup complet sur `DestroyRef.onDestroy` (unlisten + `stop_scanner_listener` + stopCdcRetry)
- **Un seul `SCANNER_RUNNING` global côté Rust** → un seul listener à la fois (safe car modules
  sur des routes distinctes, non simultanés)

---

### 1.3 Mécanisme retry/reconnexion CDC (implémenté dans commande-received)

**Problème résolu :** si l'utilisateur ouvre le menu avant de brancher la douchette, ou si
celle-ci est en mode veille, `isPortConnected` retourne `false` et le système tombait
définitivement en HID sans jamais retenter.

**Solution en 3 couches :**

```
Couche 1 — Retry automatique après fallback HID
  startCdcRetry(posteId)
    └── setInterval (8 s × max 10 = ~80 s)
          └── checkPortsConnection(allDevices)
                ├── Scanner trouvé → stopCdcRetry + setupTauriCdcScanner + notification
                └── Pas trouvé → attendre prochain cycle (ou abandonner à 10)

Couche 2 — Reconnexion après scan-error (déconnexion/veille)
  listen("scan-error")
    └── setupHidTimingFallback()     (hidModeDestroyer$.next() évite les doubles sub)
    └── startCdcRetry(currentPosteId) (surveille le rebranch/réveil)

Couche 3 — Bouton manuel dans le header
  [canReconnectScanner()] → computed signal
    └── reconnectScanner()
          └── stopCdcRetry() + setupBarcodeScannerWithFallback(currentPosteId)
```

**Flux complet avec retry :**
```
ngOnInit
  └─ setupBarcodeScanner
       └─ getCurrentPoste() → posteId stocké dans currentPosteId
            └─ setupBarcodeScannerWithFallback(posteId)
                 ├─ Scanner trouvé → SERIAL ✅  (stopCdcRetry)
                 └─ Non trouvé → HID Fallback
                       └─ startCdcRetry(posteId)
                             ├─ [8s] Scanner détecté → SERIAL ✅ + notification
                             └─ [10 tentatives épuisées] → rester HID
                                   └─ Bouton "Reconnecter scanner" toujours disponible
```

---

## 2. Analyse des impacts

### 2.1 Ce qui doit changer

| Fichier | Type de changement |
|---|---|
| `SalesHomeComponent` | Remplacer `GlobalScannerService.enable()` par `SalesScannerService.setup()` |
| `sales-home.component.html` | Indicateur mode scanner (badge SERIAL / HID) + bouton reconnexion |
| `ProductSearchComponent` | Désactiver scan local (`ScanDetectorService`) en mode SERIAL |
| **NOUVEAU** `SalesScannerService` | Orchestrateur unifié HID + CDC pour la vente (avec retry) |

### 2.2 Ce qui ne change pas

- `GlobalScannerService` — reste inchangé (utilisé hors Tauri)
- `BaseScannerService`, `PrefixSuffixScannerService` — inchangés
- `enqueueScan()` / `searchAndDispatch()` / `dispatchScannedProduct()` — inchangés
- `productHandling.onProductScanned()` — inchangé
- `ScanAudioFeedbackService` — inchangé
- Tous les composants enfants (`SaleCreationComponent`, etc.) — inchangés

---

## 3. Architecture cible

```
Mode WEB / HID (inchangé)
  └── document:keydown
        └── SalesScannerService → délègue à GlobalScannerService
              └── onScan$ → SalesHomeComponent.enqueueScan(code)

Mode TAURI CDC (nouveau)
  └── Rust : start_scanner_listener('scan-vente')
        └── serialport → app.emit('scan-vente', code)
              └── listen<string>('scan-vente') dans SalesScannerService
                    └── onScan$ → SalesHomeComponent.enqueueScan(code)
```

**`SalesScannerService`** est le seul point d'entrée pour `SalesHomeComponent`.
Il expose une interface identique dans les deux modes :

```typescript
interface SalesScannerFacade {
  readonly onScan$: Observable<string>;
  readonly scannerMode: Signal<'HID' | 'SERIAL' | null>;
  readonly canReconnect: Signal<boolean>;  // Tauri + HID + posteId présent
  setup(posteId?: number): Promise<void>;
  teardown(): Promise<void>;
  reconnect(): Promise<void>;             // Appel manuel depuis le template
}
```

---

## 4. Plan d'implémentation détaillé

### Étape 1 — Créer `SalesScannerService`

**Fichier :** `src/main/webapp/app/features/sales/data-access/services/sales-scanner.service.ts`

**Responsabilités :**
- Décider du mode (HID vs CDC) selon la disponibilité Tauri + device configuré
- En mode HID : activer `GlobalScannerService` et proxier `onScan$`
- En mode CDC : démarrer le listener Tauri et alimenter `onScan$`
- **Retry automatique CDC** (même logique que commande-received) : `startCdcRetry` / `stopCdcRetry`
- **Reconnexion sur scan-error** : HID fallback + restart retry
- Exposer `scannerMode` et `canReconnect` signals pour le template
- Gérer le cleanup complet (unlisten + stop_scanner_listener + stopCdcRetry)

**Pseudo-code :**

```typescript
@Injectable()  // NON providedIn:'root' — isolé par composant
export class SalesScannerService {
  private readonly globalScanner   = inject(GlobalScannerService);
  private readonly tauriDevice     = inject(TauriDeviceDetectionService);
  private readonly posteDevService = inject(PosteDeviceService);
  private readonly notification    = inject(NotificationService);

  readonly scannerMode = signal<'HID' | 'SERIAL' | null>(null);
  readonly canReconnect = computed(
    () => this.tauriDevice.isTauriAvailable() && this.scannerMode() === 'HID' && !!this.currentPosteId
  );

  private readonly _onScan$ = new Subject<string>();
  readonly onScan$ = this._onScan$.asObservable();

  private currentPosteId: number | null = null;
  private unlistenScan: (() => void) | null = null;
  private unlistenError: (() => void) | null = null;
  private cdcRetryInterval: ReturnType<typeof setInterval> | null = null;
  private cdcRetryCount = 0;
  private static readonly RETRY_MAX = 10;
  private static readonly RETRY_INTERVAL_MS = 8000;
  private readonly hidDestroyer$ = new Subject<void>();

  async setup(posteId?: number): Promise<void> {
    if (posteId) this.currentPosteId = posteId;
    if (this.tauriDevice.isTauriAvailable() && this.currentPosteId) {
      const started = await this.trySetupCdc(this.currentPosteId);
      if (started) return;
      // Fallback HID + retry
      this.setupHid();
      this.startCdcRetry(this.currentPosteId);
      return;
    }
    this.setupHid();
  }

  async teardown(): Promise<void> {
    this.stopCdcRetry();
    this.hidDestroyer$.next();
    this.globalScanner.disable();
    this.unlistenScan?.();
    this.unlistenError?.();
    this.unlistenScan = null;
    this.unlistenError = null;
    if (this.scannerMode() === 'SERIAL') {
      const { invoke } = await import('@tauri-apps/api/core');
      try { await invoke('stop_scanner_listener'); } catch { /* déjà fermé */ }
    }
    this.scannerMode.set(null);
  }

  async reconnect(): Promise<void> {
    if (!this.currentPosteId) return;
    this.stopCdcRetry();
    await this.setup(this.currentPosteId);
  }

  /** Traiter les touches clavier HID (délégué depuis SalesHomeComponent) */
  processKeyEvent(event: KeyboardEvent): { isScanInProgress: boolean } {
    if (this.scannerMode() === 'SERIAL') return { isScanInProgress: false };
    return this.globalScanner.processKeyEvent(event);
  }

  private setupHid(): void {
    this.hidDestroyer$.next(); // Annuler souscription HID précédente
    this.globalScanner.enable();
    this.globalScanner.onScan$
      .pipe(takeUntil(this.hidDestroyer$))
      .subscribe(code => this._onScan$.next(code));
    this.scannerMode.set('HID');
  }

  private async trySetupCdc(posteId: number): Promise<boolean> {
    try {
      // 1. Device actif
      const activeRes = await firstValueFrom(this.posteDevService.getActiveDevice(posteId, 'SCANNER'));
      const device = activeRes.body;
      if (device?.portName) {
        const connected = await this.tauriDevice.isPortConnected(device.portName);
        if (connected) return this.startCdcListener(device.portName, device.baudRate ?? 9600);
      }
      // 2. Fallback sur tous les scanners du poste
      const allRes = await firstValueFrom(this.posteDevService.fetchAll(posteId, 'SCANNER'));
      const devices = (allRes.body ?? []).filter(d => d.portName);
      if (devices.length) {
        const statuses = await this.tauriDevice.checkPortsConnection(devices.map(d => d.portName!));
        const found = devices.find(d => statuses.some(s => s.portName === d.portName && s.connected));
        if (found?.portName) return this.startCdcListener(found.portName, found.baudRate ?? 9600);
      }
    } catch { /* fallback HID */ }
    return false;
  }

  private async startCdcListener(portName: string, baudRate: number): Promise<boolean> {
    const { invoke } = await import('@tauri-apps/api/core');
    const { listen } = await import('@tauri-apps/api/event');
    await invoke('start_scanner_listener', { portName, baudRate, eventName: 'scan-vente' });
    this.stopCdcRetry();
    this.globalScanner.disable();
    this.unlistenScan?.();
    this.unlistenError?.();
    this.unlistenScan  = await listen<string>('scan-vente', ev => this._onScan$.next(ev.payload));
    this.unlistenError = await listen<string>('scan-error', () => {
      this.unlistenScan?.(); this.unlistenScan = null;
      this.unlistenError?.(); this.unlistenError = null;
      this.notification.warning('Scanner déconnecté — basculement en mode clavier', 'Scanner');
      this.setupHid();
      if (this.currentPosteId) this.startCdcRetry(this.currentPosteId);
    });
    this.scannerMode.set('SERIAL');
    return true;
  }

  private startCdcRetry(posteId: number): void {
    this.stopCdcRetry();
    this.cdcRetryCount = 0;
    this.cdcRetryInterval = setInterval(async () => {
      this.cdcRetryCount++;
      if (this.cdcRetryCount > SalesScannerService.RETRY_MAX) { this.stopCdcRetry(); return; }
      try {
        const started = await this.trySetupCdc(posteId);
        if (started) this.notification.info('Scanner CDC détecté — passage en mode série', 'Scanner');
      } catch { /* silencieux */ }
    }, SalesScannerService.RETRY_INTERVAL_MS);
  }

  private stopCdcRetry(): void {
    if (this.cdcRetryInterval) { clearInterval(this.cdcRetryInterval); this.cdcRetryInterval = null; }
    this.cdcRetryCount = 0;
  }
}
```

---

### Étape 2 — Modifier `SalesHomeComponent`

**Fichier :** `sales-home.component.ts`

**Changements :**

```typescript
// AVANT
private globalScanner = inject(GlobalScannerService);

ngOnInit(): void {
  this.globalScanner.enable();
  this.globalScanner.onScan$.pipe(...).subscribe(code => this.enqueueScan(code));
}

handleGlobalKeyboardEvent(event: KeyboardEvent): void {
  const result = this.globalScanner.processKeyEvent(event);
  ...
}
```

```typescript
// APRÈS
private readonly salesScanner = inject(SalesScannerService);
protected readonly scannerMode = this.salesScanner.scannerMode;
protected readonly canReconnectScanner = this.salesScanner.canReconnect;

ngOnInit(): void {
  this.configurationService.getCurrentPoste().subscribe(res => {
    this.salesScanner.setup(res.body?.id ?? undefined);
  });
  this.salesScanner.onScan$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(code => this.enqueueScan(code));
}

ngOnDestroy(): void {
  this.salesScanner.teardown();
}

handleGlobalKeyboardEvent(event: KeyboardEvent): void {
  const result = this.salesScanner.processKeyEvent(event); // no-op si SERIAL
  ...
}

protected reconnectScanner(): void {
  this.salesScanner.reconnect();
}
```

**Ajouter dans `providers` du composant :**
```typescript
providers: [SalesScannerService]
```

---

### Étape 3 — Modifier `sales-home.component.html`

Ajouter indicateur mode scanner + bouton reconnexion dans le header :

```html
<!-- Badge SERIAL -->
@if (scannerMode() === 'SERIAL') {
  <span class="sales-serial-badge"
        pTooltip="Douchette USB série active — lecture automatique"
        tooltipPosition="bottom">
    <i class="pi pi-wifi"></i> Scanner série
  </span>
}
<!-- Reconnexion manuelle -->
@if (canReconnectScanner()) {
  <p-button
    (click)="reconnectScanner()"
    icon="pi pi-refresh"
    label="Reconnecter scanner"
    severity="warn"
    size="small"
    pTooltip="Tenter de reconnecter la douchette USB (branchée après ouverture ou sortie de veille)"
    tooltipPosition="bottom"
  />
}
```

---

### Étape 4 — Modifier `ProductSearchComponent`

**Fichier :** `features/sales/ui/product-search/product-search.component.ts`

Quand le scanner est en mode CDC, `ScanDetectorService` (local) ne doit pas tenter de détecter des scans clavier (il n'y en a pas).

```typescript
// Injecter le mode scanner
private readonly salesScanner = inject(SalesScannerService);

private setupBarcodeScanner(): void {
  if (this.salesScanner.scannerMode() === 'SERIAL') {
    // CDC actif : pas de scan local à détecter
    return;
  }
  // ... logique existante
}
```

> **Note :** `SalesScannerService` n'est accessible depuis `ProductSearchComponent` que si
> fourni au niveau du module parent (`SalesHomeComponent`). Il faut soit le fournir à la
> racine du module vente, soit utiliser un token d'injection partagé.

**Alternative simple :** Injecter `TauriDeviceDetectionService` directement dans
`ProductSearchComponent` et vérifier `systemInfo()` pour savoir si on est en Tauri, puis
vérifier `localStorage` pour le mode scanner.

---

### Étape 5 — Cleanup SCSS

**Fichier :** `features/sales/feature/sales-home/sales-home.component.scss` (ou fichier SCSS partagé)

```scss
.sales-serial-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.65rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  background: rgba(13, 110, 253, 0.1);
  color: #0d6efd;
  border: 1px solid rgba(13, 110, 253, 0.3);

  .pi-wifi { animation: wifi-pulse 1.8s ease-in-out infinite; }
}

@keyframes wifi-pulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.4; }
}
```

---

## 5. Contraintes et points d'attention

### 5.1 Un seul `SCANNER_RUNNING` Rust

Le fichier `scanner.rs` utilise un `static AtomicBool` global. **Un seul listener peut tourner
à la fois.** Ceci est safe car :

- Module vente et module réception sont sur des routes Angular distinctes
- Angular détruit le composant (et son `DestroyRef.onDestroy`) à la navigation
- Le `teardown()` de `SalesScannerService` appelle `stop_scanner_listener` avant destruction
- Aucun risque de double-start si les cleanups sont corrects

**À vérifier :** Si l'utilisateur navigue rapidement (< 100 ms), un race condition est possible.
Mitigation : ajouter un délai de 50 ms dans `setup()` si `SCANNER_RUNNING` revient en erreur.

### 5.2 Nom de l'événement Tauri

| Module | Événement Tauri |
|---|---|
| Réception BL | `scan-reception` |
| Vente | `scan-vente` |
| Test (form-poste) | `scan-test` |

Ces noms sont distincts pour éviter toute cross-écoute accidentelle entre modules.

### 5.3 `setup()` asynchrone dans `ngOnInit`

`ngOnInit` n'est pas `async` par convention Angular. Deux options :

- Option A : `ngOnInit() { this.salesScanner.setup(posteId); }` — fire-and-forget (acceptable, le
  scanner est opérationnel en ~50 ms, avant le premier scan humain)
- Option B : utiliser `APP_INITIALIZER` pour pré-charger le device actif — complexe, non recommandé

**Recommandation :** Option A avec gestion d'erreur dans `setup()`.

### 5.4 `ProductSearchComponent` et `SalesScannerService`

`SalesScannerService` est fourni dans `SalesHomeComponent.providers`. Il n'est pas accessible par
injection depuis `ProductSearchComponent` sauf si ce dernier est un enfant dans la hiérarchie DI.

Si `ProductSearchComponent` est standalone avec son propre injector, utiliser plutôt :

```typescript
// Alternative sans injection du service vente
private readonly tauriDevice = inject(TauriDeviceDetectionService);

private get isCdcActive(): boolean {
  return this.tauriDevice.isTauriAvailable() &&
         !!localStorage.getItem('sales-scanner-mode') &&
         localStorage.getItem('sales-scanner-mode') === 'SERIAL';
}
```

Ou exposer un signal global via `SalesScannerService` en `providedIn: 'root'` — au choix selon le
couplage acceptable.

### 5.5 Gestion des doublons de souscription HID

Le `Subject hidDestroyer$` (pattern issu de `commande-received`) est **obligatoire** dans
`SalesScannerService`. Sans lui, chaque cycle scan-error → HID → retry → scan-error accumule
des souscriptions actives à `GlobalScannerService.onScan$`, causant des double-scans.

**Pattern :**
```typescript
// À chaque setupHid() :
this.hidDestroyer$.next(); // annule la sub précédente
this.globalScanner.onScan$
  .pipe(takeUntil(this.hidDestroyer$))
  .subscribe(...);
```

---

## 6. Ordre des fichiers à créer / modifier

| Priorité | Fichier | Action |
|---|---|---|
| 1 | `sales/data-access/services/sales-scanner.service.ts` | **Créer** (avec retry + reconnect) |
| 2 | `sales/feature/sales-home/sales-home.component.ts` | Modifier (inject + setup/teardown/reconnect) |
| 3 | `sales/feature/sales-home/sales-home.component.html` | Modifier (badge SERIAL + bouton reconnexion) |
| 4 | `sales/feature/sales-home/sales-home.component.scss` | Modifier (style badge) |
| 5 | `sales/ui/product-search/product-search.component.ts` | Modifier (skip scan local si SERIAL) |

---

## 7. Tests à effectuer

### Mode SERIAL (Tauri CDC)
- [ ] Scanner détecté et sélectionné au démarrage du module vente
- [ ] Scan d'un produit → ajout immédiat à la vente active
- [ ] Badge "Scanner série" visible dans le header
- [ ] Déconnexion USB → fallback automatique en mode HID + notification + retry démarré
- [ ] Rebrancher la douchette → retry détecte → basculement SERIAL automatique + notification
- [ ] Scanner en mode veille → retry détecte réveil → basculement SERIAL automatique
- [ ] Navigation vers réception → `teardown()` libère le port → réception peut démarrer son listener
- [ ] Retour sur vente → `setup()` réouvre le port correctement
- [ ] `ProductSearchComponent` : champ recherche non pollué par les caractères du scan
- [ ] Bouton "Reconnecter scanner" visible en mode HID avec poste Tauri configuré
- [ ] Clic sur "Reconnecter scanner" → setup relancé manuellement

### Mode HID (fallback)
- [ ] Comportement identique à avant (aucune régression)
- [ ] `GlobalScannerService.enable()` bien appelé
- [ ] `GlobalScannerService.disable()` appelé à la destruction
- [ ] Pas de double scan après N cycles HID → SERIAL → HID

### Coexistence
- [ ] Ouvrir réception BL puis retourner aux ventes → un seul listener actif à la fois
- [ ] Deux scans rapides consécutifs → file d'attente (`scanQueue`) traitée séquentiellement
- [ ] Retry actif dans réception → navigation vente → retry stoppe bien (destroyRef)

---

## 8. Résumé visuel

```
┌─────────────────────────────────────────────────────┐
│                SalesHomeComponent                   │
│  providers: [SalesScannerService]                   │
│                                                     │
│  ngOnInit → salesScanner.setup(posteId)             │
│  ngOnDestroy → salesScanner.teardown()              │
│  keydown → salesScanner.processKeyEvent()           │
│  salesScanner.onScan$ → enqueueScan(code)           │
│  [canReconnect()] → bouton reconnexion              │
└──────────────────────┬──────────────────────────────┘
                       │
          ┌────────────┴────────────┐
          │    SalesScannerService  │
          │                         │
     Tauri CDC ?                   │
          │                         │
    OUI ──┤                   NON ──┤
          │                         │
    ┌─────┴──────┐         ┌────────┴──────┐
    │ Rust CDC   │         │ GlobalScanner │
    │ 'scan-     │         │ Service HID   │
    │  vente'    │         │ (inchangé)    │
    └────────────┘         └───────────────┘
          │  scan-error?             │
          │  → HID + retry          │
          └────────┬────────────────┘
                   │
            onScan$ (Subject<string>)
                   │
           enqueueScan(code)
                   │
        searchAndDispatch(code)
                   │
       dispatchScannedProduct(product)
                   │
    saleXxx.onProductScanned(product)  ← INCHANGÉ
```


> **Contexte** : Le module de réception BL (`commande-received`) dispose déjà d'une intégration
> complète du scanner USB CDC via Tauri (port série Rust → événement Angular).
> Ce plan décrit comment porter ce mécanisme au module de vente sans casser l'architecture HID existante.

---

## 1. État des lieux

### 1.1 Architecture scanner actuelle (module vente)

```
Clavier (HID)
  └── document:keydown
        └── SalesHomeComponent.handleGlobalKeyboardEvent()
              └── GlobalScannerService.processKeyEvent()
                    ├── Mode TIMING  : BaseScannerService.processKey()
                    │     └── onScanCompleted() → scannedCode$.next(code)
                    └── Mode PREFIX_SUFFIX : PrefixSuffixScannerService.processKeyEvent()
                          └── onScan$.next(code)
                                └── (merge) GlobalScannerService.onScan$
                                      └── SalesHomeComponent.enqueueScan(code)
                                            └── searchAndDispatch(code)
                                                  └── dispatchScannedProduct(product)
                                                        └── saleXxx.onProductScanned(product)
                                                              └── productHandling.onProductScanned()
                                                                    └── addProductToSale(product, 1)
```

**Services impliqués :**

| Service | Rôle |
|---|---|
| `GlobalScannerService` | Orchestrateur HID — TIMING et PREFIX_SUFFIX |
| `BaseScannerService` | Détection par timing frappe (< 30 ms entre touches) |
| `PrefixSuffixScannerService` | Détection STX (Ctrl+B) / ETX (Ctrl+C) |
| `ScanDetectorService` | Scanner composant local (`ProductSearchComponent`) |
| `ScanAudioFeedbackService` | Bips succès / erreur |

**Limitations du mode HID :**
- Pollue les `<input>` actifs (les caractères du code-barres s'y insèrent)
- Dépend des timings du driver clavier USB — variable selon les PC
- Incompatible avec certains scanners en mode USB-COM (aucune frappe clavier émise)

---

### 1.2 Architecture scanner CDC — module réception (référence)

```
Scanner USB COM (port série Rust)
  └── start_scanner_listener (Tauri invoke)
        └── thread Rust → serialport::read()
              └── flush sur CR/LF ou timeout 100 ms
                    └── app.emit("scan-reception", code)
                          └── listen<string>('scan-reception') (Angular)
                                └── scanValue = code → onScanReception()
```

**Points-clés de l'implémentation réception :**

- Stratégie de sélection scanner : `setupBarcodeScannerWithFallback(posteId)`
  1. Device actif en base (`PosteDeviceService.getActiveDevice`)
  2. Vérification connexion (`TauriDeviceDetectionService.isPortConnected`)
  3. Fallback sur tous les SCANNER du poste (batch check)
  4. Fallback HID si aucun connecté
- Nom événement Tauri : `'scan-reception'` (isolé du module vente)
- Signal `scannerMode : 'HID' | 'SERIAL' | null`
- Cleanup complet sur `DestroyRef.onDestroy` (unlisten + `stop_scanner_listener`)
- **Un seul `SCANNER_RUNNING` global côté Rust** → un seul listener à la fois (safe car modules
  sur des routes distinctes, non simultanés)

---

## 2. Analyse des impacts

### 2.1 Ce qui doit changer

| Fichier | Type de changement |
|---|---|
| `SalesHomeComponent` | Remplacer `GlobalScannerService.enable()` par `SalesScannerService.setup()` |
| `sales-home.component.html` | Indicateur mode scanner (badge SERIAL / HID) |
| `ProductSearchComponent` | Désactiver scan local (`ScanDetectorService`) en mode SERIAL |
| **NOUVEAU** `SalesScannerService` | Orchestrateur unifié HID + CDC pour la vente |

### 2.2 Ce qui ne change pas

- `GlobalScannerService` — reste inchangé (utilisé hors Tauri)
- `BaseScannerService`, `PrefixSuffixScannerService` — inchangés
- `enqueueScan()` / `searchAndDispatch()` / `dispatchScannedProduct()` — inchangés
- `productHandling.onProductScanned()` — inchangé
- `ScanAudioFeedbackService` — inchangé
- Tous les composants enfants (`SaleCreationComponent`, etc.) — inchangés

---

## 3. Architecture cible

```
Mode WEB / HID (inchangé)
  └── document:keydown
        └── SalesScannerService → délègue à GlobalScannerService
              └── onScan$ → SalesHomeComponent.enqueueScan(code)

Mode TAURI CDC (nouveau)
  └── Rust : start_scanner_listener('scan-vente')
        └── serialport → app.emit('scan-vente', code)
              └── listen<string>('scan-vente') dans SalesScannerService
                    └── onScan$ → SalesHomeComponent.enqueueScan(code)
```

**`SalesScannerService`** est le seul point d'entrée pour `SalesHomeComponent`.
Il expose une interface identique dans les deux modes :

```typescript
interface SalesScannerFacade {
  readonly onScan$: Observable<string>;
  readonly scannerMode: Signal<'HID' | 'SERIAL' | null>;
  setup(posteId?: number): Promise<void>;
  teardown(): Promise<void>;
}
```

---

## 4. Plan d'implémentation détaillé

### Étape 1 — Créer `SalesScannerService`

**Fichier :** `src/main/webapp/app/features/sales/data-access/services/sales-scanner.service.ts`

**Responsabilités :**
- Décider du mode (HID vs CDC) selon la disponibilité Tauri + device configuré
- En mode HID : activer `GlobalScannerService` et proxier `onScan$`
- En mode CDC : démarrer le listener Tauri et alimenter `onScan$`
- Exposer `scannerMode` signal pour le template
- Gérer le cleanup complet

**Pseudo-code :**

```typescript
@Injectable()  // NON providedIn:'root' — isolé par composant
export class SalesScannerService {
  private readonly globalScanner   = inject(GlobalScannerService);
  private readonly tauriDevice     = inject(TauriDeviceDetectionService);
  private readonly posteDevService = inject(PosteDeviceService);
  private readonly configService   = inject(ConfigurationService);

  readonly scannerMode = signal<'HID' | 'SERIAL' | null>(null);

  private readonly _onScan$ = new Subject<string>();
  readonly onScan$ = this._onScan$.asObservable();

  private unlistenScan: (() => void) | null = null;
  private unlistenError: (() => void) | null = null;

  async setup(posteId?: number): Promise<void> {
    if (this.tauriDevice.isTauriAvailable() && posteId) {
      const started = await this.trySetupCdc(posteId);
      if (started) return;
    }
    this.setupHid();
  }

  async teardown(): Promise<void> {
    this.globalScanner.disable();
    this.unlistenScan?.();
    this.unlistenError?.();
    if (this.scannerMode() === 'SERIAL') {
      const { invoke } = await import('@tauri-apps/api/core');
      try { await invoke('stop_scanner_listener'); } catch { /* déjà fermé */ }
    }
    this.scannerMode.set(null);
  }

  /** Traiter les touches clavier HID (délégué depuis SalesHomeComponent) */
  processKeyEvent(event: KeyboardEvent): { isScanInProgress: boolean } {
    if (this.scannerMode() === 'SERIAL') return { isScanInProgress: false };
    return this.globalScanner.processKeyEvent(event);
  }

  private setupHid(): void {
    this.globalScanner.enable();
    this.globalScanner.onScan$.subscribe(code => this._onScan$.next(code));
    this.scannerMode.set('HID');
  }

  private async trySetupCdc(posteId: number): Promise<boolean> {
    // Stratégie identique à setupBarcodeScannerWithFallback() de commande-received
    try {
      // 1. Device actif
      const activeRes = await firstValueFrom(this.posteDevService.getActiveDevice(posteId, 'SCANNER'));
      const device = activeRes.body;
      if (device?.portName) {
        const connected = await this.tauriDevice.isPortConnected(device.portName);
        if (connected) return this.startCdcListener(device.portName, device.baudRate ?? 9600);
      }
      // 2. Fallback sur tous les scanners du poste
      const allRes = await firstValueFrom(this.posteDevService.fetchAll(posteId, 'SCANNER'));
      const devices = (allRes.body ?? []).filter(d => d.portName);
      if (devices.length) {
        const statuses = await this.tauriDevice.checkPortsConnection(devices.map(d => d.portName!));
        const found = devices.find(d => statuses.some(s => s.portName === d.portName && s.connected));
        if (found?.portName) return this.startCdcListener(found.portName, found.baudRate ?? 9600);
      }
    } catch { /* fallback HID */ }
    return false;
  }

  private async startCdcListener(portName: string, baudRate: number): Promise<boolean> {
    const { invoke } = await import('@tauri-apps/api/core');
    const { listen } = await import('@tauri-apps/api/event');
    await invoke('start_scanner_listener', { portName, baudRate, eventName: 'scan-vente' });
    this.unlistenScan  = await listen<string>('scan-vente', ev => this._onScan$.next(ev.payload));
    this.unlistenError = await listen<string>('scan-error',  () => {
      // Scanner déconnecté → basculer en HID
      this.teardown().then(() => this.setupHid());
    });
    this.scannerMode.set('SERIAL');
    return true;
  }
}
```

---

### Étape 2 — Modifier `SalesHomeComponent`

**Fichier :** `sales-home.component.ts`

**Changements :**

```typescript
// AVANT
private globalScanner = inject(GlobalScannerService);

ngOnInit(): void {
  this.globalScanner.enable();
  this.globalScanner.onScan$.pipe(...).subscribe(code => this.enqueueScan(code));
}

handleGlobalKeyboardEvent(event: KeyboardEvent): void {
  const result = this.globalScanner.processKeyEvent(event);
  ...
}
```

```typescript
// APRÈS
private readonly salesScanner = inject(SalesScannerService);
protected readonly scannerMode = this.salesScanner.scannerMode;  // pour le template

ngOnInit(): void {
  const posteId = this.configurationService.getCurrentPoste()?.id;
  this.salesScanner.setup(posteId);
  this.salesScanner.onScan$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(code => this.enqueueScan(code));
}

ngOnDestroy(): void {
  this.salesScanner.teardown();
}

handleGlobalKeyboardEvent(event: KeyboardEvent): void {
  const result = this.salesScanner.processKeyEvent(event); // délègue (no-op si SERIAL)
  ...
}
```

**Ajouter dans `providers` du composant :**
```typescript
providers: [SalesScannerService]
```

---

### Étape 3 — Modifier `sales-home.component.html`

Ajouter un indicateur visuel du mode scanner dans le header (même style que `cr-serial-badge` de la réception) :

```html
<!-- Dans la barre d'actions header -->
@if (scannerMode() === 'SERIAL') {
  <span class="sales-serial-badge"
        pTooltip="Douchette USB série active — lecture automatique"
        tooltipPosition="bottom">
    <i class="pi pi-wifi"></i> Scanner série
  </span>
}
```

**Position suggérée :** après le `p-tag` "Taux service" ou dans la zone des raccourcis clavier.

---

### Étape 4 — Modifier `ProductSearchComponent`

**Fichier :** `features/sales/ui/product-search/product-search.component.ts`

Quand le scanner est en mode CDC, `ScanDetectorService` (local) ne doit pas tenter de détecter des scans clavier (il n'y en a pas).

```typescript
// Injecter le mode scanner
private readonly salesScanner = inject(SalesScannerService);

private setupBarcodeScanner(): void {
  if (this.salesScanner.scannerMode() === 'SERIAL') {
    // CDC actif : pas de scan local à détecter
    return;
  }
  // ... logique existante
}
```

> **Note :** `SalesScannerService` n'est accessible depuis `ProductSearchComponent` que si
> fourni au niveau du module parent (`SalesHomeComponent`). Il faut soit le fournir à la
> racine du module vente, soit utiliser un token d'injection partagé.

**Alternative simple :** Injecter `TauriDeviceDetectionService` directement dans
`ProductSearchComponent` et vérifier `systemInfo()` pour savoir si on est en Tauri, puis
vérifier `localStorage` pour le mode scanner.

---

### Étape 5 — Cleanup SCSS

**Fichier :** `features/sales/feature/sales-home/sales-home.component.scss` (ou fichier SCSS partagé)

```scss
.sales-serial-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.65rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  background: rgba(13, 110, 253, 0.1);
  color: #0d6efd;
  border: 1px solid rgba(13, 110, 253, 0.3);

  .pi-wifi { animation: wifi-pulse 1.8s ease-in-out infinite; }
}

@keyframes wifi-pulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.4; }
}
```

---

## 5. Contraintes et points d'attention

### 5.1 Un seul `SCANNER_RUNNING` Rust

Le fichier `scanner.rs` utilise un `static AtomicBool` global. **Un seul listener peut tourner
à la fois.** Ceci est safe car :

- Module vente et module réception sont sur des routes Angular distinctes
- Angular détruit le composant (et son `DestroyRef.onDestroy`) à la navigation
- Le `teardown()` de `SalesScannerService` appelle `stop_scanner_listener` avant destruction
- Aucun risque de double-start si les cleanups sont corrects

**À vérifier :** Si l'utilisateur navigue rapidement (< 100 ms), un race condition est possible.
Mitigation : ajouter un délai de 50 ms dans `setup()` si `SCANNER_RUNNING` revient en erreur.

### 5.2 Nom de l'événement Tauri

| Module | Événement Tauri |
|---|---|
| Réception BL | `scan-reception` |
| Vente | `scan-vente` |
| Test (form-poste) | `scan-test` |

Ces noms sont distincts pour éviter toute cross-écoute accidentelle entre modules.

### 5.3 `setup()` asynchrone dans `ngOnInit`

`ngOnInit` n'est pas `async` par convention Angular. Deux options :

- Option A : `ngOnInit() { this.salesScanner.setup(posteId); }` — fire-and-forget (acceptable, le
  scanner est opérationnel en ~50 ms, avant le premier scan humain)
- Option B : utiliser `APP_INITIALIZER` pour pré-charger le device actif — complexe, non recommandé

**Recommandation :** Option A avec gestion d'erreur dans `setup()`.

### 5.4 `ProductSearchComponent` et `SalesScannerService`

`SalesScannerService` est fourni dans `SalesHomeComponent.providers`. Il n'est pas accessible par
injection depuis `ProductSearchComponent` sauf si ce dernier est un enfant dans la hiérarchie DI.

Si `ProductSearchComponent` est standalone avec son propre injector, utiliser plutôt :

```typescript
// Alternative sans injection du service vente
private readonly tauriDevice = inject(TauriDeviceDetectionService);

private get isCdcActive(): boolean {
  return this.tauriDevice.isTauriAvailable() &&
         !!localStorage.getItem('sales-scanner-mode') &&
         localStorage.getItem('sales-scanner-mode') === 'SERIAL';
}
```

Ou exposer un signal global via `SalesScannerService` en `providedIn: 'root'` — au choix selon le
couplage acceptable.

---

## 6. Ordre des fichiers à créer / modifier

| Priorité | Fichier | Action |
|---|---|---|
| 1 | `sales/data-access/services/sales-scanner.service.ts` | **Créer** |
| 2 | `sales/feature/sales-home/sales-home.component.ts` | Modifier (inject + setup/teardown) |
| 3 | `sales/feature/sales-home/sales-home.component.html` | Modifier (badge SERIAL) |
| 4 | `sales/feature/sales-home/sales-home.component.scss` | Modifier (style badge) |
| 5 | `sales/ui/product-search/product-search.component.ts` | Modifier (skip scan local si SERIAL) |

---

## 7. Tests à effectuer

### Mode SERIAL (Tauri CDC)
- [ ] Scanner détecté et sélectionné au démarrage du module vente
- [ ] Scan d'un produit → ajout immédiat à la vente active
- [ ] Badge "Scanner série" visible dans le header
- [ ] Déconnexion USB → fallback automatique en mode HID + notification
- [ ] Navigation vers réception → `teardown()` libère le port → réception peut démarrer son listener
- [ ] Retour sur vente → `setup()` réouvre le port correctement
- [ ] `ProductSearchComponent` : champ recherche non pollué par les caractères du scan

### Mode HID (fallback)
- [ ] Comportement identique à avant (aucune régression)
- [ ] `GlobalScannerService.enable()` bien appelé
- [ ] `GlobalScannerService.disable()` appelé à la destruction

### Coexistence
- [ ] Ouvrir réception BL puis retourner aux ventes → un seul listener actif à la fois
- [ ] Deux scans rapides consécutifs → file d'attente (`scanQueue`) traitée séquentiellement

---

## 8. Résumé visuel

```
┌─────────────────────────────────────────────────────┐
│                SalesHomeComponent                   │
│  providers: [SalesScannerService]                   │
│                                                     │
│  ngOnInit → salesScanner.setup(posteId)             │
│  ngOnDestroy → salesScanner.teardown()              │
│  keydown → salesScanner.processKeyEvent()           │
│  salesScanner.onScan$ → enqueueScan(code)           │
└──────────────────────┬──────────────────────────────┘
                       │
          ┌────────────┴────────────┐
          │    SalesScannerService  │
          │                         │
     Tauri CDC ?                   │
          │                         │
    OUI ──┤                   NON ──┤
          │                         │
    ┌─────┴──────┐         ┌────────┴──────┐
    │ Rust CDC   │         │ GlobalScanner │
    │ 'scan-     │         │ Service HID   │
    │  vente'    │         │ (inchangé)    │
    └────────────┘         └───────────────┘
          │                         │
          └────────┬────────────────┘
                   │
            onScan$ (Subject<string>)
                   │
           enqueueScan(code)
                   │
        searchAndDispatch(code)
                   │
       dispatchScannedProduct(product)
                   │
    saleXxx.onProductScanned(product)  ← INCHANGÉ
```
