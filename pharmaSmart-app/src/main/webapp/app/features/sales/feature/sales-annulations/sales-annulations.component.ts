import { Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy, input} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpHeaders } from "@angular/common/http";
import { NgbDateParserFormatter, NgbDateStruct, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";

import { FrenchDateParserFormatter } from "../../../../config/french-date-parser-formatter";
import { PharmaDatePickerComponent } from "../../../../shared/date-picker/pharma-date-picker.component";
import { ButtonComponent, DataTableComponent, IconFieldComponent, AppTableLazyLoadEvent, SelectComponent, ToolbarComponent } from "../../../../shared/ui";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";
import { ISales } from "../../../../shared/model";
import { IUser } from "../../../../core/user/user.model";
import { UserService } from "../../../../core/user/user.service";
import { SalesApiService } from "../../data-access/services/sales-api.service";
import { SalesStatut } from "../../models/enumerations/sales-statut.enum";

import { DeviseDirective } from 'app/shared/utils/devise';
@Component({
  selector: "app-sales-annulations",
  templateUrl: "./sales-annulations.component.html",
  styleUrl: "./sales-annulations.component.scss",
  providers: [{ provide: NgbDateParserFormatter, useClass: FrenchDateParserFormatter }],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DeviseDirective, 
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    ToolbarComponent,
    SelectComponent,
    PharmaDatePickerComponent,
    IconFieldComponent,
    NgbTooltip
  ]
})
export class SalesAnnulationsComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement. Un écran atteint depuis deux menus affiche donc le nom
   * de celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  private readonly api = inject(SalesApiService);
  private readonly userService = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);

  protected loading = signal(false);
  protected readonly sales = signal<ISales[]>([]);
  protected readonly users = signal<IUser[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly page = signal(0);
  protected readonly itemsPerPage = signal(ITEMS_PER_PAGE);

  protected search = "";
  protected selectedUserId: number | null = null;
  protected fromDate: NgbDateStruct = this.todayNgb();
  protected toDate: NgbDateStruct = this.todayNgb();
  protected readonly totalCanceledAmount = signal(0);


  ngOnInit(): void {
    this.loadAllUsers();
    this.loadPage();
  }

  private loadAllUsers(): void {
    this.userService.query().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(res => {
      this.users.set(res.body || []);
    });
  }

  protected loadPage(page?: number): void {
    const pageToLoad = page ?? this.page();
    const params = {
      statuts: [SalesStatut.CANCELED],
      search: this.search || null,
      fromDate: this.ngbDateToIso(this.fromDate),
      toDate: this.ngbDateToIso(this.toDate),
      userId: this.selectedUserId
    };
    this.loading.set(true);
    this.api.querySales({
      page: pageToLoad,
      size: this.itemsPerPage(),
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
    this.totalItems.set(Number(headers.get("X-Total-Count")));
    this.page.set(page);
    this.sales.set(data || []);
  }

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event.first != null && event.rows != null) {
      this.page.set(event.first / event.rows);
      this.itemsPerPage.set(event.rows);
      this.loadPage(this.page());
    }
  }

  protected onSearch(): void {
    this.loadPage(0);
  }

  private todayNgb(): NgbDateStruct {
    const d = new Date();
    return { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
  }

  private ngbDateToIso(date: NgbDateStruct | null): string | null {
    if (!date) return null;
    return `${date.year}-${String(date.month).padStart(2, "0")}-${String(date.day).padStart(2, "0")}`;
  }
}
