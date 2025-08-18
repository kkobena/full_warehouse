import { Component, viewChild } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import {
  DailySaleProduitHistoriqueComponent
} from './daily-sale-produit-historique/daily-sale-produit-historique.component';
import {
  YearlySaleProduitHistoriqueComponent
} from './yearly-sale-produit-historique/yearly-sale-produit-historique.component';
import { ProduitAuditingParam } from '../../../shared/model/produit-record.model';

@Component({
  selector: 'jhi-stat-sales',
  imports: [WarehouseCommonModule, DailySaleProduitHistoriqueComponent, YearlySaleProduitHistoriqueComponent],
  templateUrl: './stat-sales.component.html'
})
export class StatSalesComponent {
  readonly dailySaleProduitHistoriqueComponent = viewChild(DailySaleProduitHistoriqueComponent);
  readonly yearlySaleProduitHistoriqueComponent = viewChild(YearlySaleProduitHistoriqueComponent);
  protected active = 'daily';

  load(produitAuditingParam: ProduitAuditingParam): void {
    if (this.active === 'daily') {
      this.dailySaleProduitHistoriqueComponent()?.load(produitAuditingParam);
    } else {
      this.yearlySaleProduitHistoriqueComponent()?.load(produitAuditingParam);
    }
  }

  exportPdf(produitAuditingParam: ProduitAuditingParam): void {
    if (this.active === 'daily') {
      this.dailySaleProduitHistoriqueComponent()?.exportPdf(produitAuditingParam);
    } else {
      this.yearlySaleProduitHistoriqueComponent()?.exportPdf(produitAuditingParam);
    }
  }
}
