import { Component, inject, OnInit, viewChild, ViewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
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
import { Select } from 'primeng/select';
import { APPEND_TO } from '../../shared/constants/pagination.constants';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { DatePickerComponent } from '../../shared/date-picker/date-picker.component';
import { FloatLabel } from 'primeng/floatlabel';
import { RepartitionStockService } from './repartition-stock.service';
import { ISuggestionReassort, TypeReassort } from './repartition-stock.model';
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
    IconField,
    InputIcon,
    DatePickerComponent,
    RepartitionListComponent,
    SuggestionReassortComponent,
    ManualRepartitionComponent,
    NgbNavModule,
  ],
})
export class RepartitionStockComponent implements OnInit {
  @ViewChild('nav', { static: true }) nav!: NgbNav;

  protected translate = inject(TranslateService);
  protected repartitionService = inject(RepartitionStockService);
  protected router = inject(Router);
  protected activatedRoute = inject(ActivatedRoute);
  protected repartitionList = viewChild(RepartitionListComponent);
  protected suggestionRayonComponent = viewChild('suggestionRayon', { read: SuggestionReassortComponent });
  protected suggestionReserveComponent = viewChild('suggestionReserve', { read: SuggestionReassortComponent });

  protected search = '';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected activeTab = 'historique';
  protected readonly appendTo = APPEND_TO;

  ngOnInit(): void {
    const currentDate = new Date();
    this.fromDate = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
    this.toDate = currentDate;
  }

  protected onSearch(): void {
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
