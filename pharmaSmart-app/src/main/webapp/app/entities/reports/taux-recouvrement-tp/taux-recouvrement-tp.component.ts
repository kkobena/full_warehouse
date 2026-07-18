import { Component, computed, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

import { FactureApiService } from '../../../features/facturation/data-access/services/facture-api.service';
import { DateRangeFilterComponent } from '../../../shared/components/date-range-filter/date-range-filter.component';
import { IFacturationKpi } from "../../../features/facturation/data-access/models";
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { formatCurrency, formatDecimal, formatNumber } from 'app/shared/utils/format-utils';

@Component({
  selector: 'app-taux-recouvrement-tp',
  imports: [CommonModule, DateRangeFilterComponent],
  templateUrl: './taux-recouvrement-tp.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./taux-recouvrement-tp.component.scss'],
})
export default class TauxRecouvrementTpComponent implements OnInit {
  protected readonly kpi       = signal<IFacturationKpi | null>(null);
  protected readonly isLoading = signal(false);

  protected fromDate = signal<Date | null>(new Date(new Date().getFullYear(), 0, 1));
  protected toDate   = signal<Date | null>(new Date());

  protected readonly tauxColor = computed(() => {
    const t = this.kpi()?.tauxRecouvrement ?? 0;
    if (t >= 80) return 'text-success';
    if (t >= 50) return 'text-warning';
    return 'text-danger';
  });

  protected readonly tauxIcon = computed(() => {
    const t = this.kpi()?.tauxRecouvrement ?? 0;
    if (t >= 80) return 'pi pi-check-circle text-success';
    if (t >= 50) return 'pi pi-exclamation-triangle text-warning';
    return 'pi pi-times-circle text-danger';
  });

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatDecimal  = formatDecimal;
  protected readonly formatNumber   = formatNumber;

  private readonly svc = inject(FactureApiService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.isLoading.set(true);
    this.svc.getKpi({
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate()) ?? undefined,
      toDate:   DATE_FORMAT_ISO_DATE(this.toDate())   ?? undefined,
    }).subscribe({
      next: res => {
        this.kpi.set(res.body);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }
}
