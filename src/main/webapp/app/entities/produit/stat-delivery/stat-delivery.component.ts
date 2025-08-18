import { Component, viewChild } from '@angular/core';
import { ProduitAuditingParam } from '../../../shared/model/produit-record.model';
import {
  YearlyDeleveryProduitHistoriqueComponent
} from './yearly-delevery-produit-historique/yearly-delevery-produit-historique.component';
import {
  DailyDeleveryProduitHistoriqueComponent
} from './daily-delevery-produit-historique/daily-delevery-produit-historique.component';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-stat-delivery',
  imports: [WarehouseCommonModule, DailyDeleveryProduitHistoriqueComponent, YearlyDeleveryProduitHistoriqueComponent],
  templateUrl: './stat-delivery.component.html'
})
export class StatDeliveryComponent {
  protected readonly daily = viewChild.required(DailyDeleveryProduitHistoriqueComponent);
  protected readonly yearly = viewChild.required(YearlyDeleveryProduitHistoriqueComponent);
  protected active = 'daily';

  load(produitAuditingParam: ProduitAuditingParam): void {
    if (this.active === 'daily') {
      this.daily()?.load(produitAuditingParam);
    } else {
      this.yearly()?.load(produitAuditingParam);
    }
  }

  exportPdf(produitAuditingParam: ProduitAuditingParam): void {
    if (this.active === 'daily') {
      this.daily()?.exportPdf(produitAuditingParam);
    } else {
      this.yearly()?.exportPdf(produitAuditingParam);
    }
  }
}
