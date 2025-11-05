import { AfterViewInit, Component, inject, viewChild } from '@angular/core';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { FloatLabelModule } from 'primeng/floatlabel';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ToolbarModule } from 'primeng/toolbar';
import { TableModule } from 'primeng/table';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { IGroupeTiersPayant } from '../../../shared/model/groupe-tierspayant.model';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { HttpResponse } from '@angular/common/http';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { GroupeTiersPayantService } from '../../groupe-tiers-payant/groupe-tierspayant.service';
import { InputTextModule } from 'primeng/inputtext';
import { InvoicePaymentParam, Reglement } from '../model/reglement.model';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { RegelementStateService } from '../regelement-state.service';
import { DividerModule } from 'primeng/divider';
import { RippleModule } from 'primeng/ripple';
import { ReglementService } from '../reglement.service';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { ErrorService } from '../../../shared/error.service';
import { DetailSingleReglementComponent } from '../detail-single-reglement/detail-single-reglement.component';
import { DetailGroupReglementComponent } from '../detail-group-reglement/detail-group-reglement.component';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { DatePicker } from 'primeng/datepicker';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { TauriPrinterService } from '../../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../../shared/util/tauri-util';
import { RecapParam } from '../../ticketZ/model/recap-param.model';

@Component({
  selector: 'jhi-factures-reglees',
  imports: [
    ButtonModule,
    TableModule,
    WarehouseCommonModule,
    ToolbarModule,
    FormsModule,
    FloatLabelModule,
    AutoCompleteModule,
    InputTextModule,
    DividerModule,
    RippleModule,
    IconField,
    InputIcon,
    ToggleSwitch,
    DatePicker,
    ConfirmDialogComponent,
  ],

  templateUrl: './factures-reglees.component.html',
  styleUrl: './factures-reglees.component.scss',
})
export class FacturesRegleesComponent implements AfterViewInit {
  protected expandedRows = {};
  protected readonly translate = inject(TranslateService);
  protected readonly primeNGConfig = inject(PrimeNG);
  protected today = new Date();
  protected modelStartDate: Date = null;
  protected modelEndDate: Date = new Date();
  protected search: string | null = null;
  protected factureGroup = false;
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected selectedGroupeTiersPayant: IGroupeTiersPayant | undefined;
  protected tiersPayants: ITiersPayant[] = [];
  protected selectedTiersPayant: ITiersPayant | undefined;
  protected minLength = 2;
  protected loadingBtn = false;
  protected loadingPdf = false;
  protected removeAll = false;
  protected datas: Reglement[] = [];
  protected selectedDatas: Reglement[] = [];
  protected scrollHeight = 'calc(100vh - 350px)';
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private toDateMinus1Month = this.today;
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly regelementStateService = inject(RegelementStateService);
  private readonly reglementService = inject(ReglementService);
  private readonly errorService = inject(ErrorService);
  private readonly modalService = inject(NgbModal);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  constructor() {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });

    this.toDateMinus1Month.setMonth(this.toDateMinus1Month.getMonth() - 1);
    this.modelStartDate = this.toDateMinus1Month;
  }

  onSearch(): void {
    this.fetchData();
  }

  onRemoveAll(): void {
    this.confimDialog().onConfirm(
      () => {
        this.reglementService.deleteAll({ ids: this.selectedDatas.map(e => e.id) }).subscribe({
          next: () => {
            this.selectedDatas = [];
            this.fetchData();
          },
          error: (err: any) => {
            this.openInfoDialog(this.errorService.getErrorMessage(err), 'alert alert-danger');
          },
        });
      },
      'SUPPRESSION DE REGLEMENT',
      'Voullez-vous supprimer ces règlements ?',
    );
  }

  onView(item: Reglement): void {
    if (this.factureGroup) {
      this.onOpenGroupDetail(item);
    } else {
      this.onOpenDetail(item);
    }
  }

  onPrint(item: Reglement): void {
    if (this.tauriPrinterService.isRunningInTauri()) {
      this.printReceiptForTauri(item);
    }else{
      this.reglementService.printReceipt(item.id).subscribe();
    }

  }
  printReceiptForTauri(item: Reglement): void {
    this.reglementService.getEscPosReceiptForTauri(item.id).subscribe({
      next: async (escposData: ArrayBuffer) => {
        try {
          await this.tauriPrinterService.printEscPosFromBuffer(escposData);
        } catch (error) {}
      },
      error: () => {},
    });
  }
  onPrintPdf(): void {
    this.loadingPdf = true;
    this.reglementService.onPrintPdf(this.buildSearchParams()).subscribe({
      next: blob => {
        this.loadingPdf = false;

        if (this.tauriPrinterService.isRunningInTauri()) {
          handleBlobForTauri(blob, 'factures-reglees');
        } else {
          window.open(URL.createObjectURL(blob));
        }
      },
      error: () => (this.loadingPdf = false),
    });
  }

  onDelete(item: Reglement): void {
    this.confimDialog().onConfirm(
      () => {
        this.reglementService.delete(item.id).subscribe({
          next: () => {
            this.fetchData();
          },
          error: (err: any) => {
            this.openInfoDialog(this.errorService.getErrorMessage(err), 'alert alert-danger');
          },
        });
      },
      'SUPPRESSION DE REGLEMENT',
      'Voullez-vous supprimer cet règlement ?',
    );
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
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

  expandAll() {
    // @ts-ignore
    this.expandedRows = this.datas.reduce((acc, p) => (acc[p.organismeId] = true) && acc, {});
  }

  onOpenDetail(reglement: Reglement): void {
    const modalRef = this.modalService.open(DetailSingleReglementComponent, {
      backdrop: 'static',
      size: 'xl',
      centered: true,

      modalDialogClass: 'facture-modal-dialog',
    });
    modalRef.componentInstance.reglement = reglement;
  }

  onOpenGroupDetail(reglement: Reglement): void {
    const modalRef = this.modalService.open(DetailGroupReglementComponent, {
      backdrop: 'static',
      size: 'xl',
      centered: true,

      modalDialogClass: 'facture-modal-dialog',
    });
    modalRef.componentInstance.reglement = reglement;
  }

  ngAfterViewInit(): void {
    const previousSearch = this.regelementStateService.invoicePaymentParam();
    if (previousSearch) {
      this.factureGroup = previousSearch.grouped;
      this.search = previousSearch.search;
      if (previousSearch.grouped) {
        this.selectedGroupeTiersPayant = this.groupeTiersPayants.find(t => t.id === previousSearch.organismeId);
      } else {
        this.selectedTiersPayant = this.tiersPayants.find(t => t.id === previousSearch.organismeId);
      }
      this.modelStartDate = previousSearch.fromDate ? new Date(previousSearch.fromDate) : this.toDateMinus1Month;
      this.modelEndDate = previousSearch.toDate ? new Date(previousSearch.toDate) : new Date();
    } else {
      this.modelStartDate = this.toDateMinus1Month;
      this.modelEndDate = new Date();
    }
    this.fetchData();
  }

  protected getTotalAmountByGroup(groupeId: number): number {
    return this.datas.filter(d => d.organismeId === groupeId).reduce((acc, cur) => acc + cur.totalAmount, 0);
  }

  private buildSearchParams(): InvoicePaymentParam {
    const params = {
      search: this.search,
      organismeId: this.factureGroup ? this.selectedGroupeTiersPayant?.id : this.selectedTiersPayant?.id,
      fromDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      toDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
      grouped: this.factureGroup,
    };

    this.regelementStateService.setInvoicePaymentParam(params);
    return params;
  }

  private fetchData(): void {
    this.loadingBtn = true;
    this.reglementService.query(this.buildSearchParams()).subscribe({
      next: (res: HttpResponse<Reglement[]>) => {
        this.loadingBtn = false;
        this.datas = res.body;
        this.expandAll();
      },
      error: () => {
        this.loadingBtn = false;
        this.datas = [];
      },
    });
  }
}
