import { Injectable, signal } from '@angular/core';

/**
 * État du panneau d'aide de l'espace de vente (raccourcis et navigation au clavier).
 *
 * Le panneau est affiché par l'écran (`sales-home`, `vente-depot`) ; la touche F1, gérée plus bas par le mixin de
 * raccourcis de chaque formulaire de vente, l'ouvre en passant par ce service.
 */
@Injectable({ providedIn: 'root' })
export class SalesHelpService {
  readonly ouvert = signal(false);

  ouvrir(): void {
    this.ouvert.set(true);
  }
}
