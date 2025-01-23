import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { ISales } from 'app/shared/model/sales.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { DecimalPipe } from '@angular/common';
import { Button } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

@Component({
    selector: 'jhi-sales-detail',
    templateUrl: './sales-detail.component.html',
    imports: [WarehouseCommonModule, RouterModule, DecimalPipe, Button, ToolbarModule]
})
export class SalesDetailComponent implements OnInit {
  sales: ISales | null = null;
  classInfoSale = 'col-md-5';
  classInfoProduit = 'col-md-7';
  classCustomer = 'col-md-3';

  activatedRoute = inject(ActivatedRoute);

  constructor() {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ sales }) => {
      this.sales = sales;
      if (sales.type === 'VNO' && sales.customer) {
        this.classInfoSale = 'col-md-3';
        this.classInfoProduit = 'col-md-6';
      } else if (sales.type === 'VO') {
        this.classInfoProduit = 'col-md-5';
        this.classInfoSale = 'col-md-2';
        this.classCustomer = 'col-md-2';
      }
    });
  }

  previousState(): void {
    window.history.back();
  }
}
