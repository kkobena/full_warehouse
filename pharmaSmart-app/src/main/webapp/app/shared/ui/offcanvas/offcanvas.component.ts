import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

/**
 * Panneau latéral — remplace `p-drawer` pour un contenu statique (guide, aide),
 * sans passer par le service `NgbOffcanvas`.
 *
 * `NgbOffcanvas` impose d'ouvrir un **composant** (`offcanvasService.open(SomeComponent)`),
 * ce qui aurait forcé à extraire chaque contenu de guide dans un fichier séparé pour un
 * simple panneau d'aide statique — sans bénéfice réel ici. Ce composant reprend
 * directement le balisage `.offcanvas` de Bootstrap, piloté par classes comme `p-drawer`
 * l'était par son `[(visible)]`.
 *
 * @example
 * <app-offcanvas [(visible)]="helpDrawerVisible" width="600px">
 *   <div appOffcanvasHeader class="d-flex align-items-center gap-2">
 *     <i class="pi pi-question-circle text-primary"></i>
 *     <span class="fw-bold fs-5">Guide d'utilisation</span>
 *   </div>
 *   <div class="p-3">…</div>
 * </app-offcanvas>
 */
@Component({
  selector: 'app-offcanvas',
  template: `
    @if (visible()) {
      <div class="offcanvas-backdrop fade show" (click)="visible.set(false)"></div>
    }

    <div
      class="offcanvas"
      [class.show]="visible()"
      [class.offcanvas-end]="position() === 'right'"
      [class.offcanvas-start]="position() === 'left'"
      [style.width]="width()"
      tabindex="-1"
    >
      <div class="offcanvas-header">
        <ng-content select="[appOffcanvasHeader]" />
        <button type="button" class="btn-close" aria-label="Fermer" (click)="visible.set(false)"></button>
      </div>

      <div class="offcanvas-body">
        <ng-content />
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OffcanvasComponent {
  readonly visible = model.required<boolean>();

  readonly position = input<'left' | 'right'>('right');

  /** Largeur du panneau, ex. `600px`. Bootstrap impose `400px` par défaut sur `.offcanvas-end`. */
  readonly width = input<string>('');
}
