import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';

import {
  DiffereApiService
} from '../../../features/differes/data-access/services/differe-api.service';
import {PharmaDatePickerComponent} from '../../../shared/date-picker/pharma-date-picker.component';
import {IDiffere, IDiffereSummary} from "../../../features/differes/data-access/models";
import {DATE_FORMAT_ISO_DATE} from '../../../shared/util/warehouse-util';
import {formatCurrency, formatNumber} from 'app/shared/utils/format-utils';
import {forkJoin} from 'rxjs';
import {ButtonComponent, DataTableComponent, SortableHeaderDirective, ToolbarComponent} from '../../../shared/ui';

@Component({
  selector: 'app-vieillissement-differes',
  imports: [
    CommonModule,
    FormsModule,
    PharmaDatePickerComponent,
    ButtonComponent,
    DataTableComponent,
    SortableHeaderDirective,
    ToolbarComponent
  ],
  templateUrl: './vieillissement-differes.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./vieillissement-differes.component.scss'],
})
export default class VieillissementDifferesComponent implements OnInit {
  protected readonly differes = signal<IDiffere[]>([]);
  protected readonly summary = signal<IDiffereSummary | null>(null);
  protected readonly isLoading = signal(false);

  protected fromDate = signal<Date | null>(new Date());
  protected toDate = signal<Date | null>(new Date());

  // `computed`, pas un appel direct à `dateToStruct()` dans le template : voir
  // date-range-filter (supprimé) pour l'explication de la boucle silencieuse évitée.
  protected readonly fromStruct = computed(() => this.dateToStruct(this.fromDate()));
  protected readonly toStruct = computed(() => this.dateToStruct(this.toDate()));

  protected readonly totalSolde = computed(() => this.summary()?.rest ?? 0);
  protected readonly totalAccorde = computed(() => this.summary()?.saleAmount ?? 0);
  protected readonly totalPaye = computed(() => this.summary()?.paidAmount ?? 0);
  protected readonly tauxRemb = computed(() => {
    const acc = this.totalAccorde();
    return acc > 0 ? Math.round((this.totalPaye() / acc) * 100) : 0;
  });

  protected readonly formatCurrency = formatCurrency;
  protected readonly formatNumber = formatNumber;

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
      list: this.svc.query(params),
    }).subscribe({
      next: data => {
        this.summary.set(data.summary.body);
        this.differes.set(data.list.body ?? []);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
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
