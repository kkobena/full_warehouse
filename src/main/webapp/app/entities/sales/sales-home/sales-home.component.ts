import { Component, inject, OnInit } from '@angular/core';
import { SalesComponent } from '../sales.component';
import { PresaleComponent } from '../presale/presale.component';
import { VenteEnCoursComponent } from '../vente-en-cours/vente-en-cours.component';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { CardModule } from 'primeng/card';
import { DecimalPipe } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { NgbNav, NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';
import { ToolbarModule } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { PanelModule } from 'primeng/panel';
import { BadgeModule } from 'primeng/badge';
import { RippleModule } from 'primeng/ripple';
import { SaleToolBarService } from '../service/sale-tool-bar.service';

@Component({
    selector: 'jhi-sales-home',
    imports: [
        WarehouseCommonModule,
        ButtonModule,
        CalendarModule,
        CardModule,
        DividerModule,
        DropdownModule,
        InputTextModule,
        NgbNav,
        ToolbarModule,
        FormsModule,
        RouterModule,
        PanelModule,
        BadgeModule,
        AutoCompleteModule,
        RippleModule,
        SalesComponent,
        PresaleComponent,
        VenteEnCoursComponent,
        AutoCompleteModule,
        ButtonModule,
        CalendarModule,
        CardModule,
        DecimalPipe,
        FaIconComponent,
    ],
    templateUrl: './sales-home.component.html',
    styleUrl: './sales-home.component.scss'
})
export class SalesHomeComponent implements OnInit {
  saleToolBarService = inject(SaleToolBarService);
  protected active = 'ventes-terminees';

  constructor() {}

  ngOnInit(): void {
    if (this.saleToolBarService.toolBarParam()) {
      if (this.saleToolBarService.toolBarParam().activeTab !== this.active) {
        this.active = this.saleToolBarService.toolBarParam().activeTab;
      }
    }
  }

  onNavChange(evt: NgbNavChangeEvent): void {
    this.active = evt.nextId;
    const lastParam = this.saleToolBarService.toolBarParam();
    this.saleToolBarService.updateToolBarParam({ ...lastParam, activeTab: this.active });
  }
}
