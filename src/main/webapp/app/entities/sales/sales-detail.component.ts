import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { ISales } from 'app/shared/model/sales.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-sales-detail',
  templateUrl: './sales-detail.component.html',
  imports: [WarehouseCommonModule, RouterModule],
  standalone: true,
})
export class SalesDetailComponent implements OnInit {
  sales: ISales | null = null;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ sales }) => (this.sales = sales));
  }

  previousState(): void {
    window.history.back();
  }
}
