import { Component } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { SidebarModule } from 'primeng/sidebar';
import { RouterModule } from '@angular/router';
import { NgxSpinnerModule } from 'ngx-spinner';
import { PanelModule } from 'primeng/panel';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ReactiveFormsModule } from '@angular/forms';
import { RippleModule } from 'primeng/ripple';
import { ToolbarModule } from 'primeng/toolbar';
import { VisualisationMvtCaisseComponent } from './visualisation-mvt-caisse.component';

import { TableauPharmacienComponent } from './tableau-pharmacien/tableau-pharmacien.component';
import { BalanceMvtCaisseComponent } from './balance-mvt-caisse/balance-mvt-caisse.component';
import { TaxeReportComponent } from './taxe-report/taxe-report.component';

@Component({
  selector: 'jhi-mvt-caisse',
  standalone: true,
  providers: [ConfirmationService, DialogService],
  imports: [
    WarehouseCommonModule,
    SidebarModule,
    RouterModule,
    NgxSpinnerModule,
    PanelModule,
    ButtonModule,
    CardModule,
    InputTextModule,
    ReactiveFormsModule,
    RippleModule,
    ToolbarModule,
    VisualisationMvtCaisseComponent,
    BalanceMvtCaisseComponent,
    TableauPharmacienComponent,
    TaxeReportComponent,
  ],
  templateUrl: './mvt-caisse.component.html',
})
export class MvtCaisseComponent {
  protected active = 'mvt-caisse';

  constructor() {}
}
