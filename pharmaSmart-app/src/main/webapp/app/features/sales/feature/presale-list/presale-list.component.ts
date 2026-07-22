import { Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule } from "@angular/forms";
import { HttpHeaders } from "@angular/common/http";
import { Router, RouterLink } from "@angular/router";
import { NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { NgxSpinnerComponent } from "ngx-spinner";

import { ButtonComponent, DataTableComponent, IconFieldComponent, AppTableLazyLoadEvent, SelectComponent, ToolbarComponent } from "../../../../shared/ui";
import { SalesStatut } from "../../../../shared/model";
import { ISales, SaleId } from "../../../../shared/model/sales.model";
import { SalesApiService } from "../../data-access/services/sales-api.service";
import { NotificationService } from "../../../../shared/services/notification.service";
import { AbilityService } from "../../../../core/auth/ability.service";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";

@Component({
  selector: "app-presale-list",
  templateUrl: "./presale-list.component.html",
  styleUrl: "./presale-list.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    ToolbarComponent,
    SelectComponent,
    IconFieldComponent,
    NgbTooltip,
    NgxSpinnerComponent,
    RouterLink
  ]
})
export class PresaleListComponent implements OnInit {
  private readonly api = inject(SalesApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ability = inject(AbilityService);
  protected readonly canDeletePresale = this.ability.canSignal("execute", "ventes.presales.delete");

  protected useSimpleSale = false;
  protected loading = signal(false);
  protected transforming = signal(false);
  protected sales: ISales[] = [];
  protected totalItems = 0;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected typeVentes = ["TOUT", "VNO", "VO"];
  protected typeVenteSelected = "TOUT";
  protected search = "";
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  ngOnInit(): void {
    this.loadPage();
  }

  protected loadPage(page?: number): void {
    const pageToLoad = page ?? this.page;
    this.loading.set(true);
    this.api
      .queryManagement({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: this.search || null,
        type: this.typeVenteSelected,
        statut: [SalesStatut.PROCESSING, SalesStatut.PENDING]
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          this.onSuccess(res.body, res.headers, pageToLoad);
        },
        error: () => this.loading.set(false)
      });
  }

  private onSuccess(data: ISales[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get("X-Total-Count"));
    this.page = page;
    this.sales = data || [];
  }

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event.first != null && event.rows != null) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.loadPage(this.page);
    }
  }

  protected load(): void {
    this.loadPage(0);
  }

  protected editPresale(sale: ISales): void {
    this.router.navigate(["/sales-home/prevente"], {
      state: { saleInfo: { saleId: sale.saleId, isPresale: true } }
    });
  }


  protected confirmTransform(sale: ISales): void {
    this.confirmDialog.onConfirm(
      () => this.transformPresale(sale),
      "Transformer en vente",
      "La pré-vente va être finalisée. Voulez-vous continuer ?"
    );
  }

  private transformPresale(sale: ISales): void {
    if (!sale.saleId) return;
    this.transforming.set(true);
    const transform$ = sale.categorie === "VNO"
      ? this.api.transformPreventeToSaleComptant(sale.saleId)
      : this.api.transformPreventeToSaleAssurance(sale.saleId);

    transform$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.transforming.set(false);
        const saleId: SaleId = res.body!;
        this.router.navigate(["/sales-home"], { state: { saleInfo: { saleId } } });
      },
      error: () => {
        this.transforming.set(false);
        this.notificationService.error("Erreur lors de la transformation", "Pré-vente");
      }
    });
  }

  protected confirmDelete(sale: ISales): void {
    this.confirmDialog.onConfirm(
      () => this.deletePresale(sale),
      "Suppression de pré-vente",
      "Voulez-vous supprimer cette pré-vente ?"
    );
  }

  private deletePresale(sale: ISales): void {
    if (!sale.saleId) return;
    this.api.deletePreventeById(sale.saleId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.notificationService.success("La pré-vente a été supprimée avec succès.", "Suppression réussie");
      this.load();
    });
  }


}
