import { Component } from '@angular/core';
import {
  NgbNav,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavLinkBase,
  NgbNavOutlet
} from '@ng-bootstrap/ng-bootstrap';
import { TiersPayantComponent } from './tiers-payant.component';
import { GroupeTiersPayantComponent } from '../groupe-tiers-payant/groupe-tiers-payant.component';

@Component({
  selector: 'jhi-tiers-payant-home',
  imports: [
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    NgbNavLinkBase,
    NgbNavOutlet,
    TiersPayantComponent,
    GroupeTiersPayantComponent
  ],
  templateUrl: './tiers-payant-home.component.html',
  styleUrls: ['./tiers-payant-home.scss'],
})
export class TiersPayantHomeComponent {
  protected active = 'tiers-payant';
}
