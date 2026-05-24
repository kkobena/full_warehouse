import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';

import { DiffereApiService } from '../../../features/differes/data-access/services/differe-api.service';
import { DateRangeFilterComponent } from '../../../shared/components/date-range-filter/date-range-filter.component';
import { IDiffere, IDiffereSummary } from "../../../features/differes/data-access/models";
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { formatCurrency, formatNumber } from 'app/shared/utils/format-utils';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-vieillissement-differes',
  imports: [CommonModule, TableModule, DateRangeFilterComponent],
  templateUrl: './vieillissement-differes.component.html',
  styleUrls: ['./vieillissement-differes.component.scss'],
})
export default class VieillissementDifferesComponent implements OnInit {
  protected readonly differes    = signal<IDiffere[]>([]);
  protected readonly summary     = signal<IDiffereSummary | null>(null);
  protected readonly isLoading   = signal(false);

  protected fromDate = signal<Date | null>(new Date());
  protected toDate   = signal<Date | null>(new Date());

  protected readonly totalSolde   = computed(() => this.summary()?.rest ?? 0);
  protected readonly totalAccorde = computed(() => this.summary()?.saleAmount ?? 0);
  protected readonly totalPaye    = computed(() => this.summary()?.paidAmount ?? 0);
  protected readonly tauxRemb     = computed(() => {
    const acc = this.totalAccorde();
    return acc > 0 ? Math.round((this.totalPaye() / acc) * 100) : 0;
  });

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatNumber   = formatNumber;

  private readonly svc = inject(DiffereApiService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.isLoading.set(true);
    const params = {
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate()),
      toDate: DATE_FORMAT_ISO_DATE(this.toDate()),
    };
    forkJoin({
      summary: this.svc.getDiffereSummary(params),
      list:    this.svc.query(params),
    }).subscribe({
      next: data => {
        this.summary.set(data.summary.body);
        this.differes.set(data.list.body ?? []);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }
}
