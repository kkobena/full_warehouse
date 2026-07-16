import { Component, inject } from "@angular/core";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { CustomerDataTableComponent } from "./customer-data-table.component";
import { Card } from "primeng/card";

@Component({
  selector: "app-uninsured-customer-list",
  templateUrl: "./uninsured-customer-list.component.html",
  styleUrls: ["../../common-modal.component.scss"],
  imports: [
    ButtonModule,
    CustomerDataTableComponent,
    Card
  ]
})
export class UninsuredCustomerListComponent {
  header: string = null;
  private readonly activeModal = inject(NgbActiveModal);

  onSelectClose(event: any): void {
    this.activeModal.close(event);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}
