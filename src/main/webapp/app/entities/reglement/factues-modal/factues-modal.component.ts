import { AfterViewInit, Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Facture } from '../../facturation/facture.model';
import { LazyLoadEvent } from 'primeng/api';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { FactureService } from '../../facturation/facture.service';
import { InvoiceSearchParams } from '../../facturation/edition-search-params.model';
import { DATE_FORMAT_ISO_FROM_NGB_DATE, GET_NG_DATE } from '../../../shared/util/warehouse-util';
import { INVOICES_STATUT } from '../../../shared/constants/data-constants';
import { NgbCalendar, NgbDate, NgbDateAdapter, NgbDateParserFormatter, NgbDatepickerI18n } from '@ng-bootstrap/ng-bootstrap';
import { IGroupeTiersPayant } from '../../../shared/model/groupe-tierspayant.model';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { RegelementStateService } from '../regelement-state.service';
import { TableModule } from 'primeng/table';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToolbarModule } from 'primeng/toolbar';
import { InputSwitchModule } from 'primeng/inputswitch';
import { FormsModule } from '@angular/forms';
import { FloatLabelModule } from 'primeng/floatlabel';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { GroupeTiersPayantService } from '../../groupe-tiers-payant/groupe-tierspayant.service';
import { CustomAdapter, CustomDateParserFormatter, CustomDatepickerI18n, I18n } from '../../../shared/util/datepicker-adapter';
import { SelectedFacture } from '../model/reglement.model';

@Component({
  selector: 'jhi-factues-modal',
  standalone: true,
  imports: [
    ButtonModule,
    TableModule,
    WarehouseCommonModule,
    ToolbarModule,
    InputSwitchModule,
    FormsModule,
    FloatLabelModule,
    AutoCompleteModule,
  ],
  providers: [
    I18n,
    { provide: NgbDatepickerI18n, useClass: CustomDatepickerI18n },
    { provide: NgbDateAdapter, useClass: CustomAdapter },
    { provide: NgbDateParserFormatter, useClass: CustomDateParserFormatter },
  ],
  templateUrl: './factues-modal.component.html',
})
export class FactuesModalComponent implements AfterViewInit {
  factureService = inject(FactureService);
  calendar = inject(NgbCalendar);
  regelementStateService = inject(RegelementStateService);
  @Input() factureGroup: boolean = false;
  tiersPayantService = inject(TiersPayantService);
  groupeTiersPayantService = inject(GroupeTiersPayantService);
  @Output() selectedFacture = new EventEmitter<SelectedFacture>();
  protected loadingBtn = false;
  protected loading!: boolean;
  protected totalItems = 0;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected datas: Facture[] = [];
  protected statut: string = null;
  protected readonly statuts = INVOICES_STATUT;

  protected modelStartDate: NgbDate | null = NgbDate.from({
    year: this.calendar.getToday().month === 1 ? this.calendar.getToday().year - 1 : this.calendar.getToday().year,
    month: this.calendar.getToday().month === 1 ? 12 : this.calendar.getToday().month - 1,
    day: this.calendar.getToday().day,
  });
  protected modelEndDate: NgbDate | null = this.calendar.getToday();
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected selectedGroupeTiersPayants: IGroupeTiersPayant[] | undefined;
  protected search: string = '';
  protected tiersPayants: ITiersPayant[] = [];
  protected selectedTiersPayants: ITiersPayant[] | undefined;
  protected minLength = 2;

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
    this.selectedFacture.emit({ isGroup: this.factureGroup, facture: facture });
  }

  ngAfterViewInit(): void {
    const previousSearch = this.regelementStateService.invoiceSearchParams();

    if (previousSearch) {
      this.modelStartDate = previousSearch.startDate ? GET_NG_DATE(previousSearch.startDate) : null;
      this.modelEndDate = previousSearch.endDate ? GET_NG_DATE(previousSearch.endDate) : null;
      this.selectedTiersPayants = previousSearch.tiersPayantIds
        ? this.tiersPayants.filter(item => previousSearch.tiersPayantIds.includes(item.id))
        : undefined;
      this.selectedGroupeTiersPayants = previousSearch.groupIds
        ? this.groupeTiersPayants.filter(item => previousSearch.groupIds.includes(item.id))
        : undefined;
      this.search = previousSearch.search || '';
      this.factureGroup = previousSearch.factureGroupees || false;
      if (this.search) {
        if (this.factureGroup) {
          this.loadGroupTiersPayant(this.search);
        } else {
          this.loadTiersPayants(this.search);
        }
      }
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
      startDate: DATE_FORMAT_ISO_FROM_NGB_DATE(this.modelStartDate),
      endDate: DATE_FORMAT_ISO_FROM_NGB_DATE(this.modelEndDate),
      groupIds: this.selectedGroupeTiersPayants?.map(item => item.id),
      tiersPayantIds: this.selectedTiersPayants?.map(item => item.id),
      factureProvisoire: false,
      search: this.search,
      statuts: ['PARTIALLY_PAID', 'NOT_PAID'],
      factureGroupees: this.factureGroup,
    };
    this.regelementStateService.setInvoiceSearchParams(params);
    return params;
  }
}
