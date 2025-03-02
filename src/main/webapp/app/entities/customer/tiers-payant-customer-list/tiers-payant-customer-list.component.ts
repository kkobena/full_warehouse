import { Component, OnInit, inject } from '@angular/core';
import { ICustomer } from 'app/shared/model/customer.model';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { CustomerService } from 'app/entities/customer/customer.service';
import { ClientTiersPayant, IClientTiersPayant } from 'app/shared/model/client-tiers-payant.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { ToolbarModule } from 'primeng/toolbar';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'jhi-tiers-payant-customer-list',
  templateUrl: './tiers-payant-customer-list.component.html',
  imports: [WarehouseCommonModule, ToolbarModule, TableModule, ButtonModule, RippleModule, InputTextModule, TooltipModule],
})
export class TiersPayantCustomerListComponent implements OnInit {
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  protected customerService = inject(CustomerService);

  tiersPayants: IClientTiersPayant[] = [];
  assure?: ICustomer | null;
  tiersPayantsExisting: IClientTiersPayant[] = [];

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  ngOnInit(): void {
    this.assure = this.config.data.assure;
    this.tiersPayantsExisting = this.config.data.tiersPayants;
    this.load();
  }

  onDbleClick(tiersPayant: IClientTiersPayant): void {
    this.onSelect(tiersPayant);
  }

  onSelect(tiersPayant: IClientTiersPayant): void {
    this.ref.close(tiersPayant);
  }

  cancel(): void {
    this.ref.close();
  }

  load(): void {
    this.customerService.fetchCustomersTiersPayant(this.assure.id).subscribe(res => {
      if (res.body) {
        if (this.tiersPayantsExisting && this.tiersPayantsExisting.length > 0) {
          this.tiersPayants = res.body.filter(e => !this.tiersPayantsExisting.some(i => i.id === e.id));
        } else {
          this.tiersPayants = res.body;
        }
      }
    });
  }

  add(): void {
    this.ref.close(new ClientTiersPayant());
  }
}
