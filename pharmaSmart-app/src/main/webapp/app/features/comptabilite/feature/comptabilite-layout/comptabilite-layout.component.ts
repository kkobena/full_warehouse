import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AbilityService } from 'app/core/auth/ability.service';
import { BalanceMvtCaisseComponent } from '../../../../entities/mvt-caisse/balance-mvt-caisse/balance-mvt-caisse.component';
import { TaxeReportComponent } from '../../../../entities/mvt-caisse/taxe-report/taxe-report.component';
import { TableauPharmacienComponent } from '../../../../entities/mvt-caisse/tableau-pharmacien/tableau-pharmacien.component';
import { RecapitualtifCaisseComponent } from '../../../../entities/ticketZ/recapitualtif-caisse/recapitualtif-caisse.component';
import { ActivitySummaryComponent } from '../../../../entities/raport-gestion/activity-summary/activity-summary.component';

@Component({
  selector: 'app-comptabilite-layout',
  imports: [
    NgbNavModule,
    BalanceMvtCaisseComponent,
    TaxeReportComponent,
    TableauPharmacienComponent,
    RecapitualtifCaisseComponent,
    ActivitySummaryComponent,
  ],
  templateUrl: './comptabilite-layout.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './comptabilite-layout.component.scss',
})
export class ComptabiliteLayoutComponent {
  protected readonly active = signal<string>('balance');

  private readonly ability = inject(AbilityService);

  protected readonly showBalance           = this.ability.canSignal('display', 'comptabilite.balance');
  protected readonly showTaxeReport        = this.ability.canSignal('display', 'comptabilite.taxe-report');
  protected readonly showTableauPharmacien = this.ability.canSignal('display', 'comptabilite.tableau-pharmacien');
  protected readonly showRecapCaisse       = this.ability.canSignal('display', 'comptabilite.recapitulatif-caisse');
  protected readonly showRaportActivite    = this.ability.canSignal('display', 'comptabilite.raport-activite');
}
