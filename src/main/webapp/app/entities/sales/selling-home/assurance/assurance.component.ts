import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { ConfirmationService } from 'primeng/api';
import { AmountComputingComponent } from '../comptant/amount-computing/amount-computing.component';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { DialogService } from 'primeng/dynamicdialog';
import { RouterModule } from '@angular/router';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { SidebarModule } from 'primeng/sidebar';
import { NgxSpinnerModule } from 'ngx-spinner';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { PanelModule } from 'primeng/panel';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';
import { KeyFilterModule } from 'primeng/keyfilter';
import { TagModule } from 'primeng/tag';
import { InputSwitchModule } from 'primeng/inputswitch';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { ProductTableComponent } from '../product-table/product-table.component';
import { BaseSaleComponent } from '../base-sale/base-sale.component';

@Component({
  selector: 'jhi-assurance',
  providers: [ConfirmationService, DialogService],
  imports: [
    ConfirmDialogModule,
    DialogModule,
    AmountComputingComponent,
    DividerModule,
    DropdownModule,
    WarehouseCommonModule,
    SidebarModule,
    RouterModule,
    NgxSpinnerModule,
    TableModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    FormsModule,
    DialogModule,
    ConfirmDialogModule,
    PanelModule,
    SelectButtonModule,
    TooltipModule,
    DividerModule,
    KeyFilterModule,
    TagModule,
    DropdownModule,
    InputSwitchModule,
    OverlayPanelModule,
    ProductTableComponent,
    ModeReglementComponent,
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
