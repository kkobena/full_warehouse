import { afterNextRender, DestroyRef, Directive, ElementRef, inject } from '@angular/core';

/**
 * Pose `--hauteur-ecran-vente` : la place qui reste sous la barre de navigation, jusqu'au bas de la fenêtre.
 *
 * Les écrans de vente occupaient `100vh` alors qu'ils commencent 52 à 67 px plus bas (selon que les menus de la
 * barre tiennent sur une ou deux lignes) : leur bas, où défile la zone de travail, restait hors de l'écran et
 * toute la page défilait en plus. La valeur est recalculée au redimensionnement de la fenêtre.
 */
@Directive({ selector: '[appHauteurEcranVente]' })
export class HauteurEcranVenteDirective {
  private readonly hote = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  constructor() {
    afterNextRender(() => {
      this.ajuster();
      // Largeur de la fenêtre changée : les menus de la barre se replient ou se déplient, donc le haut de la zone bouge.
      const observateur = new ResizeObserver(() => this.ajuster());
      observateur.observe(document.documentElement);
      inject(DestroyRef).onDestroy(() => observateur.disconnect());
    });
  }

  private ajuster(): void {
    const haut = this.hote.getBoundingClientRect().top + window.scrollY;
    const hauteur = `${Math.max(320, Math.round(window.innerHeight - haut))}px`;
    if (this.hote.style.getPropertyValue('--hauteur-ecran-vente') !== hauteur) {
      this.hote.style.setProperty('--hauteur-ecran-vente', hauteur);
    }
  }
}
