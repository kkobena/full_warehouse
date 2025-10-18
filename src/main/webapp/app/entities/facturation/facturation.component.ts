import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink } from '@ng-bootstrap/ng-bootstrap';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { RouterModule } from '@angular/router';
import { RippleModule } from 'primeng/ripple';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { ToolbarModule } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { PanelModule } from 'primeng/panel';
import { FacturesComponent } from './factures/factures.component';
import { EditionComponent } from './edition/edition.component';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Divider } from 'primeng/divider';

@Component({
  selector: 'jhi-facturation',
  providers: [ConfirmationService, DialogService, MessageService],
  imports: [
    WarehouseCommonModule,
    CardModule,
    InputTextModule,
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    ButtonModule,
    RouterModule,
    RippleModule,
    DynamicDialogModule,
    ToolbarModule,
    FormsModule,
    PanelModule,
    FacturesComponent,
    EditionComponent,
    Divider,
  ],
  templateUrl: './facturation.component.html',
})
export class FacturationComponent {
  protected active = 'factures';
}
