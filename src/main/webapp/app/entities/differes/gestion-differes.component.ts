import { Component } from '@angular/core';
import { CardModule } from 'primeng/card';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase } from '@ng-bootstrap/ng-bootstrap';
import { PanelModule } from 'primeng/panel';
import { ListDifferesComponent } from './list-differes/list-differes.component';
import { ReglementDifferesComponent } from './reglement-differes/reglement-differes.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'jhi-gestion-differes',
  imports: [
    WarehouseCommonModule,
    CardModule,
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    NgbNavLinkBase,
    PanelModule,
    FormsModule,
    ListDifferesComponent,
    ReglementDifferesComponent
  ],
  templateUrl: './gestion-differes.component.html',
  styleUrl: './gestion-differes.component.scss',
})
export class GestionDifferesComponent {
  protected active = 'liste';
}
