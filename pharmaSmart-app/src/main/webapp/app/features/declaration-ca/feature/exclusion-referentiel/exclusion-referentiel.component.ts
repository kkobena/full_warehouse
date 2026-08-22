import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, signal } from '@angular/core';

import { FormsModule } from '@angular/forms';

import { ButtonComponent } from 'app/shared/ui/button/button.component';
import { BadgeComponent } from 'app/shared/ui/badge/badge.component';
import { ToolbarComponent } from 'app/shared/ui/toolbar/toolbar.component';
import { CheckboxComponent } from 'app/shared/ui/checkbox/checkbox.component';
import { DataTableComponent } from 'app/shared/ui/data-table/data-table.component';
import { HintComponent } from 'app/shared/ui/hint/hint.component';
import { AppPillOption, PillSelectorComponent } from 'app/shared/ui/pill-selector/pill-selector.component';
import { NotificationService } from 'app/shared/services/notification.service';
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
 */
@Component({
  selector: 'app-exclusion-referentiel',
  imports: [FormsModule, ButtonComponent, BadgeComponent, CheckboxComponent, DataTableComponent, HintComponent, PillSelectorComponent, ToolbarComponent],
  templateUrl: './exclusion-referentiel.component.html',
  styleUrl: './exclusion-referentiel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExclusionReferentielComponent implements OnInit {
  readonly referentiel = input.required<ReferentielExclusion>();
  readonly titre = input.required<string>();
  /** Ce que l'exclusion produit concrètement, rappelé au-dessus du tableau. */
  readonly explication = input.required<string>();

  private readonly api = inject(DeclarationCaApiService);
  private readonly notification = inject(NotificationService);

  protected readonly items = signal<ExclusionItem[]>([]);
  protected readonly chargement = signal(false);
  protected readonly filtre = signal<Filtre>('tous');
  protected readonly recherche = signal('');

  protected readonly filtreOptions: AppPillOption[] = [
    { label: 'Tous', value: 'tous' },
    { label: 'Exclus', value: 'exclus', icon: 'pi pi-eye-slash' },
    { label: 'Non exclus', value: 'non-exclus', icon: 'pi pi-eye' },
  ];
  protected readonly selection = signal<Set<number>>(new Set());

  protected readonly itemsFiltres = computed(() => {
    const terme = this.recherche().trim().toLowerCase();
    const filtre = this.filtre();
    return this.items().filter(item => {
      if (filtre === 'exclus' && !item.exclu) {
        return false;
      }
      if (filtre === 'non-exclus' && item.exclu) {
        return false;
      }
      if (!terme) {
        return true;
      }
      return item.libelle.toLowerCase().includes(terme) || (item.code ?? '').toLowerCase().includes(terme);
    });
  });

  protected readonly nombreExclus = computed(() => this.items().filter(item => item.exclu).length);
  protected readonly nombreSelectionnes = computed(() => this.selection().size);

  /** Coché seulement si toutes les lignes visibles le sont — et jamais sur une liste vide. */
  protected readonly toutSelectionne = computed(() => {
    const visibles = this.itemsFiltres();
    return visibles.length > 0 && visibles.every(item => this.selection().has(item.id));
  });

  ngOnInit(): void {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    this.api.lister(this.referentiel()).subscribe({
      next: items => {
        this.items.set(items);
        this.selection.set(new Set());
        this.chargement.set(false);
      },
      error: () => {
        this.chargement.set(false);
        this.notification.error('Impossible de charger la liste');
      },
    });
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

  /** Ne porte que sur les lignes visibles : sélectionner à l'aveugle ce qu'un filtre masque serait piégeux. */
  protected basculerToutSelectionner(): void {
    if (this.toutSelectionne()) {
      this.selection.set(new Set());
      return;
    }
    this.selection.set(new Set(this.itemsFiltres().map(item => item.id)));
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

  private messageResultat(modifies: number, exclure: boolean): string {
    if (modifies === 0) {
      return 'Aucun changement : la sélection était déjà dans cet état';
    }
    const geste = exclure ? 'exclu' : 'réintégré';
    return `${modifies} élément${modifies > 1 ? 's' : ''} ${geste}${modifies > 1 ? 's' : ''}`;
  }
}
