import { Component, inject, input, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IGapSummary } from '../../models/gap-analysis.model';
import { GapAnalysisApiService } from '../../data-access/services/gap-analysis-api.service';

@Component({
  selector: 'app-gap-summary',
  imports: [CommonModule],
  templateUrl: './gap-summary.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './gap-summary.component.scss',
})
export class GapSummaryComponent implements OnInit {
  inventoryId = input.required<number>();

  summary = signal<IGapSummary[]>([]);
  loading = signal(false);

  private readonly api = inject(GapAnalysisApiService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.getSummary(this.inventoryId()).subscribe({
      next: data => { this.summary.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  get totalQuantite(): number {
    return this.summary().reduce((acc, s) => acc + s.quantiteTotale, 0);
  }

  getCauseClass(cause: string): string {
    switch (cause) {
      case 'VOL':   return 'cause-danger';
      case 'CASSE': return 'cause-warning';
      case 'PEREMPTION': return 'cause-info';
      default: return 'cause-secondary';
    }
  }

  pct(quantite: number): number {
    const total = this.totalQuantite;
    return total === 0 ? 0 : Math.round((quantite / total) * 100);
  }
}
