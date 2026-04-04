import { Component, DestroyRef, effect, inject, input, output, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize } from "rxjs/operators";
import { HttpHeaders } from "@angular/common/http";
import { CommonModule } from "@angular/common";
import { TableLazyLoadEvent, TableModule } from "primeng/table";
import { ButtonModule } from "primeng/button";
import { TooltipModule } from "primeng/tooltip";
import { InputTextModule } from "primeng/inputtext";
import { BadgeModule } from "primeng/badge";

import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";

import { FactureApiService } from "../../data-access/services/facture-api.service";
import { FacturationStore } from "../../data-access/store/facturation.store";
import { IFacture, IInvoiceSearchParams } from "../../data-access/models";

@Component({
  selector: "app-facture-list",
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    TooltipModule,
    InputTextModule,
    BadgeModule
  ],
  templateUrl: "./facture-list.component.html",
  styleUrl: "./facture-list.component.scss"
})
export class FactureListComponent {

  readonly searchParams = input<IInvoiceSearchParams | null>(null);

  readonly factureSelected = output<IFacture>();

  protected loading = false;
  protected page = 0;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected selectedFactures = signal<IFacture[]>([]);

  protected readonly store = inject(FacturationStore);
  private readonly factureApiService = inject(FactureApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    // Déclenche le rechargement dès que le parent pousse de nouveaux paramètres
    effect(() => {
      const params = this.searchParams();
      if (params !== null) {
        this.page = 0;
        this.loadPage(params);
      }
    });
  }

  onRowSelect(facture: IFacture): void {
    this.store.selectFacture(facture);
    this.factureSelected.emit(facture);
  }

  onSelectionChange(selection: IFacture[]): void {
    this.selectedFactures.set(selection);
    this.store.setSelectedFactures(selection);
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    const params = this.searchParams();
    if (event && params) {
      this.page = Math.floor((event.first ?? 0) / (event.rows ?? this.itemsPerPage));
      this.loadPage(params, event.rows ?? this.itemsPerPage);
    }
  }

  getStatutSeverity(statut: string): string {
    switch (statut) {
      case "PAID":
        return "success";
      case "PARTIALLY_PAID":
        return "warn";
      case "NOT_PAID":
        return "danger";
      default:
        return "secondary";
    }
  }

  getStatutLabel(statut: string): string {
    switch (statut) {
      case "PAID":
        return "Réglé";
      case "PARTIALLY_PAID":
        return "Partiel";
      case "NOT_PAID":
        return "Impayé";
      default:
        return statut ?? "—";
    }
  }

  private loadPage(params: IInvoiceSearchParams, rows = this.itemsPerPage): void {
    this.loading = true;
    this.store.setLoading(true);
    this.factureApiService
      .query({ ...params, page: this.page, size: rows } as any)
      .pipe(
        finalize(() => (this.loading = false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: res => this.onSuccess(res.body, res.headers),
        error: err => {
          this.store.setLoading(false);
          this.notificationService.error(this.errorService.getErrorMessage(err), "Chargement factures");
        }
      });
  }

  private onSuccess(data: IFacture[] | null, headers: HttpHeaders): void {
    this.store.setFactures(data ?? [], Number(headers.get("X-Total-Count")));
  }
}
