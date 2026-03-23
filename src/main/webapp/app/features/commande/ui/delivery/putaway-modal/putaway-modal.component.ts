import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { IPutawayPreviewItem } from '../../../../../entities/commande/commande.service';

@Component({
  selector: 'app-putaway-modal',
  templateUrl: './putaway-modal.component.html',
  styleUrls: ['./putaway-modal.component.scss'],
  imports: [DecimalPipe, ButtonModule, TagModule, TooltipModule],
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
