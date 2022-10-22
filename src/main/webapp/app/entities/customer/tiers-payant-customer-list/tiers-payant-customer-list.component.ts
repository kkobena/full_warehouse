import { Component, OnInit } from '@angular/core';
import { ICustomer } from 'app/shared/model/customer.model';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { CustomerService } from 'app/entities/customer/customer.service';
import { ClientTiersPayant, IClientTiersPayant } from 'app/shared/model/client-tiers-payant.model';

@Component({
  selector: 'jhi-tiers-payant-customer-list',
  templateUrl: './tiers-payant-customer-list.component.html',
})
export class TiersPayantCustomerListComponent implements OnInit {
  tiersPayants: IClientTiersPayant[] = [];
  assure?: ICustomer | null;
  tiersPayantsExisting: IClientTiersPayant[] = [];

  constructor(public ref: DynamicDialogRef, public config: DynamicDialogConfig, protected customerService: CustomerService) {}

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
    this.customerService.fetchCustomersTiersPayant(this.assure?.id!).subscribe(res => {
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
