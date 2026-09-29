import {Injectable, signal} from '@angular/core';
import {Subject} from 'rxjs';
import {ProduitSearch} from 'app/shared/model';

/** Panneau fiche client de la vente : ouvert depuis la carte client ou Alt+V, lu par l'écran de vente actif. */
@Injectable({providedIn: 'root'})
export class FicheClientPanelService {
  readonly visible = signal(false);
  readonly customerId = signal<number | null>(null);
  /** Produit à re-délivrer, choisi dans l'historique du panneau. */
  readonly redelivrer$ = new Subject<ProduitSearch>();

  ouvrir(customerId: number | null | undefined): boolean {
    if (!customerId) {
      return false;
    }
    this.customerId.set(customerId);
    this.visible.set(true);
    return true;
  }
}
