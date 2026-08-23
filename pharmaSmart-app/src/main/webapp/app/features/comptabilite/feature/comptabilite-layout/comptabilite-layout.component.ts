import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AbilityService } from 'app/core/auth/ability.service';
import { BalanceMvtCaisseComponent } from '../../../../entities/mvt-caisse/balance-mvt-caisse/balance-mvt-caisse.component';
import { TaxeReportComponent } from '../../../../entities/mvt-caisse/taxe-report/taxe-report.component';
import { TableauPharmacienComponent } from '../../../../entities/mvt-caisse/tableau-pharmacien/tableau-pharmacien.component';
import { RecapitualtifCaisseComponent } from '../../../../entities/ticketZ/recapitualtif-caisse/recapitualtif-caisse.component';
import { ActivitySummaryComponent } from '../../../../entities/raport-gestion/activity-summary/activity-summary.component';
import { SkeletonComponent } from 'app/shared/ui/skeleton/skeleton.component';
import { NavSidebarComponent } from 'app/shared/ui/nav-sidebar/nav-sidebar.component';
import { NavSectionLinkComponent } from 'app/shared/ui/nav-sidebar/nav-section-link.component';

@Component({
  selector: 'app-comptabilite-layout',
  imports: [
    NgbNavModule,
    NavSidebarComponent,
    NavSectionLinkComponent,
    BalanceMvtCaisseComponent,
    TaxeReportComponent,
    TableauPharmacienComponent,
    RecapitualtifCaisseComponent,
    ActivitySummaryComponent,
    SkeletonComponent,
  ],
  templateUrl: './comptabilite-layout.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './comptabilite-layout.component.scss',
})
export class ComptabiliteLayoutComponent {
  protected readonly active = signal<string>('balance');

  /** Menu replié : l'écran de comptabilité affiche des tableaux larges, la place compte. */
  protected readonly menuReplie = signal(false);

  private readonly ability = inject(AbilityService);

  protected readonly showBalance           = this.ability.canSignal('display', 'comptabilite.balance');
  protected readonly showTaxeReport        = this.ability.canSignal('display', 'comptabilite.taxe-report');
  protected readonly showTableauPharmacien = this.ability.canSignal('display', 'comptabilite.tableau-pharmacien');
  protected readonly showRecapCaisse       = this.ability.canSignal('display', 'comptabilite.recapitulatif-caisse');
  protected readonly showRaportActivite    = this.ability.canSignal('display', 'comptabilite.raport-activite');
}
