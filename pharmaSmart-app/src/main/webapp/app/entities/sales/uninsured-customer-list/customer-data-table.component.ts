import { ChangeDetectionStrategy, AfterViewInit, Component, ElementRef, inject, output, viewChild, signal } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { ICustomer } from "../../../shared/model";
import { CustomerService } from "../../customer/customer.service";
import {
  UninsuredCustomerFormComponent
} from "../../customer/uninsured-customer-form/uninsured-customer-form.component";
import { SelectedCustomerService } from "../service/selected-customer.service";
import { NgbModal, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { showCommonModal } from "../selling-home/sale-helper";
import { ButtonComponent, DataTableComponent, IconFieldComponent } from "../../../shared/ui";

@Component({
  selector: "app-customer-data-table",
  imports: [
    ReactiveFormsModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    IconFieldComponent,
    NgbTooltip
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: "./customer-data-table.component.html",
  styleUrls: ["./customer-data-table.scss"]
})
export class CustomerDataTableComponent implements AfterViewInit {
  protected readonly customers = signal<ICustomer[]>([]);
  searchString?: string | null = "";
  readonly closeModalEvent = output<boolean>();
  protected searchInput = viewChild.required<ElementRef>("searchInput");
  protected customerService = inject(CustomerService);
  private selectedCustomerService = inject(SelectedCustomerService);
  private readonly modalService = inject(NgbModal);

  ngAfterViewInit(): void {
    this.searchInput().nativeElement.focus();
  }

  protected onSelect(customer: ICustomer): void {
    this.selectedCustomerService.setCustomer(customer);
    this.closeModalEvent.emit(true);
  }

  protected loadCustomers(): void {
    this.customerService
      .queryUninsuredCustomers({
        search: this.searchString
      })
      .subscribe(res => (this.customers.set(res.body!)));
  }

  protected addUninsuredCustomer(): void {
    showCommonModal(
      this.modalService,
      UninsuredCustomerFormComponent,
      { title: "FORMULAIRE D'AJOUT DE NOUVEAU DE CLIENT ", entity: null },
      (resp: ICustomer) => {
        if (resp) {
          this.selectedCustomerService.setCustomer(resp);
          this.closeModalEvent.emit(true);
        }
      }
    );
  }
}
