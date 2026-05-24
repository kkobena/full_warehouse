import { Injectable } from '@angular/core';

/**
 * Service de feedback audio pour les scans de code-barres.
 * Génère des bips via AudioContext (pas de fichier audio nécessaire).
 */
@Injectable({ providedIn: 'root' })
export class ScanAudioFeedbackService {
  private audioContext: AudioContext | null = null;

  beepSuccess(): void {
    this.playTone(800, 0.1); // 800Hz, 100ms - bip court aigu
  }

  beepError(): void {
    this.playTone(300, 0.3); // 300Hz, 300ms - bip grave plus long
  }

  beepWarning(): void {
    this.playTone(550, 0.18); // 550Hz, 180ms - bip intermédiaire (ambiguïté multi-produit)
  }

  private playTone(frequency: number, duration: number): void {
    if (!this.audioContext) {
      this.audioContext = new AudioContext();
    }
    const oscillator = this.audioContext.createOscillator();
    const gainNode = this.audioContext.createGain();
    oscillator.connect(gainNode);
    gainNode.connect(this.audioContext.destination);
    oscillator.frequency.value = frequency;
    gainNode.gain.value = 0.1;
    oscillator.start();
    oscillator.stop(this.audioContext.currentTime + duration);
  }
}
