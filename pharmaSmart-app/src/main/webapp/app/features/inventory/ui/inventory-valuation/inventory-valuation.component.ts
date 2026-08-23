import {Component, inject, input, OnInit, signal, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {
  IInventoryGlobalSummary,
  IValuationGroup,
  VALUATION_GROUP_OPTIONS,
  ValuationGroupBy
} from '../../models/inventory-valuation.model';
import {InventoryValuationApiService} from '../../data-access/services/inventory-valuation-api.service';
import {
  AppKpiAccent,
  DataTableComponent,
  KpiItemComponent,
  KpiStripComponent,
  SelectComponent,
} from '../../../../shared/ui';

@Component({
  selector: 'app-inventory-valuation',
  imports: [CommonModule, FormsModule, DataTableComponent, SelectComponent, KpiStripComponent, KpiItemComponent],
  templateUrl: './inventory-valuation.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './inventory-valuation.component.scss',
})
export class InventoryValuationComponent implements OnInit {
  inventoryId = input.required<number>();

  global = signal<IInventoryGlobalSummary | null>(null);
  groups = signal<IValuationGroup[]>([]);
  loading = signal(false);
  selectedGroupBy: ValuationGroupBy = 'STORAGE';

  readonly groupByOptions = VALUATION_GROUP_OPTIONS;

  private readonly api = inject(InventoryValuationApiService);

  ngOnInit(): void {
    this.loadAll();
  }

  onGroupByChange(): void {
    this.loadGroups();
  }

  get totalGapAmount(): number {
    return this.global()?.gapAmount ?? 0;
  }

  /** Colore les cellules d'écart du tableau — classes scopées de cet écran. */
  getGapClass(value: number): string {
    if (value < 0) return 'val-negative';
    if (value > 0) return 'val-positive';
    return '';
  }

  getGapAccent(value: number): AppKpiAccent {
    if (value < 0) return 'danger';
    if (value > 0) return 'success';
    return 'secondary';
  }

  /**
   * Équivalent de {@link getGapClass} pour les items du bandeau, en classes utilitaires
   * globales : le `<span>` de valeur appartient au template d'`app-kpi-item`, il porte son
   * attribut de scoping et non celui de cet écran — `.val-negative` ne l'atteindrait pas.
   */
  getGapTextClass(value: number): string {
    if (value < 0) return 'text-danger';
    if (value > 0) return 'text-success';
    return '';
  }

  private loadAll(): void {
    this.loading.set(true);
    this.api.getGlobalSummary(this.inventoryId()).subscribe({
      next: g => {
        this.global.set(g);
        this.loadGroups();
      },
      error: () => this.loading.set(false),
    });
  }

  private loadGroups(): void {
    this.api.getSummaryByGroup(this.inventoryId(), this.selectedGroupBy).subscribe({
      next: rows => {
        this.groups.set(rows);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
