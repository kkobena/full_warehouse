import { AfterViewInit, Directive, ElementRef, inject } from '@angular/core';

/**
 * Donne le focus au champ dès qu'il est visible. Le contenu d'un onglet `ngbNav` est recréé à chaque affichage : le champ
 * reprend donc le focus à l'ouverture de la fenêtre comme à chaque changement d'onglet.
 *
 * On attend que le champ soit réellement affiché : le panneau d'un onglet reste en `display: none` pendant son animation
 * (une centaine de millisecondes), et `focus()` n'a alors aucun effet. `NgbModal` place aussi le focus sur la fenêtre à
 * l'ouverture. La veille s'arrête dès que le champ a le focus, ou si l'utilisateur est déjà dans un autre champ.
 */
@Directive({ selector: '[appAutoFocus]' })
export class AutoFocusDirective implements AfterViewInit {
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  ngAfterViewInit(): void {
    let essais = 0;
    const veille = setInterval(() => {
      const actif = document.activeElement;
      const ailleurs =
        actif !== this.element && (actif instanceof HTMLInputElement || actif instanceof HTMLTextAreaElement || actif instanceof HTMLSelectElement);
      if (++essais > 20 || ailleurs || !this.element.isConnected) {
        clearInterval(veille);
        return;
      }
      if (this.element.offsetParent !== null) {
        this.element.focus();
        if (document.activeElement === this.element) {
          clearInterval(veille);
        }
      }
    }, 30);
  }
}
