import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { ICategorie } from 'app/shared/model/categorie.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-categorie-detail',
  templateUrl: './categorie-detail.component.html',
  imports: [WarehouseCommonModule, RouterModule]
})
export class CategorieDetailComponent implements OnInit {
  protected activatedRoute = inject(ActivatedRoute);

  categorie: ICategorie | null = null;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ categorie }) => (this.categorie = categorie));
  }

  previousState(): void {
    window.history.back();
  }
}
