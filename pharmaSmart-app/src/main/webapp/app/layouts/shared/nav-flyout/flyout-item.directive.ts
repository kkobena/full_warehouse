import { Directive, ElementRef, inject } from '@angular/core';
import { FocusableOption } from '@angular/cdk/a11y';

/**
 * Marque une ligne du flyout comme option navigable au clavier.
 *
 * Applique le patron « roving tabindex » : une seule ligne est atteignable par
 * `Tab` (celle qui est active), les autres restent focalisables uniquement par
 * les flèches, via le `FocusKeyManager` du panneau.
 *
 * Le `tabindex` est écrit directement sur l'élément plutôt que par un binding :
 * le panneau désigne la ligne active depuis `ngAfterViewInit`, après que les
 * directives ont été vérifiées, et un binding déclencherait NG0100.
 */
@Directive({
  selector: '[appFlyoutItem]',
  host: {
    role: 'menuitem',
    tabindex: '-1',
  },
})
export class FlyoutItemDirective implements FocusableOption {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  setActive(active: boolean): void {
    this.host.nativeElement.tabIndex = active ? 0 : -1;
  }

  focus(): void {
    this.host.nativeElement.focus();
  }

  /** Libellé utilisé par la recherche à la saisie (typeahead). */
  getLabel(): string {
    return this.host.nativeElement.textContent?.trim() ?? '';
  }
}
