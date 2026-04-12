import { Component, inject } from '@angular/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';
import { DepotComponent } from '../depot.component';
import { AchatDepotComponent } from '../achat-depot/achat-depot.component';
import { StockDepotComponent } from '../stock-depot/stock-depot.component';
import { DepotRetourListComponent } from '../depot-retour-list/depot-retour-list.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'jhi-depot-home',
  imports: [
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
  styleUrl: './depot-home.component.scss',
})
export class DepotHomeComponent {
  protected active = 'liste-depots';

  private readonly ability = inject(AbilityService);

  protected readonly showListeDepots = this.ability.canSignal('display', 'depot.liste-depots');
  protected readonly showStockDepot  = this.ability.canSignal('display', 'depot.stock-depot');
  protected readonly showAchatDepot  = this.ability.canSignal('display', 'depot.achat-depot');
  protected readonly showRetourDepot = this.ability.canSignal('display', 'depot.retour-depot');
}
