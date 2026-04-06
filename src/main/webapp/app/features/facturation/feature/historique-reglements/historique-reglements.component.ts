import { Component, DestroyRef, inject, OnInit } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize } from "rxjs/operators";
import { HttpResponse } from "@angular/common/http";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { TableModule } from "primeng/table";
import { ButtonModule } from "primeng/button";
import { ButtonGroup } from "primeng/buttongroup";
import { TooltipModule } from "primeng/tooltip";
import { FloatLabel } from "primeng/floatlabel";
import { DatePickerModule } from "primeng/datepicker";
import { Toolbar } from "primeng/toolbar";
import { AutoCompleteModule } from "primeng/autocomplete";
import { ToggleSwitch } from "primeng/toggleswitch";
import { InputTextModule } from "primeng/inputtext";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";

import { DATE_FORMAT_ISO_DATE } from "../../../../shared/util/warehouse-util";
import { TauriPrinterService } from "../../../../shared/services/tauri-printer.service";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";

import { IGroupeTiersPayant } from "../../../../shared/model/groupe-tierspayant.model";
import { ITiersPayant } from "../../../../shared/model";
import { TiersPayantService } from "../../../../entities/tiers-payant/tierspayant.service";
import { GroupeTiersPayantService } from "../../../../entities/groupe-tiers-payant/groupe-tierspayant.service";
import { ReglementService } from "../../../../entities/reglement/reglement.service";
import { InvoicePaymentParam, Reglement } from "../../../../entities/reglement/model/reglement.model";
import {
  DetailSingleReglementComponent
} from "../../../../entities/reglement/detail-single-reglement/detail-single-reglement.component";
import {
  DetailGroupReglementComponent
} from "../../../../entities/reglement/detail-group-reglement/detail-group-reglement.component";
import { TranslateService } from "@ngx-translate/core";
import { PrimeNG } from "primeng/config";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";

@Component({
  selector: "app-historique-reglements",
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    ButtonGroup,
    TooltipModule,
    FloatLabel,
    DatePickerModule,
    Toolbar,
    AutoCompleteModule,
    ToggleSwitch,
    InputTextModule,
    IconField,
    InputIcon
  ],
  templateUrl: "./historique-reglements.component.html",
  styleUrl: "./historique-reglements.component.scss"
})
export class HistoriqueReglementsComponent implements OnInit {
  protected datas: Reglement[] = [];
  protected selectedDatas: Reglement[] = [];
  protected expandedRows: Record<string, boolean> = {};
  protected loadingBtn = false;
  protected loadingPdf = false;
  protected loadingExcel = false;
  protected factureGroup = false;
  protected search: string | null = null;
  protected modelStartDate: Date;
  protected modelEndDate: Date = new Date();
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected selectedGroupeTiersPayant: IGroupeTiersPayant | undefined;
  protected tiersPayants: ITiersPayant[] = [];
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
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly downloadDocumentService = inject(BlobDownloadService);

  constructor() {
    this.translate.use("fr");
    this.translate.stream("primeng")
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: data => this.primeNGConfig.setTranslation(data)
      });

    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate = d;
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
          .deleteAll({ ids: this.selectedDatas.map(e => e.id.id) })
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.selectedDatas = [];
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
    this.loadingPdf = true;
    this.reglementService
      .onPrintPdf(this.buildParams())
      .pipe(
        finalize(() => (this.loadingPdf = false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: blob => this.downloadDocumentService.downloadPdf(blob, "reglements-factures"),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Export PDF")
      });
  }

  onExportExcel(): void {
    this.loadingExcel = true;
    this.reglementService
      .exportExcel(this.buildParams())
      .pipe(
        finalize(() => (this.loadingExcel = false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: blob => this.downloadDocumentService.downloadExcel(blob, "reglements-factures"),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Export Excel")
      });
  }

  searchTiersPayant(event: { query: string }): void {
    this.tiersPayantService
      .query({ page: 0, search: event.query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<ITiersPayant[]>) => (this.tiersPayants = res.body ?? []));
  }

  searchGroupeTiersPayant(event: { query: string }): void {
    this.groupeTiersPayantService
      .query({ page: 0, search: event.query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res: HttpResponse<IGroupeTiersPayant[]>) => (this.groupeTiersPayants = res.body ?? []));
  }

  getTotalByGroupe(organismeId: number): number {
    return this.datas
      .filter(d => d.organismeId === organismeId)
      .reduce((acc, cur) => acc + (cur.totalAmount ?? 0), 0);
  }

  expandAll(): void {
    this.expandedRows = this.datas.reduce(
      (acc, r) => ({ ...acc, [r.organismeId]: true }),
      {} as Record<string, boolean>
    );
  }

  private fetchData(): void {
    this.loadingBtn = true;
    this.reglementService
      .query(this.buildParams())
      .pipe(
        finalize(() => (this.loadingBtn = false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (res: HttpResponse<Reglement[]>) => {
          this.datas = res.body ?? [];
          this.expandAll();
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
      fromDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      toDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
      grouped: this.factureGroup
    };
  }
}
