import { Component } from '@angular/core';
import { Card } from 'primeng/card';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase } from '@ng-bootstrap/ng-bootstrap';
import { Panel } from 'primeng/panel';
import { TranslatePipe } from '@ngx-translate/core';
import { LotPerimesComponent } from './lot-perimes/lot-perimes.component';
import { LotADetruireComponent } from './lot-a-detruire/lot-a-detruire.component';
import { Divider } from 'primeng/divider';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-gestion-peremption',
  imports: [
    Card,
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    NgbNavLinkBase,
    Panel,
    TranslatePipe,
    LotPerimesComponent,
    LotADetruireComponent,
    Divider,
    WarehouseCommonModule,
  ],
  templateUrl: './gestion-peremption.component.html',
  styles: ``,
})
export class GestionPeremptionComponent {
  protected active = 'lot-perimes';
}
