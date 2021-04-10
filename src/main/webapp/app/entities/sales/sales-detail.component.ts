import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ISales } from 'app/shared/model/sales.model';

@Component({
  selector: 'jhi-sales-detail',
  templateUrl: './sales-detail.component.html',
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
