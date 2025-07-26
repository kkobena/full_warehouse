import {Component} from '@angular/core';
import {ModeReglementComponent} from '../../mode-reglement/mode-reglement.component';
import {AmountComputingComponent} from '../comptant/amount-computing/amount-computing.component';
import {RouterModule} from '@angular/router';
import {DialogService} from 'primeng/dynamicdialog';
import {ButtonModule} from 'primeng/button';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {DividerModule} from 'primeng/divider';
import {ProductTableComponent} from '../product-table/product-table.component';
import {WarehouseCommonModule} from '../../../../shared/warehouse-common/warehouse-common.module';
import {FormsModule} from '@angular/forms';
import {BaseSaleComponent} from '../base-sale/base-sale.component';
import {ConfirmDialogComponent} from "../../../../shared/dialog/confirm-dialog/confirm-dialog.component";

@Component({
  selector: 'jhi-carnet',
  providers: [DialogService],
  imports: [
    ConfirmDialogModule,
    AmountComputingComponent,
    DividerModule,
    WarehouseCommonModule,
    RouterModule,
    ButtonModule,
    FormsModule,
    ProductTableComponent,
    ModeReglementComponent,
    ConfirmDialogComponent,
  ],
  templateUrl: '../base-sale/base-sale.component.html',
})
export class CarnetComponent extends BaseSaleComponent {
  constructor() {
    super();
    this.currentSaleService.setTypeVo('CARNET');

    this.selectedCustomerService.setCustomer(null);
  }
}
