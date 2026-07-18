import {Component, ChangeDetectionStrategy} from '@angular/core';
import {CardModule} from 'primeng/card';
import {
  NgbNav,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavLinkBase,
  NgbNavOutlet
} from "@ng-bootstrap/ng-bootstrap";
import {PanelModule} from 'primeng/panel';
import {FormsModule} from '@angular/forms';
import {RemiseProduitsComponent} from '../remise-produits/remise-produits.component';
import {CodeRemiseProduitComponent} from '../code-remise-produit/code-remise-produit.component';

@Component({
  selector: 'app-remise-nav',
  imports: [
    CardModule,
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    NgbNavLinkBase,
    PanelModule,
    FormsModule,
    RemiseProduitsComponent,
    CodeRemiseProduitComponent,
    NgbNavOutlet
  ],
  templateUrl: './remise-nav.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./remise-nav.scss'],
})
export class RemiseNavComponent {
  protected active = 'remise-produit';
}
