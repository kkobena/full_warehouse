import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AbilityService } from 'app/core/auth/ability.service';
import { NavSectionLinkComponent } from 'app/shared/ui/nav-sidebar/nav-section-link.component';
import { NavSidebarComponent } from 'app/shared/ui/nav-sidebar/nav-sidebar.component';
import { SkeletonComponent } from 'app/shared/ui/skeleton/skeleton.component';
import { JournalRemisesFiltresStore } from '../../data-access/journal-remises-filtres.store';
import { JournalRemisesLignesComponent } from '../journal-lignes/journal-lignes.component';
import { JournalSyntheseComponent } from '../journal-synthese/journal-synthese.component';

/**
 * Sous-menu « Journal des remises » de la Gestion Courante : la synthèse, puis un journal par nature
 * de remise (docs/PLAN-JOURNAL-REMISES.md). La période se choisit une fois pour tous les onglets.
 */
@Component({
  selector: 'app-journal-remises-layout',
  imports: [
    NgbNavModule,
    NavSidebarComponent,
    NavSectionLinkComponent,
    SkeletonComponent,
    JournalSyntheseComponent,
    JournalRemisesLignesComponent,
  ],
  providers: [JournalRemisesFiltresStore],
  templateUrl: './journal-remises-layout.component.html',
  styleUrl: './journal-remises-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JournalRemisesLayoutComponent {
  protected readonly active = signal<string>('synthese');

  /** Menu replié : les journaux sont de larges tableaux, la place compte. */
  protected readonly menuReplie = signal(false);

  protected readonly showSynthese = inject(AbilityService).canSignal('display', 'journal-remises.synthese');
  protected readonly showPromotions = inject(AbilityService).canSignal('display', 'journal-remises.promotions');
  protected readonly showRemises = inject(AbilityService).canSignal('display', 'journal-remises.remises');
  protected readonly showUg = inject(AbilityService).canSignal('display', 'journal-remises.ug');
}
