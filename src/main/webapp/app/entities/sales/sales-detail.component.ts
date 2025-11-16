import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { ISales } from 'app/shared/model/sales.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { DecimalPipe } from '@angular/common';
import { Button } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-sales-detail',
  templateUrl: './sales-detail.component.html',
  styleUrls: ['./sales-details.component.scss', './sales-detail.component.scss'],
  imports: [WarehouseCommonModule, RouterModule, DecimalPipe, Button, ToolbarModule, Card],
})
export class SalesDetailComponent implements OnInit {
  sales: ISales | null = null;
  classInfoSale = 'col-md-5 col-sm-12 col-lg-5 col-xl-5 mb-2';
  classInfoProduit = 'col-md-7  col-sm-12 col-lg-7 col-xl-7 mb-2';
  classCustomer = 'col-md-3 col-sm-12 col-lg-3 col-xl-3 mb-2';
  classTiersPayant = 'col-md-6 col-sm-12 col-lg-6 col-xl-6 mb-2';

  activatedRoute = inject(ActivatedRoute);

  constructor() {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ sales }) => {
      this.sales = sales;
      if (sales.type === 'VNO' && sales.customer) {
        this.classInfoSale = 'col-md-3 col-sm-12 col-lg-3 col-xl-3 mb-2';
        this.classInfoProduit = 'col-md-6  col-sm-12 col-lg-6 col-xl-6 mb-2';
      } else if (sales.type === 'VO') {
        this.classInfoProduit = 'col-md-6 col-sm-12 col-lg-6 col-xl-6 mb-2';
        this.classInfoSale = 'col-md-6 col-sm-12 col-lg-6 col-xl-6 mb-2';
        this.classCustomer = 'col-md-6 col-sm-12 col-lg-6 col-xl-6 mb-2';
      }
    });
  }

  previousState(): void {
    window.history.back();
  }
}
