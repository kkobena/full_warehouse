import { Injectable } from '@angular/core';

/**
 * Où va le focus après une mise à jour de ligne ?
 *
 * Par défaut, valider une cellule du panier (Entrée) ramène le focus sur la recherche produit :
 * c'est la boucle scan → quantité → Entrée → produit suivant, qu'il ne faut jamais casser.
 *
 * Le mode « correction du panier » (↑/↓, +/-, Ctrl+↑/↓ dans la grille) veut le contraire : rester dans
 * la grille pour enchaîner plusieurs corrections. La grille le DEMANDE avant d'émettre sa mise à jour ;
 * le cycle de vie de la vente le CONSOMME quand la mise à jour aboutit, et ne rend alors pas le focus à
 * la recherche. Un seul usage par demande : le comportement par défaut revient aussitôt après.
 */
@Injectable({ providedIn: 'root' })
export class PanierFocusService {
  private maintienDemande = false;
  private expiration: ReturnType<typeof setTimeout> | null = null;

  /**
   * La prochaine mise à jour réussie ne ramènera pas le focus sur la recherche.
   * La demande expire seule : si la mise à jour échoue (stock, autorisation), elle ne doit pas
   * priver de focus une validation ultérieure, sans rapport.
   */
  demanderMaintien(delaiMs = 4000): void {
    this.maintienDemande = true;
    if (this.expiration) {
      clearTimeout(this.expiration);
    }
    this.expiration = setTimeout(() => (this.maintienDemande = false), delaiMs);
  }

  /** Vrai une seule fois par demande : l'appelant ne ramène alors pas le focus à la recherche. */
  consommer(): boolean {
    const demande = this.maintienDemande;
    this.maintienDemande = false;
    if (this.expiration) {
      clearTimeout(this.expiration);
      this.expiration = null;
    }
    return demande;
  }
}
