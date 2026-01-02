import { Component, inject, input, viewChild, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
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
import { RepartitionStockService } from './repartition-stock.service';
import { RepartitionListComponent } from './repartition-list/repartition-list.component';
import { SuggestionReassortComponent } from './suggestion-reassort/suggestion-reassort.component';
import { ManualRepartitionComponent } from './manual-repartition/manual-repartition.component';
import { NgbNav, NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

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
  protected translate = inject(TranslateService);
  protected repartitionService = inject(RepartitionStockService);
  protected router = inject(Router);
  protected activatedRoute = inject(ActivatedRoute);
  protected repartitionList = viewChild(RepartitionListComponent);
  protected suggestionRayonComponent = viewChild('suggestionRayon', { read: SuggestionReassortComponent });
  protected suggestionReserveComponent = viewChild('suggestionReserve', { read: SuggestionReassortComponent });

  protected activeTab = 'historique';
  protected readonly appendTo = APPEND_TO;

  onSearch(): void {
    if (this.activeTab === 'historique') {
      this.repartitionList()?.onSearch();
    }
  }

  protected onTabChange(navChangeEvent: any): void {
    if (this.activeTab === 'rayon') {
      this.suggestionRayonComponent()?.loadSuggestions();
    } else if (this.activeTab === 'reserve') {
      this.suggestionReserveComponent()?.loadSuggestions();
    }
  }

  protected onRefreshHistory(): void {
    this.repartitionList()?.onSearch();
  }
}
