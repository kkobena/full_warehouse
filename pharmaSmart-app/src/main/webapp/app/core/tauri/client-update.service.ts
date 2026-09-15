import { inject, Injectable, signal } from '@angular/core';
import { TauriPrinterService } from '../../shared/services/tauri-printer.service';

/** Réponse de la commande Tauri `check_for_client_update`. */
export interface ClientUpdateStatus {
  available: boolean;
  currentVersion: string;
  newVersion: string | null;
  message: string;
}

/** Étapes traversées par l'écran de mise à jour. */
export type ClientUpdatePhase = 'idle' | 'checking' | 'available' | 'applying' | 'applied' | 'error';

/**
 * Mise à jour du poste client depuis le poste serveur de l'officine.
 *
 * Les postes clients ne sont pas installés : on y dépose `pharmasmart.exe`. Mettre à
 * jour revient donc à remplacer ce fichier, ce que fait le côté Rust
 * (`src-tauri/src/self_update.rs`) : téléchargement depuis le relais du serveur,
 * mise de côté de l'ancien binaire en `.exe.old`, écriture du nouveau, relance.
 *
 * Ce service n'est qu'une façade : aucune logique de mise à jour ici, uniquement
 * l'état affichable et l'appel des trois commandes.
 *
 * Hors Tauri (navigateur), tout est inerte : `isSupported()` répond `false` et l'écran
 * affiche une explication plutôt que des boutons sans effet.
 */
@Injectable({ providedIn: 'root' })
export class ClientUpdateService {
  private readonly tauriPrinterService = inject(TauriPrinterService);

  readonly phase = signal<ClientUpdatePhase>('idle');
  readonly status = signal<ClientUpdateStatus | null>(null);
  readonly errorMessage = signal<string>('');

  /** La mise à jour n'a de sens que dans l'application de bureau. */
  isSupported(): boolean {
    return this.tauriPrinterService.isRunningInTauri();
  }

  /**
   * Interroge le poste serveur. Ne télécharge rien, ne modifie rien : c'est une
   * consultation, sans danger à répéter.
   */
  async check(): Promise<void> {
    if (!this.isSupported()) {
      return;
    }
    this.phase.set('checking');
    this.errorMessage.set('');
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      const status = await invoke<ClientUpdateStatus>('check_for_client_update');
      this.status.set(status);
      this.phase.set(status.available ? 'available' : 'idle');
    } catch (error: unknown) {
      this.fail(error);
    }
  }

  /**
   * Télécharge la nouvelle version et remplace l'exécutable. L'application doit
   * ensuite être relancée pour que la bascule prenne effet — c'est le rôle de
   * [`restart`]. Tant qu'on n'a pas relancé, la version en cours d'exécution reste
   * l'ancienne.
   */
  async apply(): Promise<void> {
    if (!this.isSupported()) {
      return;
    }
    this.phase.set('applying');
    this.errorMessage.set('');
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      await invoke<string>('apply_client_update');
      this.phase.set('applied');
    } catch (error: unknown) {
      this.fail(error);
    }
  }

  /** Relance l'application pour démarrer sur la version fraîchement installée. */
  async restart(): Promise<void> {
    if (!this.isSupported()) {
      return;
    }
    const { invoke } = await import('@tauri-apps/api/core');
    await invoke('restart_app');
  }

  /**
   * Le côté Rust renvoie des messages déjà rédigés pour un opérateur d'officine
   * (serveur injoignable, téléchargement interrompu, bascule impossible…). On les
   * affiche tels quels plutôt que de les reformuler : ce sont eux qui portent
   * l'information utile au support.
   */
  private fail(error: unknown): void {
    this.errorMessage.set(typeof error === 'string' ? error : String(error));
    this.phase.set('error');
  }
}
