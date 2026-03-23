import { Component, inject, ViewChild, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { InputTextModule } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { NgbNavModule, NgbNav } from '@ng-bootstrap/ng-bootstrap';
import { APPEND_TO } from 'app/shared/constants/pagination.constants';
import { DATE_FORMAT_ISO_DATE } from 'app/shared/util/warehouse-util';
import { TauriPrinterService } from 'app/shared/services/tauri-printer.service';
import { handleBlobForTauri } from 'app/shared/util/tauri-util';
import { RepartitionStockService } from '../../../../entities/repartition-stock/repartition-stock.service';
import { AppRepartitionListComponent } from './ui/repartition-list/repartition-list.component';
import { AppSuggestionReassortComponent } from './ui/suggestion-reassort/suggestion-reassort.component';
import { AppManualRepartitionComponent } from './ui/manual-repartition/manual-repartition.component';

@Component({
  selector: 'app-repartition-stock',
  templateUrl: './repartition-stock.component.html',
  styleUrls: ['./repartition-stock.scss'],
  imports: [
    FormsModule,
    ButtonModule,
    ToolbarModule,
    InputTextModule,
    IconField,
    InputIcon,
    DatePicker,
    FloatLabel,
    TableModule,
    CardModule,
    DividerModule,
    RouterModule,
    NgbNavModule,
    AppRepartitionListComponent,
    AppSuggestionReassortComponent,
    AppManualRepartitionComponent,
  ],
})
export class AppRepartitionStockComponent {
  @ViewChild('nav', { static: true }) nav!: NgbNav;

  protected search = '';
  protected dtStart: Date | null = new Date();
  protected dtEnd: Date | null = new Date();
  protected activeTab = 'historique';
  protected readonly appendTo = APPEND_TO;

  private readonly repartitionStockService = inject(RepartitionStockService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly repartitionList = viewChild(AppRepartitionListComponent);
  private readonly suggestionRayonComponent = viewChild<AppSuggestionReassortComponent>('suggestionRayon');
  private readonly suggestionReserveComponent = viewChild<AppSuggestionReassortComponent>('suggestionReserve');

  onSearch(): void {
    if (this.activeTab === 'historique') {
      this.repartitionList()?.onSearch();
    }
  }

  exportToPdf(): void {
    this.repartitionStockService
      .exportToPdf({
        dateDebut: DATE_FORMAT_ISO_DATE(this.dtStart),
        dateFin: DATE_FORMAT_ISO_DATE(this.dtEnd),
        searchTerm: this.search || null,
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
