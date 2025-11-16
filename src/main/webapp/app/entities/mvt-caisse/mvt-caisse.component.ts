import { Component } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
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
import { GestionCaisseComponent } from './gestion-caisse/gestion-caisse.component';
import { ActivitySummaryComponent } from '../raport-gestion/activity-summary/activity-summary.component';
import { RecapitualtifCaisseComponent } from '../ticketZ/recapitualtif-caisse/recapitualtif-caisse.component';

@Component({
  selector: 'jhi-mvt-caisse',
  providers: [ConfirmationService, DialogService],
  imports: [
    WarehouseCommonModule,
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
    GestionCaisseComponent,
    ActivitySummaryComponent,
    RecapitualtifCaisseComponent,
  ],
  templateUrl: './mvt-caisse.component.html',
  styleUrl: './mvt-caisse.component.scss',
})
export class MvtCaisseComponent {
  protected active = 'mvt-caisse';
}
