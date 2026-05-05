import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom, Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TauriDeviceDetectionService } from '../services/tauri-device-detection.service';
import { PosteDeviceService } from '../../features/settings/feature/poste/poste-device.service';
import { NotificationService } from '../services/notification.service';

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
 *  - Détection automatique du scanner CDC au démarrage
 *  - Fallback HID si aucun device CDC connecté
 *  - Retry périodique CDC (8 s × 10 max) après fallback
 *  - Reconnexion auto sur scan-error (déconnexion / veille)
 *  - Bouton reconnexion manuelle (canReconnect)
 *  - Délai de grâce 300 ms après start_scanner_listener (vide le buffer UART)
 *
 * Doit être déclaré en `providers` au niveau composant — instance isolée par contexte.
 * Avant tout `setup()`, appeler `configure()` pour fournir source HID + nom d'event Tauri.
 */
@Injectable()
export class ScanOrchestratorService {
  private readonly tauriDevice = inject(TauriDeviceDetectionService);
  private readonly posteDevService = inject(PosteDeviceService);
  private readonly notification = inject(NotificationService);

  /** État interne typé. */
  readonly status = signal<ScannerStatus>('IDLE');

  /** Compatibilité ascendante : 'SERIAL' | 'HID' | null (utilisé par les badges UI existants). */
  readonly scannerMode = computed<'SERIAL' | 'HID' | null>(() => {
    const s = this.status();
    if (s === 'SERIAL') return 'SERIAL';
    if (s === 'HID' || s === 'RETRYING') return 'HID';
    return null;
  });

  readonly canReconnect = computed(
    () => this.tauriDevice.isTauriAvailable() && this.scannerMode() === 'HID' && this.currentPosteId !== null,
  );

  private readonly _onScan$ = new Subject<string>();
  readonly onScan$: Observable<string> = this._onScan$.asObservable();

  private config: ScanOrchestratorConfig | null = null;
  private currentPosteId: number | null = null;
  private unlistenScan: (() => void) | null = null;
  private unlistenError: (() => void) | null = null;
  /** Timer pour le prochain retry CDC (setTimeout récursif → backoff exponentiel). */
  private cdcRetryTimer: ReturnType<typeof setTimeout> | null = null;
  private cdcRetryCount = 0;
  /**
   * Pas de limite stricte sur le nombre de retries CDC.
   *
   * Justification : l'utilisateur peut rebrancher la douchette puis ne scanner
   * que plusieurs heures plus tard (vente / réception ponctuelle). Si on
   * abandonne après N tentatives, le scanner reste en HID jusqu'au prochain
   * teardown manuel — le caissier doit alors cliquer sur "reconnexion".
   *
   * Coût d'une tentative au cap (60 s) : 1 appel `available_ports()` Windows
   * (~5 ms) + éventuellement `serialport::open()`. Négligeable même sur la
   * journée entière.
   */
  private static readonly RETRY_MAX = Number.MAX_SAFE_INTEGER;
  /** Backoff exponentiel : delay = min(BASE × 2^attempt, CAP). */
  private static readonly RETRY_BASE_MS = 8000;
  private static readonly RETRY_CAP_MS = 60000;
  private static readonly UART_GRACE_PERIOD_MS = 300;
  /**
   * Cooldown après OPEN_FAILED standard : laisse le driver CDC Windows finir son initialisation.
   * Windows peut prendre 15-30 s après reconnexion physique avant d'être prêt.
   */
  private static readonly OPEN_FAILED_COOLDOWN_MS = 10_000;
  /**
   * Back-off "filet de sécurité" pour ERROR_GEN_FAILURE (0x1F).
   *
   * Le déclencheur PRIMAIRE de reconnexion est l'événement Win32
   * `WM_DEVICECHANGE / DBT_DEVICEARRIVAL` (émis par le moniteur USB côté Rust
   * dès que la pile USB du device est prête). Le polling timé n'est qu'un
   * fallback au cas où l'événement serait raté.
   *
   * Cooldowns longs (1 min → 5 min) parce que :
   *   - chaque `serialport::open()` envoie des transferts USB de contrôle qui,
   *     sur firmwares CDC bas de gamme, retardent la sortie de l'état
   *     GEN_FAILURE post-rebranchement
   *   - on ne veut PAS « poller » : on attend l'événement OS.
   */
  private static readonly GEN_FAILURE_COOLDOWNS_MS: readonly number[] = [
    60_000, // 1 min
    120_000, // 2 min
    300_000, // 5 min — cap (filet de sécurité uniquement)
  ];
  /**
   * Nombre maximum d'OPEN_FAILED consécutifs (erreurs NON-GEN_FAILURE) avant
   * d'abandonner et de basculer définitivement en HID (port occupé, baud rate
   * incorrect, câble défectueux…).
   */
  private static readonly MAX_CONSECUTIVE_OPEN_FAILED = 8;
  /**
   * Fragment de message Windows pour l'erreur ERROR_GEN_FAILURE (0x1F).
   * Détecté dans le champ `details` du payload scan-error.
   */
  private static readonly WIN_GEN_FAILURE_FRAGMENT = 'ne fonctionne pas correctement';
  /** Compteur d'échecs OPEN_FAILED consécutifs (réinitialisé sur succès de scan ou DISCONNECTED). */
  private consecutiveOpenFailedCount = 0;

  /** Port et baud rate actifs (retenus pour le watchdog et la reconnexion directe). */
  private currentPortName: string | null = null;
  private currentBaudRate = 9600;
  /**
   * Watchdog "port silencieux" désactivé — timer conservé uniquement pour teardown/clear.
   *
   * Le primer CDC Rust (séquence DTR via CreateFileW) gère maintenant l'init CDC
   * directement dans start_scanner_listener. Fermer une connexion active après 15 s
   * d'inactivité causait une boucle ERROR_GEN_FAILURE en pharmacie (caissier inactif).
   */
  private staleSerialTimer: ReturnType<typeof setTimeout> | null = null;

  /**
   * Sonde FE indépendante : vérifie périodiquement que le port CDC est toujours
   * énuméré par le système. Filet de sécurité côté frontend si le thread Rust
   * rate la détection de débranchement (handle obsolète, heartbeat trop lent).
   * Active uniquement en `status === 'SERIAL'`.
   */
  private aliveProbeTimer: ReturnType<typeof setInterval> | null = null;
  private static readonly ALIVE_PROBE_INTERVAL_MS = 5_000;

  /**
   * Écouteur Tauri pour les événements USB Windows émis par le moniteur Rust
   * (`device_monitor` qui écoute WM_DEVICECHANGE). C'est le déclencheur
   * primaire de reconnexion : il fire dès que `usbser.sys` a fini d'enregistrer
   * la nouvelle énumération du device, donc l'ouverture qui suit réussit du
   * premier coup sans avoir besoin de tirer au hasard.
   */
  private unlistenUsbArrived: (() => void) | null = null;
  /** Debounce des notifications USB (Windows émet parfois plusieurs DBT_DEVICEARRIVAL en rafale). */
  private lastUsbArrivedAt = 0;
  private static readonly USB_ARRIVED_DEBOUNCE_MS = 1_000;
  /**
   * Délai post-`DBT_DEVICEARRIVAL` avant la 1ère tentative d'ouverture.
   *
   * `DBT_DEVICEARRIVAL` fire quand `usbser.sys` enregistre l'interface PnP
   * du device — soit AVANT que le driver ait fini son init complète. Tenter
   * un `serialport::open()` trop tôt rate avec ERROR_GEN_FAILURE et chaque
   * échec pollue la pile USB, retardant la récupération.
   *
   * 7 s = compromis pragmatique : assez long pour que la majorité des firmwares
   * CDC standard (y compris dongles 2.4G) finissent l'init, assez court pour
   * rester quasi-transparent pour le caissier qui rebranche puis scanne.
   */
  private static readonly USB_ARRIVED_REOPEN_DELAY_MS = 7_000;

  /** Délai de grâce après start_scanner_listener : ignore les codes du buffer UART matériel. */
  private scanReady = false;
  /** Positionné à true dans teardown() — toutes les callbacks async vérifient ce flag. */
  private destroyed = false;
  /** Annule la souscription HID active avant d'en créer une nouvelle (évite les doublons). */
  private readonly hidDestroyer$ = new Subject<void>();

  configure(config: ScanOrchestratorConfig): void {
    this.config = config;
  }

  /**
   * Démarre le scanner.
   * Si Tauri + posteId → tente CDC, fallback HID + retry.
   * Sinon → HID direct.
   */
  async setup(posteId?: number): Promise<void> {
    if (!this.config) {
      throw new Error('ScanOrchestratorService.configure() doit être appelé avant setup()');
    }
    this.destroyed = false; // réinitialiser au cas où le composant réutilise l'instance
    if (posteId !== undefined && posteId !== null) {
      this.currentPosteId = posteId;
    }
    if (this.tauriDevice.isTauriAvailable() && this.currentPosteId) {
      // Attache l'écouteur USB une seule fois — fire-and-forget pour ne pas
      // bloquer le setup. Si l'attachement échoue, on retombera sur le timer
      // de back-off, mais en pratique l'écouteur est dispo dès le boot.
      void this.setupUsbDeviceListener();
      this.status.set('DETECTING');
      const started = await this.trySetupCdc(this.currentPosteId);
      if (started) return;
      this.setupHid('RETRYING');
      this.startCdcRetry(this.currentPosteId);
      return;
    }
    this.setupHid('HID');
  }

  /** Stoppe proprement HID + SERIAL + retry. À appeler dans onDestroy du composant. */
  async teardown(): Promise<void> {
    this.destroyed = true; // guard : empêche tout callback async de continuer après destroy
    this.stopCdcRetry();
    this.stopAliveProbe();
    this.detachUsbDeviceListener();
    this.hidDestroyer$.next();
    this.config?.hidDisable();
    this.unlistenScan?.();
    this.unlistenError?.();
    this.unlistenScan = null;
    this.unlistenError = null;
    this.scanReady = false;
    this.consecutiveOpenFailedCount = 0;
    this.clearStaleSerialWatchdog();
    // Toujours arrêter le thread Rust, quel que soit le statut courant :
    // si le composant est détruit en état RETRYING, le thread Rust peut quand même
    // être en cours (ou dans un état intermédiaire) et verrouiller le port COM.
    if (this.tauriDevice.isTauriAvailable()) {
      try {
        const { invoke } = await import('@tauri-apps/api/core');
        // Arrêt ciblé si le port est connu, sinon fallback global.
        const stopArgs = this.currentPortName ? { portName: this.currentPortName } : { portName: '' };
        await invoke('stop_scanner_listener', stopArgs);
      } catch {
        /* déjà fermé ou jamais démarré */
      }
    }
    this.status.set('IDLE');
  }

  /** Reconnexion manuelle (bouton header). */
  async reconnect(): Promise<void> {
    if (!this.currentPosteId) return;
    this.stopCdcRetry();
    this.consecutiveOpenFailedCount = 0;
    await this.setup(this.currentPosteId);
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
  // Mode SERIAL (Tauri CDC)
  // ─────────────────────────────────────────────────────────────────────────

  private async trySetupCdc(posteId: number): Promise<boolean> {
    if (this.destroyed) return false;
    try {
      // 1. Device préféré (actif en base)
      const activeRes = await firstValueFrom(this.posteDevService.getActiveDevice(posteId, 'SCANNER'));
      if (this.destroyed) return false; // composant détruit pendant la requête HTTP
      const device = activeRes.body;
      if (device?.portName) {
        const connected = await this.tauriDevice.isPortConnected(device.portName);
        if (this.destroyed) return false;
        if (connected) {
          // Délai de stabilisation : le driver usbser (Windows) peut mettre 1-2 s
          // après avoir enregistré le port avant que celui-ci soit réellement ouvrable.
          await ScanOrchestratorService.driverStabilizationDelay();
          if (this.destroyed) return false;
          return this.startSerialListener(device.portName, device.baudRate ?? 9600);
        }
      }
      // 2. Fallback batch sur tous les scanners du poste
      const allRes = await firstValueFrom(this.posteDevService.fetchAll(posteId, 'SCANNER'));
      if (this.destroyed) return false;
      const devices = (allRes.body ?? []).filter(d => d.portName);
      if (devices.length) {
        const statuses = await this.tauriDevice.checkPortsConnection(devices.map(d => d.portName!));
        if (this.destroyed) return false;
        const found = devices.find(d => statuses.some(s => s.portName === d.portName && s.connected));
        if (found?.portName) {
          await ScanOrchestratorService.driverStabilizationDelay();
          if (this.destroyed) return false;
          return this.startSerialListener(found.portName, found.baudRate ?? 9600);
        }
      }
    } catch (err) {
      // Ne pas logger ni tenter de fallback si le composant a été détruit entre-temps
      // (cas typique : NG0205 — l'injecteur Angular a été détruit pendant un await HTTP)
      if (this.destroyed) return false;
      console.warn('[ScanOrchestrator] Erreur détection CDC, fallback HID:', err);
    }
    return false;
  }

  /**
   * Délai de stabilisation du driver usbser (Windows).
   *
   * 1500 ms : marge générique couvrant les douchettes filaires (CDC standard,
   * ~200 ms) ET les douchettes 2.4G/Bluetooth dont le dongle a besoin de
   * ré-énumérer USB + ré-appairer la radio (1-2 s).
   * Le surcoût n'est facturé qu'à l'ouverture (setup initial / reconnexion),
   * pas pendant la lecture.
   */
  private static driverStabilizationDelay(): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, 1500));
  }

  /**
   * Démarre le listener Tauri sur le port série et enregistre les unlisten callbacks.
   * Séquence sécurisée :
   *  1. stop_scanner_listener global (arrête TOUS les threads Rust → libère le handle)
   *  2. Attente 800 ms — laisse le driver libérer le handle UART
   *  3. start_scanner_listener (nouveau thread Rust)
   *  4. Délai 300 ms → vide le buffer UART matériel
   *  5. scanReady = true → début de la capture réelle
   */
  private async startSerialListener(portName: string, baudRate: number): Promise<boolean> {
    if (!this.config) return false;
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      const { listen } = await import('@tauri-apps/api/event');

      // Arrêt ciblé en priorité (Rust identifie le thread par portName).
      // Si le portName est inconnu, on tente un arrêt global en fallback.
      // Dans les deux cas on capture l'erreur : idempotent si déjà arrêté.
      const knownPort = this.currentPortName ?? portName;
      try {
        await invoke('stop_scanner_listener', { portName: knownPort });
      } catch {
        try {
          // Fallback global : arrête tous les listeners actifs
          await invoke('stop_scanner_listener', { portName: '' });
        } catch {
          /* aucun listener actif — ignoré */
        }
      }
      // 800 ms : laisse le driver usbser.sys / FTDI libérer le handle UART.
      // Empiriquement nécessaire sur Windows pour éviter ERROR_GEN_FAILURE (#0x1F)
      // quand le port était déjà détenu par un précédent thread Rust.
      await new Promise(resolve => setTimeout(resolve, 800));
      if (this.destroyed) return false;
      console.info(`[ScanOrchestrator] Ouverture ${portName} @ ${baudRate} bps`);


      await invoke('start_scanner_listener', {
        portName,
        baudRate,
        eventName: this.config.eventName,
      });

      this.stopCdcRetry();
      this.clearStaleSerialWatchdog();
      this.scanReady = false;
      this.currentPortName = portName;
      this.currentBaudRate = baudRate;
      this.status.set('SERIAL');
      this.startAliveProbe();

      // Nettoyage des anciens listeners avant réenregistrement (cas reconnexion)
      this.unlistenScan?.();
      this.unlistenError?.();

      this.unlistenScan = await listen<string>(this.config.eventName, ev => {
        if (!this.scanReady) return;
        this.clearStaleSerialWatchdog();
        if (this.consecutiveOpenFailedCount > 0 || this.cdcRetryCount > 0) {
          this.consecutiveOpenFailedCount = 0;
          this.notification.info('Scanner CDC connecté — mode série actif', 'Scanner');
        }
        this._onScan$.next(ev.payload);
      });

      // Écouter la confirmation du mode CDC brut (fallback open_raw côté Rust)
      // Émis quand serialport::open() a échoué mais CreateFileW/ReadFile fonctionne.
      const unlistenRaw = await listen<string>('scan-raw-mode', () => {
        console.info(`[ScanOrchestrator] Mode CDC brut actif sur ${portName} (bypass GetCommState)`);
        this.consecutiveOpenFailedCount = 0;
        this.notification.info(`Scanner ${portName} connecté — mode CDC brut actif`, 'Scanner');
        this.scanReady = true;
      });

      // Diagnostic : premier octet reçu via ReadFile (confirme que les données circulent)
      const unlistenRawByte = await listen<number>('scan-raw-byte', ev => {
        console.info(`[ScanOrchestrator] Premier octet reçu en mode brut : 0x${ev.payload.toString(16)} ('${String.fromCharCode(ev.payload)}')`);
      });

      const originalUnlistenScan = this.unlistenScan;
      this.unlistenScan = () => { originalUnlistenScan?.(); unlistenRaw(); unlistenRawByte(); };

      this.unlistenError = await listen<ScanErrorPayload>('scan-error', ev => {
        const { code, portName } = ev.payload;

        this.scanReady = false;
        this.stopAliveProbe();
        this.clearStaleSerialWatchdog();
        this.unlistenScan?.();
        this.unlistenError?.();
        this.unlistenScan = null;
        this.unlistenError = null;

        if (code === 'OPEN_FAILED') {
          this.consecutiveOpenFailedCount++;
          const isGenFailure = ev.payload.details?.includes(ScanOrchestratorService.WIN_GEN_FAILURE_FRAGMENT) ?? false;

          // ── Gestion ERROR_GEN_FAILURE ────────────────────────────────────
          // Pas d'abandon, pas de notification utilisateur : la reconnexion
          // est gérée par l'événement Win32 `scan-usb-arrived` (cf.
          // setupUsbDeviceListener). Le timer de retry n'est qu'un filet
          // de sécurité avec back-off long pour ne pas polluer la pile USB.
          if (isGenFailure) {
            const attempt = this.consecutiveOpenFailedCount;
            const cooldownMs =
              ScanOrchestratorService.GEN_FAILURE_COOLDOWNS_MS[
                Math.min(attempt - 1, ScanOrchestratorService.GEN_FAILURE_COOLDOWNS_MS.length - 1)
              ];

            console.warn(
              `[ScanOrchestrator] OPEN_FAILED #${attempt} sur ${portName} ` +
                `[ERROR_GEN_FAILURE] — attente passive de WM_DEVICECHANGE, ` +
                `filet de sécurité dans ${cooldownMs / 1000} s.`,
            );

            this.setupHid('RETRYING');
            if (this.currentPosteId) {
              this.startCdcRetry(this.currentPosteId, { resetCount: false, cooldownMs });
            }
            return;
          }

          const cooldownMs = ScanOrchestratorService.OPEN_FAILED_COOLDOWN_MS;
          console.warn(
            `[ScanOrchestrator] OPEN_FAILED #${this.consecutiveOpenFailedCount} sur ${portName}` +
              ` — baud rate: ${this.currentBaudRate} bps` +
              ` — détails: ${ev.payload.details}` +
              ` — prochain essai dans ${cooldownMs / 1000} s`,
          );

          // ── Gestion des autres OPEN_FAILED (port occupé, driver, câble) ──────
          // Aide diagnostic dès la 2e tentative
          if (this.consecutiveOpenFailedCount === 2) {
            console.info(
              `[ScanOrchestrator] Diagnostic ${portName} : ` +
                `vérifiez (1) qu'aucune autre application n'utilise ce port, ` +
                `(2) que le baud rate configuré (${this.currentBaudRate} bps) correspond au scanner, ` +
                `(3) que le câble USB est correctement branché.`,
            );
          }

          // Seuil atteint → abandonner les retries, rester en HID (bouton reconnexion dispo)
          if (this.consecutiveOpenFailedCount >= ScanOrchestratorService.MAX_CONSECUTIVE_OPEN_FAILED) {
            this.setupHid('HID');
            this.notification.warning(
              `Scanner ${portName} : inaccessible après ${this.consecutiveOpenFailedCount} tentatives. ` +
                `Causes possibles : port occupé par une autre application, ` +
                `baud rate incorrect (actuel : ${this.currentBaudRate} bps), câble défectueux. ` +
                `Cliquez sur le bouton de reconnexion après vérification.`,
              'Scanner',
            );
            return;
          }

          this.setupHid('RETRYING');
          if (this.currentPosteId) {
            this.startCdcRetry(this.currentPosteId, { resetCount: false, cooldownMs });
          }
        } else {
          // Déconnexion réelle (câble, veille) → notifier + reset complet
          this.consecutiveOpenFailedCount = 0;
          this.notification.warning(this.formatScanErrorMessage(ev.payload), 'Scanner');
          this.setupHid('RETRYING');
          if (this.currentPosteId) {
            // Attendre 5 s après la déconnexion avant le premier retry :
            // Windows doit désenregistrer puis réenregistrer le port CDC.
            this.startCdcRetry(this.currentPosteId, { resetCount: true, cooldownMs: 5_000 });
          }
        }
      });

      // Délai de grâce : vider le buffer matériel UART
      setTimeout(() => {
        this.scanReady = true;
      }, ScanOrchestratorService.UART_GRACE_PERIOD_MS);

      // Watchdog désactivé : voir commentaire sur STALE_SERIAL_TIMEOUT_MS.
      // Le primer CDC Rust gère l'init sans fermer la connexion active.

      // Désactiver HID pendant SERIAL
      this.hidDestroyer$.next();
      this.config.hidDisable();

      return true;
    } catch (err) {
      console.warn('[ScanOrchestrator] Échec démarrage CDC, fallback HID:', err);
      this.scanReady = false;
      if (this.destroyed) return false;
      this.setupHid('RETRYING');
      if (this.currentPosteId) {
        this.startCdcRetry(this.currentPosteId, { resetCount: false, cooldownMs: ScanOrchestratorService.OPEN_FAILED_COOLDOWN_MS });
      }
      return false;
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Retry automatique CDC
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Démarre (ou reprend) le retry CDC.
   *
   * @param resetCount - `true` (défaut) pour réinitialiser le compteur (après vraie
   *   déconnexion). `false` pour conserver le compteur courant (après OPEN_FAILED :
   *   le port est apparu mais le driver n'est pas encore prêt → on continue le compte).
   * @param cooldownMs - Délai fixe avant la PREMIÈRE tentative (utile après OPEN_FAILED
   *   pour laisser le driver Windows finir son initialisation).
   */
  private startCdcRetry(
    posteId: number,
    { resetCount = true, cooldownMs = 0 }: { resetCount?: boolean; cooldownMs?: number } = {},
  ): void {
    // Ne pas démarrer un retry si une reconnexion USB est déjà en cours
    if (this.status() === 'SERIAL' || this.status() === 'DETECTING') return;
    // Annuler le timer en cours sans toucher au compteur ici
    if (this.cdcRetryTimer) {
      clearTimeout(this.cdcRetryTimer);
      this.cdcRetryTimer = null;
    }
    if (resetCount) {
      this.cdcRetryCount = 0;
    }
    if (cooldownMs > 0) {
      // Cooldown fixe → puis backoff exponentiel normal
      this.cdcRetryTimer = setTimeout(() => {
        this.cdcRetryTimer = null;
        if (this.destroyed) return; // guard : composant détruit pendant le cooldown
        this.scheduleNextRetry(posteId);
      }, cooldownMs);
    } else {
      this.scheduleNextRetry(posteId);
    }
  }

  /** Backoff exponentiel : 8 s, 16 s, 32 s, 60 s, 60 s, … (capé par RETRY_CAP_MS). */
  private scheduleNextRetry(posteId: number): void {
    const delay = Math.min(
      ScanOrchestratorService.RETRY_BASE_MS * Math.pow(2, this.cdcRetryCount),
      ScanOrchestratorService.RETRY_CAP_MS,
    );
    this.cdcRetryTimer = setTimeout(async () => {
      this.cdcRetryTimer = null;
      // Guard NG0205 : le composant a été détruit entre le schedule et l'exécution
      if (this.destroyed) return;
      // Ne pas interférer si un événement USB a déjà déclenché une reconnexion
      if (this.status() === 'SERIAL' || this.status() === 'DETECTING') return;

      this.cdcRetryCount++;
      if (this.cdcRetryCount > ScanOrchestratorService.RETRY_MAX) {
        // Limite atteinte — rester en mode HID, bouton reconnexion disponible
        this.status.set('HID');
        return;
      }
      try {
        const started = await this.trySetupCdc(posteId);
        if (started) {
          // Ne pas notifier ici : le port n'est pas encore confirmé ouvert côté Rust.
          // La confirmation arrivera via le premier scan reçu ou l'absence d'erreur.
          return;
        }
      } catch {
        /* silencieux — nouvelle tentative au prochain cycle */
      }
      if (this.status() !== 'SERIAL') {
        this.scheduleNextRetry(posteId);
      }
    }, delay);
  }

  private stopCdcRetry(): void {
    if (this.cdcRetryTimer) {
      clearTimeout(this.cdcRetryTimer);
      this.cdcRetryTimer = null;
    }
    // Ne PAS toucher à cdcRetryCount ici — le reset est explicite dans startCdcRetry.
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Watchdog (supprimé — clearStaleSerialWatchdog conservé pour teardown)
  // ─────────────────────────────────────────────────────────────────────────


  private clearStaleSerialWatchdog(): void {
    if (this.staleSerialTimer) {
      clearTimeout(this.staleSerialTimer);
      this.staleSerialTimer = null;
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Sonde FE indépendante (détection de port disparu côté frontend)
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

  /**
   * Attache l'écouteur de l'événement Tauri `scan-usb-arrived` (émis par le
   * moniteur Rust à chaque WM_DEVICECHANGE / DBT_DEVICEARRIVAL sur un port
   * COM). À la réception, déclenche immédiatement une reconnexion CDC si
   * besoin — sans attendre le prochain tick du timer de back-off.
   */
  private async setupUsbDeviceListener(): Promise<void> {
    if (!this.tauriDevice.isTauriAvailable() || this.unlistenUsbArrived) return;
    try {
      const { listen } = await import('@tauri-apps/api/event');
      this.unlistenUsbArrived = await listen('scan-usb-arrived', () => this.onUsbArrived());
    } catch (e) {
      console.warn('[ScanOrchestrator] Échec attachement listener WM_DEVICECHANGE :', e);
    }
  }

  private detachUsbDeviceListener(): void {
    this.unlistenUsbArrived?.();
    this.unlistenUsbArrived = null;
  }

  /**
   * Réagit à l'arrivée d'un port COM côté Windows. Annule le timer de back-off
   * en cours et relance immédiatement la séquence de setup CDC. Premier essai
   * d'ouverture quasi-garanti de réussir car `usbser.sys` vient juste de
   * publier l'interface device.
   */
  private onUsbArrived(): void {
    if (this.destroyed || !this.currentPosteId || this.status() === 'SERIAL') return;

    const now = Date.now();
    if (now - this.lastUsbArrivedAt < ScanOrchestratorService.USB_ARRIVED_DEBOUNCE_MS) return;
    this.lastUsbArrivedAt = now;

    console.info('[ScanOrchestrator] WM_DEVICECHANGE/DBT_DEVICEARRIVAL — reconnexion immédiate');
    this.stopCdcRetry();
    this.consecutiveOpenFailedCount = 0; // device fraîchement énuméré → repartir à zéro
    this.status.set('DETECTING'); // Verrou : empêche le timer de back-off de continuer

    setTimeout(() => {
      if (this.destroyed || !this.currentPosteId || this.status() === 'SERIAL') return;
      this.setup(this.currentPosteId);
    }, ScanOrchestratorService.USB_ARRIVED_REOPEN_DELAY_MS);
  }

  /**
   * Vérifie périodiquement la présence du port. Si le port a disparu sans que le
   * thread Rust ne l'ait détecté, force la bascule HID + retry CDC. Mécanisme
   * découplé du thread Rust et du modèle de douchette.
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

    const lostPort = this.currentPortName;
    console.warn(`[ScanOrchestrator] Sonde FE : ${lostPort} absent — bascule HID + retry`);
    this.notification.warning(`Scanner ${lostPort} déconnecté — bascule mode clavier`, 'Scanner');

    // Miroir de la branche DISCONNECTED du handler scan-error : on coupe les
    // listeners FE, on stoppe le thread Rust (handle obsolète), on repasse HID.
    this.scanReady = false;
    this.stopAliveProbe();
    this.unlistenScan?.();
    this.unlistenError?.();
    this.unlistenScan = null;
    this.unlistenError = null;
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      await invoke('stop_scanner_listener', { portName: lostPort });
    } catch {
      /* idempotent */
    }
    this.consecutiveOpenFailedCount = 0;
    this.setupHid('RETRYING');
    if (this.currentPosteId !== null) {
      this.startCdcRetry(this.currentPosteId, { resetCount: true, cooldownMs: 5_000 });
    }
  }

  /** Construit un message lisible à partir du payload typé scan-error. */
  private formatScanErrorMessage(payload: ScanErrorPayload): string {
    const { code, portName, details } = payload;
    switch (code) {
      case 'PORT_DROPPED':
        return `Scanner ${portName} : port disparu — bascule mode clavier`;
      case 'OPEN_FAILED':
        return `Scanner ${portName} : ouverture impossible (${details}) — bascule mode clavier`;
      case 'DISCONNECTED':
        return `Scanner ${portName} : déconnexion (${details}) — bascule mode clavier`;
      default:
        return `Scanner ${portName} déconnecté — bascule mode clavier`;
    }
  }
}
