import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface ScanEvent {
  type: 'start' | 'complete' | 'reset';
  code?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ScanDetectorService {
  private buffer = '';
  private timestamps: number[] = [];
  private scanTimeout: ReturnType<typeof setTimeout> | null = null;
  private readonly SCAN_DELAY_MS = 30;
  private readonly SCAN_MIN_LENGTH = 6;
  private readonly GLOBAL_SCAN_MAX_TIME = 500;
  private readonly END_SCAN_TIMEOUT = 100;
  private scanInProgress = false;

  // RxJS Subject pour émettre les événements de scan
  private readonly scanEvent$ = new Subject<ScanEvent>();

  // Observable public pour les abonnés
  readonly onScanEvent$ = this.scanEvent$.asObservable();

  keyPressed(key: string): string | null {
    // Ignorer les touches modificatrices et de contrôle
    const ignoredKeys = [
      'Shift', 'Control', 'Alt', 'Meta', 'CapsLock', 'Tab', 'Escape',
      'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight',
      'Backspace', 'Delete', 'Insert', 'Home', 'End', 'PageUp', 'PageDown',
      'F1', 'F2', 'F3', 'F4', 'F5', 'F6', 'F7', 'F8', 'F9', 'F10', 'F11', 'F12',
      'PrintScreen', 'ScrollLock', 'Pause', 'ContextMenu', 'NumLock', 'Clear',
    ];

    if (ignoredKeys.includes(key)) {
      return null;
    }

    // Ignorer les touches non imprimables (longueur > 1 sauf Enter qui est géré séparément)
    if (key.length > 1 && key !== 'Enter') {
      return null;
    }

    const now = Date.now();

    if (key === 'Enter') {
      if (this.timestamps.length > 0) {
        const totalDuration = now - this.timestamps[0];

        if (this.buffer.length >= this.SCAN_MIN_LENGTH && totalDuration <= this.GLOBAL_SCAN_MAX_TIME) {
          const code = this.buffer;
          this.reset();
          return code;
        }
      }

      this.reset();
      return null;
    }

    if (this.timestamps.length > 0) {
      const timeDiff = now - this.timestamps[this.timestamps.length - 1];

      if (timeDiff > this.SCAN_DELAY_MS * 5) {
        this.reset();
      }
    }

    this.buffer += key;
    this.timestamps.push(now);

    // Limiter le tableau des timestamps pour éviter la croissance mémoire
    if (this.timestamps.length > 50) {
      this.timestamps = [this.timestamps[0], ...this.timestamps.slice(-10)];
    }

    if (!this.scanInProgress && this.timestamps.length >= 2) {
      const timeDiff = this.timestamps[this.timestamps.length - 1] - this.timestamps[this.timestamps.length - 2];
      if (timeDiff <= this.SCAN_DELAY_MS) {
        this.scanInProgress = true;
        // Émettre via Subject
        this.scanEvent$.next({ type: 'start' });
      }
    }

    if (this.scanTimeout) {
      clearTimeout(this.scanTimeout);
    }

    this.scanTimeout = setTimeout(() => {
      if (this.timestamps.length === 0 || this.buffer.length === 0) {
        this.reset();
        return;
      }

      const totalDuration = this.timestamps[this.timestamps.length - 1] - this.timestamps[0];
      const isValidScan = this.buffer.length >= this.SCAN_MIN_LENGTH && totalDuration <= this.GLOBAL_SCAN_MAX_TIME;

      if (isValidScan) {
        const code = this.buffer;
        this.reset();

        // Émettre via Subject
        this.scanEvent$.next({ type: 'complete', code });
      } else {
        this.reset();
      }
    }, this.END_SCAN_TIMEOUT);

    return null;
  }

  private reset(): void {
    if (this.scanTimeout) {
      clearTimeout(this.scanTimeout);
      this.scanTimeout = null;
    }
    this.buffer = '';
    this.timestamps = [];
    this.scanInProgress = false;
  }
}
