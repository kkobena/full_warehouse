import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface PrefixSuffixResult {
  handled: boolean;
  isScanInProgress: boolean;
}

/**
 * Service de détection de scan par préfixe/suffixe.
 * Détecte les codes-barres encadrés par des caractères STX (Ctrl+B) et ETX (Ctrl+C)
 * configurés sur le scanner physique.
 */
@Injectable({ providedIn: 'root' })
export class PrefixSuffixScannerService {
  private scanning = false;
  private buffer = '';
  private readonly scanCompleted$ = new Subject<string>();
  readonly onScan$ = this.scanCompleted$.asObservable();

  processKeyEvent(event: KeyboardEvent, prefixKey: string, suffixKey: string): PrefixSuffixResult {
    // Détecter préfixe (Ctrl+B par défaut → key='b' avec ctrlKey)
    if (this.isPrefixKey(event, prefixKey)) {
      this.scanning = true;
      this.buffer = '';
      return { handled: true, isScanInProgress: true };
    }

    // Détecter suffixe (Ctrl+C par défaut → key='c' avec ctrlKey)
    if (this.scanning && this.isSuffixKey(event, suffixKey)) {
      const code = this.buffer;
      this.scanning = false;
      this.buffer = '';
      if (code.length >= 6) {
        this.scanCompleted$.next(code);
      }
      return { handled: true, isScanInProgress: false };
    }

    // Accumuler pendant le scan
    if (this.scanning) {
      if (event.key.length === 1) {
        this.buffer += event.key;
      }
      return { handled: true, isScanInProgress: true };
    }

    return { handled: false, isScanInProgress: false };
  }

  isScanActive(): boolean {
    return this.scanning;
  }

  forceReset(): void {
    this.scanning = false;
    this.buffer = '';
  }

  private isPrefixKey(event: KeyboardEvent, prefixKey: string): boolean {
    // STX (\x02) est envoyé par Ctrl+B
    if (prefixKey === '\x02') {
      return event.ctrlKey && event.key.toLowerCase() === 'b';
    }
    return event.key === prefixKey;
  }

  private isSuffixKey(event: KeyboardEvent, suffixKey: string): boolean {
    // ETX (\x03) est envoyé par Ctrl+C
    if (suffixKey === '\x03') {
      return event.ctrlKey && event.key.toLowerCase() === 'c';
    }
    return event.key === suffixKey;
  }
}
