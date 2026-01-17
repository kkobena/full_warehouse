import { Component, inject, input, viewChild, ViewChild } from '@angular/core';
import { RouterModule } from '@angular/router';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { NgxSpinnerModule } from 'ngx-spinner';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { APPEND_TO } from '../../shared/constants/pagination.constants';
import { RepartitionListComponent } from './repartition-list/repartition-list.component';
import { SuggestionReassortComponent } from './suggestion-reassort/suggestion-reassort.component';
import { ManualRepartitionComponent } from './manual-repartition/manual-repartition.component';
import { NgbNav, NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { RepartitionStockService } from './repartition-stock.service';
import { DATE_FORMAT_ISO_DATE } from '../../shared/util/warehouse-util';
import { formatDate } from '@angular/common';
import { TauriPrinterService } from '../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../shared/util/tauri-util';

@Component({
  selector: 'jhi-repartition-stock',
  templateUrl: './repartition-stock.component.html',
  styleUrls: ['./repartition-stock.component.scss'],
  imports: [
    WarehouseCommonModule,
    DividerModule,
    ButtonModule,
    TableModule,
    NgxSpinnerModule,
    CardModule,
    ToolbarModule,
    RouterModule,
    FormsModule,
    InputTextModule,
    RepartitionListComponent,
    SuggestionReassortComponent,
    ManualRepartitionComponent,
    NgbNavModule,
  ],
})
export class RepartitionStockComponent {
  @ViewChild('nav', { static: true }) nav!: NgbNav;
  readonly dtStart = input<Date | null>(null);
  readonly dtEnd = input<Date | null>(null);
  readonly search = input('');
  protected repartitionList = viewChild(RepartitionListComponent);
  protected suggestionRayonComponent = viewChild('suggestionRayon', { read: SuggestionReassortComponent });
  protected suggestionReserveComponent = viewChild('suggestionReserve', { read: SuggestionReassortComponent });

  protected activeTab = 'historique';
  protected readonly appendTo = APPEND_TO;
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly repartitionStockService = inject(RepartitionStockService);

  onSearch(): void {
    if (this.activeTab === 'historique') {
      this.repartitionList()?.onSearch();
    }
  }

  exportToPdf(): void {
    this.repartitionStockService
      .exportToPdf({
        dateDebut: DATE_FORMAT_ISO_DATE(this.dtStart()),
        dateFin: DATE_FORMAT_ISO_DATE(this.dtEnd()),
        searchTerm: this.search() || null,
      })
      .subscribe(blob => {
        if (this.tauriPrinterService.isRunningInTauri()) {
          handleBlobForTauri(blob, 'Repartition_Stock');
        } else {
          window.open(URL.createObjectURL(blob));
        }
      });
  }

  protected onTabChange(navChangeEvent: any): void {
    if (this.activeTab === 'rayon') {
      this.suggestionRayonComponent()?.reloadSuggestions();
    } else if (this.activeTab === 'reserve') {
      this.suggestionReserveComponent()?.reloadSuggestions();
    }
  }
}
