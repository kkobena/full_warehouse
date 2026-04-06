import { Component } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { FacturationHomeComponent } from '../facturation-home/facturation-home.component';
import { HistoriqueReglementsComponent } from '../historique-reglements/historique-reglements.component';
import { FacturationEditionComponent } from '../facturation-edition/facturation-edition.component';
import { RecapitulatifComponent } from '../recapitulatif/recapitulatif.component';
import { RapprochementComponent } from '../rapprochement/rapprochement.component';
import { AvoirComponent } from '../avoir/avoir.component';
import { PlanificationComponent } from '../planification/planification.component';

@Component({
  selector: 'app-facturation-layout',
  imports: [
    NgbNavModule,
    FacturationHomeComponent,
    HistoriqueReglementsComponent,
    FacturationEditionComponent,
    RecapitulatifComponent,
    RapprochementComponent,
    AvoirComponent,
    PlanificationComponent,
  ],
  templateUrl: './facturation-layout.component.html',
  styleUrl: './facturation-layout.component.scss',
})
export class FacturationLayoutComponent {
  protected active = 'factures';
}
