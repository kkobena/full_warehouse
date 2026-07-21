import {Component, ChangeDetectionStrategy} from '@angular/core';
import {
  NgbNav,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavLinkBase,
  NgbNavOutlet
} from "@ng-bootstrap/ng-bootstrap";
import {FormsModule} from '@angular/forms';
import {RemiseProduitsComponent} from '../remise-produits/remise-produits.component';
import {CodeRemiseProduitComponent} from '../code-remise-produit/code-remise-produit.component';

@Component({
  selector: 'app-remise-nav',
  imports: [
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    NgbNavLinkBase,
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
