import {Component, inject, ChangeDetectionStrategy} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {ButtonModule} from 'primeng/button';
import {ICustomer} from '../../../../shared/model';
import {
  CustomerSearchTableComponent
} from '../customer-search-table/customer-search-table.component';

/**
 * Modal de sélection de client pour les ventes différées
 * Composant autonome qui retourne le client sélectionné via modalRef.result
 * Utilise le composant partagé CustomerSearchTableComponent
 */
@Component({
  selector: 'app-customer-selection-modal',
  imports: [ButtonModule, CustomerSearchTableComponent],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">{{ modalTitle }}</h5>
      <button type="button" class="btn-close" aria-label="Close" (click)="cancel()"></button>
    </div>

    <div class="modal-body">
      <app-customer-search-table [(customers)]="customers"
                                 (customerSelected)="selectCustomer($event)" />
    </div>

    <div class="modal-footer">
      <p-button (click)="cancel()" [raised]="true" icon="pi pi-times" label="Annuler"
                severity="secondary" type="button" />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [
    `
      .modal-body {
        max-height: 70vh;
        overflow-y: auto;
      }
    `,
  ],
})
export class CustomerSelectionModalComponent {
  modalTitle: string = 'Sélection client';
  customers: ICustomer[];

  private activeModal = inject(NgbActiveModal);

  selectCustomer(customer: ICustomer): void {
    this.activeModal.close(customer);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}
