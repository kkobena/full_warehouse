import { Component } from '@angular/core';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe } from '@ngx-translate/core';
import { LotPerimesComponent } from './lot-perimes/lot-perimes.component';
import { LotADetruireComponent } from './lot-a-detruire/lot-a-detruire.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-gestion-peremption',
  imports: [
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    NgbNavLinkBase,
    TranslatePipe,
    LotPerimesComponent,
    LotADetruireComponent,
    WarehouseCommonModule
  ],
  templateUrl: './gestion-peremption.component.html',
  styleUrls: ['./gestion-peremption.scss'],
})
export class GestionPeremptionComponent {
  protected active = 'lot-perimes';
}
