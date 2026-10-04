import { Directive, ElementRef, OnDestroy, inject } from '@angular/core';

/**
 * Rend une fenêtre modale déplaçable à la souris (et au doigt) : se pose sur l'en-tête `.modal-header`, qui sert de poignée.
 *
 * La fenêtre (`.modal-dialog`) se déplace par `transform`, ce qui ne touche ni à sa mise en page ni au centrage de
 * NgbModal. Elle ne sort jamais de l'écran : l'en-tête reste atteignable, pour pouvoir la reprendre. Le déplacement ne survit pas
 * à la fermeture. Un clic sur un bouton ou un champ de l'en-tête (« Fermer ») ne lance pas de déplacement.
 *
 * @example
 * <div class="modal-header" appModalDeplacable> … </div>
 */
@Directive({
  selector: '[appModalDeplacable]',
  host: {
    '(pointerdown)': 'debuter($event)',
    style: 'cursor: move; touch-action: none; user-select: none;',
  },
})
export class ModalDeplacableDirective implements OnDestroy {
  private readonly poignee = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  private fenetre: HTMLElement | null = null;
  private departX = 0;
  private departY = 0;
  private decalageX = 0;
  private decalageY = 0;
  private limites = { minX: 0, maxX: 0, minY: 0, maxY: 0 };

  private readonly deplacer = (event: PointerEvent): void => {
    if (!this.fenetre) return;
    const x = Math.min(this.limites.maxX, Math.max(this.limites.minX, this.decalageX + event.clientX - this.departX));
    const y = Math.min(this.limites.maxY, Math.max(this.limites.minY, this.decalageY + event.clientY - this.departY));
    this.fenetre.style.transform = `translate(${x}px, ${y}px)`;
  };

  private readonly terminer = (event: PointerEvent): void => {
    if (this.fenetre) {
      this.decalageX = Math.min(this.limites.maxX, Math.max(this.limites.minX, this.decalageX + event.clientX - this.departX));
      this.decalageY = Math.min(this.limites.maxY, Math.max(this.limites.minY, this.decalageY + event.clientY - this.departY));
    }
    this.arreter();
  };

  protected debuter(event: PointerEvent): void {
    if (event.button !== 0 || (event.target as HTMLElement).closest('button, a, input, select, textarea')) {
      return;
    }
    const fenetre = this.poignee.closest<HTMLElement>('.modal-dialog');
    if (!fenetre) return;
    this.fenetre = fenetre;
    // Bootstrap anime le `transform` de la fenêtre (ouverture) : sans cela elle traîne derrière la souris.
    fenetre.style.transition = 'none';
    this.departX = event.clientX;
    this.departY = event.clientY;

    // Bornes du décalage : l'en-tête garde au moins 48 px visibles dans la fenêtre du navigateur.
    const rect = fenetre.getBoundingClientRect();
    const marge = 48;
    this.limites = {
      minX: this.decalageX + marge - rect.right,
      maxX: this.decalageX + window.innerWidth - marge - rect.left,
      minY: this.decalageY - rect.top,
      maxY: this.decalageY + window.innerHeight - marge - rect.top,
    };
    document.addEventListener('pointermove', this.deplacer);
    document.addEventListener('pointerup', this.terminer);
    document.addEventListener('pointercancel', this.terminer);
    event.preventDefault();
  }

  private arreter(): void {
    document.removeEventListener('pointermove', this.deplacer);
    document.removeEventListener('pointerup', this.terminer);
    document.removeEventListener('pointercancel', this.terminer);
    this.fenetre = null;
  }

  ngOnDestroy(): void {
    this.arreter();
  }
}
