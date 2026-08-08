import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {forkJoin, of} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {InventoryApiService} from '../../data-access/services/inventory-api.service';
import {InventoryValuationApiService} from '../../data-access/services/inventory-valuation-api.service';
import {InventoryProgressRecord} from '../../models';
import {IInventoryGlobalSummary} from '../../models/inventory-valuation.model';
import {ButtonComponent, KpiItemComponent, KpiStripComponent} from '../../../../shared/ui';

/**
 * Récapitulatif présenté avant la clôture d'un inventaire.
 *
 * La clôture est irréversible : l'opérateur doit voir ce qui reste à compter et
 * l'écart valorisé avant de confirmer.
 */
@Component({
  selector: 'app-inventory-close-modal',
  imports: [CommonModule, ButtonComponent, KpiStripComponent, KpiItemComponent],
  templateUrl: './inventory-close-modal.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './inventory-close-modal.component.scss',
})
export class InventoryCloseModalComponent implements OnInit {
  readonly activeModal = inject(NgbActiveModal);

  inventoryId!: number;

  protected readonly loading = signal(true);
  protected readonly progress = signal<InventoryProgressRecord | null>(null);
  protected readonly valuation = signal<IInventoryGlobalSummary | null>(null);

  protected readonly remainingLines = computed(() => {
    const p = this.progress();
    if (!p) {
      return 0;
    }
    return Math.max(0, p.totalLines - p.updatedLines);
  });

  protected readonly hasRemaining = computed(() => this.remainingLines() > 0);

  private readonly api = inject(InventoryApiService);
  private readonly valuationApi = inject(InventoryValuationApiService);

  ngOnInit(): void {

    forkJoin({
      progress: this.api.getProgress(this.inventoryId).pipe(catchError(() => of(null))),
      valuation: this.valuationApi.getGlobalSummary(this.inventoryId).pipe(catchError(() => of(null))),
    }).subscribe(({progress, valuation}) => {
      this.progress.set(progress?.body ?? null);
      this.valuation.set(valuation);
      this.loading.set(false);
    });
  }

  protected confirm(): void {
    this.activeModal.close(true);
  }
}
