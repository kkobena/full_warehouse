import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabelModule } from 'primeng/floatlabel';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { SelectModule } from 'primeng/select';
import { Toolbar } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';

import { DATE_FORMAT_ISO_DATE } from '../../../../shared/util/warehouse-util';
import { INVOICES_STATUT } from '../../../../shared/constants/data-constants';
import { CodeValue } from '../../../../shared/code-value';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { TauriPrinterService } from '../../../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../../../shared/util/tauri-util';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { ITiersPayant } from '../../../../shared/model/tierspayant.model';
import { IGroupeTiersPayant } from '../../../../shared/model/groupe-tierspayant.model';
import { TiersPayantService } from '../../../../entities/tiers-payant/tierspayant.service';
import { GroupeTiersPayantService } from '../../../../entities/groupe-tiers-payant/groupe-tierspayant.service';

import { FacturationStore } from '../../data-access/store/facturation.store';
import { FactureApiService } from '../../data-access/services/facture-api.service';
import { IFacture, IInvoiceSearchParams } from '../../data-access/models';
import { FactureKpiBannerComponent } from '../../ui/facture-kpi-banner/facture-kpi-banner.component';
import { FactureListComponent } from '../../ui/facture-list/facture-list.component';
import { FactureDetailPanelComponent } from '../../ui/facture-detail-panel/facture-detail-panel.component';
import { TranslateService } from "@ngx-translate/core";
import { PrimeNG } from "primeng/config";

@Component({
  selector: 'app-facturation-home',
  imports: [
    FormsModule,
    Toolbar,
    ButtonModule,
    DatePicker,
    FloatLabelModule,
    AutoCompleteModule,
    ToggleSwitch,
    SelectModule,
    FactureKpiBannerComponent,
    FactureListComponent,
    FactureDetailPanelComponent,
  ],
  templateUrl: './facturation-home.component.html',
  styleUrl: './facturation-home.component.scss',
})
export class FacturationHomeComponent implements OnInit {
  // Store & computed
  protected readonly store = inject(FacturationStore);
  protected readonly panelOpen = computed(() => this.store.panelOpen());

  // Toolbar state
  protected readonly statutOptions: CodeValue[] = INVOICES_STATUT;
  protected readonly minLength = 2;
  protected factureGroupees = false;
  protected factureProvisoire = false;
  protected modelStartDate: Date;
  protected modelEndDate: Date = new Date();
  protected search = '';
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
  protected readonly showHint = signal<boolean>(localStorage.getItem('facturation-hint-dismissed') !== '1');
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly factureApiService = inject(FactureApiService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly destroyRef = inject(DestroyRef);

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
    this.loadKpi();
    this.onSearch();
  }

  onSearch(): void {
    this.loadingBtn = true;
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
          if (this.tauriPrinterService.isRunningInTauri()) {
            handleBlobForTauri(blob, 'factures', 'xlsx');
          } else {
            const a = document.createElement('a');
            a.href = URL.createObjectURL(blob);
            a.download = 'factures.xlsx';
            a.click();
          }
        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Export Excel'),
      });
  }

  onDeleteSelected(): void {
    const selected = this.store.selectedFactures();
    this.confirmDialog.onConfirm(
      () => this.deleteAll(selected),
      'Suppression',
      `Supprimer les ${selected.length} facture(s) sélectionnée(s) ?`,
    );
  }

  onSearchLoaded(): void {
    this.loadingBtn = false;
  }

  dismissHint(): void {
    localStorage.setItem('facturation-hint-dismissed', '1');
    this.showHint.set(false);
  }

  searchTiersPayant(event: { query: string }): void {
    this.tiersPayantService
      .query({ page: 0, search: event.query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.tiersPayants = res.body ?? []));
  }

  searchGroupTiersPayant(event: { query: string }): void {
    this.groupeTiersPayantService
      .query({ page: 0, search: event.query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.groupeTiersPayants = res.body ?? []));
  }

  private deleteAll(selected: IFacture[]): void {
    // Suppression une par une (pas d'endpoint batch sur les factures)
    selected.forEach(f => {
      if (f.factureItemId) {
        this.factureApiService
          .delete(f.factureItemId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => this.store.removeFactureFromList(f.factureItemId!.id),
            error: err =>
              this.notificationService.error(this.errorService.getErrorMessage(err), 'Suppression'),
          });
      }
    });
    this.store.clearSelection();
  }

  private loadKpi(): void {
    this.store.setKpiLoading(true);
    this.factureApiService
      .getKpi({})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.store.setKpi(res.body ?? null),
        error: () => this.store.setKpi(null),
      });
  }

  private buildSearchParams(): IInvoiceSearchParams {
    const params: IInvoiceSearchParams = {
      startDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      endDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
      search: this.search || undefined,
      factureGroupees: this.factureGroupees,
      factureProvisoire: this.factureProvisoire,
      groupIds: this.selectedGroupeTiersPayants.map(g => g.id),
      tiersPayantIds: this.selectedTiersPayants.map(t => t.id),
    };
    if (this.selectedStatut) {
      params.statuts = [this.selectedStatut];
    }
    return params;
  }
}
