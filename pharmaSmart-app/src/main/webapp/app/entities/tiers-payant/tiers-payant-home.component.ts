import {ChangeDetectionStrategy, Component, signal} from '@angular/core';
import {
  NgbNav,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavLinkBase
} from '@ng-bootstrap/ng-bootstrap';
import {TiersPayantComponent} from './tiers-payant.component';
import {GroupeTiersPayantComponent} from '../groupe-tiers-payant/groupe-tiers-payant.component';

import {NavSidebarComponent} from 'app/shared/ui/nav-sidebar/nav-sidebar.component';
import {NavSectionLinkComponent} from 'app/shared/ui/nav-sidebar/nav-section-link.component';

@Component({
  selector: 'app-tiers-payant-home',
  imports: [NavSidebarComponent, NavSectionLinkComponent, NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase, TiersPayantComponent, GroupeTiersPayantComponent],
  templateUrl: './tiers-payant-home.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./tiers-payant-home.scss'],
})
export class TiersPayantHomeComponent {
  protected active = 'tiers-payant';

  /** Menu replié : rend la largeur au contenu quand il en manque. */
  protected readonly menuReplie = signal(false);
}
