import { Component, inject, input, OnInit, output, signal, ViewEncapsulation } from '@angular/core';
import { IUser } from '../../../../core/user/user.model';
import { ISales } from '../../../../shared/model/sales.model';
import { SalesService } from '../../sales.service';
import { APPEND_TO } from '../../../../shared/constants/pagination.constants';
import { HttpResponse } from '@angular/common/http';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { CurrentSaleService } from '../../service/current-sale.service';
import { CustomerService } from '../../../customer/customer.service';
import { SelectedCustomerService } from '../../service/selected-customer.service';
import { ICustomer } from '../../../../shared/model/customer.model';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputGroup } from 'primeng/inputgroup';
import { InputGroupAddon } from 'primeng/inputgroupaddon';
import { Select } from 'primeng/select';
import { UserVendeurService } from '../../service/user-vendeur.service';

@Component({
  selector: 'jhi-prevente-modal',
  templateUrl: './prevente-modal.component.html',
  styleUrls: ['./prevente-modal.component.scss'],
  encapsulation: ViewEncapsulation.None,
  imports: [
    WarehouseCommonModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    TableModule,
    AutoCompleteModule,
    ToolbarModule,
    DividerModule,
    IconField,
    InputIcon,
    InputGroup,
    InputGroupAddon,
    Select
  ]
})
export class PreventeModalComponent implements OnInit {
  readonly user = input<IUser>();
  userSignal = signal(this.user());
  readonly pendingSalesSidebarChange = output<boolean>();
  salesService = inject(SalesService);
  currentSaleService = inject(CurrentSaleService);
  customerService = inject(CustomerService);
  selectedCustomerService = inject(SelectedCustomerService);
  protected typeVenteSelected = 'TOUT';
  protected preventes: ISales[] = [];
  protected users: IUser[] = [];
  protected search = '';
  protected userSeller?: IUser;
  protected readonly appendTo = APPEND_TO;
  protected selectedRowIndex?: number;
  protected userVendeurService = inject(UserVendeurService);

  ngOnInit(): void {
    this.selectedRowIndex = 0;
    this.userSeller = this.user();
    this.userSignal.set(this.user());
    this.loadPreventes();
  }

  onSelectUser(): void {
    this.userSignal.set(this.userSeller);
    this.loadPreventes();
  }

  onSearch(event: any): void {
    this.search = event.target.value;
    this.loadPreventes();
  }

  loadPreventes(): void {
    this.salesService
      .queryPrevente({
        search: this.search,
        type: this.typeVenteSelected,
        userId: this.userSignal().id
      })
      .subscribe(res => {
        this.preventes = res.body ?? [];
      });
  }

  onSelect(sale: ISales): void {
    this.selectedRowIndex = sale.id;
  }

  onDbleSelect(sale: ISales): void {
    if (sale.customer) {
      this.customerService
        .find(sale.customer.id)
        .subscribe({ next: (resp: HttpResponse<ICustomer>) => this.selectedCustomerService.setCustomer(resp.body) });
    }
    this.currentSaleService.setCurrentSale(sale);
    this.pendingSalesSidebarChange.emit(false);
  }
}
