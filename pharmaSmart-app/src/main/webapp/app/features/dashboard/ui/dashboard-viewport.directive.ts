import { afterNextRender, Directive, ElementRef, inject, signal } from '@angular/core';

/** Marge laissée sous la page, comme au pilotage. */
const MARGE_BAS = 16;

/**
 * Donne à la page la hauteur restante de l'écran : la barre d'outils reste en place et seule la
 * zone `.dashboard-scroll` défile. Même mesure que la page de pilotage.
 */
@Directive({
  selector: '[appDashboardViewport]',
  host: {
    '(window:resize)': 'measure()',
    '[style.height]': 'height()',
    '[style.display]': '"flex"',
    '[style.flex-direction]': '"column"',
    '[style.min-height]': '"24rem"',
  },
})
export class DashboardViewportDirective {
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  readonly height = signal('calc(100dvh - 9rem)');

  constructor() {
    afterNextRender(() => this.measure());
  }

  measure(): void {
    const top = this.el.nativeElement.getBoundingClientRect().top + window.scrollY;
    // réserve du bas de la zone principale (bannière de licence)
    const zone = document.getElementById('main-content');
    const reserveBas = zone ? parseFloat(getComputedStyle(zone).paddingBottom) || 0 : 0;
    this.height.set(`${Math.max(0, window.innerHeight - top - reserveBas - MARGE_BAS)}px`);
  }
}
