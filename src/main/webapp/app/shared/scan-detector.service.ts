import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ScanDetectorService {
  private buffer = '';
  private timestamps: number[] = [];
  private scanTimeout: any;
  private readonly SCAN_DELAY_MS = 30; // Temps entre frappes max pour scan
  private readonly SCAN_MIN_LENGTH = 6; // Minimum de caractères pour être considéré un scan
  private readonly GLOBAL_SCAN_MAX_TIME = 500; // Max 500ms pour tout un scan
  private readonly END_SCAN_TIMEOUT = 100; // Timeout après la dernière frappe pour valider le scan
  private scanCallback: ((code: string) => void) | null = null;
  private scanStartCallback: (() => void) | null = null;
  private scanInProgress = false;

  constructor() {}

  setScanCallback(callback: ((code: string) => void) | null): void {
    this.scanCallback = callback;
  }

  setScanStartCallback(callback: (() => void) | null): void {
    this.scanStartCallback = callback;
  }

  keyPressed(key: string): string | null {
    // Ignorer les touches modificatrices et les touches de contrôle
    const ignoredKeys = ['Shift', 'Control', 'Alt', 'Meta', 'CapsLock', 'Tab', 'Escape', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'];

    if (ignoredKeys.includes(key)) {
      return null;
    }

    const now = Date.now();

    // Gérer la touche Enter comme fin de scan potentielle
    if (key === 'Enter') {
      if (this.timestamps.length > 0) {
        const totalDuration = now - this.timestamps[0];

        if (this.buffer.length >= this.SCAN_MIN_LENGTH && totalDuration <= this.GLOBAL_SCAN_MAX_TIME) {
          const code = this.buffer;
          this.reset();
          return code; // C'est un scan valide
        }
      }

      this.reset();
      return null;
    }

    // Vérifier si c'est une nouvelle séquence ou continuation
    if (this.timestamps.length > 0) {
      const timeDiff = now - this.timestamps[this.timestamps.length - 1];

      if (timeDiff > this.SCAN_DELAY_MS * 5) {
        // Si temps trop long entre deux frappes, reset (trop lent)
        this.reset();
      }
    }

    // Ajouter la touche au buffer
    this.buffer += key;
    this.timestamps.push(now);

    // Si c'est le début d'un scan potentiel (2ème touche rapide), notifier
    if (!this.scanInProgress && this.timestamps.length >= 2) {
      const timeDiff = this.timestamps[this.timestamps.length - 1] - this.timestamps[this.timestamps.length - 2];
      if (timeDiff <= this.SCAN_DELAY_MS) {
        // Frappes rapides détectées, probablement un scan
        this.scanInProgress = true;
        if (this.scanStartCallback) {
          this.scanStartCallback();
        }
      }
    }

    // Annuler le timeout précédent
    if (this.scanTimeout) {
      clearTimeout(this.scanTimeout);
    }

    // Démarrer un nouveau timeout pour détecter la fin du scan
    this.scanTimeout = setTimeout(() => {
      // Vérifier qu'on a des données à traiter
      if (this.timestamps.length === 0 || this.buffer.length === 0) {
        this.reset();
        return;
      }

      const totalDuration = this.timestamps[this.timestamps.length - 1] - this.timestamps[0];
      const isValidScan = this.buffer.length >= this.SCAN_MIN_LENGTH && totalDuration <= this.GLOBAL_SCAN_MAX_TIME;

      if (isValidScan) {
        const code = this.buffer;
        // Réinitialiser avant le callback pour éviter les problèmes de réentrance
        this.reset();

        // Appeler le callback après reset pour garantir un état propre
        if (this.scanCallback) {
          this.scanCallback(code);
        }
      } else {
        this.reset();
      }
    }, this.END_SCAN_TIMEOUT);

    return null;
  }

  private reset() {
    if (this.scanTimeout) {
      clearTimeout(this.scanTimeout);
      this.scanTimeout = null;
    }
    this.buffer = '';
    this.timestamps = [];
    this.scanInProgress = false;
  }
}
