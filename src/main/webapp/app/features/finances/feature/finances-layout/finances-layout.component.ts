import { Component, inject, signal } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AbilityService } from 'app/core/auth/ability.service';
import { FinancesDashboardComponent } from '../finances-dashboard/finances-dashboard.component';
import { DeclarationTvaComponent } from '../declaration-tva/declaration-tva.component';
import { ComptesFournisseursComponent } from '../comptes-fournisseurs/comptes-fournisseurs.component';
import { RemisesRfaComponent } from '../remises-rfa/remises-rfa.component';
import { ExportComptableComponent } from '../export-comptable/export-comptable.component';

@Component({
  selector: 'app-finances-layout',
  imports: [
    NgbNavModule,
    FinancesDashboardComponent,
    DeclarationTvaComponent,
    ComptesFournisseursComponent,
    RemisesRfaComponent,
    ExportComptableComponent,
  ],
  templateUrl: './finances-layout.component.html',
  styleUrl: './finances-layout.component.scss',
})
export class FinancesLayoutComponent {
  active = signal<string>('dashboard');

  private readonly ability = inject(AbilityService);

  protected readonly showDashboard          = this.ability.canSignal('display', 'finances.dashboard');
  protected readonly showComptesFournisseurs = this.ability.canSignal('display', 'finances.comptes-fournisseurs');
  protected readonly showDeclarationTva     = this.ability.canSignal('display', 'finances.declaration-tva');
  protected readonly showRemisesRfa         = this.ability.canSignal('display', 'finances.remises-rfa');
  protected readonly showExport             = this.ability.canSignal('display', 'finances.export');

  onNavigateToTab(tabId: string): void {
    this.active.set(tabId);
  }
}
