import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';

import { AvoirApiService } from '../../../features/facturation/data-access/services/avoir-api.service';
import { DateRangeFilterComponent } from '../../../shared/components/date-range-filter/date-range-filter.component';
import { IAvoir } from "../../../features/facturation/data-access/models";
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { formatCurrency, formatDateFR, formatNumber } from 'app/shared/utils/format-utils';

type AvoirStatut = 'DRAFT' | 'EMIS' | 'IMPUTE' | 'ANNULE';

interface AvoirStatutStat {
  statut: AvoirStatut;
  label: string;
  count: number;
  montant: number;
  badgeClass: string;
}

@Component({
  selector: 'app-avoirs-analytics',
  imports: [CommonModule, TableModule, DateRangeFilterComponent],
  templateUrl: './avoirs-analytics.component.html',
  styleUrls: ['./avoirs-analytics.component.scss'],
})
export default class AvoirsAnalyticsComponent implements OnInit {
  protected readonly avoirs    = signal<IAvoir[]>([]);
  protected readonly isLoading = signal(false);

  protected fromDate = signal<Date | null>(new Date(new Date().getFullYear(), 0, 1)); // 1er jan
  protected toDate   = signal<Date | null>(new Date());

  protected readonly totalMontant = computed(() =>
    this.avoirs()
      .filter(a => a.statut === 'EMIS' || a.statut === 'IMPUTE')
      .reduce((s, a) => s + (a.montantAvoir ?? 0), 0)
  );

  protected readonly montantImpute = computed(() =>
    this.avoirs()
      .filter(a => a.statut === 'IMPUTE')
      .reduce((s, a) => s + (a.montantAvoir ?? 0), 0)
  );

  protected readonly tauxImputation = computed(() => {
    const total = this.totalMontant();
    return total > 0 ? Math.round((this.montantImpute() / total) * 100) : 0;
  });

  protected readonly stats = computed<AvoirStatutStat[]>(() => {
    const avoirs = this.avoirs();
    const byStatut = (s: AvoirStatut) => avoirs.filter(a => a.statut === s);
    return [
      { statut: 'EMIS',    label: 'Émis',    count: byStatut('EMIS').length,    montant: byStatut('EMIS').reduce((s, a) => s + (a.montantAvoir ?? 0), 0),    badgeClass: 'bg-primary-subtle text-primary' },
      { statut: 'IMPUTE',  label: 'Imputés', count: byStatut('IMPUTE').length,  montant: byStatut('IMPUTE').reduce((s, a) => s + (a.montantAvoir ?? 0), 0),  badgeClass: 'bg-success-subtle text-success' },
      { statut: 'DRAFT',   label: 'Brouillons', count: byStatut('DRAFT').length,   montant: byStatut('DRAFT').reduce((s, a) => s + (a.montantAvoir ?? 0), 0),   badgeClass: 'bg-secondary-subtle text-secondary' },
      { statut: 'ANNULE',  label: 'Annulés', count: byStatut('ANNULE').length,  montant: byStatut('ANNULE').reduce((s, a) => s + (a.montantAvoir ?? 0), 0),  badgeClass: 'bg-danger-subtle text-danger' },
    ];
  });

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatNumber   = formatNumber;
  protected readonly formatDateFR   = formatDateFR;

  private readonly svc = inject(AvoirApiService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.isLoading.set(true);
    this.svc.query({
      startDate: DATE_FORMAT_ISO_DATE(this.fromDate()),
      endDate:   DATE_FORMAT_ISO_DATE(this.toDate()),
      size: 9999,
      page: 0,
    }).subscribe({
      next: res => {
        this.avoirs.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  protected statutLabel(statut?: string): string {
    switch (statut) {
      case 'DRAFT':  return 'Brouillon';
      case 'EMIS':   return 'Émis';
      case 'IMPUTE': return 'Imputé';
      case 'ANNULE': return 'Annulé';
      default:       return statut ?? '';
    }
  }

  protected statutBadge(statut?: string): string {
    switch (statut) {
      case 'EMIS':   return 'bg-primary-subtle text-primary';
      case 'IMPUTE': return 'bg-success-subtle text-success';
      case 'DRAFT':  return 'bg-secondary-subtle text-secondary';
      case 'ANNULE': return 'bg-danger-subtle text-danger';
      default:       return 'bg-light text-muted';
    }
  }
}
