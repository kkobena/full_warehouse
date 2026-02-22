import {
  AfterViewInit,
  Component,
  ElementRef,
  inject,
  model,
  output,
  signal,
  viewChild
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {TableModule} from 'primeng/table';
import {TooltipModule} from 'primeng/tooltip';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ICustomer} from '../../../../shared/model';
import {CustomerSearchService} from '../../data-access/services/customer-search.service';
import {
  UninsuredCustomerFormComponent
} from '../../../../entities/customer/uninsured-customer-form/uninsured-customer-form.component';
import {showCommonModal} from '../../../../entities/sales/selling-home/sale-helper';

/**
 * Composant de recherche et sélection de client réutilisable.
 * Utilisé dans :
 * - CustomerOverlayPanelComponent (popover)
 * - CustomerSelectionModalComponent (modal)
 */
@Component({
  selector: 'app-customer-search-table',
  imports: [CommonModule, FormsModule, ButtonModule, InputTextModule, TableModule, TooltipModule, IconField, InputIcon],
  templateUrl: './customer-search-table.component.html',
  styles: [
    `
      @import 'app/shared/scss/table-common';

      :host {
        display: block;
      }
    `,
  ],
})
export class CustomerSearchTableComponent implements AfterViewInit {
  readonly customerSelected = output<ICustomer>();

  searchTerm = '';
  customers = model<ICustomer[]>([]);
  loading = signal(false);

  protected searchInput = viewChild<ElementRef>('searchInput');

  private readonly customerSearchService = inject(CustomerSearchService);
  private readonly modalService = inject(NgbModal);

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.searchInput()?.nativeElement.focus();
    }, 100);
  }

  onSearchChange(term: string): void {
    if (term && term.length >= 2) {
      this.loading.set(true);
      this.customerSearchService.search(term, 20).subscribe({
        next: customers => {
          this.customers.set(customers);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
    } else {
      this.customers.set([]);
    }
  }

  selectCustomer(customer: ICustomer): void {
    this.customerSelected.emit(customer);
  }

  addNewCustomer(): void {
    showCommonModal(
      this.modalService,
      UninsuredCustomerFormComponent,
      {title: "FORMULAIRE D'AJOUT DE NOUVEAU CLIENT", entity: null},
      (newCustomer: ICustomer) => {
        if (newCustomer) {
          this.customerSelected.emit(newCustomer);
        }
      },
    );
  }
}
