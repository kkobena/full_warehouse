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

  constructor() {}

  keyPressed(key: string): string | null {
    const now = Date.now();

    if (this.timestamps.length > 0) {
      const timeDiff = now - this.timestamps[this.timestamps.length - 1];

      if (timeDiff > this.SCAN_DELAY_MS * 5) {
        // Si temps trop long entre deux frappes, reset (trop lent)
        this.reset();
      }
    }

    this.timestamps.push(now);

    if (key === 'Enter') {
      const totalDuration = this.timestamps[this.timestamps.length - 1] - this.timestamps[0];

      if (this.buffer.length >= this.SCAN_MIN_LENGTH && totalDuration <= this.GLOBAL_SCAN_MAX_TIME) {
        const code = this.buffer;
        this.reset();
        return code; // C'est un scan valide
      } else {
        this.reset(); // Frappe manuelle ou scan invalide
        return null;
      }
    } else {
      this.buffer += key;
      return null;
    }
  }

  private reset() {
    this.buffer = '';
    this.timestamps = [];
  }
}
