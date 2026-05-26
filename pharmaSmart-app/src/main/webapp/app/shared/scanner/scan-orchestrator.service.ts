import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { TauriDeviceDetectionService } from '../services/tauri-device-detection.service';
import { NotificationService } from '../services/notification.service';
import type { BarcodeType } from '../model/reception-scan-result.model';

export type { BarcodeType };

/** Code scanné avec son type détecté côté client (sans appel réseau). */
export interface BarcodeScanEvent {
  raw: string;
  barcodeType: BarcodeType;
}

export interface ScanOrchestratorConfig {
  /** Nom de l'event Tauri émis par le thread Rust scanner (ex: 'scan-vente', 'scan-reception'). */
  eventName: string;
  /** Source HID — observable de codes complets en mode clavier. */
  hidSource$: Observable<string>;
  /** Active la capture HID (souscription / activation du buffer global). */
  hidEnable: () => void;
  /** Désactive la capture HID. */
  hidDisable: () => void;
}

export type ScannerStatus = 'IDLE' | 'DETECTING' | 'SERIAL' | 'HID' | 'RETRYING';

/** Codes d'erreur typés émis par le thread Rust scanner sur l'event `scan-error`. */
export type ScanErrorCode = 'OPEN_FAILED' | 'DISCONNECTED' | 'PORT_DROPPED';

export interface ScanErrorPayload {
  code: ScanErrorCode;
  portName: string;
  details: string;
}

/**
 * Orchestrateur unifié HID + Tauri SERIAL (USB CDC).
 *
 * Mécanismes :
 *  - `detectScannerUsbMode()` : détecte en ~10 ms si la douchette est en mode CDC ou HID
 *      via les VID USB reconnus (Zebra DS2208 → 0x05E0, Honeywell → 0x0C2E, Netum → 0x28E9…).
 *      • CDC  → connexion directe sur le port détecté (pas de requête DB).
 *      • HID  → `setupHid('HID')` immédiat (pas de tentative port série).
 *  - Reconnexion auto WM_DEVICECHANGE (déclencheur primaire côté Rust).
 *  - Retry périodique CDC avec backoff exponentiel (filet de sécurité).
 *  - Bouton reconnexion manuelle (canReconnect).
 *  - Délai de grâce 300 ms après start_scanner_listener (vide le buffer UART).
 *
 * Aucune requête base de données — 100 % plug-and-play.
 * Le paramètre `posteId` de `setup()` est conservé pour compatibilité d'API descendante
 * mais n'est plus utilisé en interne.
 *
 * Doit être déclaré en `providers` au niveau composant — instance isolée par contexte.
 * Avant tout `setup()`, appeler `configure()` pour fournir source HID + nom d'event Tauri.
 */
@Injectable()
export class ScanOrchestratorService {
  private readonly tauriDevice = inject(TauriDeviceDetectionService);
  private readonly notification = inject(NotificationService);

  /** État interne typé. */
  readonly status = signal<ScannerStatus>('IDLE');

  /**
   * Compatibilité ascendante — utilisé par les badges UI existants.
   *  - `SERIAL`   → `'SERIAL'`
   *  - `HID`      → `'HID'`
   *  - `RETRYING` → `'HID'` : HID est actif comme fallback pendant les retries CDC
   *  - `DETECTING`→ `null`  : détection en cours, aucun mode confirmé
   *  - `IDLE`     → `null`
   */
  readonly scannerMode = computed<'SERIAL' | 'HID' | null>(() => {
    const s = this.status();
    if (s === 'SERIAL') return 'SERIAL';
    if (s === 'HID' || s === 'RETRYING') return 'HID';
    return null; // IDLE | DETECTING
  });

  /**
   * Vrai si au moins une connexion SERIAL a été établie avec succès dans cette session.
   * Évite d'afficher le bouton "Reconnecter" quand aucun scanner n'a jamais fonctionné.
   */
  private readonly _hadSerial = signal(false);

  /**
   * Bouton reconnexion visible si :
   *  - Tauri disponible (app desktop)
   *  - Mode HID actif (= RETRYING ou HID pur)
   *  - Une connexion SERIAL a déjà réussi dans cette session
   *    (évite d'afficher le bouton si la douchette n'a jamais été en mode CDC)
   */
  readonly canReconnect = computed(
    () => this.tauriDevice.isTauriAvailable() && this.scannerMode() === 'HID' && this._hadSerial(),
  );

  private readonly _onScan$ = new Subject<string>();
  readonly onScan$: Observable<string> = this._onScan$.asObservable();

  /**
   * Émission enrichie : code brut + type détecté côté client en ~0 ms.
   * Utilisé par les consommateurs qui ont besoin du type sans aller-retour HTTP.
   */
  readonly onScanEvent$: Observable<BarcodeScanEvent> = this.onScan$.pipe(
    map(raw => ({ raw, barcodeType: ScanOrchestratorService.detectBarcodeType(raw) })),
  );

  private config: ScanOrchestratorConfig | null = null;
  private unlistenScan: (() => void) | null = null;
  private unlistenError: (() => void) | null = null;
  /** Timer retry CDC (backoff exponentiel). */
  private cdcRetryTimer: ReturnType<typeof setTimeout> | null = null;
  private cdcRetryCount = 0;

  private static readonly RETRY_MAX = Number.MAX_SAFE_INTEGER;
  /** Backoff exponentiel : delay = min(BASE × 2^attempt, CAP). */
  private static readonly RETRY_BASE_MS = 8_000;
  private static readonly RETRY_CAP_MS = 60_000;
  private static readonly UART_GRACE_PERIOD_MS = 300;
  /** Cooldown après OPEN_FAILED standard (driver CDC pas encore prêt). */
  private static readonly OPEN_FAILED_COOLDOWN_MS = 10_000;
  /** Cooldowns spécifiques pour ERROR_GEN_FAILURE (0x1F) — back-off long. */
  private static readonly GEN_FAILURE_COOLDOWNS_MS: readonly number[] = [
    60_000,  // 1 min
    120_000, // 2 min
    300_000, // 5 min — cap filet de sécurité
  ];
  /** Seuil d'OPEN_FAILED non-GEN avant abandon CDC → bascule HID définitive. */
  private static readonly MAX_CONSECUTIVE_OPEN_FAILED = 8;
  private static readonly WIN_GEN_FAILURE_FRAGMENT = 'ne fonctionne pas correctement';
  private consecutiveOpenFailedCount = 0;

  /** Port et baud rate retenus pour la reconnexion directe après erreur. */
  private currentPortName: string | null = null;
  private currentBaudRate = 9600;

  /** Sonde FE : vérifie périodiquement que le port est toujours énuméré (filet sécurité). */
  private aliveProbeTimer: ReturnType<typeof setInterval> | null = null;
  private static readonly ALIVE_PROBE_INTERVAL_MS = 5_000;

  private unlistenRawMode: (() => void) | null = null;
  private unlistenRawByteDebug: (() => void) | null = null;

  /** Écouteur WM_DEVICECHANGE/DBT_DEVICEARRIVAL — déclencheur primaire de reconnexion. */
  private unlistenUsbArrived: (() => void) | null = null;
  /** Écouteur WM_DEVICECHANGE/DBT_DEVICEREMOVECOMPLETE — nettoyage immédiat. */
  private unlistenUsbRemoved: (() => void) | null = null;
  private lastUsbArrivedAt = 0;
  private static readonly USB_ARRIVED_DEBOUNCE_MS = 1_000;
  /**
   * Délai post-DBT_DEVICEARRIVAL avant ouverture : laisse usbser.sys finir son init.
   * 7 s couvre les douchettes filaires (CDC ~200 ms) ET les dongles 2.4G (1-2 s).
   */
  private static readonly USB_ARRIVED_REOPEN_DELAY_MS = 7_000;

  private scanReady = false;
  private destroyed = false;
  private readonly hidDestroyer$ = new Subject<void>();

  configure(config: ScanOrchestratorConfig): void {
    this.config = config;
  }

  /**
   * Démarre le scanner.
   *
   * Séquence (Tauri disponible) :
   *  1. `detectScannerUsbMode()` — détecte CDC / HID / NOT_CONNECTED en ~10 ms.
   *     • `HID`           → `setupHid('HID')` immédiat.
   *     • `CDC` + port    → `startSerialListener()` direct.
   *     • `NOT_CONNECTED` → `RETRYING` + attente `WM_DEVICECHANGE`.
   *  2. Tauri absent → `setupHid('HID')` direct (navigateur web).
   *
   * Le paramètre `_posteId` est conservé pour compatibilité API descendante uniquement.
   */
  async setup(_posteId?: number): Promise<void> {
    if (!this.config) {
      throw new Error('ScanOrchestratorService.configure() doit être appelé avant setup()');
    }
    this.destroyed = false;
    this.stopRetry();
    this.cdcRetryCount = 0;

    if (this.tauriDevice.isTauriAvailable()) {
      void this.setupUsbDeviceListener();
      this.status.set('DETECTING');

      const started = await this.tryConnectCdc();
      if (this.destroyed) return;
      if (!started) {
        // Aucun scanner CDC/HID détecté : activer HID comme fallback temporaire
        // (saisie clavier active pendant les retries CDC).
        // La souscription HID sera annulée par hidDestroyer$ dès que startSerialListener réussit.
        this.setupHid('RETRYING');
        this.scheduleRetry(); // backoff initial : 8 s
      }
      return;
    }
    this.setupHid('HID');
  }

  /** Stoppe proprement HID + SERIAL + retry. À appeler dans onDestroy du composant. */
  async teardown(): Promise<void> {
    this.destroyed = true;
    this.stopRetry();
    this.stopAliveProbe();
    this.detachUsbDeviceListener();
    this.hidDestroyer$.next();
    this.config?.hidDisable();
    this.unlistenScan?.();
    this.unlistenError?.();
    this.unlistenRawMode?.();
    this.unlistenRawByteDebug?.();
    this.unlistenScan = null;
    this.unlistenError = null;
    this.unlistenRawMode = null;
    this.unlistenRawByteDebug = null;
    this.scanReady = false;
    this.consecutiveOpenFailedCount = 0;
    this._hadSerial.set(false);
    if (this.tauriDevice.isTauriAvailable()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        const stopArgs = this.currentPortName ? { portName: this.currentPortName } : { portName: '' };
        await invoke('stop_scanner_listener', stopArgs);
      } catch { /* déjà fermé */ }
    }
    this.status.set('IDLE');
  }

  /** Reconnexion manuelle (bouton header). */
  async reconnect(): Promise<void> {
    this.consecutiveOpenFailedCount = 0;
    await this.setup();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Mode HID
  // ─────────────────────────────────────────────────────────────────────────

  private setupHid(targetStatus: 'HID' | 'RETRYING'): void {
    if (!this.config) return;
    this.hidDestroyer$.next();
    this.config.hidEnable();
    this.config.hidSource$
      .pipe(takeUntil(this.hidDestroyer$))
      .subscribe(code => this._onScan$.next(code));
    this.status.set(targetStatus);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Détection + connexion CDC (utilisée par setup() ET le retry loop)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Tente une détection + connexion en deux passes :
   *
   * **Passe 1 — `detectScannerUsbMode()`** (~10 ms, VID filtré côté Rust) :
   *  - `HID`  → active la capture clavier immédiatement, retourne `true`
   *  - `CDC`  → connexion directe sur le port détecté, retourne le résultat
   *
   * **Passe 2 — `listSerialPorts()` (fallback plug-and-play)** :
   *  Exécutée si la passe 1 retourne `NOT_CONNECTED` (VID non reconnu par
   *  `detect_scanner_usb_mode`, ou port COM non encore lié au VID dans le
   *  registre Windows). Filtre par `suggestedRole === 'scanner'` — même logique
   *  que l'ancienne étape 3 de `trySetupCdc()`, qui fonctionnait sur Zebra DS2208.
   */
  private async tryConnectCdc(): Promise<boolean> {
    if (this.destroyed) return false;
    try {

      const usbMode = await this.tauriDevice.detectScannerUsbMode();
      if (this.destroyed) return false;

      if (usbMode.mode === 'HID') {
        const label = usbMode.manufacturer ?? `VID ${usbMode.vid?.toString(16) ?? '?'}`;
        console.info(`[ScanOrchestrator] Scanner ${label} détecté en mode HID — saisie clavier directe`);
        this.notification.info(`Scanner ${label} en mode clavier (HID) — scan par frappe active`, 'Scanner');
        this.setupHid('HID');
        return true;
      }

      if (usbMode.mode === 'CDC' && usbMode.portName) {
        const label = usbMode.manufacturer ?? usbMode.portName;
        console.info(`[ScanOrchestrator] Scanner CDC ${label} sur ${usbMode.portName} — connexion directe`);
        await ScanOrchestratorService.driverStabilizationDelay();
        if (this.destroyed) return false;
        return this.startSerialListener(usbMode.portName, 9600);
      }
    } catch (err) {
      if (this.destroyed) return false;
      console.warn('[ScanOrchestrator] detect_scanner_usb_mode échoué, passage au fallback:', err);
    }


    // Filet si detect_scanner_usb_mode ne reconnaît pas le VID (ex: firmware
    // personnalisé, scanner générique, pilote virtuel COM).
    try {
      const systemPorts = await this.tauriDevice.listSerialPorts();
      if (this.destroyed) return false;
      const autoPort = systemPorts.find(p => p.suggestedRole === 'scanner');
      if (autoPort?.portName) {
        const label = autoPort.manufacturer ?? autoPort.portName;
        console.info(
          `[ScanOrchestrator] Plug-and-play fallback : ${autoPort.portName} (${label})` +
          ` — démarrage CDC sans enregistrement préalable`,
        );
        await ScanOrchestratorService.driverStabilizationDelay();
        if (this.destroyed) return false;
        return this.startSerialListener(autoPort.portName, 9600);
      }
    } catch (err) {
      if (this.destroyed) return false;
      console.warn('[ScanOrchestrator] Fallback listSerialPorts échoué:', err);
    }

    console.info('[ScanOrchestrator] Aucun scanner détecté — attente rebranchement USB');
    return false;
  }

  /**
   * Délai de stabilisation driver usbser.
   * 1500 ms : couvre filaires CDC (~200 ms) ET dongles 2.4G (1-2 s).
   */
  private static driverStabilizationDelay(): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, 1500));
  }

  /**
   * Démarre le listener Tauri sur le port série.
   *
   * Séquence :
   *  1. stop_scanner_listener ciblé → libère le handle Rust
   *  2. 800 ms → driver usbser libère le handle UART
   *  3. start_scanner_listener
   *  4. 300 ms grace → vide le buffer UART matériel
   *  5. scanReady = true
   *
   * @param portName - Port COM cible (ex: "COM7").
   * @param baudRate - Baud rate (défaut 9600).
   */
  private async startSerialListener(portName: string, baudRate: number): Promise<boolean> {
    if (!this.config) return false;
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      const { listen } = await import('@tauri-apps/api/event');

      const knownPort = this.currentPortName ?? portName;
      try {
        await invoke('stop_scanner_listener', { portName: knownPort });
      } catch {
        try { await invoke('stop_scanner_listener', { portName: '' }); } catch { /* ignoré */ }
      }

      await new Promise(resolve => setTimeout(resolve, 800));
      if (this.destroyed) return false;
      console.info(`[ScanOrchestrator] Ouverture ${portName} @ ${baudRate} bps`);

      await invoke('start_scanner_listener', { portName, baudRate, eventName: this.config.eventName });

      this.stopRetry();
      this.scanReady = false;
      this.currentPortName = portName;
      this.currentBaudRate = baudRate;
      this._hadSerial.set(true);
      this.status.set('SERIAL');
      this.startAliveProbe();

      this.unlistenScan?.();
      this.unlistenError?.();
      this.unlistenRawMode?.();
      this.unlistenRawByteDebug?.();

      this.unlistenScan = await listen<string>(this.config.eventName, ev => {
        if (!this.scanReady) return;
        if (this.consecutiveOpenFailedCount > 0 || this.cdcRetryCount > 0) {
          this.consecutiveOpenFailedCount = 0;
        }
        this._onScan$.next(ev.payload);
      });

      this.unlistenRawMode = await listen<string>('scan-raw-mode', () => {
        console.info(`[ScanOrchestrator] Mode CDC brut actif sur ${portName} (bypass GetCommState)`);
        this.consecutiveOpenFailedCount = 0;
        this.scanReady = true;
      });

      this.unlistenRawByteDebug = await listen<number>('scan-raw-byte', ev => {
        console.info(
          `[ScanOrchestrator] Premier octet brut : 0x${ev.payload.toString(16)}` +
          ` ('${String.fromCharCode(ev.payload)}')`,
        );
      });

      this.unlistenError = await listen<ScanErrorPayload>('scan-error', ev => {
        const { code, portName: errPort } = ev.payload;

        this.scanReady = false;
        this.stopAliveProbe();
        this.unlistenScan?.(); this.unlistenError?.(); this.unlistenRawMode?.(); this.unlistenRawByteDebug?.();
        this.unlistenScan = null; this.unlistenError = null; this.unlistenRawMode = null; this.unlistenRawByteDebug = null;

        if (code === 'OPEN_FAILED') {
          this.consecutiveOpenFailedCount++;
          const isGenFailure = ev.payload.details?.includes(ScanOrchestratorService.WIN_GEN_FAILURE_FRAGMENT) ?? false;

          // ── GEN_FAILURE : driver CDC pas encore prêt, attente WM_DEVICECHANGE ──────
          if (isGenFailure) {
            const attempt = this.consecutiveOpenFailedCount;
            const cooldownMs = ScanOrchestratorService.GEN_FAILURE_COOLDOWNS_MS[
              Math.min(attempt - 1, ScanOrchestratorService.GEN_FAILURE_COOLDOWNS_MS.length - 1)
            ];
            console.warn(
              `[ScanOrchestrator] OPEN_FAILED #${attempt} sur ${errPort} ` +
              `[GEN_FAILURE] — filet de sécurité dans ${cooldownMs / 1000} s`,
            );
            this.status.set('RETRYING');
            this.scheduleRetry({ resetCount: false, delayMs: cooldownMs });
            return;
          }

          // ── Autres OPEN_FAILED : port occupé / baud rate / câble ──────────────────
          const cooldownMs = ScanOrchestratorService.OPEN_FAILED_COOLDOWN_MS;
          console.warn(
            `[ScanOrchestrator] OPEN_FAILED #${this.consecutiveOpenFailedCount} sur ${errPort}` +
            ` — ${this.currentBaudRate} bps — ${ev.payload.details} — retry dans ${cooldownMs / 1000} s`,
          );
          if (this.consecutiveOpenFailedCount === 2) {
            console.info(
              `[ScanOrchestrator] Diagnostic ${errPort} : (1) port non occupé ? ` +
              `(2) baud rate correct (${this.currentBaudRate} bps) ? (3) câble OK ?`,
            );
          }
          // Seuil atteint → abandon CDC, bascule HID (problème permanent confirmé)
          if (this.consecutiveOpenFailedCount >= ScanOrchestratorService.MAX_CONSECUTIVE_OPEN_FAILED) {
            this.setupHid('HID');
            this.notification.warning(
              `Scanner ${errPort} : inaccessible après ${this.consecutiveOpenFailedCount} tentatives. ` +
              `Port occupé, baud rate incorrect (${this.currentBaudRate} bps) ou câble défectueux. ` +
              `Cliquez sur "Reconnecter" après vérification.`,
              'Scanner',
            );
            return;
          }
          this.status.set('RETRYING');
          this.scheduleRetry({ resetCount: false, delayMs: cooldownMs });

        } else {
          // ── DISCONNECTED / PORT_DROPPED ───────────────────────────────────────────
          // Si onUsbRemoved() a déjà pris la main, ce handler n'est pas atteint
          // (listeners FE déjà nettoyés). Filet si WM_DEVICECHANGE est raté.
          this.consecutiveOpenFailedCount = 0;
          this.notification.info(this.formatScanErrorMessage(ev.payload), 'Scanner');
          // Attente passive de DBT_DEVICEARRIVAL — pas de bascule HID.
          this.status.set('DETECTING');
          setTimeout(() => {
            if (!this.destroyed && this.status() === 'DETECTING') {
              console.info('[ScanOrchestrator] Filet DISCONNECTED — tentative auto-détection');
              void this.setup();
            }
          }, 12_000);
        }
      });

      setTimeout(() => { this.scanReady = true; }, ScanOrchestratorService.UART_GRACE_PERIOD_MS);

      this.hidDestroyer$.next();
      this.config.hidDisable();
      return true;

    } catch (err) {
      console.warn('[ScanOrchestrator] Échec démarrage CDC:', err);
      this.scanReady = false;
      if (this.destroyed) return false;
      this.status.set('RETRYING');
      this.scheduleRetry({ resetCount: false, delayMs: ScanOrchestratorService.OPEN_FAILED_COOLDOWN_MS });
      return false;
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Retry automatique CDC (backoff exponentiel)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Planifie la prochaine tentative de connexion CDC.
   * - `resetCount = true`  : repart de 0 (déconnexion franche).
   * - `resetCount = false` : continue le backoff (OPEN_FAILED).
   * - `delayMs`            : délai fixe prioritaire sur le backoff calculé.
   */
  private scheduleRetry(
    { resetCount = false, delayMs }: { resetCount?: boolean; delayMs?: number } = {},
  ): void {
    if (this.status() === 'SERIAL' || this.status() === 'DETECTING') return;
    this.stopRetry();
    if (resetCount) this.cdcRetryCount = 0;

    const backoffDelay = Math.min(
      ScanOrchestratorService.RETRY_BASE_MS * Math.pow(2, this.cdcRetryCount),
      ScanOrchestratorService.RETRY_CAP_MS,
    );
    const actualDelay = delayMs ?? backoffDelay;

    this.cdcRetryTimer = setTimeout(() => {
      this.cdcRetryTimer = null;
      if (this.destroyed) return;
      if (this.status() === 'SERIAL' || this.status() === 'DETECTING') return;
      void this.doRetryAttempt();
    }, actualDelay);
  }

  /** Exécute une tentative de connexion CDC et replanifie si échec. */
  private async doRetryAttempt(): Promise<void> {
    if (this.cdcRetryCount >= ScanOrchestratorService.RETRY_MAX) {
      this.status.set('RETRYING'); // reste visible, bouton reconnexion dispo
      return;
    }
    this.cdcRetryCount++;
    this.status.set('DETECTING');
    const started = await this.tryConnectCdc();
    if (!started && !this.destroyed && this.status() !== 'SERIAL') {
      this.status.set('RETRYING');
      this.scheduleRetry(); // prochain backoff (count incrémenté)
    }
  }

  private stopRetry(): void {
    if (this.cdcRetryTimer) {
      clearTimeout(this.cdcRetryTimer);
      this.cdcRetryTimer = null;
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Sonde FE indépendante (filet si le thread Rust rate la déconnexion)
  // ─────────────────────────────────────────────────────────────────────────

  private startAliveProbe(): void {
    this.stopAliveProbe();
    this.aliveProbeTimer = setInterval(
      () => this.checkPortAlive(),
      ScanOrchestratorService.ALIVE_PROBE_INTERVAL_MS,
    );
  }

  private stopAliveProbe(): void {
    if (this.aliveProbeTimer) {
      clearInterval(this.aliveProbeTimer);
      this.aliveProbeTimer = null;
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Écouteur USB Win32 (déclencheur primaire de reconnexion)
  // ─────────────────────────────────────────────────────────────────────────

  private async setupUsbDeviceListener(): Promise<void> {
    if (!this.tauriDevice.isTauriAvailable() || this.unlistenUsbArrived) return;
    try {
      const { listen } = await import('@tauri-apps/api/event');
      this.unlistenUsbArrived = await listen('scan-usb-arrived', () => this.onUsbArrived());
      this.unlistenUsbRemoved = await listen('scan-usb-removed', () => this.onUsbRemoved());
    } catch (e) {
      console.warn('[ScanOrchestrator] Échec attachement listener WM_DEVICECHANGE :', e);
    }
  }

  private detachUsbDeviceListener(): void {
    this.unlistenUsbArrived?.(); this.unlistenUsbArrived = null;
    this.unlistenUsbRemoved?.(); this.unlistenUsbRemoved = null;
  }

  /**
   * DBT_DEVICEARRIVAL — port CDC disponible. Lance `setup()` après le délai de stabilisation
   * driver. `setup()` repart de zéro (reset count, detection complète).
   */
  private onUsbArrived(): void {
    if (this.destroyed || this.status() === 'SERIAL') return;
    const now = Date.now();
    if (now - this.lastUsbArrivedAt < ScanOrchestratorService.USB_ARRIVED_DEBOUNCE_MS) return;
    this.lastUsbArrivedAt = now;

    console.info(
      '[ScanOrchestrator] WM_DEVICECHANGE/DBT_DEVICEARRIVAL — reconnexion dans ' +
      `${ScanOrchestratorService.USB_ARRIVED_REOPEN_DELAY_MS / 1000} s`,
    );
    this.stopRetry();
    this.consecutiveOpenFailedCount = 0;
    this.status.set('DETECTING');

    setTimeout(() => {
      if (this.destroyed || this.status() === 'SERIAL') return;
      void this.setup(); // reset count + détection complète
    }, ScanOrchestratorService.USB_ARRIVED_REOPEN_DELAY_MS);
  }

  /**
   * DBT_DEVICEREMOVECOMPLETE — port retiré physiquement.
   *
   * On nettoie les listeners, on arrête le thread Rust (libère le handle COM pour la
   * réouverture post-rebranchement) et on attend DBT_DEVICEARRIVAL.
   */
  private onUsbRemoved(): void {
    if (this.destroyed || this.status() !== 'SERIAL') return;
    console.info('[ScanOrchestrator] WM_DEVICECHANGE/DBT_DEVICEREMOVECOMPLETE — attente rebranchement');
    this.notification.info(
      `Scanner ${this.currentPortName} débranché — reconnexion automatique à l'insertion`,
      'Scanner',
    );

    this.stopAliveProbe();
    this.scanReady = false;
    this.unlistenScan?.(); this.unlistenError?.(); this.unlistenRawMode?.(); this.unlistenRawByteDebug?.();
    this.unlistenScan = null; this.unlistenError = null; this.unlistenRawMode = null; this.unlistenRawByteDebug = null;

    const port = this.currentPortName;
    if (port) {
      void import('@tauri-apps/api/core').then(({ invoke }) =>
        invoke('stop_scanner_listener', { portName: port }).catch(() => { /* idempotent */ }),
      );
    }

    this.consecutiveOpenFailedCount = 0;
    this.status.set('DETECTING'); // attend DBT_DEVICEARRIVAL — pas de bascule HID
  }

  /**
   * Sonde FE : délègue à `onUsbRemoved()` si le port a disparu sans WM_DEVICECHANGE
   * (filet si le thread Rust rate la détection).
   */
  private async checkPortAlive(): Promise<void> {
    if (this.destroyed || this.status() !== 'SERIAL' || !this.currentPortName) return;
    let alive: boolean;
    try {
      alive = await this.tauriDevice.isPortConnected(this.currentPortName);
    } catch {
      return; // erreur transitoire — réessai au prochain tick
    }
    if (alive || this.destroyed || this.status() !== 'SERIAL') return;
    this.onUsbRemoved(); // centralise la logique de nettoyage
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Détection de type de code-barres (miroir de DataMatrixParserService.java)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Détecte le type de code-barres sans appel réseau.
   * Logique alignée sur DataMatrixParserService.BarcodeType (Java).
   */
  static detectBarcodeType(code: string): BarcodeType {
    if (!code) return 'UNKNOWN';
    // DataMatrix : identifiant de symbologie ]d2, séparateur GS (ASCII 29),
    // préfixe STX () ou placeholders texte émis par certains décodeurs.
    if (
      code.startsWith(']d2') ||
      code.includes('') ||
      code.startsWith('') ||
      code.includes('<GS>') ||
      code.includes('{GS}')
    ) {
      return 'DATAMATRIX';
    }
    // Supprime un éventuel identifiant de symbologie (ex: ]C1, ]E0, ]d1)
    const stripped = code.replace(/^\][A-Za-z]\d/, '');
    if (!/^\d+$/.test(stripped)) return 'UNKNOWN';
    if (stripped.length === 7) return 'CIP_7';
    if (stripped.length === 8) return 'EAN_8';
    if (stripped.length === 13 && stripped.startsWith('340')) return 'CIP_13';
    if (stripped.length === 13) return 'EAN_13';
    return 'UNKNOWN';
  }

  private formatScanErrorMessage(payload: ScanErrorPayload): string {
    const { code, portName, details } = payload;
    switch (code) {
      case 'PORT_DROPPED':   return `Scanner ${portName} : port disparu`;
      case 'OPEN_FAILED':    return `Scanner ${portName} : ouverture impossible (${details})`;
      case 'DISCONNECTED':   return `Scanner ${portName} : déconnexion (${details})`;
      default:               return `Scanner ${portName} déconnecté`;
    }
  }
}
