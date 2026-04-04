import { Component } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { DifferesHomeComponent } from '../differes-home/differes-home.component';
import { HistoriqueReglementsDifferesComponent } from '../historique-reglements-differes/historique-reglements-differes.component';

@Component({
  selector: 'app-differes-layout',
  imports: [NgbNavModule, DifferesHomeComponent, HistoriqueReglementsDifferesComponent],
  templateUrl: './differes-layout.component.html',
  styleUrl: './differes-layout.component.scss',
})
export class DifferesLayoutComponent {
  protected active = 'differes';
}
