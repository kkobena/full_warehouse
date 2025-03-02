import { Component, inject } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Facture } from '../facture.model';
import { GroupeFactureDetailComponent } from './groupe-facture-detail.component';

@Component({
  selector: 'jhi-groupe-facture-detail-dialog',
  imports: [GroupeFactureDetailComponent],
  templateUrl: './groupe-facture-detail-dialog.component.html',
  styles: ``,
})
export class GroupeFactureDetailDialogComponent {
  modalService = inject(NgbModal);
  activeModal = inject(NgbActiveModal);
  facture: Facture | null = null;

  cancel(): void {
    this.activeModal.dismiss();
  }
}
