import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs/operators';
import { Router } from '@angular/router';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { TauriPrinterService } from '../../../../shared/services/tauri-printer.service';

import { DiffereApiService } from '../../data-access/services/differe-api.service';
import { DiffereStore } from '../../data-access/store/differe.store';
import {
  IDiffere,
  INewReglementDiffere,
  IPaymentIdDiffere,
  IReglementDiffere,
} from '../../data-access/models';
import { ReglementDiffereFormComponent } from '../reglement-differe-form/reglement-differe-form.component';

@Component({
  selector: 'app-differe-detail-panel',
  imports: [
    WarehouseCommonModule,
    NgbNavModule,
    ButtonModule,
    TableModule,
    TooltipModule,
    ProgressSpinnerModule,
    ReglementDiffereFormComponent,
  ],
  templateUrl: './differe-detail-panel.component.html',
  styleUrl: './differe-detail-panel.component.scss',
})
export class DiffereDetailPanelComponent {
  readonly differe = input<IDiffere | null>(null);

  protected activeTab = signal<string>('detail');
  protected loadingReglements = false;
  protected isSaving = false;
  protected monnaie = signal(0);
  protected reglements = signal<IReglementDiffere[]>([]);

  private currentCustomerId: number | null = null;

  private readonly differeApiService = inject(DiffereApiService);
  protected readonly store = inject(DiffereStore);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const d = this.differe();
      if (!d?.customerId) return;

      const isNew = d.customerId !== this.currentCustomerId;
      this.currentCustomerId = d.customerId;

      this.reglements.set([]);
      this.monnaie.set(0);

      if (isNew) {
        this.activeTab.set('detail');
      }
    });
  }

  onTabChange(tab: string | number): void {
    const tabId = String(tab);
    this.activeTab.set(tabId);
    if (tabId === 'historique' && this.reglements().length === 0) {
      this.loadReglements();
    }
  }

  onClose(): void {
    this.store.selectDiffere(null);
  }

  onMonnaieChange(montant: number): void {
    this.monnaie.set(montant);
  }

  onSaveReglement(params: INewReglementDiffere): void {
    this.isSaving = true;
    this.differeApiService
      .doReglement(params)
      .pipe(
        finalize(() => (this.isSaving = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: res => {
          if (res.body) {
            this.onPrintReceipt(res.body.idReglement);
          }
        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur règlement'),
      });
  }

  private onPrintReceipt(paymentId: IPaymentIdDiffere): void {
    this.confirmDialog.onConfirm(
      () => {
        if (this.tauriPrinterService.isRunningInTauri()) {
          this.printReceiptForTauri(paymentId);
        } else {
          this.differeApiService
            .printReceipt(paymentId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              error: err =>
                this.notificationService.error(this.errorService.getErrorMessage(err), 'Impression'),
            });
        }
        this.onReglementComplete();
      },
      'Ticket règlement',
      'Voulez-vous imprimer le ticket ?',
    );
  }

  private printReceiptForTauri(paymentId: IPaymentIdDiffere): void {
    this.differeApiService
      .getEscPosReceiptForTauri(paymentId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: async (data: ArrayBuffer) => {
          try {
            await this.tauriPrinterService.printEscPosFromBuffer(data);
          } catch { /* silently ignore printer errors */ }
        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Impression'),
      });
  }

  private onReglementComplete(): void {
    // Recharger le differe pour mettre à jour les soldes
    const d = this.differe();
    if (!d?.customerId) return;
    this.differeApiService
      .findByCustomer(d.customerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          if (res.body) {
            this.store.refreshDiffereInList(res.body);
            if ((res.body.rest ?? 0) <= 0) {
              this.store.selectDiffere(null);
            }
          }
        },
      });
  }

  private loadReglements(): void {
    const d = this.differe();
    if (!d?.customerId) return;
    this.loadingReglements = true;
    this.differeApiService
      .getReglementsDifferes({ customerId: d.customerId, page: 0, size: 50 })
      .pipe(
        finalize(() => (this.loadingReglements = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: res => this.reglements.set(res.body ?? []),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Chargement historique'),
      });
  }
}
