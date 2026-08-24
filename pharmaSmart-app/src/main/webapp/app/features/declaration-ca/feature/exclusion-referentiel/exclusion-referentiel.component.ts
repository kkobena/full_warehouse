import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { HttpResponse } from '@angular/common/http';
import { debounceTime, distinctUntilChanged, skip } from 'rxjs/operators';

import { FormsModule } from '@angular/forms';

import { ButtonComponent } from 'app/shared/ui/button/button.component';
import { BadgeComponent } from 'app/shared/ui/badge/badge.component';
import { ToolbarComponent } from 'app/shared/ui/toolbar/toolbar.component';
import { CheckboxComponent } from 'app/shared/ui/checkbox/checkbox.component';
import { DataTableComponent } from 'app/shared/ui/data-table/data-table.component';
import { AppTableLazyLoadEvent } from 'app/shared/ui/data-table/table.types';
import { HintComponent } from 'app/shared/ui/hint/hint.component';
import { AppPillOption, PillSelectorComponent } from 'app/shared/ui/pill-selector/pill-selector.component';
import { NotificationService } from 'app/shared/services/notification.service';
import { ITEMS_PER_PAGE, TOTAL_COUNT_RESPONSE_HEADER } from 'app/config/pagination.constants';
import {
  DeclarationCaApiService,
  ExclusionItem,
  ReferentielExclusion,
} from '../../data-access/services/declaration-ca-api.service';

type Filtre = 'tous' | 'exclus' | 'non-exclus';

/**
 * Écran d'exclusion d'un référentiel — rayons ou tiers-payants.
 *
 * <p>Un seul composant pour les deux : le geste est identique (cocher des lignes, appliquer) et
 * seuls le libellé et l'URL changent. Deux composants auraient dupliqué la sélection de masse, le
 * filtre et la recherche, pour diverger dès la première colonne ajoutée d'un côté seulement.
 *
 * <p>Filtre, recherche et découpage sont faits par le serveur : sur un référentiel de plusieurs
 * milliers de tiers-payants, tout charger pour n'en afficher que vingt lignes coûtait une requête
 * entière à chaque ouverture, et filtrer en mémoire une page déjà découpée rendrait des pages
 * trouées et un total faux.
 */
@Component({
  selector: 'app-exclusion-referentiel',
  imports: [
    FormsModule,
    ButtonComponent,
    BadgeComponent,
    CheckboxComponent,
    DataTableComponent,
    HintComponent,
    PillSelectorComponent,
    ToolbarComponent,
  ],
  templateUrl: './exclusion-referentiel.component.html',
  styleUrl: './exclusion-referentiel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExclusionReferentielComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit alors le libellé du menu — ou son
   * `titre_long` quand la barre nomme plus longuement — au lieu d'une valeur figée dans le gabarit.
   */
  readonly navCode = input<string>('');

  readonly referentiel = input.required<ReferentielExclusion>();
  readonly titre = input.required<string>();
  /** Ce que l'exclusion produit concrètement, rappelé au-dessus du tableau. */
  readonly explication = input.required<string>();

  private readonly api = inject(DeclarationCaApiService);
  private readonly notification = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  /** La page courante telle que renvoyée par le serveur — jamais l'intégralité du référentiel. */
  protected readonly items = signal<ExclusionItem[]>([]);
  /** Total de l'ensemble filtré, lu dans l'en-tête `X-Total-Count`. */
  protected readonly totalItems = signal(0);
  protected readonly chargement = signal(false);
  protected readonly filtre = signal<Filtre>('tous');
  protected readonly recherche = signal('');

  /** Index de la première ligne affichée : ce que la table pilote pour sa pagination. */
  protected readonly first = signal(0);
  protected readonly rows = signal(ITEMS_PER_PAGE);

  /** Compté sur la totalité du référentiel et non sur la page : c'est un état d'ensemble. */
  protected readonly nombreExclus = signal(0);

  protected readonly filtreOptions: AppPillOption[] = [
    { label: 'Tous', value: 'tous' },
    { label: 'Exclus', value: 'exclus', icon: 'pi pi-eye-slash' },
    { label: 'Non exclus', value: 'non-exclus', icon: 'pi pi-eye' },
  ];
  protected readonly selection = signal<Set<number>>(new Set());

  protected readonly nombreSelectionnes = computed(() => this.selection().size);

  /** Coché seulement si toute la page affichée l'est — et jamais sur une page vide. */
  protected readonly toutSelectionne = computed(() => {
    const visibles = this.items();
    return visibles.length > 0 && visibles.every(item => this.selection().has(item.id));
  });

  constructor() {
    // La frappe ne déclenche pas une requête par caractère : elle attend une pause, et une valeur
    // identique à la précédente (un aller-retour de curseur) ne recharge rien. `skip(1)` laisse le
    // chargement initial à `ngOnInit`, qui part sans attendre le délai.
    toObservable(this.recherche)
      .pipe(skip(1), debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.rechargerDepuisLeDebut());
  }

  ngOnInit(): void {
    this.charger();
  }

  /** Recharge la page courante, en conservant le rang atteint dans la pagination. */
  protected charger(): void {
    this.chargement.set(true);
    this.api
      .lister(this.referentiel(), {
        page: this.pageCourante(),
        size: this.rows(),
        exclus: this.exclusFiltre(),
        search: this.recherche().trim(),
      })
      .subscribe({
        next: (reponse: HttpResponse<ExclusionItem[]>) => {
          this.items.set(reponse.body ?? []);
          this.totalItems.set(Number(reponse.headers.get(TOTAL_COUNT_RESPONSE_HEADER) ?? 0));
          this.selection.set(new Set());
          this.chargement.set(false);
        },
        error: () => {
          this.chargement.set(false);
          this.notification.error('Impossible de charger la liste');
        },
      });
    this.compterExclus();
  }

  protected changerFiltre(filtre: Filtre): void {
    this.filtre.set(filtre);
    this.rechargerDepuisLeDebut();
  }

  /** Le découpage est serveur : la table ne fait que signaler la tranche qu'elle veut voir. */
  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    this.rows.set(event.rows);
    this.first.set(event.first);
    this.charger();
  }

  protected estSelectionne(id: number): boolean {
    return this.selection().has(id);
  }

  protected basculerSelection(id: number): void {
    const suivante = new Set(this.selection());
    if (suivante.has(id)) {
      suivante.delete(id);
    } else {
      suivante.add(id);
    }
    this.selection.set(suivante);
  }

  /**
   * Ne porte que sur la page affichée : cocher d'un geste des lignes que l'utilisateur n'a pas sous
   * les yeux — toutes les autres pages — serait piégeux.
   */
  protected basculerToutSelectionner(): void {
    if (this.toutSelectionne()) {
      this.selection.set(new Set());
      return;
    }
    this.selection.set(new Set(this.items().map(item => item.id)));
  }

  protected appliquer(exclure: boolean): void {
    const ids = [...this.selection()];
    if (ids.length === 0) {
      return;
    }
    this.chargement.set(true);
    this.api.majExclusion(this.referentiel(), ids, exclure).subscribe({
      next: ({ modifies }) => {
        this.notification.success(this.messageResultat(modifies, exclure));
        this.charger();
      },
      error: () => {
        this.chargement.set(false);
        this.notification.error('La mise à jour a échoué');
      },
    });
  }

  /** Un filtre ou une recherche change l'ensemble : rester page 12 n'y afficherait que du vide. */
  private rechargerDepuisLeDebut(): void {
    this.first.set(0);
    this.charger();
  }

  private pageCourante(): number {
    return this.rows() > 0 ? Math.floor(this.first() / this.rows()) : 0;
  }

  /** `null` quand aucun filtre d'état n'est demandé : le serveur renvoie alors les deux états. */
  private exclusFiltre(): boolean | null {
    switch (this.filtre()) {
      case 'exclus':
        return true;
      case 'non-exclus':
        return false;
      default:
        return null;
    }
  }

  /**
   * Le compteur du bandeau, obtenu par un décompte serveur sur une page d'une ligne plutôt qu'en
   * comptant les lignes affichées : celles-ci ne sont qu'une page, le total resterait faux.
   */
  private compterExclus(): void {
    this.api.lister(this.referentiel(), { page: 0, size: 1, exclus: true }).subscribe({
      next: reponse => this.nombreExclus.set(Number(reponse.headers.get(TOTAL_COUNT_RESPONSE_HEADER) ?? 0)),
      error: () => this.nombreExclus.set(0),
    });
  }

  private messageResultat(modifies: number, exclure: boolean): string {
    if (modifies === 0) {
      return 'Aucun changement : la sélection était déjà dans cet état';
    }
    const geste = exclure ? 'exclu' : 'réintégré';
    return `${modifies} élément${modifies > 1 ? 's' : ''} ${geste}${modifies > 1 ? 's' : ''}`;
  }
}
