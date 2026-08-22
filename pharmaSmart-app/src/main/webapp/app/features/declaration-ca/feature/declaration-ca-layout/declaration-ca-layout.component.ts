import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import { AbilityService } from 'app/core/auth/ability.service';
import { SkeletonComponent } from 'app/shared/ui/skeleton/skeleton.component';
import { ExclusionReferentielComponent } from '../exclusion-referentiel/exclusion-referentiel.component';
import { ExclusionParametresComponent } from '../exclusion-parametres/exclusion-parametres.component';
import { JournalLignesComponent } from '../journal-lignes/journal-lignes.component';
import { JournalTiersPayantComponent } from '../journal-tiers-payant/journal-tiers-payant.component';
import { PonctionComponent } from '../ponction/ponction.component';
import { PonctionHistoriqueComponent } from '../ponction-historique/ponction-historique.component';
import { AuditComponent } from '../audit/audit.component';
// Trois imports et non un barrel : chacun n'est atteint que par son bloc `@defer`, et un module
// commun les aurait ramenés ensemble — près de 3 Mo pour un seul onglet ouvert.
import { BalanceReelleComponent } from '../vues-reelles/balance-reelle.component';
import { TaxeReportReelComponent } from '../vues-reelles/taxe-report-reel.component';
import { TableauPharmacienReelComponent } from '../vues-reelles/tableau-pharmacien-reel.component';

/**
 * Module « Retraitement du CA ».
 *
 * <p>Volontairement distinct de Comptabilité : ces écrans ne consultent pas le chiffre d'affaires,
 * ils décident de ce qui en sera déclaré. Les réunir mêlerait la lecture et la décision.
 *
 * <p>« Retraitement » et non « Déclaration » : rien ne se déclare depuis ici. On y écarte des ventes,
 * on y prélève un montant, on y vérifie la cohérence — autant de retraitements appliqués au chiffre
 * d'affaires <em>avant</em> qu'il ne parte à la comptabilité. C'est aussi le mot employé partout
 * ailleurs dans le code (« modules de retraitement », « hors retraitements »).
 */
@Component({
  selector: 'app-declaration-ca-layout',
  imports: [
    NgbNavModule,
    SkeletonComponent,
    ExclusionReferentielComponent,
    ExclusionParametresComponent,
    JournalLignesComponent,
    JournalTiersPayantComponent,
    PonctionComponent,
    PonctionHistoriqueComponent,
    BalanceReelleComponent,
    TaxeReportReelComponent,
    TableauPharmacienReelComponent,
    AuditComponent,
  ],
  templateUrl: './declaration-ca-layout.component.html',
  styleUrl: './declaration-ca-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeclarationCaLayoutComponent {
  protected readonly active = signal<string>('exclusion-rayon');

  private readonly ability = inject(AbilityService);

  protected readonly showRayon = this.ability.canSignal('display', 'declaration-ca.exclusion-rayon');
  protected readonly showTiersPayant = this.ability.canSignal('display', 'declaration-ca.exclusion-tp');
  protected readonly showParametres = this.ability.canSignal('display', 'declaration-ca.parametres');
  protected readonly showJournalTp = this.ability.canSignal('display', 'declaration-ca.journal-tp');
  protected readonly showJournalUg = this.ability.canSignal('display', 'declaration-ca.journal-ug');
  protected readonly showJournalRayon = this.ability.canSignal('display', 'declaration-ca.journal-rayon');
  protected readonly showPonction = this.ability.canSignal('display', 'declaration-ca.ponction');
  protected readonly showHistorique = this.ability.canSignal('display', 'declaration-ca.ponction-historique');
  protected readonly showBalance = this.ability.canSignal('display', 'declaration-ca.balance-reelle');
  protected readonly showTaxeReport = this.ability.canSignal('display', 'declaration-ca.taxe-report-reel');
  protected readonly showTableau = this.ability.canSignal('display', 'declaration-ca.tableau-pharmacien-reel');
  protected readonly showAudit = this.ability.canSignal('display', 'declaration-ca.audit');
}
