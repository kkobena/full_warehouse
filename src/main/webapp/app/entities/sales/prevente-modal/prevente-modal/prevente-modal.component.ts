import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { IUser, User } from '../../../../core/user/user.model';
import { ISales } from '../../../../shared/model/sales.model';
import { SalesService } from '../../sales.service';
import { APPEND_TO } from '../../../../shared/constants/pagination.constants';
import { HttpResponse } from '@angular/common/http';
import { UserService } from '../../../../core/user/user.service';
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

@Component({
  selector: 'jhi-prevente-modal',
  templateUrl: './prevente-modal.component.html',
  styleUrls: ['./prevente-modal.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: true,
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
  ],
})
export class PreventeModalComponent implements OnInit {
  @Input() user: IUser;
  @Output() pendingSalesSidebarChange: EventEmitter<boolean> = new EventEmitter<boolean>();
  protected typeVenteSelected = 'TOUT';
  protected preventes: ISales[] = [];
  protected users: IUser[] = [];
  protected search = '';
  protected userSeller?: IUser;
  protected readonly appendTo = APPEND_TO;
  protected selectedRowIndex?: number;

  constructor(
    protected salesService: SalesService,
    protected userService: UserService,
    protected currentSaleService: CurrentSaleService,
    protected customerService: CustomerService,
    protected selectedCustomerService: SelectedCustomerService,
  ) {}

  ngOnInit(): void {
    this.selectedRowIndex = 0;
    this.userSeller = this.user;
    this.loadPreventes();
  }

  onSelectUser(): void {
    this.user = this.userSeller;
    this.loadPreventes();
  }

  onSearch(event: any): void {
    this.search = event.target.value;
    this.loadPreventes();
  }

  loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => (this.users = res.body || []));
  }

  searchUser(): void {
    this.loadAllUsers();
  }

  loadPreventes(): void {
    this.salesService
      .queryPrevente({
        search: this.search,
        type: this.typeVenteSelected,
        userId: this.user.id,
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
