import { inject, Injectable, NgZone } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { TauriPrinterService } from '../../shared/services/tauri-printer.service';
import { AppSettingsService } from '../config/app-settings.service';

export interface BackendStatus {
  status: string;
  progress: number;
  message: string;
}

export interface BackendHealthStatus {
  available: boolean;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class BackendStatusService {
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly appSettingsService = inject(AppSettingsService);
  private backendStatus$ = new BehaviorSubject<BackendStatus>({
    status: 'initializing',
    progress: 0,
    message: 'Vérification du backend...',
  });

  private checkInterval?: ReturnType<typeof setInterval>;
  private lastJavaError = '';
  private readonly ngZone = inject(NgZone);

  constructor() {
    void this.initializeTauriListener();
  }

  getBackendStatus(): Observable<BackendStatus> {
    return this.backendStatus$.asObservable();
  }

  private async initializeTauriListener(): Promise<void> {
    try {
      if (!this.tauriPrinterService.isRunningInTauri()) {
        // Web mode — backend assumed ready
        this.backendStatus$.next({ status: 'ready', progress: 100, message: 'Backend disponible' });
        return;
      }

      const { invoke } = await import('@tauri-apps/api/core');
      const { listen } = await import('@tauri-apps/api/event');

      // Sync the backend URL from Tauri config into AppSettingsService when the
      // stored URL matches the compiled-in default (i.e. user hasn't set a custom URL).
      try {
        const tauriUrl = await invoke<string>('get_backend_url_command');
        const stored = this.appSettingsService.getApiServerUrl();
        if (!stored || stored === 'http://localhost:9080') {
          this.appSettingsService.updateApiServerUrl(tauriUrl);
        }
      } catch {
        console.warn('[BackendStatusService] Impossible de récupérer l\'URL du backend, utilisation de la valeur par défaut');
      }

      // ── Bundled backend mode ──────────────────────────────────────────────
      let bundledMode = false;
      try {
        // Probe bundled mode first. If this command is missing (standard mode),
        // it throws and we fall through to the external-backend polling below.
        const initialStatus = await invoke<BackendStatus>('get_backend_status');
        bundledMode = true;
        this.backendStatus$.next(initialStatus);

        // Stream real-time status updates from the Rust backend manager.
        // IMPORTANT: register the listener BEFORE the catch-up fetch below.
        // When a backend is already running (e.g. relaunch with an orphaned
        // process or a Windows service), the Rust side reaches `ready` within
        // microseconds during setup() — often before this listener exists.
        // Registering first guarantees every later event is captured.
        await listen<BackendStatus>('backend-status', event => {
          this.ngZone.run(() => {
            const s = event.payload;
            // Do not resurface the splash for operational stop/restart states
            if (s.status === 'stopped' || s.status === 'stopping') {
              return;
            }
            // If error, enrich message with the last Java exception if available
            if (s.status === 'error' && this.lastJavaError) {
              this.backendStatus$.next({ ...s, message: `${s.message}\n${this.lastJavaError}` });
              return;
            }
            this.backendStatus$.next(s);
          });
        });

        // Catch-up fetch: re-read the current status now that the listener is
        // active. This closes the race window — any terminal state (e.g. `ready`)
        // reached between the first snapshot and the listener registration is
        // recovered here, so the splash never stays stuck on an early message.
        try {
          const catchUp = await invoke<BackendStatus>('get_backend_status');
          this.ngZone.run(() => this.backendStatus$.next(catchUp));
        } catch {
          /* non-fatal — the listener will deliver subsequent updates */
        }

        // Listen for critical Java log lines (ERROR / FATAL / Exception)
        await listen<string>('backend-log', event => {
          this.ngZone.run(() => {
            const text = event.payload;
            this.lastJavaError = text.slice(0, 200); // keep first 200 chars
            const current = this.backendStatus$.value;
            // If the backend is already in error state, update the message with the Java log
            if (current.status === 'error') {
              this.backendStatus$.next({ ...current, message: `Erreur backend :\n${this.lastJavaError}` });
            }
          });
        });
      } catch {
        console.log('[BackendStatusService] Mode standard — surveillance du backend externe');
      }

      // ── Standard (non-bundled) mode ──────────────────────────────────────
      if (!bundledMode) {
        const backendUrl = this.appSettingsService.getApiServerUrl();
        this.backendStatus$.next({
          status: 'waiting',
          progress: 10,
          message: `Attente du serveur backend à ${backendUrl}...`,
        });

        // Single polling mechanism — avoids the double-poll with listen + setInterval
        this.startHealthChecking(invoke);
      }
    } catch (error) {
      console.warn('[BackendStatusService] API Tauri non disponible :', error);
      this.backendStatus$.next({ status: 'ready', progress: 100, message: 'Backend disponible' });
    }
  }

  private startHealthChecking(invoke: <T>(cmd: string, args?: Record<string, unknown>) => Promise<T>): void {
    let attempts = 0;
    const maxAttempts = 60; // 30 secondes

    this.checkInterval = setInterval(async () => {
      attempts++;

      try {
        const health = await invoke<BackendHealthStatus>('check_backend_health', {
          backendUrl: this.appSettingsService.getApiServerUrl(),
        });

        this.ngZone.run(() => {
          if (health.available) {
            this.backendStatus$.next({ status: 'ready', progress: 100, message: 'Le backend est prêt' });
            this.stopHealthChecking();
            return;
          }

          if (attempts >= maxAttempts) {
            this.backendStatus$.next({
              status: 'error',
              progress: 0,
              message: `Serveur backend indisponible à ${this.appSettingsService.getApiServerUrl()}. Vérifiez que le serveur est démarré.`,
            });
            this.stopHealthChecking();
            return;
          }

          const progress = Math.round(Math.min(10 + (attempts / maxAttempts) * 80, 90));
          this.backendStatus$.next({
            status: 'waiting',
            progress,
            message: `Connexion au serveur backend... (${attempts}/${maxAttempts})`,
          });
        });
      } catch {
        this.ngZone.run(() => {
          if (attempts >= maxAttempts) {
            this.backendStatus$.next({
              status: 'error',
              progress: 0,
              message: `Impossible de se connecter au serveur backend à ${this.appSettingsService.getApiServerUrl()}.`,
            });
            this.stopHealthChecking();
          }
        });
      }
    }, 500);
  }

  private stopHealthChecking(): void {
    if (this.checkInterval) {
      clearInterval(this.checkInterval);
      this.checkInterval = undefined;
    }
  }
}
