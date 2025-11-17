import { inject, Injectable, NgZone } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

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
  private backendStatus$ = new BehaviorSubject<BackendStatus>({
    status: 'initializing',
    progress: 0,
    message: 'Checking backend status...',
  });

  private backendUrl: string = 'http://localhost:9080';
  private checkInterval?: any;
  private readonly ngZone = inject(NgZone);

  constructor() {
    void this.initializeTauriListener();
  }

  getBackendStatus(): Observable<BackendStatus> {
    return this.backendStatus$.asObservable();
  }

  private async initializeTauriListener(): Promise<void> {
    try {
      // Check if running in Tauri
      if (typeof window !== 'undefined' && '__TAURI__' in window) {
        const { invoke } = await import('@tauri-apps/api/core');
        const { listen } = await import('@tauri-apps/api/event');

        // Get the configured backend URL from Tauri
        try {
          this.backendUrl = await invoke<string>('get_backend_url_command');
          console.log('[BackendStatusService] Backend URL:', this.backendUrl);
        } catch (error) {
          console.warn('[BackendStatusService] Failed to get backend URL, using default:', error);
        }

        // Try to get bundled backend status first
        try {
          const initialStatus = await invoke<BackendStatus>('get_backend_status');
          console.log('[BackendStatusService] Bundled backend - Initial status:', initialStatus);
          this.backendStatus$.next(initialStatus);

          // Listen for bundled backend status updates
          await listen<BackendStatus>('backend-status', event => {
            console.log('[BackendStatusService] Bundled backend - Status event:', event.payload);
            this.ngZone.run(() => {
              this.backendStatus$.next(event.payload);
            });
          });
        } catch (error) {
          console.log('[BackendStatusService] Standard Tauri mode - monitoring external backend');

          // Standard mode: Monitor external backend
          this.backendStatus$.next({
            status: 'waiting',
            progress: 10,
            message: `Waiting for backend server at ${this.backendUrl}...`,
          });

          // Listen for backend health status from Tauri's monitoring
          await listen<BackendHealthStatus>('backend-health-status', event => {
            console.log('[BackendStatusService] Backend health status:', event.payload);
            this.ngZone.run(() => {
              if (event.payload.available) {
                this.backendStatus$.next({
                  status: 'ready',
                  progress: 100,
                  message: 'Backend is ready',
                });
              } else {
                this.backendStatus$.next({
                  status: 'error',
                  progress: 0,
                  message: event.payload.message,
                });
              }
            });
          });

          // Also poll manually using the check_backend_health command
          this.startHealthChecking(invoke);
        }
      } else {
        // Not running in Tauri, assume backend is ready (web mode)
        console.log('[BackendStatusService] Web mode - backend assumed ready');
        this.backendStatus$.next({
          status: 'ready',
          progress: 100,
          message: 'Backend is ready',
        });
      }
    } catch (error) {
      console.warn('Tauri APIs not available:', error);
      // Not running in Tauri, mark as ready
      this.backendStatus$.next({
        status: 'ready',
        progress: 100,
        message: 'Backend is ready',
      });
    }
  }

  private startHealthChecking(invoke: any): void {
    let attempts = 0;
    const maxAttempts = 60; // 30 seconds

    this.checkInterval = setInterval(async () => {
      attempts++;

      try {
        // @ts-ignore
        const health = await invoke<BackendHealthStatus>('check_backend_health', {
          backendUrl: this.backendUrl,
        });

        console.log(`[BackendStatusService] Health check (${attempts}/${maxAttempts}):`, health);

        this.ngZone.run(() => {
          if (health.available) {
            this.backendStatus$.next({
              status: 'ready',
              progress: 100,
              message: 'Backend is ready',
            });
            this.stopHealthChecking();
          } else {
            const progress = Math.min(10 + (attempts / maxAttempts) * 80, 90);
            this.backendStatus$.next({
              status: 'waiting',
              progress: Math.round(progress),
              message: `Waiting for backend at ${this.backendUrl}... (${attempts}/${maxAttempts})`,
            });

            if (attempts >= maxAttempts) {
              this.backendStatus$.next({
                status: 'error',
                progress: 0,
                message: `Backend not available at ${this.backendUrl}. Please start the backend server or set BACKEND_URL environment variable.`,
              });
              this.stopHealthChecking();
            }
          }
        });
      } catch (error) {
        console.error('[BackendStatusService] Health check failed:', error);
        this.ngZone.run(() => {
          if (attempts >= maxAttempts) {
            this.backendStatus$.next({
              status: 'error',
              progress: 0,
              message: `Failed to connect to backend server at ${this.backendUrl}.`,
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
