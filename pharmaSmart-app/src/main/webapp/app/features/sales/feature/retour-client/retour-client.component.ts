import { Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { NgbDateParserFormatter, NgbDateStruct, NgbModal } from "@ng-bootstrap/ng-bootstrap";

import { FrenchDateParserFormatter } from "../../../../config/french-date-parser-formatter";
import { PharmaDatePickerComponent } from "../../../../shared/date-picker/pharma-date-picker.component";
import { ButtonComponent, DataTableComponent, IconFieldComponent, AppTableLazyLoadEvent, ToolbarComponent } from "../../../../shared/ui";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";
import { DATE_FORMAT_ISO_DATE } from "../../../../shared/util/warehouse-util";
import {
  IRetourClient,
  ModeReglementRetour,
  MotifRetourClient,
  RetourClientApiService
} from "../../data-access/services/retour-client-api.service";
import { RetourClientModalComponent } from "../../ui/retour-client-modal/retour-client-modal.component";
import { ISales } from "../../../../shared/model";

@Component({
  selector: "app-retour-client",
  templateUrl: "./retour-client.component.html",
  styleUrl: "./retour-client.component.scss",
  providers: [{ provide: NgbDateParserFormatter, useClass: FrenchDateParserFormatter }],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    ToolbarComponent,
    PharmaDatePickerComponent,
    IconFieldComponent
  ]
})
export class RetourClientComponent implements OnInit {
  private readonly api = inject(RetourClientApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly modalService = inject(NgbModal);

  protected loading = signal(false);
  protected retours: IRetourClient[] = [];
  protected totalItems = 0;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;

  protected search = "";
  protected fromDate: NgbDateStruct = this.todayNgb();
  protected toDate: NgbDateStruct = this.todayNgb();

  protected saleIdInput: number | null = null;
  protected saleDateInput: Date = new Date();
  protected readonly today = new Date();

  protected readonly motifOptions: { label: string; value: MotifRetourClient }[] = [
    { label: "Erreur de dispensation", value: "ERREUR_DISPENSATION" },
    { label: "Produit défectueux", value: "PRODUIT_DEFECTUEUX" },
    { label: "Erreur de quantité", value: "ERREUR_QUANTITE" },
    { label: "Insatisfaction client", value: "INSATISFACTION" },
    { label: "Autre", value: "AUTRE" }
  ];

  protected readonly modeReglementOptions: { label: string; value: ModeReglementRetour; icon: string }[] = [
    { label: "Remboursement espèces", value: "REMBOURSEMENT_ESPECES", icon: "pi pi-money-bill" },
    { label: "Remboursement CB", value: "REMBOURSEMENT_CB", icon: "pi pi-credit-card" },
    { label: "Avoir client", value: "AVOIR_CLIENT", icon: "pi pi-ticket" }
  ];

  ngOnInit(): void {
    this.loadPage();
  }

  protected loadPage(page?: number): void {
    const p = page ?? this.page;
    this.loading.set(true);
    this.api.query({
      page: p,
      size: this.itemsPerPage,
      search: this.search || null,
      fromDate: this.ngbDateToIso(this.fromDate),
      toDate: this.ngbDateToIso(this.toDate)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          this.totalItems = Number(res.headers.get("X-Total-Count"));
          this.page = p;
          this.retours = res.body ?? [];
        },
        error: () => this.loading.set(false)
      });
  }

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event.first != null && event.rows != null) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.loadPage(this.page);
    }
  }

  protected onSearch(): void {
    this.loadPage(0);
  }

  protected openRetourModal(sale?: ISales): void {
    let input: ISales;
    if (sale) {
      input = sale;
    } else {
      if (!this.saleIdInput) return;
      const saleDate = DATE_FORMAT_ISO_DATE(this.saleDateInput ?? new Date());
      if (!saleDate) return;
      input = { saleId: { id: this.saleIdInput, saleDate } } as ISales;
    }
    const ref = this.modalService.open(RetourClientModalComponent, { centered: true, size: "xl", backdrop: "static" });
    ref.componentInstance.sale = input;
    ref.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      if (result) this.loadPage(0);
    });
  }

  protected motifLabelOf(motif?: string): string {
    return this.motifOptions.find(o => o.value === motif)?.label ?? (motif ?? "—");
  }

  protected modeLabelOf(mode?: string): string {
    return this.modeReglementOptions.find(o => o.value === mode)?.label ?? (mode ?? "—");
  }

  protected modeIconOf(mode?: string): string {
    return this.modeReglementOptions.find(o => o.value === mode)?.icon ?? "pi pi-undo";
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
