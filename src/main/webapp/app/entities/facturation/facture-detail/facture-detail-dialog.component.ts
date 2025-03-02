import { Component, inject } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Facture } from '../facture.model';
import { FactureDetailComponent } from './facture-detail.component';

@Component({
  selector: 'jhi-facture-detail-dialog',
  imports: [FormsModule, ReactiveFormsModule, FactureDetailComponent],
  templateUrl: './facture-detail-dialog.component.html',
  styles: ``,
})
export class FactureDetailDialogComponent {
  modalService = inject(NgbModal);
  activeModal = inject(NgbActiveModal);
  facture: Facture | null = null;

  cancel(): void {
    this.activeModal.dismiss();
  }
}
