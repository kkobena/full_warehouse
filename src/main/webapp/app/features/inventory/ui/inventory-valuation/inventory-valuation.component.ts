import {Component, inject, input, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {TableModule} from 'primeng/table';
import {Select} from 'primeng/select';
import {
  IInventoryGlobalSummary,
  IValuationGroup,
  VALUATION_GROUP_OPTIONS,
  ValuationGroupBy
} from '../../models/inventory-valuation.model';
import {InventoryValuationApiService} from '../../data-access/services/inventory-valuation-api.service';

@Component({
  selector: 'app-inventory-valuation',
  imports: [CommonModule, FormsModule, TableModule, Select],
  templateUrl: './inventory-valuation.component.html',
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

  getGapClass(value: number): string {
    if (value < 0) return 'val-negative';
    if (value > 0) return 'val-positive';
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
