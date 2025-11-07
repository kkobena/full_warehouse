import { Component } from '@angular/core';
import {
  NgbNav,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavLinkBase,
  NgbNavOutlet
} from '@ng-bootstrap/ng-bootstrap';
import { DepotComponent } from '../depot.component';
import { AchatDepotComponent } from '../achat-depot/achat-depot.component';
import { StockDepotComponent } from '../stock-depot/stock-depot.component';
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
    CommonModule
  ],
  templateUrl: './depot-home.component.html',
  styleUrl: './depot-home.component.scss',
})
export class DepotHomeComponent {
  protected active = 'liste-depots';
}
