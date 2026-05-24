import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { Button } from 'primeng/button';

import { TiersPayantReportService } from '../services/tiers-payant-report.service';
import { BlobDownloadService } from '../../../shared/services/blob-download.service';
import { ITiersPayantCreancesSummary } from 'app/shared/model/report/tiers-payant-report.model';
import { formatCurrency, formatNumber } from 'app/shared/utils/format-utils';

@Component({
  selector: 'app-situation-creances',
  imports: [CommonModule, TableModule, Button],
  templateUrl: './situation-creances.component.html',
  styleUrls: ['./situation-creances.component.scss'],
})
export default class SituationCreancesComponent implements OnInit {
  protected readonly summary     = signal<ITiersPayantCreancesSummary[]>([]);
  protected readonly isLoading   = signal(false);
  protected readonly exportLoading = signal(false);

  protected readonly totalCreances    = computed(() => this.summary().reduce((s, c) => s + (c.montantTotal ?? 0), 0));
  protected readonly totalPlusDe90j   = computed(() => this.summary().reduce((s, c) => s + (c.montantPlusDe90Jours ?? 0), 0));
  protected readonly totalNbFactures  = computed(() => this.summary().reduce((s, c) => s + (c.nombreFactures ?? 0), 0));
  protected readonly pctRisque        = computed(() => {
    const total = this.totalCreances();
    return total > 0 ? Math.round((this.totalPlusDe90j() / total) * 100) : 0;
  });

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatNumber   = formatNumber;

  private readonly svc           = inject(TiersPayantReportService);
  private readonly blobDownload  = inject(BlobDownloadService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.isLoading.set(true);
    this.svc.getCreancesSummary().subscribe({
      next: res => {
        this.summary.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  protected exportPdf(): void {
    this.blobDownload.downloadFromObservable(
      this.svc.exportCreancesToPdf(),
      'situation-creances',
      'pdf',
      () => this.exportLoading.set(true),
      () => this.exportLoading.set(false),
    );
  }
}
