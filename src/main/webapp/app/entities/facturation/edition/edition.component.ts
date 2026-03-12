import {Component, DestroyRef, inject, OnInit} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {WarehouseCommonModule} from '../../../shared/warehouse-common/warehouse-common.module';
import {FactureService} from '../facture.service';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {
  CATEGORIE_TIRERS_PAYANT,
  MODE_EDITIONS_FACTURE
} from '../../../shared/constants/data-constants';
import {TiersPayantService} from '../../tiers-payant/tierspayant.service';
import {GroupeTiersPayantService} from '../../groupe-tiers-payant/groupe-tierspayant.service';
import {HttpHeaders, HttpResponse} from '@angular/common/http';
import {IGroupeTiersPayant} from '../../../shared/model/groupe-tierspayant.model';
import {ITiersPayant} from '../../../shared/model';
import {TableLazyLoadEvent, TableModule} from 'primeng/table';
import {TiersPayantDossierFacture} from '../tiers-payant-dossier-facture.model';
import {DossierFacture} from '../dossier-facture.model';
import {InputTextModule} from 'primeng/inputtext';
import {TooltipModule} from 'primeng/tooltip';
import {EditionSearchParams} from '../edition-search-params.model';
import {DATE_FORMAT_ISO_DATE} from '../../../shared/util/warehouse-util';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {AutoCompleteModule} from 'primeng/autocomplete';
import {ErrorService} from '../../../shared/error.service';
import {AlertInfoComponent} from '../../../shared/alert/alert-info.component';
import {FactureEditionResponse} from '../facture-edition-response';
import {ButtonModule} from 'primeng/button';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {TranslateService} from '@ngx-translate/core';
import {PrimeNG} from 'primeng/config';
import {DatePicker} from 'primeng/datepicker';
import {Card} from 'primeng/card';
import {Select} from 'primeng/select';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {ConfirmationService} from 'primeng/api';
import {acceptButtonProps, rejectButtonProps} from '../../../shared/util/modal-button-props';
import {TauriPrinterService} from "../../../shared/services/tauri-printer.service";
import {handleBlobForTauri} from "../../../shared/util/tauri-util";
import {finalize} from "rxjs/operators";

@Component({
  selector: 'jhi-edition',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    TableModule,
    InputTextModule,
    TooltipModule,
    AutoCompleteModule,
    ButtonModule,
    ToggleSwitch,
    IconField,
    InputIcon,
    DatePicker,
    Card,
    Select,
    ConfirmDialog,
  ],
  templateUrl: './edition.component.html',
  styleUrls: ['./edition.component.scss'],
  providers: [ConfirmationService],
})
export class EditionComponent implements OnInit {
  protected minLength = 2;
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected selectedGroupeTiersPayants: IGroupeTiersPayant[] | undefined;
  protected tiersPayants: ITiersPayant[] = [];
  protected selectedTiersPayants: ITiersPayant[] | undefined;
  protected ids: number[] = [];
  protected all = false;
  protected factureProvisoire = false;
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
  protected modelStartDate: Date = new Date();
  protected modelEndDate: Date = new Date();
  protected searching = false;
  protected editing = false;
  protected loading!: boolean;
  protected exporting = false;
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly errorService = inject(ErrorService);
  private readonly factureService = inject(FactureService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly modalService = inject(NgbModal);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly tauriPrinter = inject(TauriPrinterService);

  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng')
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: data => this.primeNGConfig.setTranslation(data),
      });
  }

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
      .query({page: 0, search: query, size: 10})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: HttpResponse<IGroupeTiersPayant[]>) => {
          this.groupeTiersPayants = res.body || [];
        },
        error: (err: any) => this.onError(err),
      });
  }

  loadTiersPayants(search?: string): void {
    const query: string = search || '';
    this.tiersPayantService
      .query({page: 0, search: query, size: 10})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: HttpResponse<ITiersPayant[]>) => {
          this.tiersPayants = res.body || [];
        },
        error: (err: any) => this.onError(err),
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
    this.factureService.editInvoices(this.buildEditionParams())
      .pipe(
        finalize(() => (this.editing = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res: HttpResponse<FactureEditionResponse>) => {
          if (res.body) {
            this.onPrintAllInvoices(res.body);
          }
        },
        error: (err: any) => this.onError(err),
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
      .pipe(
        finalize(() => (this.searching = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res: HttpResponse<DossierFacture[]>) => this.onSearchBonSuccess(res.body, res.headers, pageToLoad),
        error: (err: any) => this.onError(err),
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
      .pipe(
        finalize(() => {
          this.loading = false;
          this.searching = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res: HttpResponse<TiersPayantDossierFacture[]>) => this.onSearchSuccess(res.body, res.headers, pageToLoad),
        error: (err: any) => this.onError(err),
      });
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.pageTp = event.first / event.rows;
      this.loading = true;
      this.factureService
        .queryEditionData({
          page: this.pageTp,
          size: event.rows,
          ...this.buildSearchParams(),
        })
        .pipe(
          finalize(() => (this.loading = false)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: (res: HttpResponse<TiersPayantDossierFacture[]>) => this.onSearchSuccess(res.body, res.headers, this.pageTp),
          error: (err: any) => this.onError(err),
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
      startDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      endDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
      groupIds: this.selectedGroupeTiersPayants?.map(item => item.id),
      tiersPayantIds: this.selectedTiersPayants?.map(item => item.id),
      all: this.all,
      categorieTiersPayants: this.typeTiersPayant ? [this.typeTiersPayant] : [],
      factureProvisoire: this.factureProvisoire,
      modeEdition: this.modeEdition ? this.modeEdition : 'ALL',
    };
  }

  private buildEditionParams(): EditionSearchParams {
    let selectedIds: number[] = [];
    if (this.modeEdition === 'SELECTION_BON' || this.modeEdition === 'SELECTED' || this.modeEdition === 'GROUP') {
      if (this.modeEdition === 'SELECTION_BON') {
        selectedIds = this.selectedDossiers?.map(item => item.id);
      } else {
        selectedIds = this.selectedTiersPayantDossiers?.map(item => item.id);
      }
    }
    return {
      ...this.buildSearchParams(),
      ids: selectedIds,
    };
  }

  private onPrintAllInvoices(response: FactureEditionResponse): void {
    this.confirmationService.confirm({
      message: 'Êtes-vous sûr de vouloir imprimer les factures ?',
      header: ' IMPRESSION DE FACTURE',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.exporting = true;
        this.factureService.exportAllInvoices(response)
          .pipe(
            finalize(() => (this.exporting = false)),
            takeUntilDestroyed(this.destroyRef),
          )
          .subscribe({
            next: (res: Blob) => {
              if (this.tauriPrinter.isRunningInTauri()) {
                handleBlobForTauri(res, `factures_${new Date().getTime()}`);
              } else {
                window.open(URL.createObjectURL(res));
              }
            },
            error: (err: any) => this.onError(err),
          });
        this.resetForm();
      },
      reject: () => {
        this.resetForm();
      },
    });
  }
}
