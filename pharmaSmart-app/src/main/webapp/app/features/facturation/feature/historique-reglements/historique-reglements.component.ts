import {ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, input, signal } from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {finalize} from "rxjs/operators";
import {HttpResponse} from "@angular/common/http";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {NgbDateStruct, NgbModal, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";

import {NGB_DATE_TO_ISO, TODAY_NGB_DATE} from "../../../../shared/util/warehouse-util";
import {TauriPrinterService} from "../../../../shared/services/tauri-printer.service";
import {
  NgbConfirmDialogService
} from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {NotificationService} from "../../../../shared/services/notification.service";
import {ErrorService} from "../../../../shared/error.service";

import {IGroupeTiersPayant} from "../../../../shared/model/groupe-tierspayant.model";
import {ITiersPayant} from "../../../../shared/model";
import {TiersPayantService} from "../../../../entities/tiers-payant/tierspayant.service";
import {
  GroupeTiersPayantService
} from "../../../../entities/groupe-tiers-payant/groupe-tierspayant.service";
import {ReglementService} from "../../../../entities/reglement/reglement.service";
import {InvoicePaymentParam, Reglement} from "../../../../entities/reglement/model/reglement.model";
import {
  DetailSingleReglementComponent
} from "../../../../entities/reglement/detail-single-reglement/detail-single-reglement.component";
import {
  DetailGroupReglementComponent
} from "../../../../entities/reglement/detail-group-reglement/detail-group-reglement.component";
import {BlobDownloadService} from "../../../../shared/services/blob-download.service";
import {
  ButtonComponent,
  DataTableComponent,
  FloatLabelComponent,
  HeaderCheckboxComponent,
  IconFieldComponent,
  RowCheckboxComponent,
  SelectSearchComponent,
  SwitchComponent,
  ToolbarComponent
} from "../../../../shared/ui";
import {
  PharmaDatePickerComponent
} from "../../../../shared/date-picker/pharma-date-picker.component";

@Component({
  selector: "app-historique-reglements",
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    FloatLabelComponent,
    HeaderCheckboxComponent,
    IconFieldComponent,
    RowCheckboxComponent,
    SelectSearchComponent,
    SwitchComponent,
    ToolbarComponent,
    PharmaDatePickerComponent,
    NgbTooltip
  ],
  templateUrl: "./historique-reglements.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: "./historique-reglements.component.scss"
})
export class HistoriqueReglementsComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement. Un écran atteint depuis deux menus affiche donc le nom
   * de celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  protected readonly datas = signal<Reglement[]>([]);
  protected readonly selectedDatas = signal<Reglement[]>([]);
  protected readonly loadingBtn = signal(false);
  protected readonly loadingPdf = signal(false);
  protected readonly loadingExcel = signal(false);
  protected factureGroup = false;
  protected search: string | null = null;
  protected readonly modelStartDate = signal<NgbDateStruct | undefined>(undefined);
  protected modelEndDate: NgbDateStruct = TODAY_NGB_DATE();
  protected readonly groupeTiersPayants = signal<IGroupeTiersPayant[]>([]);
  protected selectedGroupeTiersPayant: IGroupeTiersPayant | undefined;
  protected readonly tiersPayants = signal<ITiersPayant[]>([]);
  protected selectedTiersPayant: ITiersPayant | undefined;
  protected readonly minLength = 2;

  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly reglementService = inject(ReglementService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly modalService = inject(NgbModal);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly downloadDocumentService = inject(BlobDownloadService);

  constructor() {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate.set({year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate()});
  }

  ngOnInit(): void {
    this.fetchData();
  }

  onSearch(): void {
    this.fetchData();
  }

  onView(item: Reglement): void {
    if (this.factureGroup) {
      const ref = this.modalService.open(DetailGroupReglementComponent, {
        backdrop: "static", size: "xl", centered: true,
        modalDialogClass: "facture-modal-dialog"
      });
      ref.componentInstance.reglement = item;
    } else {
      const ref = this.modalService.open(DetailSingleReglementComponent, {
        backdrop: "static", size: "xl", centered: true,
        modalDialogClass: "facture-modal-dialog"
      });
      ref.componentInstance.reglement = item;
    }
  }

  onPrint(item: Reglement): void {
    if (this.tauriPrinterService.isRunningInTauri()) {
      this.reglementService
        .getEscPosReceiptForTauri(item.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: async (data: ArrayBuffer) => {
            try {
              await this.tauriPrinterService.printEscPosFromBuffer(data);
            } catch {
            }
          }
        });
    } else {
      this.reglementService.printReceipt(item.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
    }
  }

  onDelete(item: Reglement): void {
    this.confirmDialog.onConfirm(
      () => {
        this.reglementService
          .delete(item.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => this.fetchData(),
            error: err =>
              this.notificationService.error(this.errorService.getErrorMessage(err), "Suppression")
          });
      },
      "SUPPRESSION DE RÈGLEMENT",
      "Voulez-vous supprimer ce règlement ?"
    );
  }

  onDeleteAll(): void {
    this.confirmDialog.onConfirm(
      () => {
        this.reglementService
          .deleteAll({ids: this.selectedDatas().map(e => e.id.id)})
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.selectedDatas.set([]);
              this.fetchData();
            },
            error: err =>
              this.notificationService.error(this.errorService.getErrorMessage(err), "Suppression")
          });
      },
      "SUPPRESSION DE RÈGLEMENTS",
      "Voulez-vous supprimer ces règlements ?"
    );
  }

  onPrintPdf(): void {
    this.loadingPdf.set(true);
    this.reglementService
      .onPrintPdf(this.buildParams())
      .pipe(
        finalize(() => (this.loadingPdf.set(false))),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: blob => this.downloadDocumentService.downloadPdf(blob, "reglements-factures"),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Export PDF")
      });
  }

  onExportExcel(): void {
    this.loadingExcel.set(true);
    this.reglementService
      .exportExcel(this.buildParams())
      .pipe(
        finalize(() => (this.loadingExcel.set(false))),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: blob => this.downloadDocumentService.downloadExcel(blob, "reglements-factures"),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Export Excel")
      });
  }

  searchTiersPayant(query: string): void {
    this.tiersPayantService
      .query({page: 0, search: query, size: 10})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<ITiersPayant[]>) => (this.tiersPayants.set(res.body ?? [])));
  }

  searchGroupeTiersPayant(query: string): void {
    this.groupeTiersPayantService
      .query({page: 0, search: query, size: 10})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<IGroupeTiersPayant[]>) => (this.groupeTiersPayants.set(res.body ?? [])));
  }

  getTotalByGroupe(organismeId: number): number {
    return this.datas()
      .filter(d => d.organismeId === organismeId)
      .reduce((acc, cur) => acc + (cur.totalAmount ?? 0), 0);
  }

  private fetchData(): void {
    this.loadingBtn.set(true);
    this.reglementService
      .query(this.buildParams())
      .pipe(
        finalize(() => (this.loadingBtn.set(false))),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (res: HttpResponse<Reglement[]>) => {
          this.datas.set(res.body ?? []);
        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Chargement")
      });
  }

  private buildParams(): InvoicePaymentParam {
    return {
      search: this.search,
      organismeId: this.factureGroup
        ? this.selectedGroupeTiersPayant?.id
        : this.selectedTiersPayant?.id,
      fromDate: NGB_DATE_TO_ISO(this.modelStartDate()),
      toDate: NGB_DATE_TO_ISO(this.modelEndDate),
      grouped: this.factureGroup
    };
  }
}
