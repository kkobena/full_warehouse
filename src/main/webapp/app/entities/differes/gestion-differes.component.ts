import { Component } from '@angular/core';
import { CardModule } from 'primeng/card';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase } from '@ng-bootstrap/ng-bootstrap';
import { PanelModule } from 'primeng/panel';
import { ListDifferesComponent } from './list-differes/list-differes.component';
import { ReglementDifferesComponent } from './reglement-differes/reglement-differes.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { Divider } from 'primeng/divider';

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
    ReglementDifferesComponent,
    Divider
  ],
  templateUrl: './gestion-differes.component.html',
  styles: ``
})
export class GestionDifferesComponent {
  protected active = 'liste';
}
