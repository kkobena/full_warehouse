import { Component, inject, signal, OnInit, AfterViewInit, viewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { ICustomer } from '../../../../shared/model/customer.model';
import { CustomerSearchService } from '../../data-access/services/customer-search.service';
import { UninsuredCustomerFormComponent } from '../../../../entities/customer/uninsured-customer-form/uninsured-customer-form.component';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';

/**
 * Modal de sélection de client pour les ventes différées
 * Composant autonome qui retourne le client sélectionné via modalRef.result
 */
@Component({
  selector: 'app-customer-selection-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    InputTextModule,
    TableModule,
    TooltipModule,
    IconField,
    InputIcon,
  ],
  template: `
    <div class="modal-header">
      <h5 class="modal-title">{{ modalTitle }}</h5>
      <button type="button" class="btn-close" aria-label="Close" (click)="cancel()"></button>
    </div>
    
    <div class="modal-body">
      <p-table
      class="pharma-table"
        [value]="customers()"
        [paginator]="true"
        [rows]="10"
        [rowsPerPageOptions]="[5, 8, 10, 20]"
        [showCurrentPageReport]="true"
        [loading]="loading()"
        class="pharma-table"
        currentPageReportTemplate="Affichage de {first} à {last} sur {totalRecords} clients"
        dataKey="id"
        selectionMode="single"
      >
        <ng-template #caption>
          <div class="row">
            <div class="col-md-8">
              <p-iconfield>
                <p-inputicon class="pi pi-search" />
                <input
                  #searchInput
                  type="text"
                  pInputText
                  [(ngModel)]="searchTerm"
                  (input)="onSearchChange(searchTerm)"
                  placeholder="Rechercher un client (nom, téléphone)..."
                  style="width: 350px"
                />
              </p-iconfield>
            </div>
            <div class="col-md-4">
              <p-button
                (click)="addNewCustomer()"
                icon="pi pi-user"
                label="Nouveau client"
                severity="help"
                size="small"
              ></p-button>
            </div>
          </div>
        </ng-template>
        
        <ng-template #header>
          <tr class="pharma-table-head">
            <th style="width: 5%">#</th>
            <th style="width: 100px">CODE</th>
            <th>NOM</th>
            <th>PRENOM(S)</th>
            <th>TELEPHONE</th>
            <th style="width: 100px"></th>
          </tr>
        </ng-template>
        
        <ng-template #body let-customer let-rowIndex="rowIndex">
          <tr (click)="selectCustomer(customer)" style="cursor: pointer">
            <td style="text-align: left">
              <span class="pharma-code">{{ rowIndex + 1 }}</span>
            </td>
            <td>{{ customer.code }}</td>
            <td>{{ customer.firstName }}</td>
            <td>{{ customer.lastName }}</td>
            <td>{{ customer.phone }}</td>
            <td style="text-align: right">
              <p-button
                (click)="selectCustomer(customer); $event.stopPropagation()"
                [rounded]="true"
                icon="pi pi-check-circle"
                pTooltip="Sélectionner ce client"
                severity="success"
                size="small"
                [text]="true"
              ></p-button>
            </td>
          </tr>
        </ng-template>
        
        <ng-template #emptymessage>
          <tr>
            <td colspan="6" class="text-center">
              {{ searchTerm ? 'Aucun client trouvé' : 'Saisissez un terme de recherche pour trouver un client' }}
            </td>
          </tr>
        </ng-template>
      </p-table>
    </div>

    <div class="modal-footer">
      <button type="button" class="btn btn-secondary" (click)="cancel()">
        <i class="pi pi-times"></i> Annuler
      </button>
    </div>
  `,
  styles: [`
    @import 'app/shared/scss/table-common';
    
    .modal-body {
      max-height: 70vh;
      overflow-y: auto;
    }
  `]
})
export class CustomerSelectionModalComponent implements OnInit, AfterViewInit {
  modalTitle: string = 'Sélection client';
  searchTerm = '';
  
  customers = signal<ICustomer[]>([]);
  loading = signal(false);
  
  protected searchInput = viewChild<ElementRef>('searchInput');
  
  private activeModal = inject(NgbActiveModal);
  private modalService = inject(NgbModal);
  private customerSearchService = inject(CustomerSearchService);

  ngOnInit(): void {
    // Le focus sera géré par AfterViewInit via viewChild
  }

  ngAfterViewInit(): void {
    // Focus sur le champ de recherche après l'initialisation de la vue
    if (this.searchInput()) {
      this.searchInput()!.nativeElement.focus();
    }
  }

  onSearchChange(term: string): void {
    if (term && term.length >= 2) {
      this.loading.set(true);
      this.customerSearchService.search(term, 20).subscribe({
        next: (customers) => {
          this.customers.set(customers);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        }
      });
    } else {
      this.customers.set([]);
    }
  }

  selectCustomer(customer: ICustomer): void {
    // Retourner le client sélectionné via le résultat du modal
    this.activeModal.close(customer);
  }

  addNewCustomer(): void {
    // Ouvrir le formulaire de création de client
    showCommonModal(
      this.modalService,
      UninsuredCustomerFormComponent,
      { title: "FORMULAIRE D'AJOUT DE NOUVEAU CLIENT", entity: null },
      (newCustomer: ICustomer) => {
        if (newCustomer) {
          // Retourner immédiatement le nouveau client créé
          this.activeModal.close(newCustomer);
        }
      },
    );
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}
