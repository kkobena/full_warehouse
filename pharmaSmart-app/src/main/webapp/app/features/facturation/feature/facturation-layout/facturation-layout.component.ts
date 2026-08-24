import { Component, inject, ChangeDetectionStrategy, signal} from '@angular/core';
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
import { SkeletonComponent } from 'app/shared/ui/skeleton/skeleton.component';

import { NavSidebarComponent } from 'app/shared/ui/nav-sidebar/nav-sidebar.component';
import { NavSectionLinkComponent } from 'app/shared/ui/nav-sidebar/nav-section-link.component';
@Component({
  selector: 'app-facturation-layout',
  imports: [NavSidebarComponent, NavSectionLinkComponent, 
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
    SkeletonComponent,
  ],
  templateUrl: './facturation-layout.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './facturation-layout.component.scss',
})
export class FacturationLayoutComponent {
  protected active = 'edition';

  /** Menu replié : rend la largeur au contenu quand il en manque. */
  protected readonly menuReplie = signal(false);
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
