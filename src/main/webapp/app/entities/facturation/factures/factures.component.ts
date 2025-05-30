import { AfterViewInit, Component, inject, OnInit } from '@angular/core';
import { ErrorService } from '../../../shared/error.service';
import { FactureService } from '../facture.service';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { GroupeTiersPayantService } from '../../groupe-tiers-payant/groupe-tierspayant.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmationService, LazyLoadEvent, MenuItem, PrimeIcons } from 'primeng/api';
import { ToolbarModule } from 'primeng/toolbar';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { IGroupeTiersPayant } from '../../../shared/model/groupe-tierspayant.model';
import { INVOICES_STATUT } from '../../../shared/constants/data-constants';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { ButtonGroupModule } from 'primeng/buttongroup';
import { ButtonModule } from 'primeng/button';
import { SplitButtonModule } from 'primeng/splitbutton';
import { FloatLabelModule } from 'primeng/floatlabel';
import { InvoiceSearchParams } from '../edition-search-params.model';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { Facture } from '../facture.model';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { TableModule } from 'primeng/table';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { FactureDetailDialogComponent } from '../facture-detail/facture-detail-dialog.component';
import { GroupeFactureDetailDialogComponent } from '../groupe-facture-detail/groupe-facture-detail-dialog.component';
import { FactureStateService } from '../facture-state.service';
import { Tooltip } from 'primeng/tooltip';
import { RouterLink } from '@angular/router';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { InputText } from 'primeng/inputtext';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { DatePicker } from 'primeng/datepicker';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';
import { Subscription } from 'rxjs';

@Component({
  selector: 'jhi-factures',
  providers: [ConfirmationService],
  imports: [
    ToolbarModule,
    WarehouseCommonModule,
    FormsModule,
    AutoCompleteModule,
    ButtonGroupModule,
    ButtonModule,
    SplitButtonModule,
    FloatLabelModule,
    TableModule,
    ConfirmDialogModule,
    ToolbarModule,
    Tooltip,
    RouterLink,
    InputText,
    ToggleSwitch,
    DatePicker,
  ],
  templateUrl: './factures.component.html',
  styles: ``,
})
export class FacturesComponent implements OnInit, AfterViewInit {
  public readonly errorService = inject(ErrorService);
  public readonly factureService = inject(FactureService);
  public readonly tiersPayantService = inject(TiersPayantService);
  public readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  public readonly modalService = inject(NgbModal);
  public readonly factureStateService = inject(FactureStateService);
  minLength = 2;
  public readonly confirmationService = inject(ConfirmationService);
  public readonly translate = inject(TranslateService);
  public readonly primeNGConfig = inject(PrimeNG);
  btnExports: MenuItem[];
  btnAction: MenuItem[];
  protected factureProvisoire = false;
  protected factureGroup = false;
  protected modelStartDate: Date = null;
  protected modelEndDate: Date = new Date();
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected search = '';
  protected statut: string = null;
  protected readonly statuts = INVOICES_STATUT;
  protected tiersPayants: ITiersPayant[] = [];
  protected selectedTiersPayants: ITiersPayant[] | undefined;
  protected selectedGroupeTiersPayants: IGroupeTiersPayant[] | undefined;
  protected totalItems = 0;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected page = 0;
  protected datas: Facture[] = [];
  protected loadingBtn = false;
  protected loading!: boolean;
  protected exporting = false;
  private primngtranslate: Subscription;
  private toDateMinusOneMonth: Date = null;
  constructor() {
    this.translate.use('fr');
    this.primngtranslate = this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    const toDate = new Date();
    toDate.setMonth(toDate.getMonth() - 1);
    this.toDateMinusOneMonth = toDate;
    this.modelStartDate = this.toDateMinusOneMonth;
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
    this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  ngOnInit(): void {
    this.btnExports = [
      {
        label: 'Pdf',
        icon: PrimeIcons.FILE_PDF,
      },
      {
        label: 'Excel',
        icon: PrimeIcons.FILE_EXCEL,
      },
    ];

    this.btnAction = [
      {
        label: 'Pdf',
        icon: PrimeIcons.FILE_PDF,
        command: () => this.exportPdf(1),
      },
      {
        label: 'Excel',
        icon: PrimeIcons.FILE_EXCEL,
      },
      {
        label: 'Word',
        icon: PrimeIcons.FILE_WORD,
      },
    ];
  }

  onDelete(id: number): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer cette facture ?',
      header: 'SUPPRESSION DE FACTURE ',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.factureService.delete(id).subscribe({
          next: () => {
            this.loadPage();
          },
          error: (err: any) => {
            this.openInfoDialog(this.errorService.getErrorMessage(err), 'alert alert-danger');
          },
        });
      },

      key: 'delete',
    });
  }

  exportPdf(id: number): void {
    this.exporting = true;
    this.factureService.exportToPdf(id).subscribe({
      next: blod => {
        this.exporting = false;
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
      },
      error: err => {
        this.exporting = false;
        this.openInfoDialog(this.errorService.getErrorMessage(err), 'alert alert-danger');
      },
    });
  }

  onOpenDetail(facture: Facture): void {
    const modalRef = this.modalService.open(FactureDetailDialogComponent, {
      backdrop: 'static',
      size: 'xl',
      centered: true,
      animation: true,
      modalDialogClass: 'facture-modal-dialog',
    });
    modalRef.componentInstance.facture = facture;
  }

  onOpenGroupeDetail(facture: Facture): void {
    const modalRef = this.modalService.open(GroupeFactureDetailDialogComponent, {
      backdrop: 'static',
      size: 'xl',
      centered: true,
      animation: true,
      modalDialogClass: 'facture-modal-dialog',
    });
    modalRef.componentInstance.facture = facture;
  }

  ngAfterViewInit(): void {
    const previousSearch = this.factureStateService.invoiceSearchParams();

    if (previousSearch) {
      this.modelStartDate = previousSearch.startDate ? new Date(previousSearch.startDate) : this.toDateMinusOneMonth;
      this.modelEndDate = previousSearch.endDate ? new Date(previousSearch.endDate) : new Date();
      this.selectedTiersPayants = previousSearch.tiersPayantIds
        ? this.tiersPayants.filter(item => previousSearch.tiersPayantIds.includes(item.id))
        : undefined;
      this.selectedGroupeTiersPayants = previousSearch.groupIds
        ? this.groupeTiersPayants.filter(item => previousSearch.groupIds.includes(item.id))
        : undefined;
      this.search = previousSearch.search || '';
      this.statut = previousSearch.statuts.length > 0 ? previousSearch.statuts[0] : null;
      this.factureProvisoire = previousSearch.factureProvisoire || false;
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
    let statuts: string[] = [];
    if (this.statut) {
      if (this.statut === 'PARTIALLY_PAID') {
        statuts = ['PARTIALLY_PAID', 'NOT_PAID'];
      } else {
        statuts = [this.statut];
      }
    }
    const params = {
      startDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      endDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
      groupIds: this.selectedGroupeTiersPayants?.map(item => item.id),
      tiersPayantIds: this.selectedTiersPayants?.map(item => item.id),
      factureProvisoire: this.factureProvisoire,
      search: this.search,
      statuts,
      factureGroupees: this.factureGroup,
    };
    this.factureStateService.setInvoiceSearchParams(params);
    return params;
  }
}
