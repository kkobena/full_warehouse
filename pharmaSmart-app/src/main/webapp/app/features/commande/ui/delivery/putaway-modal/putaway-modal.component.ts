import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { NgbActiveModal, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { BadgeComponent, ButtonComponent } from 'app/shared/ui';
import { IPutawayPreviewItem } from '../../../../../entities/commande/commande.service';

@Component({
  selector: 'app-putaway-modal',
  templateUrl: './putaway-modal.component.html',
  styleUrls: ['./putaway-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [DecimalPipe, ButtonComponent, BadgeComponent, NgbTooltip],
})
export class PutawayModalComponent {
  items: IPutawayPreviewItem[] = [];
  header = 'Répartition rayon → réserve';

  private readonly activeModal = inject(NgbActiveModal);

  protected transfer(): void {
    this.activeModal.close(true);
  }

  protected ignore(): void {
    this.activeModal.close(false);
  }

  protected abcSeverity(classe: string): 'danger' | 'warn' | 'info' | 'secondary' {
    if (classe === 'A+') return 'danger';
    if (classe === 'A') return 'warn';
    if (classe === 'B') return 'info';
    return 'secondary';
  }
}
