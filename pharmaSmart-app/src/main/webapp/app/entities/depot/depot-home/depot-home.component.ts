import { Component, inject, ChangeDetectionStrategy, signal} from '@angular/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';
import { DepotComponent } from '../depot.component';
import { AchatDepotComponent } from '../achat-depot/achat-depot.component';
import { StockDepotComponent } from '../stock-depot/stock-depot.component';
import { DepotRetourListComponent } from '../depot-retour-list/depot-retour-list.component';
import { CommonModule } from '@angular/common';

import { NavSidebarComponent } from 'app/shared/ui/nav-sidebar/nav-sidebar.component';
import { NavSectionLinkComponent } from 'app/shared/ui/nav-sidebar/nav-section-link.component';
@Component({
  selector: 'jhi-depot-home',
  imports: [NavSidebarComponent, NavSectionLinkComponent, 
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    NgbNavLinkBase,
    NgbNavOutlet,
    DepotComponent,
    AchatDepotComponent,
    StockDepotComponent,
    DepotRetourListComponent,
    CommonModule,
  ],
  templateUrl: './depot-home.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './depot-home.component.scss',
})
export class DepotHomeComponent {
  protected active = 'liste-depots';


  /** Menu replié : rend la largeur au contenu quand il en manque. */
  protected readonly menuReplie = signal(false);
  private readonly ability = inject(AbilityService);

  protected readonly showListeDepots = this.ability.canSignal('display', 'depot.liste-depots');
  protected readonly showStockDepot  = this.ability.canSignal('display', 'depot.stock-depot');
  protected readonly showAchatDepot  = this.ability.canSignal('display', 'depot.achat-depot');
  protected readonly showRetourDepot = this.ability.canSignal('display', 'depot.retour-depot');
}
