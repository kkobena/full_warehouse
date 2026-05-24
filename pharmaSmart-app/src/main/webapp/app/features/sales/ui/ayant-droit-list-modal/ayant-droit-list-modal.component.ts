import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { ToolbarModule } from 'primeng/toolbar';
import { ICustomer } from '../../../../shared/model';
import { CustomerService } from '../../../../entities/customer/customer.service';
import { FormAyantDroitComponent } from '../../../../entities/customer/form-ayant-droit/form-ayant-droit.component';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';

@Component({
  selector: 'app-ayant-droit-list-modal',
  templateUrl: './ayant-droit-list-modal.component.html',
  styleUrls: ['./ayant-droit-list.scss'],
  imports: [CommonModule, Button, TableModule, TooltipModule, ToolbarModule],
})
export class AyantDroitListModalComponent implements OnInit {
  assure: ICustomer;
  tilte: string;

  customers = signal<ICustomer[]>([]);
  loading = signal(false);

  private readonly activeModal = inject(NgbActiveModal);
  private readonly modalService = inject(NgbModal);
  private readonly customerService = inject(CustomerService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.loadCustomers();
  }

  onSelect(customer: ICustomer): void {
    this.activeModal.close(customer);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  addAyantDroit(): void {
    showCommonModal(
      this.modalService,
      FormAyantDroitComponent,
      {
        entity: undefined,
        assure: this.assure,
        title: "FORMULAIRE D'AJOUT D'AYANT DROIT",
      },
      (newAyantDroit: ICustomer) => {
        if (newAyantDroit) {
          // Rafraîchir la liste puis fermer avec le nouvel ayant droit
          this.customers.update(list => [newAyantDroit, ...list]);
          this.activeModal.close(newAyantDroit);
        }
      },
      'xl',
    );
  }

  private loadCustomers(): void {
    this.loading.set(true);
    this.customerService
      .queryAyantDroits(this.assure.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.customers.set(res.body || []);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
