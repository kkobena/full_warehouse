import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ISalesLine } from 'app/shared/model/sales-line.model';

@Component({
  selector: 'jhi-sales-line-detail',
  templateUrl: './sales-line-detail.component.html',
})
export class SalesLineDetailComponent implements OnInit {
  salesLine: ISalesLine | null = null;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ salesLine }) => (this.salesLine = salesLine));
  }

  previousState(): void {
    window.history.back();
  }
}
