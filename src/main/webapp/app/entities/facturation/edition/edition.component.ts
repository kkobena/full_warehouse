import { Component, inject, OnInit } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FactureService } from '../facture.service';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CATEGORIE_TIRERS_PAYANT, MODE_EDITIONS_FACTURE } from '../../../shared/constants/data-constants';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { GroupeTiersPayantService } from '../../groupe-tiers-payant/groupe-tierspayant.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { IGroupeTiersPayant } from '../../../shared/model/groupe-tierspayant.model';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { TiersPayantDossierFacture } from '../tiers-payant-dossier-facture.model';
import { DossierFacture } from '../dossier-facture.model';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { EditionSearchParams } from '../edition-search-params.model';
import { DATE_FORMAT_ISO_FROM_NGB_DATE } from '../../../shared/util/warehouse-util';
import { StyleClassModule } from 'primeng/styleclass';
import {
  NgbCalendar,
  NgbDate,
  NgbDateAdapter,
  NgbDateParserFormatter,
  NgbDatepickerI18n,
  NgbDatepickerModule,
  NgbModal,
} from '@ng-bootstrap/ng-bootstrap';
import { CustomAdapter, CustomDateParserFormatter, CustomDatepickerI18n, I18n } from '../../../shared/util/datepicker-adapter';
import { InputSwitchModule } from 'primeng/inputswitch';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ErrorService } from '../../../shared/error.service';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, LazyLoadEvent } from 'primeng/api';
import { FactureEditionResponse } from '../facture-edition-response';
import { ButtonModule } from 'primeng/button';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';

@Component({
  selector: 'jhi-edition',
  providers: [
    ConfirmationService,
    I18n,
    { provide: NgbDatepickerI18n, useClass: CustomDatepickerI18n },
    { provide: NgbDateAdapter, useClass: CustomAdapter },
    { provide: NgbDateParserFormatter, useClass: CustomDateParserFormatter },
  ],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    DropdownModule,
    ReactiveFormsModule,
    TableModule,
    InputTextModule,
    TooltipModule,
    StyleClassModule,
    NgbDatepickerModule,
    InputSwitchModule,
    AutoCompleteModule,
    ConfirmDialogModule,
    ButtonModule,
  ],
  templateUrl: './edition.component.html',
  styles: ``,
})
export class EditionComponent implements OnInit {
  errorService = inject(ErrorService);
  factureService = inject(FactureService);
  tiersPayantService = inject(TiersPayantService);
  groupeTiersPayantService = inject(GroupeTiersPayantService);
  calendar = inject(NgbCalendar);
  modalService = inject(NgbModal);
  minLength = 2;
  confirmationService = inject(ConfirmationService);
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected selectedGroupeTiersPayants: IGroupeTiersPayant[] | undefined;
  protected tiersPayants: ITiersPayant[] = [];
  protected selectedTiersPayants: ITiersPayant[] | undefined;
  protected ids: number[] = [];
  protected all: boolean = false;
  protected factureProvisoire: boolean = false;
  protected modeEdition: string;
  protected typeTiersPayant: string;
  protected modeEditions = MODE_EDITIONS_FACTURE;
  protected typeTiersPayants = CATEGORIE_TIRERS_PAYANT;
  protected tiersPayantDossierFactures: TiersPayantDossierFacture[] = [];
  protected dossierFactures: DossierFacture[] = [];
  protected selectedDossiers: DossierFacture[] = [];
  protected selectedTiersPayantDossiers: TiersPayantDossierFacture[] = [];
  protected totalItems = 0;
  protected readonly itemsPerPage = 15;
  protected page = 0;
  protected totalItemsTp = 0;
  protected pageTp = 0;
  protected modelStartDate: NgbDate | null = this.calendar.getToday();
  protected modelEndDate: NgbDate | null = this.calendar.getToday();
  protected searching = false;
  protected editing = false;
  protected loading!: boolean;
  protected exporting = false;

  constructor() {}

  ngOnInit(): void {
    this.loadGroupTiersPayant();
    this.loadTiersPayants();
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

  onModeEditionChange(): void {
    this.resetForm();
  }

  onSearch(): void {
    if (this.modeEdition === 'SELECTION_BON') {
      this.loadBons();
    } else {
      this.load();
    }
  }

  onEdit(): void {
    this.editing = true;
    this.factureService.editInvoices(this.buildEditionParams()).subscribe({
      next: (res: HttpResponse<FactureEditionResponse>) => {
        this.editing = false;
        if (res.body) {
          this.onPrintAllInvoices(res.body);
        }
      },
      complete: () => (this.editing = false),
      error: (error: any) => this.onError(error),
    });
  }

  onError(error: any): void {
    this.editing = false;

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

  loadBons(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.searching = true;
    this.factureService
      .queryBons({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...this.buildSearchParams(),
      })
      .subscribe({
        next: (res: HttpResponse<DossierFacture[]>) => this.onSearchBonSuccess(res.body, res.headers, pageToLoad),
        complete: () => (this.searching = false),
        error: () => (this.searching = false),
      });
  }

  load(page?: number): void {
    const pageToLoad: number = page || this.pageTp;
    this.loading = true;
    this.searching = true;
    this.factureService
      .queryEditionData({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...this.buildSearchParams(),
      })
      .subscribe({
        next: (res: HttpResponse<TiersPayantDossierFacture[]>) => this.onSearchSuccess(res.body, res.headers, pageToLoad),
        complete: () => {
          this.loading = false;
          this.searching = false;
        },
        error: () => {
          this.loading = false;
          this.searching = false;
        },
      });
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.pageTp = event.first / event.rows;
      this.loading = true;
      this.factureService
        .queryEditionData({
          page: this.pageTp,
          size: event.rows,
          ...this.buildSearchParams(),
        })
        .subscribe({
          next: (res: HttpResponse<TiersPayantDossierFacture[]>) => this.onSearchSuccess(res.body, res.headers, this.pageTp),
          error: () => {
            this.loading = false;
            this.searching = false;
          },
          complete: () => {
            this.loading = false;
            this.searching = false;
          },
        });
    }
  }

  protected onSearchBonSuccess(data: DossierFacture[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.dossierFactures = data || [];
  }

  protected onSearchSuccess(data: TiersPayantDossierFacture[] | null, headers: HttpHeaders, page: number): void {
    this.totalItemsTp = Number(headers.get('X-Total-Count'));
    this.pageTp = page;
    this.tiersPayantDossierFactures = data || [];
  }

  private resetForm(): void {
    this.tiersPayantDossierFactures = [];
    this.typeTiersPayant = null;
    this.selectedGroupeTiersPayants = [];
    this.selectedTiersPayants = [];
    this.dossierFactures = [];
    this.selectedTiersPayantDossiers = [];
  }

  private buildSearchParams(): EditionSearchParams {
    return {
      startDate: DATE_FORMAT_ISO_FROM_NGB_DATE(this.modelStartDate),
      endDate: DATE_FORMAT_ISO_FROM_NGB_DATE(this.modelEndDate),
      groupIds: this.selectedGroupeTiersPayants?.map(item => item.id),
      tiersPayantIds: this.selectedTiersPayants?.map(item => item.id),
      all: this.all,
      categorieTiersPayants: [this.typeTiersPayant],
      factureProvisoire: this.factureProvisoire,
      modeEdition: this.modeEdition ? this.modeEdition : 'ALL',
    };
  }

  private buildEditionParams(): EditionSearchParams {
    let selectedIds: number[] = [];
    if (this.modeEdition === 'SELECTION_BON' || this.modeEdition === 'SELECTED' || this.modeEdition === 'GROUP') {
      if (this.modeEdition === 'SELECTION_BON') {
        selectedIds = this.selectedDossiers.map(item => item.id);
      } else {
        selectedIds = this.selectedTiersPayantDossiers.map(item => item.id);
      }
    }
    return {
      ...this.buildSearchParams(),
      ids: selectedIds,
    };
  }

  private onPrintAllInvoices(response: FactureEditionResponse): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous imprimer les factures ?',
      header: 'IMPRESSION DE FACTURE ',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.exporting = true;
        this.factureService.exportAllInvoices(response).subscribe({
          next: (res: Blob) => {
            this.exporting = false;
            //const file = new Blob([res], { type: 'application/pdf' });
            const fileURL = URL.createObjectURL(res);
            window.open(fileURL);
          },
          error: (err: any) => {
            this.exporting = false;
            this.openInfoDialog(this.errorService.getErrorMessage(err), 'alert alert-danger');
          },
        });
        this.resetForm();
      },
      reject: () => {
        this.resetForm();
      },
      key: 'printialog',
    });
  }
}
