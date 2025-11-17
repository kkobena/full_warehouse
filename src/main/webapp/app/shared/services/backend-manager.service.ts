import { inject, Injectable } from '@angular/core';
import { invoke } from '@tauri-apps/api/core';
import { from, Observable } from 'rxjs';
import { TauriPrinterService } from './tauri-printer.service';

/**
 * Service to manage the Spring Boot backend in Tauri standalone mode
 * Only works when running in Tauri with bundled backend
 */
@Injectable({
  providedIn: 'root',
})
export class BackendManagerService {
  private readonly tauriPrinterService = inject(TauriPrinterService);
  restartBackend(): Observable<string> {
    return from(
      invoke<string>('restart_backend_main').catch(error => {
        console.error('Failed to restart backend:', error);
        throw error;
      }),
    );
  }

  /**
   * Stop the Spring Boot backend
   * @returns Observable that completes when backend is stopped
   */
  stopBackend(): Observable<string> {
    return from(
      invoke<string>('stop_backend_main').catch(error => {
        console.error('Failed to stop backend:', error);
        throw error;
      }),
    );
  }

  /**
   * Check if running in Tauri environment
   * @returns true if running in Tauri, false otherwise
   */
  isTauriEnvironment(): boolean {
    // Check if __TAURI__ is defined in window object
    return typeof (window as any).__TAURI__ !== 'undefined';
  }

  /**
   * Check if bundled backend is available
   * This checks if the restart_backend_main command is available
   * @returns Promise that resolves to true if bundled backend is available
   */
  async isBundledBackendAvailable(): Promise<boolean> {
    if (!this.tauriPrinterService.isRunningInTauri()) {
      return false;
    }

    try {
      // Try to call the command - if it exists, it will work
      // If it doesn't exist (non-bundled mode), it will throw an error
      await invoke('get_backend_status');
      return true;
    } catch (error) {
      return false;
    }
  }
}
