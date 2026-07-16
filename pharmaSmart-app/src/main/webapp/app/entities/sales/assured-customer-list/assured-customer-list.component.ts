import { Component, inject, OnDestroy, OnInit } from "@angular/core";
import { ICustomer, ISales } from "../../../shared/model";
import { CustomerService } from "../../customer/customer.service";
import { FormsModule } from "@angular/forms";
import { ButtonModule } from "primeng/button";
import { InputTextModule } from "primeng/inputtext";
import { RippleModule } from "primeng/ripple";
import { TableLazyLoadEvent, TableModule } from "primeng/table";
import { TooltipModule } from "primeng/tooltip";
import { HttpHeaders, HttpResponse } from "@angular/common/http";
import { ITEMS_PER_PAGE } from "../../../shared/constants/pagination.constants";
import { CurrentSaleService } from "../service/current-sale.service";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { Card } from "primeng/card";
import { Subject } from "rxjs";
import { debounceTime, distinctUntilChanged, takeUntil } from "rxjs/operators";
import { CommonModule } from "@angular/common";

@Component({
  selector: "app-assured-customer-list",
  templateUrl: "./assured-customer-list.component.html",
  styleUrls: ["./assured-customer-list.component.scss"],
  imports: [
    CommonModule,
    FormsModule,
    TooltipModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    TableModule,
    IconField,
    InputIcon,
    Card
  ]
})
export class AssuredCustomerListComponent implements OnInit, OnDestroy {
  customers: ICustomer[] = [];
  searchString?: string | null = "";
  headerLibelle: string;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected ngbPaginationPage = 1;
  protected totalItems = 0;
  protected loading!: boolean;
  protected selectedCustomer: ICustomer | null = null;
  private readonly currentSaleService = inject(CurrentSaleService);
  private readonly customerService = inject(CustomerService);
  private readonly activeModal = inject(NgbActiveModal);
  private searchSubject$ = new Subject<string>();
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Debounce search input
    this.searchSubject$.pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe(() => {
      this.loadPage(1);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected onSearchInput(): void {
    this.searchSubject$.next(this.searchString || "");
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === "Escape") {
      this.cancel();
    } else if (event.key === "Enter" && this.selectedCustomer) {
      this.onSelect(this.selectedCustomer);
    }
  }

  protected onRowSelect(customer: ICustomer): void {
    this.selectedCustomer = customer;
  }

  protected onDbleClick(customer: ICustomer): void {
    this.onSelect(customer);
  }

  protected onSelect(customer: ICustomer): void {
    this.activeModal.close(customer);
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected loadPage(page?: number): void {
    const pageToLoad: number = page || this.page || 1;
    this.customerService
      .queryAssuredCustomer({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        search: this.searchString,
        typeTiersPayant: this.currentSaleService.typeVo()
      })
      .subscribe({
        next: (res: HttpResponse<ICustomer[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError()
      });
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.customerService
        .queryAssuredCustomer({
          page: this.page,
          size: event.rows,
          search: this.searchString,
          typeTiersPayant: this.currentSaleService.typeVo()
        })
        .subscribe({
          next: (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError()
        });
    }
  }

  private onSuccess(data: ICustomer[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get("X-Total-Count"));

    this.page = page;
    this.customers = data || [];
    this.ngbPaginationPage = this.page;
    this.loading = false;
  }

  private onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
    this.loading = false;
  }
}
