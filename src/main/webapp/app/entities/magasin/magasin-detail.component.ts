import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { IMagasin } from 'app/shared/model/magasin.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { PanelModule } from 'primeng/panel';

@Component({
  selector: 'jhi-magasin-detail',
  templateUrl: './magasin-detail.component.html',
  imports: [WarehouseCommonModule, PanelModule, RouterModule],
})
export class MagasinDetailComponent implements OnInit {
  protected activatedRoute = inject(ActivatedRoute);

  magasin: IMagasin | null = null;

  /** Inserted by Angular inject() migration for backwards compatibility */

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ magasin }) => (this.magasin = magasin));
  }

  previousState(): void {
    window.history.back();
  }
}
