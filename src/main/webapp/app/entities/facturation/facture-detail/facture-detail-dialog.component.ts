import { Component, inject } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Facture } from '../facture.model';
import { FactureDetailComponent } from './facture-detail.component';
import { Card } from 'primeng/card';
import { Button } from 'primeng/button';

@Component({
  selector: 'jhi-facture-detail-dialog',
  imports: [FormsModule, ReactiveFormsModule, FactureDetailComponent, Card, Button],
  templateUrl: './facture-detail-dialog.component.html',
  styleUrls: ['../../common-modal.component.scss'],
})
export class FactureDetailDialogComponent {
  modalService = inject(NgbModal);
  activeModal = inject(NgbActiveModal);
  facture: Facture | null = null;

  cancel(): void {
    this.activeModal.dismiss();
  }
}
