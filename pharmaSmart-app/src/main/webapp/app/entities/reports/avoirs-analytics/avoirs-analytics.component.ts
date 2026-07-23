import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';

import {
  AvoirApiService
} from '../../../features/facturation/data-access/services/avoir-api.service';
import {PharmaDatePickerComponent} from '../../../shared/date-picker/pharma-date-picker.component';
import {IAvoir} from "../../../features/facturation/data-access/models";
import {DATE_FORMAT_ISO_DATE} from '../../../shared/util/warehouse-util';
import {formatCurrency, formatDateFR, formatNumber} from 'app/shared/utils/format-utils';
import {ButtonComponent, DataTableComponent, SortableHeaderDirective, ToolbarComponent} from '../../../shared/ui';

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
  imports: [
    CommonModule,
    FormsModule,
    PharmaDatePickerComponent,
    ButtonComponent,
    DataTableComponent,
    SortableHeaderDirective,
    ToolbarComponent
  ],
  templateUrl: './avoirs-analytics.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./avoirs-analytics.component.scss'],
})
export default class AvoirsAnalyticsComponent implements OnInit {
  protected readonly avoirs = signal<IAvoir[]>([]);
  protected readonly isLoading = signal(false);

  protected fromDate = signal<Date | null>(new Date(new Date().getFullYear(), 0, 1)); // 1er jan
  protected toDate = signal<Date | null>(new Date());

  // `computed`, pas un appel direct à `dateToStruct()` dans le template : voir
  // date-range-filter (supprimé) pour l'explication de la boucle silencieuse évitée.
  protected readonly fromStruct = computed(() => this.dateToStruct(this.fromDate()));
  protected readonly toStruct = computed(() => this.dateToStruct(this.toDate()));

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
      {
        statut: 'EMIS',
        label: 'Émis',
        count: byStatut('EMIS').length,
        montant: byStatut('EMIS').reduce((s, a) => s + (a.montantAvoir ?? 0), 0),
        badgeClass: 'bg-primary-subtle text-primary'
      },
      {
        statut: 'IMPUTE',
        label: 'Imputés',
        count: byStatut('IMPUTE').length,
        montant: byStatut('IMPUTE').reduce((s, a) => s + (a.montantAvoir ?? 0), 0),
        badgeClass: 'bg-success-subtle text-success'
      },
      {
        statut: 'DRAFT',
        label: 'Brouillons',
        count: byStatut('DRAFT').length,
        montant: byStatut('DRAFT').reduce((s, a) => s + (a.montantAvoir ?? 0), 0),
        badgeClass: 'bg-secondary-subtle text-secondary'
      },
      {
        statut: 'ANNULE',
        label: 'Annulés',
        count: byStatut('ANNULE').length,
        montant: byStatut('ANNULE').reduce((s, a) => s + (a.montantAvoir ?? 0), 0),
        badgeClass: 'bg-danger-subtle text-danger'
      },
    ];
  });

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatNumber = formatNumber;
  protected readonly formatDateFR = formatDateFR;

  private readonly svc = inject(AvoirApiService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.isLoading.set(true);
    this.svc.query({
      startDate: DATE_FORMAT_ISO_DATE(this.fromDate()),
      endDate: DATE_FORMAT_ISO_DATE(this.toDate()),
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
      case 'DRAFT':
        return 'Brouillon';
      case 'EMIS':
        return 'Émis';
      case 'IMPUTE':
        return 'Imputé';
      case 'ANNULE':
        return 'Annulé';
      default:
        return statut ?? '';
    }
  }

  protected statutBadge(statut?: string): string {
    switch (statut) {
      case 'EMIS':
        return 'bg-primary-subtle text-primary';
      case 'IMPUTE':
        return 'bg-success-subtle text-success';
      case 'DRAFT':
        return 'bg-secondary-subtle text-secondary';
      case 'ANNULE':
        return 'bg-danger-subtle text-danger';
      default:
        return 'bg-light text-muted';
    }
  }

  protected dateToStruct(d: Date | null): NgbDateStruct | null {
    if (!d) return null;
    return { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
  }

  protected structToDate(s: NgbDateStruct | null): Date | null {
    if (!s) return null;
    return new Date(s.year, s.month - 1, s.day);
  }
}
