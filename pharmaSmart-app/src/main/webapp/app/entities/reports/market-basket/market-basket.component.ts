import {ChangeDetectionStrategy, Component, inject, input, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {HttpResponse} from '@angular/common/http';

import {NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';

import {
  BadgeComponent,
  ButtonComponent,
  FloatLabelComponent,
  InputNumberComponent,
  ToolbarComponent
} from '../../../shared/ui';
import {PharmaDatePickerComponent} from '../../../shared/date-picker/pharma-date-picker.component';
import {NGB_DATE_TO_ISO} from '../../../shared/util/warehouse-util';


import {MarketBasketService} from '../services/market-basket.service';
import {
  IMarketBasketSummary,
  IProductAssociation
} from 'app/shared/model/report/market-basket.model';
import {formatDecimal, formatNumber, formatPercent} from 'app/shared/utils/format-utils';

@Component({
  selector: 'app-market-basket',
  imports: [
    CommonModule,
    FormsModule,
    BadgeComponent,
    ButtonComponent,
    FloatLabelComponent,
    InputNumberComponent,
    ToolbarComponent,
    PharmaDatePickerComponent,
  ],
  templateUrl: './market-basket.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './market-basket.component.scss',
})
export default class MarketBasketComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement.
   */
  readonly navCode = input<string>('');
  // Signals
  associations = signal<IProductAssociation[]>([]);
  summary = signal<IMarketBasketSummary | null>(null);
  isLoading = signal<boolean>(false);
  // Filter values
  startDate: NgbDateStruct = this.dateToNgbStruct(new Date(new Date().setMonth(new Date().getMonth() - 6)));
  endDate: NgbDateStruct = this.dateToNgbStruct(new Date());
  minSupport = 1.0;
  minConfidence = 10.0;
  limit = 50;
  // Format methods using shared utilities
  formatNumber = formatNumber;
  formatPercent = formatPercent;
  formatDecimal = formatDecimal;
  private marketBasketService = inject(MarketBasketService);

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);

    const startDateStr = NGB_DATE_TO_ISO(this.startDate)!;
    const endDateStr = NGB_DATE_TO_ISO(this.endDate)!;

    // Load associations
    this.marketBasketService.getProductAssociations(startDateStr, endDateStr, this.minSupport, this.minConfidence, this.limit).subscribe({
      next: (res: HttpResponse<IProductAssociation[]>) => {
        this.associations.set(res.body ?? []);
      },
      error: () => {
        this.associations.set([]);
      },
    });

    // Load summary
    this.marketBasketService.getSummary(startDateStr, endDateStr).subscribe({
      next: (res: HttpResponse<IMarketBasketSummary>) => {
        this.summary.set(res.body ?? null);
        this.isLoading.set(false);
      },
      error: () => {
        this.summary.set(null);
        this.isLoading.set(false);
      },
    });
  }

  onFilterChange(): void {
    this.loadData();
  }

  getLiftSeverity(lift: number | undefined): 'success' | 'warn' | 'danger' | 'secondary' {
    if (!lift) {
      return 'secondary';
    }
    if (lift >= 2.0) {
      return 'success';
    }
    if (lift >= 1.0) {
      return 'warn';
    }
    return 'danger';
  }

  getConfidenceSeverity(confidence: number | undefined): 'success' | 'warn' | 'danger' | 'secondary' {
    if (!confidence) {
      return 'secondary';
    }
    if (confidence >= 50) {
      return 'success';
    }
    if (confidence >= 25) {
      return 'warn';
    }
    return 'danger';
  }

  getLiftLabel(lift: number | undefined): string {
    if (!lift) {
      return 'Neutre';
    }
    if (lift >= 2.0) {
      return 'Très forte';
    }
    if (lift >= 1.5) {
      return 'Forte';
    }
    if (lift >= 1.0) {
      return 'Positive';
    }
    return 'Faible';
  }

  private dateToNgbStruct(date: Date): NgbDateStruct {
    return {year: date.getFullYear(), month: date.getMonth() + 1, day: date.getDate()};
  }
}
