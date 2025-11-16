import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { AmountComputingComponent } from '../comptant/amount-computing/amount-computing.component';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { RouterModule } from '@angular/router';
import { DividerModule } from 'primeng/divider';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { ProductTableComponent } from '../product-table/product-table.component';
import { BaseSaleComponent } from '../base-sale/base-sale.component';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { CardModule } from 'primeng/card';
import { SpinnerComponent } from '../../../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-assurance',
  imports: [
    AmountComputingComponent,
    DividerModule,
    WarehouseCommonModule,
    RouterModule,
    ButtonModule,
    FormsModule,
    ProductTableComponent,
    ModeReglementComponent,
    ConfirmDialogComponent,
    CardModule,
    SpinnerComponent,
  ],
  templateUrl: '../base-sale/base-sale.component.html',
  styleUrls: ['../base-sale/base-sale.scss'],
})
export class AssuranceComponent extends BaseSaleComponent {
  constructor() {
    super();
    this.currentSaleService.setTypeVo('ASSURANCE');
    this.selectedCustomerService.setCustomer(null);
  }
}
