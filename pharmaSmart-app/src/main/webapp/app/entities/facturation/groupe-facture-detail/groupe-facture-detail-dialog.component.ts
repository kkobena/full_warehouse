import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Facture } from '../facture.model';
import { GroupeFactureDetailComponent } from './groupe-facture-detail.component';

@Component({
  selector: 'jhi-groupe-facture-detail-dialog',
  imports: [GroupeFactureDetailComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './groupe-facture-detail-dialog.component.html',
})
export class GroupeFactureDetailDialogComponent {
  activeModal = inject(NgbActiveModal);
  facture: Facture | null = null;

  cancel(): void {
    this.activeModal.dismiss();
  }
}
