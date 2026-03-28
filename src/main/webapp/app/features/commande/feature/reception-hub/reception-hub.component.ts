import { Component } from '@angular/core';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';
import { CommandeReceivedHomeComponent } from '../commande-received-home/commande-received-home.component';
import { AppListBonsComponent } from '../../ui/list-bons/list-bons.component';

@Component({
  selector: 'app-reception-hub',
  templateUrl: './reception-hub.component.html',
  styleUrls: ['./reception-hub.component.scss'],
  imports: [
    NgbNav,
    NgbNavItem,
    NgbNavLink,
    NgbNavContent,
    NgbNavOutlet,
    CommandeReceivedHomeComponent,
    AppListBonsComponent,
  ],
})
export class ReceptionHubComponent {
  activeTab = 'en-cours';
}

