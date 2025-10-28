import { Component } from '@angular/core';
import {
  NgbNav,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavLinkBase,
  NgbNavOutlet
} from '@ng-bootstrap/ng-bootstrap';
import { FournisseurComponent } from './fournisseur.component';
import { GroupeFournisseurComponent } from '../groupe-fournisseur/groupe-fournisseur.component';

@Component({
  selector: 'jhi-fournisseur-home',
  imports: [
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    NgbNavLinkBase,
    FournisseurComponent,
    GroupeFournisseurComponent,
    NgbNavOutlet
  ],
  templateUrl: './fournisseur-home.component.html',
  styleUrl: './fournisseur-home.component.scss',
})
export class FournisseurHomeComponent {
  protected active = 'fournisseur';
}
