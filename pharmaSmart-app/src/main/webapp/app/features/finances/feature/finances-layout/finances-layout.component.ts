import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AbilityService } from 'app/core/auth/ability.service';
import { FinancesDashboardComponent } from '../finances-dashboard/finances-dashboard.component';
import { DeclarationTvaComponent } from '../declaration-tva/declaration-tva.component';
import { ComptesFournisseursComponent } from '../comptes-fournisseurs/comptes-fournisseurs.component';
import { RemisesRfaComponent } from '../remises-rfa/remises-rfa.component';
import { ExportComptableComponent } from '../export-comptable/export-comptable.component';

import { NavSidebarComponent } from 'app/shared/ui/nav-sidebar/nav-sidebar.component';
import { NavSectionLinkComponent } from 'app/shared/ui/nav-sidebar/nav-section-link.component';
@Component({
  selector: 'app-finances-layout',
  imports: [NavSidebarComponent, NavSectionLinkComponent, 
    NgbNavModule,
    FinancesDashboardComponent,
    DeclarationTvaComponent,
    ComptesFournisseursComponent,
    RemisesRfaComponent,
    ExportComptableComponent,
  ],
  templateUrl: './finances-layout.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './finances-layout.component.scss',
})
export class FinancesLayoutComponent {
  active = signal<string>('dashboard');


  /** Menu replié : rend la largeur au contenu quand il en manque. */
  protected readonly menuReplie = signal(false);
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
