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
      // Le haut de la zone bouge quand la fenêtre change de largeur (les menus se replient), mais aussi quand ce qui la précède
      // change de hauteur sans que la fenêtre bouge : les entrées du menu arrivent après le chargement et la barre passe de
      // 44 à 55 px. On observe donc la fenêtre et chaque élément qui précède la zone (barre, bandeau de licence, titre).
      const observateur = new ResizeObserver(() => this.ajuster());
      observateur.observe(document.documentElement);
      for (let n: HTMLElement | null = this.hote; n && n !== document.body; n = n.parentElement) {
        for (let s = n.previousElementSibling; s; s = s.previousElementSibling) {
          observateur.observe(s);
        }
      }
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
