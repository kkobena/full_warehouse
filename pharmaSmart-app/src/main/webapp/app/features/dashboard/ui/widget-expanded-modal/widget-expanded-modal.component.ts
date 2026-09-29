import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { DashboardItem, WidgetAvailability } from '../../models/dashboard.model';
import { widgetLabel } from '../../data-access/store/dashboard.store';
import { WidgetTileComponent } from '../widget-tile/widget-tile.component';

/**
 * Une tuile en grand, pour lire un tableau long ou un graphique dense. Ouverte avec l'injecteur de
 * la grille : le widget y retrouve la période du tableau de bord.
 */
@Component({
  selector: 'app-widget-expanded-modal',
  imports: [WidgetTileComponent],
  template: `
    <div class="modal-header">
      <h2 class="modal-title fs-5">{{ title() }}</h2>
      <button type="button" class="btn-close" aria-label="Fermer" (click)="activeModal.dismiss()"></button>
    </div>
    <div class="modal-body expanded-body">
      @if (item(); as current) {
        <app-widget-tile [item]="current" [availability]="availability()" [editMode]="false" [inModal]="true" />
      }
    </div>
  `,
  styles: `
    .expanded-body {
      height: 70vh;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WidgetExpandedModalComponent {
  readonly activeModal = inject(NgbActiveModal);

  readonly item = signal<DashboardItem | null>(null);
  readonly availability = signal<WidgetAvailability>('OK');
  protected readonly title = () => {
    const item = this.item();
    return item ? widgetLabel(item) : '';
  };
}
