import { Component, inject, OnInit, ChangeDetectionStrategy } from "@angular/core";

import { Customer, ICustomer } from "app/shared/model/customer.model";
import { CustomerService } from "app/entities/customer/customer.service";
import { FormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { CommonModule } from "@angular/common";
import { ButtonComponent, CardComponent, ToolbarComponent } from "../../../shared/ui";

@Component({
  selector: "app-ayant-droit-customer-list",
  templateUrl: "./ayant-droit-customer-list.component.html",
  styleUrls: ["./ayant-droit-customer-list.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    CardComponent,
    ToolbarComponent
  ]
})
export class AyantDroitCustomerListComponent implements OnInit {
  customers: ICustomer[] = [];
  assure?: ICustomer | null;
  header: string;
  private readonly customerService = inject(CustomerService);
  private readonly activeModal = inject(NgbActiveModal);

  ngOnInit(): void {
    this.loadCustomers();
  }

  onDbleClick(customer: ICustomer): void {
    this.onSelect(customer);
  }

  onSelect(customer: ICustomer): void {
    this.activeModal.close(customer);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  loadCustomers(): void {
    this.customerService.queryAyantDroits(this.assure.id).subscribe(res => (this.customers = res.body!));
  }

  addAyantDroit(): void {
    this.activeModal.close(new Customer());
  }
}
