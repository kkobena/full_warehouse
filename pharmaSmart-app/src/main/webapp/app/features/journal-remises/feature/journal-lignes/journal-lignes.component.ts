import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { AbilityService } from 'app/core/auth/ability.service';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { ButtonComponent } from 'app/shared/ui/button/button.component';
import { CardComponent } from 'app/shared/ui/card/card.component';
import { DataTableComponent } from 'app/shared/ui/data-table/data-table.component';
import { AppTableLazyLoadEvent } from 'app/shared/ui/data-table/table.types';
import { HintComponent } from 'app/shared/ui/hint/hint.component';
import { KpiItemComponent } from 'app/shared/ui/kpi-strip/kpi-item.component';
import { KpiStripComponent } from 'app/shared/ui/kpi-strip/kpi-strip.component';
import { SplitButtonComponent } from 'app/shared/ui/split-button/split-button.component';
import { ToolbarComponent } from 'app/shared/ui/toolbar/toolbar.component';
import { JournalRemisesApiService } from '../../data-access/journal-remises-api.service';
import { JournalRemisesFiltresStore } from '../../data-access/journal-remises-filtres.store';
import { JournalRemisesTelechargementService } from '../../data-access/journal-remises-telechargement.service';
import {
  ILigneJournal,
  ISyntheseJournal,
  NATURE_JOURNAL_LIBELLES,
  NATURE_VENTE_LIBELLES,
  NatureJournal,
  NatureVente,
  ORIGINE_LIBELLES,
  OrigineRemise,
} from '../../models/journal-remises.model';
import { JournalFiltresComponent } from '../../ui/journal-filtres/journal-filtres.component';

const ICONES: Record<NatureJournal, string> = {
  PROMOTIONS: 'pi pi-tags',
  REMISES: 'pi pi-percentage',
  UG: 'pi pi-gift',
};

const EXPLICATIONS: Record<NatureJournal, string> = {
  PROMOTIONS:
    'Chaque ligne de vente remisée par une promotion, unités offertes des offres N+M comprises. Une ligne qui cumule une promotion et une remise produit figure aussi au journal des remises produit, chacun avec sa part.',
  REMISES:
    'Chaque ligne de vente remisée par la grille de remise du produit, seule ou cumulée à une promotion. Seule la part due à la grille est comptée ici.',
  UG: "Les unités gratuites du fournisseur vendues au prix normal — pas les unités offertes par une promotion. Elles n'ont rien coûté : leur HT net est leur marge.",
};

/**
 * Un journal ligne à ligne : promotions, remises produit ou UG vendues. Un seul composant pour les
 * trois, comme le journal du Retraitement du CA : mêmes filtres, même pagination ; seules les
 * colonnes changent.
 *
 * <p>Le tableau est paginé par le serveur. Les indicateurs viennent de la synthèse, calculée sur
 * toute la période : une page n'en dit rien.
 */
@Component({
  selector: 'app-journal-remises-lignes',
  imports: [
    DatePipe,
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
  templateUrl: './journal-lignes.component.html',
  styleUrl: './journal-lignes.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JournalRemisesLignesComponent {
  readonly nature = input.required<NatureJournal>();
  /** L'entrée de menu dont l'écran est le contenu : le titre de la barre suit son libellé. */
  readonly navCode = input<string>('');

  protected readonly filtres = inject(JournalRemisesFiltresStore);
  protected readonly telechargement = inject(JournalRemisesTelechargementService);

  protected readonly lignesParPage = 50;
  protected readonly first = signal(0);
  protected readonly lignes = signal<ILigneJournal[]>([]);
  protected readonly total = signal(0);
  protected readonly synthese = signal<ISyntheseJournal | null>(null);
  protected readonly chargement = signal(false);

  protected readonly titre = computed(() => NATURE_JOURNAL_LIBELLES[this.nature()]);
  protected readonly icone = computed(() => ICONES[this.nature()]);
  protected readonly explication = computed(() => EXPLICATIONS[this.nature()]);
  protected readonly estUg = computed(() => this.nature() === 'UG');
  protected readonly estPromotions = computed(() => this.nature() === 'PROMOTIONS');
  protected readonly peutExporter = computed(() => !this.navCode() || this.ability.can('export', this.navCode()));
  protected readonly formats = computed(() => this.telechargement.formats(this.nature(), () => this.filtres.demande()));

  /** Les totaux de cette nature, sur toute la période et les filtres appliqués. */
  protected readonly totaux = computed(() => {
    const s = this.synthese();
    if (!s) {
      return null;
    }
    return this.nature() === 'PROMOTIONS' ? s.promotions : s.remisesProduit;
  });

  private readonly api = inject(JournalRemisesApiService);
  private readonly notification = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly ability = inject(AbilityService);

  constructor() {
    effect(() => {
      const filtre = this.filtres.demande();
      untracked(() => {
        this.first.set(0);
        this.chargerPage();
        this.api.synthese(filtre).subscribe({ next: s => this.synthese.set(s), error: () => this.synthese.set(null) });
      });
    });
  }

  protected afficher(): void {
    this.filtres.appliquer();
  }

  /** Changement de page : la position vient de l'événement, le tableau ne la renvoie pas d'abord. */
  protected onLazyLoad(event: AppTableLazyLoadEvent): void {
    this.first.set(event.first);
    this.chargerPage();
  }

  protected natureVente(nature: NatureVente): string {
    return NATURE_VENTE_LIBELLES[nature] ?? nature;
  }

  protected origine(origine: OrigineRemise | null): string {
    return origine ? ORIGINE_LIBELLES[origine] : '';
  }

  private chargerPage(): void {
    this.chargement.set(true);
    const page = Math.floor(this.first() / this.lignesParPage);
    this.api.lignes(this.nature(), this.filtres.demande(), page, this.lignesParPage).subscribe({
      next: res => {
        this.lignes.set(res.body ?? []);
        this.total.set(Number(res.headers.get('X-Total-Count') ?? 0));
        this.chargement.set(false);
      },
      error: (err: unknown) => {
        this.chargement.set(false);
        this.notification.error(this.errorService.getErrorMessage(err, 'Impossible de charger le journal.'));
      },
    });
  }
}
