import { Component, ChangeDetectionStrategy, signal} from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { DifferesHomeComponent } from '../differes-home/differes-home.component';
import { HistoriqueReglementsDifferesComponent } from '../historique-reglements-differes/historique-reglements-differes.component';

import { NavSidebarComponent } from 'app/shared/ui/nav-sidebar/nav-sidebar.component';
import { NavSectionLinkComponent } from 'app/shared/ui/nav-sidebar/nav-section-link.component';
@Component({
  selector: 'app-differes-layout',
  imports: [NavSidebarComponent, NavSectionLinkComponent, NgbNavModule, DifferesHomeComponent, HistoriqueReglementsDifferesComponent],
  templateUrl: './differes-layout.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './differes-layout.component.scss',
})
export class DifferesLayoutComponent {
  protected active = 'differes';

  /** Menu replié : rend la largeur au contenu quand il en manque. */
  protected readonly menuReplie = signal(false);
}
