import { Component, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { CommonModule, DatePipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpHeaders } from "@angular/common/http";
import { Button } from "primeng/button";
import { TableLazyLoadEvent, TableModule } from "primeng/table";
import { Toolbar } from "primeng/toolbar";
import { Select } from "primeng/select";
import { DatePicker } from "primeng/datepicker";
import { InputText } from "primeng/inputtext";
import { TooltipModule } from "primeng/tooltip";
import { FloatLabel } from "primeng/floatlabel";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";

import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";
import { ISales } from "../../../../shared/model";
import { IUser } from "../../../../core/user/user.model";
import { UserService } from "../../../../core/user/user.service";
import { SalesApiService } from "../../data-access/services/sales-api.service";
import { SalesStatut } from "../../models/enumerations/sales-statut.enum";

@Component({
  selector: "app-sales-annulations",
  templateUrl: "./sales-annulations.component.html",
  styleUrl: "./sales-annulations.component.scss",
  providers: [DatePipe],
  imports: [
    CommonModule,
    FormsModule,
    Button,
    TableModule,
    Toolbar,
    Select,
    DatePicker,
    InputText,
    TooltipModule,
    FloatLabel,
    IconField,
    InputIcon
  ]
})
export class SalesAnnulationsComponent implements OnInit {
  private readonly api = inject(SalesApiService);
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly datePipe = inject(DatePipe);

  protected readonly SalesStatut = SalesStatut;
  protected loading = signal(false);
  protected sales: ISales[] = [];
  protected users: IUser[] = [];
  protected totalItems = 0;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;

  protected search = "";
  protected selectedUserId: number | null = null;
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected readonly totalCanceledAmount = signal(0);


  ngOnInit(): void {
    this.loadAllUsers();
    this.loadPage();
  }

  private loadAllUsers(): void {
    this.userService.query().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(res => {
      this.users = res.body || [];
    });
  }

  protected loadPage(page?: number): void {
    const pageToLoad = page ?? this.page;
    const params = {
      statuts: [SalesStatut.CANCELED],
      search: this.search || null,
      fromDate: this.datePipe.transform(this.fromDate, "yyyy-MM-dd"),
      toDate: this.datePipe.transform(this.toDate, "yyyy-MM-dd"),
      userId: this.selectedUserId
    };
    this.loading.set(true);
    this.api.querySales({
      page: pageToLoad,
      size: this.itemsPerPage,
      ...params
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          this.onSuccess(res.body, res.headers, pageToLoad);
        },
        error: () => this.loading.set(false)
      });
    this.api
      .querySalesTotalAmount(params)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: total => this.totalCanceledAmount.set(total),
        error: () => {
        }
      });
  }

  private onSuccess(data: ISales[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get("X-Total-Count"));
    this.page = page;
    this.sales = data || [];
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event.first != null && event.rows != null) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.loadPage(this.page);
    }
  }

  protected onSearch(): void {
    this.loadPage(0);
  }
}
