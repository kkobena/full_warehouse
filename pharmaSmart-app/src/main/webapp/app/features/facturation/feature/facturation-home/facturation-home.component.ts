import { Component, computed, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { catchError, finalize, tap } from "rxjs/operators";
import { forkJoin, of } from "rxjs";
import { FormsModule } from "@angular/forms";
import { NgbDateStruct } from "@ng-bootstrap/ng-bootstrap";
import {
  ButtonComponent,
  FloatLabelComponent,
  MultiSelectComponent,
  SelectComponent,
  SwitchComponent,
  ToolbarComponent
} from "../../../../shared/ui";
import { PharmaDatePickerComponent } from "../../../../shared/date-picker/pharma-date-picker.component";

import { NGB_DATE_TO_ISO, TODAY_NGB_DATE } from "../../../../shared/util/warehouse-util";
import { INVOICES_STATUT } from "../../../../shared/constants/data-constants";
import { CodeValue } from "../../../../shared/code-value";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { ITiersPayant } from "../../../../shared/model";
import { IGroupeTiersPayant } from "../../../../shared/model/groupe-tierspayant.model";
import { TiersPayantService } from "../../../../entities/tiers-payant/tierspayant.service";
import { GroupeTiersPayantService } from "../../../../entities/groupe-tiers-payant/groupe-tierspayant.service";

import { AbilityService } from "app/core/auth/ability.service";
import { FacturationStore } from "../../data-access/store/facturation.store";
import { FactureApiService } from "../../data-access/services/facture-api.service";
import { IFacture, IInvoiceSearchParams } from "../../data-access/models";
import { FactureKpiBannerComponent } from "../../ui/facture-kpi-banner/facture-kpi-banner.component";
import { FactureListComponent } from "../../ui/facture-list/facture-list.component";
import { FactureDetailPanelComponent } from "../../ui/facture-detail-panel/facture-detail-panel.component";
import { TranslateService } from "@ngx-translate/core";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";

@Component({
  selector: "app-facturation-home",
  imports: [
    FormsModule,
    ButtonComponent,
    FloatLabelComponent,
    MultiSelectComponent,
    PharmaDatePickerComponent,
    SelectComponent,
    SwitchComponent,
    ToolbarComponent,
    FactureKpiBannerComponent,
    FactureListComponent,
    FactureDetailPanelComponent
  ],
  templateUrl: "./facturation-home.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./facturation-home.component.scss"
})
export class FacturationHomeComponent implements OnInit {
  // Store & computed
  protected readonly store = inject(FacturationStore);
  protected readonly panelOpen = computed(() => this.store.panelOpen());
  protected readonly requestedDetailTab = signal<string | null>(null);

  private readonly ability = inject(AbilityService);
  protected readonly canExecute = this.ability.canSignal('execute', 'factures');
  protected readonly canDelete  = this.ability.canSignal('delete',  'factures');
  protected readonly canExport  = this.ability.canSignal('export',  'factures');

  // Toolbar state
  protected readonly statutOptions: CodeValue[] = INVOICES_STATUT;
  protected readonly minLength = 2;
  protected factureGroupees = false;
  protected factureProvisoire = false;
  protected deleteAllSpinner = false;
  protected modelStartDate: NgbDateStruct;
  protected modelEndDate: NgbDateStruct = TODAY_NGB_DATE();
  protected search = "";
  protected selectedStatut: string | null = null;
  protected tiersPayants: ITiersPayant[] = [];
  protected selectedTiersPayants: ITiersPayant[] = [];
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected selectedGroupeTiersPayants: IGroupeTiersPayant[] = [];
  protected loadingBtn = false;
  protected loadingExport = false;

  // Signal transmis à la liste pour déclencher la recherche
  protected readonly currentSearchParams = signal<IInvoiceSearchParams | null>(null);

  // Hint premier usage
  protected readonly showHint = signal<boolean>(localStorage.getItem("facturation-hint-dismissed") !== "1");
  private readonly translate = inject(TranslateService);
  private readonly factureApiService = inject(FactureApiService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly downloadDocumentService = inject(BlobDownloadService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.translate.use("fr");

    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate = { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
  }

  ngOnInit(): void {
    this.loadKpi();
    this.onSearch();
  }

  onCreateAvoir(facture: IFacture): void {
    this.requestedDetailTab.set('avoirs');
    this.store.selectFacture(facture);
  }

  onSearch(): void {
    this.currentSearchParams.set(this.buildSearchParams());
  }

  onGroupToggle(): void {
    this.selectedTiersPayants = [];
    this.selectedGroupeTiersPayants = [];
    this.onSearch();
  }

  onExport(): void {
    this.loadingExport = true;
    this.factureApiService
      .exportExcel(this.buildSearchParams())
      .pipe(finalize(() => (this.loadingExport = false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          this.downloadDocumentService.downloadExcel(blob, "factures");

        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Export Excel")
      });
  }

  onDeleteSelected(): void {
    const selected = this.store.selectedFactures();
    this.confirmDialog.onConfirm(
      () => this.deleteAll(selected),
      "Suppression",
      `Supprimer les ${selected.length} facture(s) sélectionnée(s) ?`
    );
  }


  dismissHint(): void {
    localStorage.setItem("facturation-hint-dismissed", "1");
    this.showHint.set(false);
  }

  searchTiersPayant(query: string): void {
    this.tiersPayantService
      .query({ page: 0, search: query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.tiersPayants = res.body ?? []));
  }

  searchGroupTiersPayant(query: string): void {
    this.groupeTiersPayantService
      .query({ page: 0, search: query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.groupeTiersPayants = res.body ?? []));
  }

  private deleteAll(selected: IFacture[]): void {
    const deletable = selected.filter(f => f.factureItemId && f.statut === 'NOT_PAID');
    if (!deletable.length) {
      this.store.clearSelection();
      return;
    }

    this.deleteAllSpinner = true;

    // Chaque requête gère sa propre erreur → forkJoin peut toujours se terminer
    const deletes$ = deletable.map(f =>
      this.factureApiService.delete(f.factureItemId!).pipe(
        tap(() => this.store.removeFactureFromList(f.factureItemId!.id)),
        catchError(err => {
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Suppression');
          return of(null); // ne pas bloquer forkJoin sur une erreur partielle
        }),
      ),
    );

    // PAS de takeUntilDestroyed : on laisse les requêtes HTTP se terminer
    // naturellement pour éviter "message channel closed before a response was received"
    forkJoin(deletes$)
      .pipe(finalize(() => (this.deleteAllSpinner = false)))
      .subscribe({
        next: () => {
          this.store.clearSelection();
          this.notificationService.success(`${deletable.length} facture(s) supprimée(s)`);
        },
      });
  }

  private loadKpi(): void {
    this.store.setKpiLoading(true);
    this.factureApiService
      .getKpi({})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.store.setKpi(res.body ?? null),
        error: () => this.store.setKpi(null)
      });
  }

  private buildSearchParams(): IInvoiceSearchParams {
    const params: IInvoiceSearchParams = {
      startDate: NGB_DATE_TO_ISO(this.modelStartDate),
      endDate: NGB_DATE_TO_ISO(this.modelEndDate),
      search: this.search || undefined,
      factureGroupees: this.factureGroupees,
      factureProvisoire: this.factureProvisoire,
      groupIds: this.selectedGroupeTiersPayants.map(g => g.id),
      tiersPayantIds: this.selectedTiersPayants.map(t => t.id)
    };
    if (this.selectedStatut) {
      params.statuts = [this.selectedStatut];
    }
    return params;
  }
}
