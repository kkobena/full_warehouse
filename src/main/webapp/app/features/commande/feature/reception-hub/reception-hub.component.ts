import { Component } from '@angular/core';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';
import { AppBonEnCoursComponent } from '../../ui/bon-en-cours/bon-en-cours.component';
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
    AppBonEnCoursComponent,
    AppListBonsComponent,
  ],
})
export class ReceptionHubComponent {
  activeTab = 'en-cours';
}

