import { Component } from '@angular/core';
import { CardModule } from 'primeng/card';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase } from '@ng-bootstrap/ng-bootstrap';
import { PanelModule } from 'primeng/panel';
import { RemiseComponent } from '../remise.component';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { RemiseProduitsComponent } from '../remise-produits/remise-produits.component';
import { CodeRemiseProduitComponent } from '../code-remise-produit/code-remise-produit.component';

@Component({
  selector: 'jhi-remise-nav',
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
    RemiseComponent,
    RemiseProduitsComponent,
    CodeRemiseProduitComponent,
  ],
  templateUrl: './remise-nav.component.html',
  styleUrls: ['./remise-nav.scss'],
})
export class RemiseNavComponent {
  protected active = 'remise-produit';
}
