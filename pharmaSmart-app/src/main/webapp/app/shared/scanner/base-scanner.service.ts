import { inject } from '@angular/core';
import { Subject } from 'rxjs';
import { SCANNER_CONFIG, ScannerConfig, shouldIgnoreKey } from './scanner.config';

/**
 * Résultat du traitement d'une touche par le scanner.
 */
export interface ScanProcessResult {
  /** Indique si un scan est actuellement en cours de détection */
  isScanInProgress: boolean;
  /** Le code complété si un scan vient de se terminer, null sinon */
  completedCode: string | null;
}

/**
 * Événement émis par le scanner.
 */
export interface ScanEvent {
  type: 'start' | 'complete' | 'reset';
  code?: string;
}

/**
 * Service de base pour la détection de codes-barres.
 * Contient la logique commune de détection basée sur la vitesse de frappe.
 *
 * Cette classe abstraite est destinée à être étendue par :
 * - GlobalScannerService : détection au niveau de la page
 * - ScanDetectorService : détection au niveau d'un composant
 */
export abstract class BaseScannerService {
  protected readonly config: ScannerConfig = inject(SCANNER_CONFIG);

  protected buffer = '';
  protected timestamps: number[] = [];
  protected scanInProgress = false;
  protected resetTimeout: ReturnType<typeof setTimeout> | null = null;

  // Subject pour émettre les événements de scan
  protected readonly scanEvent$ = new Subject<ScanEvent>();

  /** Observable public pour les événements de scan */
  readonly onScanEvent$ = this.scanEvent$.asObservable();

  /**
   * Traite une touche pressée et détermine si c'est un scan.
   * @param key - La touche pressée (event.key)
   * @returns Résultat indiquant l'état du scan
   */
  processKey(key: string): ScanProcessResult {
    // Ignorer les touches non imprimables
    if (shouldIgnoreKey(key)) {
      return { isScanInProgress: this.scanInProgress, completedCode: null };
    }

    const now = Date.now();

    // Enter = fin potentielle de scan
    if (key === 'Enter') {
      return this.handleEnter(now);
    }

    // Vérifier si on doit reset (pause trop longue)
    if (this.timestamps.length > 0) {
      const timeSinceLastKey = now - this.timestamps[this.timestamps.length - 1];
      if (timeSinceLastKey > this.config.resetDelay) {
        const wasScanning = this.scanInProgress;
        this.reset();
        // Même garde-fou qu'ailleurs dans ce fichier : si une frappe rapide avait déclenché
        // `scanInProgress`, la touche suivante arrivant après une pause normale (saisie
        // manuelle) abandonne ce scan silencieusement — sans cet événement, le composant
        // abonné reste bloqué en mode « scan en cours » indéfiniment, y compris quand
        // l'utilisateur tape lentement, une touche à la fois.
        if (wasScanning) {
          this.scanEvent$.next({ type: 'reset' });
        }
      }
    }

    // Ajouter au buffer
    this.buffer += key;
    this.timestamps.push(now);

    // Limiter le tableau des timestamps pour éviter la croissance mémoire
    if (this.timestamps.length > 50) {
      this.timestamps = [this.timestamps[0], ...this.timestamps.slice(-10)];
    }

    // Détecter si c'est un scan (frappes rapides consécutives)
    if (!this.scanInProgress && this.timestamps.length >= 2) {
      const timeDiff = now - this.timestamps[this.timestamps.length - 2];
      if (timeDiff <= this.config.scanDelayMs) {
        this.scanInProgress = true;
        this.onScanStartDetected();
      }
    }

    // Programmer un reset automatique
    this.scheduleReset();

    return {
      isScanInProgress: this.scanInProgress,
      completedCode: null,
    };
  }

  /**
   * Force un reset du buffer et de l'état.
   */
  forceReset(): void {
    this.reset();
  }

  /**
   * Vérifie si un scan est actuellement en cours.
   */
  isScanActive(): boolean {
    return this.scanInProgress;
  }

  /**
   * Appelé quand un début de scan est détecté.
   * Peut être surchargé par les classes filles.
   */
  protected onScanStartDetected(): void {
    this.scanEvent$.next({ type: 'start' });
  }

  /**
   * Appelé quand un scan est complété avec succès.
   * Peut être surchargé par les classes filles.
   */
  protected onScanCompleted(code: string): void {
    this.scanEvent$.next({ type: 'complete', code });
  }

  /**
   * Gère la touche Enter (fin potentielle de scan).
   */
  protected handleEnter(now: number): ScanProcessResult {
    const wasScanning = this.scanInProgress;

    if (this.scanInProgress && this.buffer.length >= this.config.scanMinLength) {
      const totalDuration = now - this.timestamps[0];
      if (totalDuration <= this.config.scanMaxTime) {
        const code = this.buffer;
        this.reset();
        this.onScanCompleted(code);
        return { isScanInProgress: false, completedCode: code };
      }
    }

    this.reset();
    // Même garde-fou que dans `handleAutoReset` : un scan était en cours (frappe rapide
    // détectée) mais la touche Entrée arrive hors bornes de longueur/durée — sans cet
    // événement, les abonnés qui avaient positionné `isScanning` sur le `start` restent
    // bloqués indéfiniment.
    if (wasScanning) {
      this.scanEvent$.next({ type: 'reset' });
    }
    return { isScanInProgress: false, completedCode: null };
  }

  /**
   * Programme un reset automatique après un délai.
   */
  protected scheduleReset(): void {
    if (this.resetTimeout) {
      clearTimeout(this.resetTimeout);
    }

    this.resetTimeout = setTimeout(() => {
      this.handleAutoReset();
    }, this.config.endScanTimeout);
  }

  /**
   * Gère le reset automatique après timeout.
   * Vérifie si un scan valide est en attente de traitement.
   */
  protected handleAutoReset(): void {
    const wasScanning = this.scanInProgress;

    if (this.timestamps.length === 0 || this.buffer.length === 0) {
      this.reset();
      if (wasScanning) {
        this.scanEvent$.next({ type: 'reset' });
      }
      return;
    }

    const totalDuration = this.timestamps[this.timestamps.length - 1] - this.timestamps[0];
    const isValidScan = this.buffer.length >= this.config.scanMinLength && totalDuration <= this.config.scanMaxTime;

    if (isValidScan && this.scanInProgress) {
      const code = this.buffer;
      this.reset();
      this.onScanCompleted(code);
    } else {
      this.reset();
      // Un scan avait été détecté (frappe rapide) mais n'a jamais abouti à un code valide
      // (longueur/durée hors bornes, ou jamais de touche Entrée) : sans cet événement,
      // les composants abonnés qui avaient positionné leur propre drapeau `isScanning`
      // sur `onScanStartDetected()` ne sont jamais notifiés de l'abandon et restent
      // bloqués en mode « scan en cours » indéfiniment — ce qui, combiné à la boucle de
      // nettoyage de la saisie de certains composants, rend le champ inutilisable au
      // clavier tant que la page n'est pas rechargée.
      if (wasScanning) {
        this.scanEvent$.next({ type: 'reset' });
      }
    }
  }

  /**
   * Remet à zéro l'état du scanner.
   */
  protected reset(): void {
    if (this.resetTimeout) {
      clearTimeout(this.resetTimeout);
      this.resetTimeout = null;
    }
    this.buffer = '';
    this.timestamps = [];
    this.scanInProgress = false;
  }
}
