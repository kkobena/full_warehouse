import {Injectable, signal} from '@angular/core';

/** Panneau ordonnance de la vente : ouvert depuis la carte client, lu par l'écran de vente actif. */
@Injectable({providedIn: 'root'})
export class OrdonnancePanelService {
  readonly visible = signal(false);
  readonly customerId = signal<number | null>(null);
  /** Incrémenté à chaque lecture ou modification des ordonnances : les pastilles de comptage se rafraîchissent. */
  readonly changements = signal(0);

  notifierChangement(): void {
    this.changements.update(n => n + 1);
  }

  ouvrir(customerId: number | null | undefined): boolean {
    if (!customerId) {
      return false;
    }
    this.customerId.set(customerId);
    this.visible.set(true);
    return true;
  }
}
