import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AbilityService } from 'app/core/auth/ability.service';
import { NavSectionLinkComponent } from 'app/shared/ui/nav-sidebar/nav-section-link.component';
import { NavSidebarComponent } from 'app/shared/ui/nav-sidebar/nav-sidebar.component';
import { SkeletonComponent } from 'app/shared/ui/skeleton/skeleton.component';
import { JournalRemisesFiltresStore } from 'app/features/journal-remises/data-access/journal-remises-filtres.store';
import { JournalRemisesLignesComponent } from 'app/features/journal-remises/feature/journal-lignes/journal-lignes.component';
import { PromotionHomeComponent } from '../promotion-home/promotion-home.component';

/**
 * Module Promotions : la liste des promotions, et le journal des lignes qu'elles ont remisées — le
 * même que l'onglet « Promotions » du Journal des remises, pour qui gère les promotions sans avoir
 * accès à la Gestion Courante.
 */
@Component({
  selector: 'app-promotion-layout',
  imports: [
    NgbNavModule,
    NavSidebarComponent,
    NavSectionLinkComponent,
    SkeletonComponent,
    PromotionHomeComponent,
    JournalRemisesLignesComponent,
  ],
  providers: [JournalRemisesFiltresStore],
  templateUrl: './promotion-layout.component.html',
  styleUrl: './promotion-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromotionLayoutComponent {
  protected readonly active = signal<string>('liste');

  /** Menu replié : la liste et son panneau d'articles prennent toute la largeur. */
  protected readonly menuReplie = signal(false);

  protected readonly showListe = inject(AbilityService).canSignal('display', 'promotions.liste');
  protected readonly showJournal = inject(AbilityService).canSignal('display', 'promotions.journal');
}
