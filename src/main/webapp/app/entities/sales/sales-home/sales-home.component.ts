import { Component } from '@angular/core';
import { SalesComponent } from '../sales.component';
import { PresaleComponent } from '../presale/presale.component';
import { VenteEnCoursComponent } from '../vente-en-cours/vente-en-cours.component';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { CardModule } from 'primeng/card';
import { DecimalPipe } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { NgbNav } from '@ng-bootstrap/ng-bootstrap';
import { ToolbarModule } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { PanelModule } from 'primeng/panel';
import { BadgeModule } from 'primeng/badge';
import { RippleModule } from 'primeng/ripple';

@Component({
  selector: 'jhi-sales-home',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    CalendarModule,
    CardModule,
    DividerModule,
    DropdownModule,
    InputTextModule,
    NgbNav,
    ToolbarModule,
    FormsModule,
    RouterModule,
    PanelModule,
    BadgeModule,
    AutoCompleteModule,
    RippleModule,
    SalesComponent,
    PresaleComponent,
    VenteEnCoursComponent,
    AutoCompleteModule,
    ButtonModule,
    CalendarModule,
    CardModule,
    DecimalPipe,
    FaIconComponent,
  ],
  templateUrl: './sales-home.component.html',
  styleUrl: './sales-home.component.scss',
})
export class SalesHomeComponent {
  protected active = 'ventes-terminees';
}
