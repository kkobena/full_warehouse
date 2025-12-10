import { Component, viewChild } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import {
  DailySaleProduitHistoriqueComponent
} from './daily-sale-produit-historique/daily-sale-produit-historique.component';
import {
  YearlySaleProduitHistoriqueComponent
} from './yearly-sale-produit-historique/yearly-sale-produit-historique.component';
import { ProduitAuditingParam } from '../../../shared/model/produit-record.model';
import { Button } from 'primeng/button';
import { DatePickerComponent } from '../../../shared/date-picker/date-picker.component';
import {
  ProduitAutocompleteComponent
} from '../../../shared/produit-autocomplete/produit-autocomplete.component';
import { Toolbar } from 'primeng/toolbar';
import { Tooltip } from 'primeng/tooltip';
import { PRODUIT_COMBO_RESULT_SIZE } from '../../../shared/constants/pagination.constants';

@Component({
  selector: 'jhi-stat-sales',
  imports: [
    WarehouseCommonModule,
    DailySaleProduitHistoriqueComponent,
    YearlySaleProduitHistoriqueComponent,
    Button,
    DatePickerComponent,
    ProduitAutocompleteComponent,
    Toolbar,
    Tooltip,
  ],
  templateUrl: './stat-sales.component.html',
  styleUrl: './stat-sales.component.scss',
})
export class StatSalesComponent {
  readonly dailySaleProduitHistoriqueComponent = viewChild(DailySaleProduitHistoriqueComponent);
  readonly yearlySaleProduitHistoriqueComponent = viewChild(YearlySaleProduitHistoriqueComponent);
  protected active = 'daily';
  protected readonly PRODUIT_COMBO_RESULT_SIZE = PRODUIT_COMBO_RESULT_SIZE;

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
