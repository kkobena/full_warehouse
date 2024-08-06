import { Component } from '@angular/core';
import { ModeReglementComponent } from '../../mode-reglement/mode-reglement.component';
import { AmountComputingComponent } from '../comptant/amount-computing/amount-computing.component';
import { RouterModule } from '@angular/router';
import { ConfirmationService, Footer, PrimeTemplate } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { ButtonDirective, ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { NgxSpinnerModule } from 'ngx-spinner';
import { ProductTableComponent } from '../product-table/product-table.component';
import { DialogModule } from 'primeng/dialog';
import { WarehouseCommonModule } from '../../../../shared/warehouse-common/warehouse-common.module';
import { PreventeModalComponent } from '../../prevente-modal/prevente-modal/prevente-modal.component';
import { SidebarModule } from 'primeng/sidebar';
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
import { BaseSaleComponent } from '../base-sale/base-sale.component';

@Component({
  selector: 'jhi-carnet',
  standalone: true,
  providers: [ConfirmationService, DialogService],
  imports: [
    ButtonDirective,
    ConfirmDialogModule,
    DialogModule,
    Footer,
    PrimeTemplate,
    AmountComputingComponent,
    DividerModule,
    DropdownModule,
    WarehouseCommonModule,
    PreventeModalComponent,
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
export class CarnetComponent extends BaseSaleComponent {
  constructor() {
    super();
    this.currentSaleService.setTypeVo('CARNET');

    this.selectedCustomerService.setCustomer(null);
  }
}
