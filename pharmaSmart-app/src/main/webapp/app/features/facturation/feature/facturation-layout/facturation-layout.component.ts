import { Component, inject } from '@angular/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { AlertBadgeService } from 'app/shared/services/alert-badge.service';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { FacturationHomeComponent } from '../facturation-home/facturation-home.component';
import { HistoriqueReglementsComponent } from '../historique-reglements/historique-reglements.component';
import { FacturationEditionComponent } from '../facturation-edition/facturation-edition.component';
import { RecapitulatifComponent } from '../recapitulatif/recapitulatif.component';
import { RapprochementComponent } from '../rapprochement/rapprochement.component';
import { AvoirComponent } from '../avoir/avoir.component';
import { PlanificationComponent } from '../planification/planification.component';
import { ComptesFournisseursComponent } from '../../../finances/feature/comptes-fournisseurs/comptes-fournisseurs.component';
import { RemisesRfaComponent } from '../../../finances/feature/remises-rfa/remises-rfa.component';

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
    ComptesFournisseursComponent,
    RemisesRfaComponent,
  ],
  templateUrl: './facturation-layout.component.html',
  styleUrl: './facturation-layout.component.scss',
})
export class FacturationLayoutComponent {
  protected active = 'edition';
  /** Fonctionnalité non stable — masquée en attendant correction */
  protected readonly showAvoir = true;

  private readonly ability = inject(AbilityService);
  protected readonly alertBadgeService = inject(AlertBadgeService);

  protected readonly showFactures       = this.ability.canSignal('display', 'facturation.factures');
  protected readonly showHistorique     = this.ability.canSignal('display', 'facturation.historique');
  protected readonly showEdition        = this.ability.canSignal('display', 'facturation.edition');
  protected readonly showRecapitulatif  = this.ability.canSignal('display', 'facturation.recapitulatif');
  protected readonly showRapprochement  = this.ability.canSignal('display', 'facturation.rapprochement');
  protected readonly showAvoirs               = this.ability.canSignal('display', 'facturation.avoirs');
  protected readonly showAutomatisation       = this.ability.canSignal('display', 'facturation.automatisation');
  protected readonly showComptesFournisseurs  = this.ability.canSignal('display', 'facturation.comptes-fournisseurs');
  protected readonly showRemisesRfa           = this.ability.canSignal('display', 'facturation.remises-rfa');
}
