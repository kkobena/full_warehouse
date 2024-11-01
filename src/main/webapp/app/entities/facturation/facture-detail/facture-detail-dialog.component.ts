import { Component, inject } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import TranslateDirective from '../../../shared/language/translate.directive';
import { Facture } from '../facture.model';
import { FactureDetailComponent } from './facture-detail.component';

@Component({
  selector: 'jhi-facture-detail-dialog',
  standalone: true,
  imports: [FaIconComponent, FormsModule, ReactiveFormsModule, TranslateDirective, FactureDetailComponent],
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
