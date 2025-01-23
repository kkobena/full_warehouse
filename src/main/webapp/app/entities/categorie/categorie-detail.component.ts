import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { ICategorie } from 'app/shared/model/categorie.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
    selector: 'jhi-categorie-detail',
    templateUrl: './categorie-detail.component.html',
    imports: [WarehouseCommonModule, RouterModule]
})
export class CategorieDetailComponent implements OnInit {
  categorie: ICategorie | null = null;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ categorie }) => (this.categorie = categorie));
  }

  previousState(): void {
    window.history.back();
  }
}
