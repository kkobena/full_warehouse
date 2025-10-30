import { Component, inject } from '@angular/core';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { AmountComputingComponent } from '../comptant/amount-computing/amount-computing.component';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { ProductTableComponent } from '../product-table/product-table.component';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { BaseSaleComponent } from '../base-sale/base-sale.component';
import { ConfirmDialogComponent } from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { CardModule } from 'primeng/card';
import { SpinnerComponent } from '../../../../shared/spinner/spinner.component';
import { DepotAgreeService } from '../../service/depot-agree.service';

@Component({
  selector: 'jhi-vente-depot-agree',
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
    SpinnerComponent
  ],
  templateUrl: '../base-sale/base-sale.component.html'
})
export class VenteDepotAgreeComponent extends BaseSaleComponent {
  protected readonly depotAgreeService = inject(DepotAgreeService);

  constructor() {
    super();
    this.currentSaleService.setTypeVo('DEPOT_AGREE');
    this.selectedCustomerService.setCustomer(null);
    // Reset depot selection when component initializes
    this.depotAgreeService.setSelectedDepot(null);
  }
}
