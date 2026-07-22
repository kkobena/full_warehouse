import { Component, input } from '@angular/core';

/**
 * Bloc de chargement du Design System — remplace `p-skeleton`.
 * S'appuie sur `.placeholder` / `.placeholder-glow` de Bootstrap 5.
 *
 * @example
 * <app-skeleton width="12rem" />
 * <app-skeleton shape="circle" size="3rem" />
 */
@Component({
  selector: 'app-skeleton',
  host: {
    'aria-hidden': 'true',
    '[class.placeholder-glow]': 'true',
  },
  template: `
    <span
      class="placeholder"
      [class.rounded-circle]="shape() === 'circle'"
      [style.width]="shape() === 'circle' ? size() : width()"
      [style.height]="shape() === 'circle' ? size() : height()"
      [style.border-radius]="shape() === 'rectangle' ? borderRadius() : null"
    ></span>
  `,
  styles: `
    :host {
      display: block;
    }

    .placeholder {
      display: inline-block;
    }
  `,
})
export class SkeletonComponent {
  readonly shape = input<'rectangle' | 'circle'>('rectangle');

  readonly width = input<string>('100%');

  readonly height = input<string>('1rem');

  /** Diamètre, utilisé uniquement quand `shape` vaut `circle`. */
  readonly size = input<string>('2rem');

  readonly borderRadius = input<string>('var(--bs-border-radius)');
}
