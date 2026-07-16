import { Component, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule } from "@angular/forms";
import { HttpHeaders } from "@angular/common/http";
import { Router, RouterLink } from "@angular/router";
import { Button } from "primeng/button";
import { TableLazyLoadEvent, TableModule } from "primeng/table";
import { Toolbar } from "primeng/toolbar";
import { Select } from "primeng/select";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { InputText } from "primeng/inputtext";
import { TooltipModule } from "primeng/tooltip";

import { ISales, SalesStatut } from "../../../../shared/model";
import { SalesApiService } from "../../data-access/services/sales-api.service";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";
import { ButtonGroup } from "primeng/buttongroup";
import { AbilityService } from "../../../../core/auth/ability.service";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../../shared/services/notification.service";
import { Toast } from "primeng/toast";


@Component({
  selector: "app-sales-en-cours",
  templateUrl: "./sales-en-cours.component.html",
  styleUrls: ["./sales-en-cours.component.scss"],
  imports: [
    CommonModule,
    FormsModule,
    Button,
    TableModule,
    Toolbar,
    Select,
    IconField,
    InputIcon,
    InputText,
    TooltipModule,
    ButtonGroup,
    RouterLink,
    Toast
  ]
})
export class SalesEnCoursComponent implements OnInit {
  protected readonly ability = inject(AbilityService);
  protected readonly canDeleteEnCours = this.ability.canSignal("execute", "ventes.en-cours.delete");

  protected loading = signal(false);
  protected sales: ISales[] = [];
  protected totalItems = 0;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected typeVentes = ["TOUT", "VNO", "VO"];
  protected typeVenteSelected = "TOUT";
  protected search = "";
  protected useSimpleSale = false;
  private readonly api = inject(SalesApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);

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
        statut: [SalesStatut.ACTIVE]
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

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event.first != null && event.rows != null) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.loadPage(this.page);
    }
  }

  protected load(): void {
    this.loadPage(0);
  }


  protected confirmDelete(sale: ISales): void {

    this.confirmDialog.onConfirm(
      () => this.deleteSale(sale),
      "Suppression",
      "Voulez-vous supprimer cette vente en cours ?"
    );

  }

  private deleteSale(sale: ISales): void {
    if (!sale.saleId) return;
    this.api.deletePreventeById(sale.saleId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.notificationService.success(
        "Vente en cours supprimée avec succès",
        "Suppression réussie"
      );
      this.load();
    });
  }

  protected navigateToSale(sale: ISales): void {
    this.router.navigate(["/sales-home"], {
      state: { saleInfo: { saleId: sale.saleId } }
    });
  }

  protected openNewSalesHome(): void {
    this.router.navigate(["/sales-home"]);
  }
}
