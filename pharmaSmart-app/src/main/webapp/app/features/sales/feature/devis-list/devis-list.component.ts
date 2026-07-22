import { Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { NgbDateParserFormatter, NgbDateStruct, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { NgxSpinnerComponent } from "ngx-spinner";

import { FrenchDateParserFormatter } from "../../../../config/french-date-parser-formatter";
import { PharmaDatePickerComponent } from "../../../../shared/date-picker/pharma-date-picker.component";
import { ButtonComponent, DataTableComponent, IconFieldComponent, SelectComponent, ToolbarComponent } from "../../../../shared/ui";
import { ISales, SaleId } from "../../../../shared/model/sales.model";
import { SalesApiService } from "../../data-access/services/sales-api.service";
import { NotificationService } from "../../../../shared/services/notification.service";
import { TauriPrinterService } from "../../../../shared/services/tauri-printer.service";
import { AbilityService } from "../../../../core/auth/ability.service";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";

@Component({
  selector: "app-devis-list",
  templateUrl: "./devis-list.component.html",
  styleUrls: ["./devis-list.component.scss"],
  providers: [{ provide: NgbDateParserFormatter, useClass: FrenchDateParserFormatter }],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    ToolbarComponent,
    SelectComponent,
    PharmaDatePickerComponent,
    IconFieldComponent,
    NgbTooltip,
    NgxSpinnerComponent,
    RouterLink

  ]
})
export class DevisListComponent implements OnInit {
  private readonly api = inject(SalesApiService);
  private readonly router = inject(Router);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ability = inject(AbilityService);
  protected readonly canDeleteDevis = this.ability.canSignal("execute", "ventes.devis.delete");
  protected readonly canExportDevis = this.ability.canSignal("execute", "ventes.devis.export");
  protected loading = signal(false);
  protected sales: ISales[] = [];
  protected typeVentes = ["TOUT", "VNO", "VO"];
  protected typeVenteSelected = "TOUT";
  protected search = "";
  protected fromDate: NgbDateStruct = this.todayNgb();
  protected toDate: NgbDateStruct = this.todayNgb();
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly blobDownloadService = inject(BlobDownloadService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.api
      .queryDevis({
        search: this.search || null,
        type: this.typeVenteSelected,
        fromDate: this.ngbDateToIso(this.fromDate),
        toDate: this.ngbDateToIso(this.toDate)
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.sales = res.body || [];
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected editDevis(sale: ISales): void {
    this.router.navigate(["/sales-home/devis"], {
      state: { saleInfo: { saleId: sale.saleId, isDevis: true } }
    });
  }

  protected confirmTransform(sale: ISales): void {
    this.confirmDialog.onConfirm(
      () => this.transformDevis(sale),
      "Transformer en vente",
      "Le devis sera supprimé après transformation. Voulez-vous continuer ?"
    );
  }

  private transformDevis(sale: ISales): void {
    if (!sale.saleId) return;
    const transform$ = sale.categorie === "VNO"
      ? this.api.transformPreventeToSaleComptant(sale.saleId)
      : this.api.transformPreventeToSaleAssurance(sale.saleId);

    transform$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        const saleId: SaleId = res.body!;
        this.router.navigate(["/sales-home"], { state: { saleInfo: { saleId } } });
      },
      error: () => this.notificationService.error("Erreur lors de la transformation", "Proforma")
    });
  }

  protected confirmClone(sale: ISales): void {
    this.confirmDialog.onConfirm(
      () => this.cloneDevis(sale),
      "Cloner le devis",
      "Voulez-vous créer une copie de ce devis ?"
    );
  }

  private cloneDevis(sale: ISales): void {
    if (!sale.saleId) return;
    const clone$ = sale.categorie === "VNO"
      ? this.api.cloneDevisComptant(sale.saleId)
      : this.api.cloneDevisAssurance(sale.saleId);

    clone$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notificationService.success("Devis cloné", "Proforma");
        this.load();
      },
      error: () => this.notificationService.error("Erreur lors du clonage", "Proforma")
    });
  }

  protected printDevis(sale: ISales): void {
    if (!sale.saleId) return;
    this.api.printInvoice(sale.saleId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(blob => {
      this.blobDownloadService.download(blob, `devis-${sale.numberTransaction}`,'pdf');
    });
  }

  protected confirmDelete(sale: ISales): void {
    this.confirmDialog.onConfirm(
      () => this.deleteDevis(sale),
      "Suppression de devis",
      "Voulez-vous supprimer ce devis ?"
    );
  }

  private deleteDevis(sale: ISales): void {
    if (!sale.saleId) return;
    this.api.deletePreventeById(sale.saleId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.notificationService.success("Le devis a été supprimé avec succès.", "Suppression réussie");
      this.load();
    });
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
