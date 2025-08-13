import { Component } from '@angular/core';
import { Card } from 'primeng/card';
import { Divider } from 'primeng/divider';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';
import { FournisseurComponent } from './fournisseur.component';
import { GroupeFournisseurComponent } from '../groupe-fournisseur/groupe-fournisseur.component';

@Component({
  selector: 'jhi-fournisseur-home',
  imports: [
    Card,
    Divider,
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    NgbNavLinkBase,
    FournisseurComponent,
    GroupeFournisseurComponent,
    NgbNavOutlet,
  ],
  templateUrl: './fournisseur-home.component.html',
})
export class FournisseurHomeComponent {
  protected active = 'fournisseur';
}
