import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { AmountComputingComponent } from '../comptant/amount-computing/amount-computing.component';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { DialogService } from 'primeng/dynamicdialog';
import { RouterModule } from '@angular/router';
import { DividerModule } from 'primeng/divider';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { NgxSpinnerModule } from 'ngx-spinner';
import { FormsModule } from '@angular/forms';
import { ProductTableComponent } from '../product-table/product-table.component';
import { BaseSaleComponent } from '../base-sale/base-sale.component';
import {ConfirmDialogComponent} from "../../../../shared/dialog/confirm-dialog/confirm-dialog.component";

@Component({
  selector: 'jhi-assurance',
  providers: [ DialogService],
  imports: [
    ConfirmDialogModule,
    AmountComputingComponent,
    DividerModule,
    WarehouseCommonModule,
    RouterModule,
    NgxSpinnerModule,
    ButtonModule,
    FormsModule,
    ProductTableComponent,
    ModeReglementComponent,
    ConfirmDialogComponent,
  ],
  templateUrl: '../base-sale/base-sale.component.html',
})
export class AssuranceComponent extends BaseSaleComponent {
  constructor() {
    super();
    this.currentSaleService.setTypeVo('ASSURANCE');
    this.selectedCustomerService.setCustomer(null);
  }
}
