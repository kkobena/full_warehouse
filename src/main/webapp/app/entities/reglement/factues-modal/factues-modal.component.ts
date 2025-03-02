import { AfterViewInit, Component, inject, input, signal, output } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Facture } from '../../facturation/facture.model';
import { LazyLoadEvent } from 'primeng/api';
import { FactureService } from '../../facturation/facture.service';
import { InvoiceSearchParams } from '../../facturation/edition-search-params.model';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { INVOICES_STATUT } from '../../../shared/constants/data-constants';
import { IGroupeTiersPayant } from '../../../shared/model/groupe-tierspayant.model';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { RegelementStateService } from '../regelement-state.service';
import { TableModule } from 'primeng/table';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { FloatLabelModule } from 'primeng/floatlabel';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { GroupeTiersPayantService } from '../../groupe-tiers-payant/groupe-tierspayant.service';
import { SelectedFacture } from '../model/reglement.model';
import { InputText } from 'primeng/inputtext';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Toolbar } from 'primeng/toolbar';
import { DatePicker } from 'primeng/datepicker';

@Component({
  selector: 'jhi-factues-modal',
  imports: [
    ButtonModule,
    TableModule,
    WarehouseCommonModule,
    FormsModule,
    FloatLabelModule,
    AutoCompleteModule,
    InputText,
    ToggleSwitch,
    Toolbar,
    DatePicker,
  ],

  templateUrl: './factues-modal.component.html',
})
export class FactuesModalComponent implements AfterViewInit {
  protected readonly factureService = inject(FactureService);

  protected readonly regelementStateService = inject(RegelementStateService);
  factureGroup = input<boolean>(false);
  factureGroupWritable = signal(this.factureGroup());
  protected readonly tiersPayantService = inject(TiersPayantService);
  protected readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  protected readonly selectedFacture = output<SelectedFacture>();
  protected loadingBtn = false;
  protected loading!: boolean;
  protected totalItems = 0;
  protected readonly itemsPerPage = 20;
  protected page = 0;
  protected datas: Facture[] = [];
  protected statut: string = null;
  protected readonly statuts = INVOICES_STATUT;
  private readonly toDate = new Date();
  protected modelStartDate: Date = null;
  protected modelEndDate: Date = new Date();
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected selectedGroupeTiersPayants: IGroupeTiersPayant[] | undefined;
  protected search = '';
  protected tiersPayants: ITiersPayant[] = [];
  protected selectedTiersPayants: ITiersPayant[] | undefined;
  protected minLength = 2;
  constructor() {
    this.toDate.setMonth(this.toDate.getMonth() - 1);
    this.modelStartDate = this.toDate;
  }

  searchTiersPayant(event: any): void {
    this.loadTiersPayants(event.query);
  }

  searchGroupTiersPayant(event: any): void {
    this.loadGroupTiersPayant(event.query);
  }

  loadGroupTiersPayant(search?: string): void {
    const query: string = search || '';
    this.groupeTiersPayantService
      .query({
        page: 0,
        search: query,
        size: 10,
      })
      .subscribe((res: HttpResponse<IGroupeTiersPayant[]>) => {
        this.groupeTiersPayants = res.body || [];
      });
  }

  loadTiersPayants(search?: string): void {
    const query: string = search || '';
    this.tiersPayantService
      .query({
        page: 0,
        search: query,
        size: 10,
      })
      .subscribe({
        next: (res: HttpResponse<ITiersPayant[]>) => {
          this.tiersPayants = res.body || [];
        },
      });
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.loadingBtn = true;
    this.factureService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...this.buildSearchParams(),
      })
      .subscribe({
        next: (res: HttpResponse<Facture[]>) => this.onSearchSuccess(res.body, res.headers, pageToLoad),
        error: (error: any) => this.onError(error),
      });
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.factureService
        .query({
          page: this.page,
          size: event.rows,
          ...this.buildSearchParams(),
        })
        .subscribe({
          next: (res: HttpResponse<Facture[]>) => this.onSearchSuccess(res.body, res.headers, this.page),
          error: (error: any) => this.onError(error),
        });
    }
  }

  onSearch(): void {
    this.loadPage();
  }

  onError(error: any): void {
    this.loading = false;
    this.loadingBtn = false;
  }

  onSelectFacture(facture: Facture): void {
    this.selectedFacture.emit({ isGroup: this.factureGroupWritable(), facture });
  }

  ngAfterViewInit(): void {
    const previousSearch = this.regelementStateService.invoiceSearchParams();

    if (previousSearch) {
      this.modelStartDate = previousSearch.startDate ? new Date(previousSearch.startDate) : new Date();
      this.modelEndDate = previousSearch.endDate ? new Date(previousSearch.endDate) : new Date();
      this.selectedTiersPayants = previousSearch.tiersPayantIds
        ? this.tiersPayants.filter(item => previousSearch.tiersPayantIds.includes(item.id))
        : undefined;
      this.selectedGroupeTiersPayants = previousSearch.groupIds
        ? this.groupeTiersPayants.filter(item => previousSearch.groupIds.includes(item.id))
        : undefined;
      this.search = previousSearch.search || '';
      this.factureGroupWritable.set(previousSearch.factureGroupees || false);
      if (this.search) {
        if (previousSearch.factureGroupees && previousSearch.factureGroupees) {
          this.loadGroupTiersPayant(this.search);
        } else {
          this.loadTiersPayants(this.search);
        }
      }
    } else {
      this.factureGroupWritable.set(this.factureGroup());
    }

    this.onSearch();
  }

  protected onSearchSuccess(data: Facture[] | null, headers: HttpHeaders, page: number): void {
    this.loading = false;
    this.loadingBtn = false;
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.datas = data || [];
  }

  private buildSearchParams(): InvoiceSearchParams {
    const params = {
      startDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      endDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
      groupIds: this.selectedGroupeTiersPayants?.map(item => item.id),
      tiersPayantIds: this.selectedTiersPayants?.map(item => item.id),
      factureProvisoire: false,
      search: this.search,
      statuts: ['PARTIALLY_PAID', 'NOT_PAID'],
      factureGroupees: this.factureGroupWritable(),
    };
    this.regelementStateService.setInvoiceSearchParams(params);
    return params;
  }
}
