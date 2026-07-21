import {Component, inject, OnInit, signal, ChangeDetectionStrategy} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';


import {IStockAlert, StockAlertType} from 'app/shared/model/report/stock-alert.model';
import {StockAlertReportService} from '../services/stock-alert-report.service';
import {TauriPrinterService} from "../../../shared/services/tauri-printer.service";
import {handleBlobForTauri} from "../../../shared/util/tauri-util";
import {
  AppTableLazyLoadEvent,
  ButtonComponent,
  DataTableComponent,
  MultiSelectComponent,
  ToolbarComponent
} from '../../../shared/ui';

const ITEMS_PER_PAGE = 15;

@Component({
  selector: 'jhi-stock-alerts',
  templateUrl: './stock-alerts.component.html',
  styleUrl: './stock-alerts.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    MultiSelectComponent,
    ToolbarComponent
  ],
})
export default class StockAlertsComponent implements OnInit {
  alerts = signal<IStockAlert[]>([]);
  alertCounts = signal<Record<string, number>>({});
  selectedAlertTypes = signal<StockAlertType[]>([]);
  isLoading = signal<boolean>(false);
  totalItems = signal<number>(0);
  rowsPerPage = ITEMS_PER_PAGE;

  // Expose StockAlertType enum to template
  readonly StockAlertType = StockAlertType;

  alertTypeOptions = [
    {label: 'Rupture de stock', value: StockAlertType.RUPTURE},
    {label: 'Alerte stock bas', value: StockAlertType.ALERTE},
    {label: 'Proche péremption', value: StockAlertType.PEREMPTION},
  ];

  private readonly stockAlertService = inject(StockAlertReportService);
  private readonly tauriPrinter = inject(TauriPrinterService);

  ngOnInit(): void {
    this.loadAlertCounts();
  }

  loadAlerts(event?: AppTableLazyLoadEvent): void {
    this.isLoading.set(true);
    const page = event ? event.first / event.rows : 0;
    const size = event?.rows ?? this.rowsPerPage;
    const types = this.selectedAlertTypes().length > 0 ? this.selectedAlertTypes() : undefined;

    this.stockAlertService
      .getStockAlerts({page, size, sort: ['alertType,asc', 'libelle,asc'], types})
      .subscribe({
        next: (res: HttpResponse<IStockAlert[]>) => {
          this.totalItems.set(Number(res.headers.get('X-Total-Count')));
          this.alerts.set(res.body ?? []);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        },
      });
  }

  loadAlertCounts(): void {
    this.stockAlertService.getStockAlertsCount().subscribe({
      next: (res: HttpResponse<Record<StockAlertType, number>>) => {
        this.alertCounts.set(res.body ?? {});
      },
    });
  }

  onFilterChange(): void {
    this.loadAlerts();
  }

  exportToPdf(): void {
    const types = this.selectedAlertTypes().length > 0 ? this.selectedAlertTypes() : undefined;

    this.stockAlertService.exportStockAlertsToPdf(types)
      .subscribe(resp => {
        if (this.tauriPrinter.isRunningInTauri()) {
          handleBlobForTauri(resp.body, `stock-alerts`);
        } else {
          window.open(URL.createObjectURL(resp.body));
        }
      });


  }

  getAlertClass(alertType?: StockAlertType): string {
    switch (alertType) {
      case StockAlertType.RUPTURE:    return 'alert-badge alert-rupture';
      case StockAlertType.ALERTE:     return 'alert-badge alert-alerte';
      case StockAlertType.PEREMPTION: return 'alert-badge alert-peremption';
      default:                        return 'alert-badge alert-peremption';
    }
  }

  getAlertLabel(alertType?: StockAlertType): string {
    switch (alertType) {
      case StockAlertType.RUPTURE:
        return 'Rupture';
      case StockAlertType.ALERTE:
        return 'Stock bas';
      case StockAlertType.PEREMPTION:
        return 'Péremption';
      default:
        return '';
    }
  }
}
