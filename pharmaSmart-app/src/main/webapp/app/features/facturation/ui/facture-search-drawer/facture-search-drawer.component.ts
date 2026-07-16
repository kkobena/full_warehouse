import { AfterViewInit, Component, DestroyRef, inject, input, output, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { HttpHeaders, HttpResponse } from "@angular/common/http";
import { FormsModule } from "@angular/forms";
import { TableLazyLoadEvent, TableModule } from "primeng/table";
import { ButtonModule } from "primeng/button";
import { AutoCompleteModule } from "primeng/autocomplete";
import { FloatLabelModule } from "primeng/floatlabel";
import { InputText } from "primeng/inputtext";
import { ToggleSwitch } from "primeng/toggleswitch";
import { Toolbar } from "primeng/toolbar";
import { DatePicker } from "primeng/datepicker";
import { INVOICES_STATUT } from "../../../../shared/constants/data-constants";
import { IGroupeTiersPayant } from "../../../../shared/model/groupe-tierspayant.model";
import { ITiersPayant } from "../../../../shared/model";
import { TiersPayantService } from "../../../../entities/tiers-payant/tierspayant.service";
import { GroupeTiersPayantService } from "../../../../entities/groupe-tiers-payant/groupe-tierspayant.service";
import { DATE_FORMAT_ISO_DATE } from "../../../../shared/util/warehouse-util";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";

import { FactureApiService } from "../../data-access/services/facture-api.service";
import { FacturationStore } from "../../data-access/store/facturation.store";
import { IFacture, IInvoiceSearchParams, ISelectedFacture } from "../../data-access/models";
import { CommonModule } from "@angular/common";

@Component({
  selector: "app-facture-search-drawer",
  imports: [
    ButtonModule,
    TableModule,
    CommonModule,
    FormsModule,
    FloatLabelModule,
    AutoCompleteModule,
    InputText,
    ToggleSwitch,
    Toolbar,
    DatePicker
  ],
  templateUrl: "./facture-search-drawer.component.html"
})
export class FactureSearchDrawerComponent implements AfterViewInit {
  readonly factureGroup = input<boolean>(false);
  readonly selectedFacture = output<ISelectedFacture>();

  protected factureGroupWritable = signal(this.factureGroup());
  protected loading = false;
  protected loadingBtn = false;
  protected totalItems = 0;
  protected page = 0;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected datas: IFacture[] = [];
  protected statut: string = null;
  protected readonly statuts = INVOICES_STATUT;
  protected modelStartDate: Date;
  protected modelEndDate: Date = new Date();
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected selectedGroupeTiersPayants: IGroupeTiersPayant[] | undefined;
  protected search = "";
  protected tiersPayants: ITiersPayant[] = [];
  protected selectedTiersPayants: ITiersPayant[] | undefined;
  protected readonly minLength = 2;

  private readonly factureApiService = inject(FactureApiService);
  private readonly facturationStore = inject(FacturationStore);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate = d;
  }

  ngAfterViewInit(): void {
    const previousSearch = this.facturationStore.searchParams();
    if (previousSearch) {
      this.modelStartDate = previousSearch.startDate ? new Date(previousSearch.startDate) : this.modelStartDate;
      this.modelEndDate = previousSearch.endDate ? new Date(previousSearch.endDate) : new Date();
      this.search = previousSearch.search ?? "";
      this.factureGroupWritable.set(previousSearch.factureGroupees ?? false);
    } else {
      this.factureGroupWritable.set(this.factureGroup());
    }
  }

  onSearch(): void {
    this.page = 0;
    this.loadPage();
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loadPage(event.rows);
    }
  }

  onSelectFacture(facture: IFacture): void {
    this.selectedFacture.emit({ isGroup: this.factureGroupWritable(), facture: facture as any });
  }

  searchTiersPayant(event: any): void {
    this.loadTiersPayants(event.query);
  }

  searchGroupTiersPayant(event: any): void {
    this.loadGroupTiersPayant(event.query);
  }

  loadGroupTiersPayant(search = ""): void {
    this.groupeTiersPayantService
      .query({ page: 0, search, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<IGroupeTiersPayant[]>) => {
        this.groupeTiersPayants = res.body ?? [];
      });
  }

  loadTiersPayants(search = ""): void {
    this.tiersPayantService
      .query({ page: 0, search, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<ITiersPayant[]>) => {
        this.tiersPayants = res.body ?? [];
      });
  }

  private loadPage(rows = this.itemsPerPage): void {
    this.loading = true;
    this.loadingBtn = true;
    const params = this.buildSearchParams();
    this.factureApiService
      .query({ ...params, page: this.page, size: rows } as any)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: HttpResponse<IFacture[]>) => this.onSuccess(res.body, res.headers, this.page),
        error: () => {
          this.loading = false;
          this.loadingBtn = false;
        }
      });
  }

  private onSuccess(data: IFacture[] | null, headers: HttpHeaders, page: number): void {
    this.loading = false;
    this.loadingBtn = false;
    this.totalItems = Number(headers.get("X-Total-Count"));
    this.page = page;
    this.datas = data ?? [];
  }

  private buildSearchParams(): IInvoiceSearchParams {
    const params: IInvoiceSearchParams = {
      startDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      endDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
      groupIds: this.selectedGroupeTiersPayants?.map(item => item.id),
      tiersPayantIds: this.selectedTiersPayants?.map(item => item.id),
      factureProvisoire: false,
      search: this.search,
      statuts: ["PARTIALLY_PAID", "NOT_PAID"],
      factureGroupees: this.factureGroupWritable()
    };
    this.facturationStore.setSearchParams(params);
    return params;
  }
}
