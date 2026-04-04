import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs/operators';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { BadgeModule } from 'primeng/badge';
import { TooltipModule } from 'primeng/tooltip';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { TauriPrinterService } from '../../../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../../../shared/util/tauri-util';

import { FactureApiService } from '../../data-access/services/facture-api.service';
import { ReglementApiService } from '../../data-access/services/reglement-api.service';
import { FacturationStore } from '../../data-access/store/facturation.store';
import {
  IFacture,
  IFactureItem,
  IReglement,
  IReglementFactureDossier,
  IDossierFactureProjection,
} from '../../data-access/models';
import { ReglementWorkspaceComponent } from '../reglement-workspace/reglement-workspace.component';

@Component({
  selector: 'app-facture-detail-panel',
  imports: [
    WarehouseCommonModule,
    NgbNavModule,
    ButtonModule,
    TableModule,
    BadgeModule,
    TooltipModule,
    ProgressSpinnerModule,
    ReglementWorkspaceComponent,
  ],
  templateUrl: './facture-detail-panel.component.html',
  styleUrl: './facture-detail-panel.component.scss',
})
export class FactureDetailPanelComponent {
  readonly facture = input<IFacture | null>(null);

  protected loadingItems = false;
  protected loadingReglements = false;
  protected loadingPdf = false;
  protected factureItems = signal<IFactureItem[]>([]);
  protected reglements = signal<IReglement[]>([]);
  protected dossierFactureProjection = signal<IDossierFactureProjection | null>(null);
  protected reglementDossiers = signal<IReglementFactureDossier[]>([]);
  protected activeTab = signal<string>('detail');

  private currentFactureId: number | null = null;

  private readonly factureApiService = inject(FactureApiService);
  private readonly reglementApiService = inject(ReglementApiService);
  protected readonly store = inject(FacturationStore);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly destroyRef = inject(DestroyRef);

  get isGroupe(): boolean {
    return this.facture()?.groupeFactureId != null;
  }

  getStatutSeverity(statut: string): string {
    switch (statut) {
      case 'PAID':           return 'success';
      case 'PARTIALLY_PAID': return 'warn';
      case 'NOT_PAID':       return 'danger';
      default:               return 'secondary';
    }
  }

  getStatutLabel(statut: string): string {
    switch (statut) {
      case 'PAID':           return 'Réglé';
      case 'PARTIALLY_PAID': return 'Partiel';
      case 'NOT_PAID':       return 'Impayé';
      default:               return statut ?? '—';
    }
  }

  constructor() {
    effect(() => {
      const f = this.facture();
      if (!f?.factureItemId) return;

      const isNew = f.factureItemId.id !== this.currentFactureId;
      this.currentFactureId = f.factureItemId.id;

      this.factureItems.set([]);
      this.reglements.set([]);
      this.dossierFactureProjection.set(null);
      this.reglementDossiers.set([]);

      if (isNew) {
        this.activeTab.set('detail');
      }

      this.loadItems(f);
    });
  }

  onTabChange(tab: string | number): void {
    const tabId = String(tab);
    this.activeTab.set(tabId);
    const f = this.facture();
    if (!f) return;

    if (tabId === 'regler' && !this.dossierFactureProjection()) {
      this.loadReglementContext(f);
    }
    if (tabId === 'versements' && this.reglements().length === 0) {
      this.loadReglements(f);
    }
  }

  onClose(): void {
    this.store.selectFacture(null);
  }

  onExportPdf(): void {
    const f = this.facture();
    if (!f?.factureItemId) return;
    this.loadingPdf = true;
    this.factureApiService
      .exportToPdf(f.factureItemId)
      .pipe(
        finalize(() => (this.loadingPdf = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: blob => {
          const name = f.numFacture ?? 'facture';
          if (this.tauriPrinterService.isRunningInTauri()) {
            handleBlobForTauri(blob, name, 'pdf');
          } else {
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `${name}.pdf`;
            a.click();
            URL.revokeObjectURL(url);
          }
        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Export PDF'),
      });
  }

  private loadItems(f: IFacture): void {
    if (!f.factureItemId) return;
    this.loadingItems = true;
    this.factureApiService
      .find(f.factureItemId)
      .pipe(
        finalize(() => (this.loadingItems = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: res => this.factureItems.set(res.body?.items ?? []),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Chargement dossiers'),
      });
  }

  private loadReglementContext(f: IFacture): void {
    if (!f.factureItemId) return;
    const isGroup = this.isGroupe;
    const typeFacture = isGroup ? 'groupes' : 'individuelle';

    this.factureApiService
      .findDossierFactureProjection(f.factureItemId, { isGroup })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.dossierFactureProjection.set(res.body),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Chargement projection'),
      });

    this.factureApiService
      .findDossierReglement(f.factureItemId, typeFacture, { page: 0, size: 50 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.reglementDossiers.set(res.body ?? []),
        error: () => this.reglementDossiers.set([]),
      });
  }

  private loadReglements(f: IFacture): void {
    if (!f.factureItemId) return;
    this.loadingReglements = true;
    this.reglementApiService
      .query({
        grouped: this.isGroupe,
        fromDate: f.factureItemId.invoiceDate,
        organismeId: f.groupeFactureId ?? f.factureId,
      })
      .pipe(
        finalize(() => (this.loadingReglements = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: res => this.reglements.set(res.body ?? []),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Chargement versements'),
      });
  }
}
