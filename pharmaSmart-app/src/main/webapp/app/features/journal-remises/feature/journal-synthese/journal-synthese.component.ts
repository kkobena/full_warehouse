import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { AbilityService } from 'app/core/auth/ability.service';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { ButtonComponent } from 'app/shared/ui/button/button.component';
import { CardComponent } from 'app/shared/ui/card/card.component';
import { DataTableComponent } from 'app/shared/ui/data-table/data-table.component';
import { HintComponent } from 'app/shared/ui/hint/hint.component';
import { KpiItemComponent } from 'app/shared/ui/kpi-strip/kpi-item.component';
import { KpiStripComponent } from 'app/shared/ui/kpi-strip/kpi-strip.component';
import { SplitButtonComponent } from 'app/shared/ui/split-button/split-button.component';
import { ToolbarComponent } from 'app/shared/ui/toolbar/toolbar.component';
import { JournalRemisesApiService } from '../../data-access/journal-remises-api.service';
import { JournalRemisesFiltresStore } from '../../data-access/journal-remises-filtres.store';
import { JournalRemisesTelechargementService } from '../../data-access/journal-remises-telechargement.service';
import { ecartMargeUg, ISyntheseJournal, MECANIQUE_LIBELLES, TypePromotion } from '../../models/journal-remises.model';
import { JournalFiltresComponent } from '../../ui/journal-filtres/journal-filtres.component';

/**
 * Ce que les remises ont coûté sur la période, et ce que les UG du fournisseur ont rapporté.
 *
 * <p>L'export d'ici porte tout : la synthèse et les trois journaux, une feuille chacun.
 */
@Component({
  selector: 'app-journal-synthese',
  imports: [
    DecimalPipe,
    ButtonComponent,
    CardComponent,
    DataTableComponent,
    HintComponent,
    KpiItemComponent,
    KpiStripComponent,
    SplitButtonComponent,
    ToolbarComponent,
    JournalFiltresComponent,
  ],
  templateUrl: './journal-synthese.component.html',
  styleUrl: '../journal-lignes/journal-lignes.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JournalSyntheseComponent {
  /** L'entrée de menu dont l'écran est le contenu : le titre de la barre suit son libellé. */
  readonly navCode = input<string>('');

  protected readonly filtres = inject(JournalRemisesFiltresStore);
  protected readonly telechargement = inject(JournalRemisesTelechargementService);

  protected readonly synthese = signal<ISyntheseJournal | null>(null);
  protected readonly chargement = signal(false);

  protected readonly peutExporter = computed(() => !this.navCode() || this.ability.can('export', this.navCode()));
  protected readonly formats = this.telechargement.formats(null, () => this.filtres.demande());
  protected readonly ecartMarge = computed(() => {
    const s = this.synthese();
    return s ? ecartMargeUg(s.ug) : 0;
  });

  private readonly api = inject(JournalRemisesApiService);
  private readonly notification = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly ability = inject(AbilityService);

  constructor() {
    effect(() => {
      const filtre = this.filtres.demande();
      untracked(() => this.charger(filtre));
    });
  }

  protected afficher(): void {
    this.filtres.appliquer();
  }

  protected mecanique(type: TypePromotion | null): string {
    return type ? MECANIQUE_LIBELLES[type] : '';
  }

  private charger(filtre = this.filtres.demande()): void {
    this.chargement.set(true);
    this.api.synthese(filtre).subscribe({
      next: s => {
        this.synthese.set(s);
        this.chargement.set(false);
      },
      error: (err: unknown) => {
        this.chargement.set(false);
        this.notification.error(this.errorService.getErrorMessage(err, 'Impossible de charger la synthèse.'));
      },
    });
  }
}
