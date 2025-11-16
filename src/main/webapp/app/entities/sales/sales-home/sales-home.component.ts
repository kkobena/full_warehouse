import { Component, inject, OnInit } from '@angular/core';
import { SalesComponent } from '../sales.component';
import { PresaleComponent } from '../presale/presale.component';
import { VenteEnCoursComponent } from '../vente-en-cours/vente-en-cours.component';
import { CardModule } from 'primeng/card';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { NgbNav, NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SaleToolBarService } from '../service/sale-tool-bar.service';

@Component({
  selector: 'jhi-sales-home',
  imports: [WarehouseCommonModule, CardModule, NgbNav, FormsModule, RouterModule, SalesComponent, PresaleComponent, VenteEnCoursComponent],
  templateUrl: './sales-home.component.html',
  styleUrl: './sales-home.component.scss',
})
export class SalesHomeComponent implements OnInit {
  saleToolBarService = inject(SaleToolBarService);
  protected active = 'ventes-terminees';

  ngOnInit(): void {
    if (this.saleToolBarService.toolBarParam()) {
      if (this.saleToolBarService.toolBarParam().activeTab !== this.active) {
        this.active = this.saleToolBarService.toolBarParam().activeTab;
      }
    }
  }

  protected onNavChange(evt: NgbNavChangeEvent): void {
    this.active = evt.nextId;
    const lastParam = this.saleToolBarService.toolBarParam();
    this.saleToolBarService.updateToolBarParam({ ...lastParam, activeTab: this.active });
  }
}
