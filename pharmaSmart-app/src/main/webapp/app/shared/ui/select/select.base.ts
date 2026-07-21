import { Directive, computed, input, output } from '@angular/core';
import { Subject } from 'rxjs';

import { ControlValueAccessorBase } from '../forms/control-value-accessor.base';

/**
 * Socle commun à `AppSelectSearch` (sélection simple) et `AppMultiSelect` (multiple).
 *
 * Les deux wrappent `@ng-select/ng-select` (décision §14.2 du plan) et n'exposent que
 * les entrées réellement utilisées dans l'app — le but des wrappers est justement
 * d'**isoler les écrans de la dépendance** : aucun template applicatif ne doit importer
 * `NgSelectComponent` directement, pour qu'un futur changement de lib reste local.
 *
 * L'apparence est calée sur celle de `p-select` via `content/scss/_ng-select-pharma.scss`.
 */
@Directive()
export abstract class SelectBase<TValue> extends ControlValueAccessorBase<TValue> {
  /** Options proposées. Objets ou primitives. */
  readonly items = input.required<readonly unknown[]>();

  /** Propriété affichée quand `items` contient des objets. */
  readonly bindLabel = input<string>('');

  /** Propriété utilisée comme valeur du modèle. Vide = l'objet entier est émis. */
  readonly bindValue = input<string>('');

  readonly placeholder = input<string>('');

  readonly disabled = input<boolean>(false);

  readonly invalid = input<boolean>(false);

  readonly loading = input<boolean>(false);

  /**
   * Sens d'ouverture du panneau. `'auto'` (défaut) laisse ng-select choisir haut/bas
   * selon la place disponible.
   *
   * Sur un viewport bas (écrans ≤16 pouces, ~460-500px de hauteur utile), la bascule
   * vers `'top'` peut mal se positionner : `ng-select` calcule alors la position à
   * partir du rectangle de son `appendTo` (ici `<body>`), et un recalcul déclenché à un
   * instant où la mise en page a légèrement bougé peut placer le panneau hors écran —
   * il s'ouvre vers le haut puis semble se refermer. Forcer `'bottom'` sur un champ
   * proche du haut d'un formulaire court-circuite cette branche.
   */
  readonly dropdownPosition = input<'auto' | 'bottom' | 'top'>('auto');

  /**
   * Croix de remise à zéro à droite du champ.
   *
   * `false` par défaut, comme `p-select` / `p-multiselect` dont `showClear` vaut `false` :
   * activée, elle s'ajoutait à la croix que `ng-select` pose déjà sur chaque chip en mode
   * multiple, d'où deux croix là où PrimeNG n'en montrait aucune.
   */
  readonly clearable = input<boolean>(false);

  readonly searchable = input<boolean>(true);

  /** Affiche une liste virtualisée — à activer au-delà de quelques centaines d'options. */
  readonly virtualScroll = input<boolean>(false);

  /** Propriété de regroupement des options. Vide = pas de regroupement. */
  readonly groupBy = input<string>('');

  readonly notFoundText = input<string>('Aucun résultat');

  readonly loadingText = input<string>('Chargement…');

  /**
   * Message affiché à l'ouverture, avant toute saisie, quand `[typeahead]` est branché
   * (recherche serveur) — remplace le "Type to search" anglais par défaut de ng-select.
   */
  readonly typeToSearchText = input<string>('Tapez pour rechercher…');

  /** Compacte le champ, équivalent de `.p-select-sm`. */
  readonly small = input<boolean>(false);

  /**
   * Nom accessible du champ, quand aucun `<label>` ne lui est associé — cas d'un select
   * isolé dans une barre d'outils ou un pied de tableau.
   */
  readonly ariaLabel = input<string>('');

  /**
   * `id` porté par la zone de saisie, à faire correspondre au `for` du `<label>`.
   * Sans lui, cliquer le libellé n'ouvre pas la liste.
   */
  readonly inputId = input<string>('');

  /**
   * Rattache le panneau au `body`. Indispensable dans une modale ou un conteneur
   * en `overflow: hidden`, sinon le menu est rogné.
   */
  readonly appendTo = input<string>('');

  /**
   * Styles en ligne posés sur le `<ng-select>`, équivalent du `[style]` de `p-select`.
   *
   * Réservé au calage dimensionnel que seul l'appelant connaît — largeur dans une barre
   * d'outils, `min-width` dans une grille. Tout ce qui relève de l'apparence (couleurs,
   * bordures, états) appartient à `_ng-select-pharma.scss` : le passer ici ferait diverger
   * ce champ du reste du Design System.
   */
  readonly style = input<Record<string, unknown>>({});

  /** Alimente une recherche côté serveur ; laisser vide pour un filtrage local. */
  readonly typeahead = input<Subject<string> | undefined>(undefined);

  /** Terme de recherche saisi — à brancher sur un chargement distant. */
  readonly searched = output<string>();

  /**
   * Longueur minimale du terme avant d'émettre `searched`. Reprend `minQueryLength` de
   * `p-autocomplete`.
   *
   * `ng-select` émet dès le premier caractère, là où `p-autocomplete` attendait le seuil.
   * Sans ce garde-fou, une migration littérale multiplie les appels serveur sur les
   * champs alimentés à distance. `0` désactive le filtrage.
   *
   * L'effacement du champ (terme vide) est toujours transmis : il doit pouvoir
   * réinitialiser la liste côté appelant.
   */
  readonly minSearchLength = input<number>(0);

  /** Émis à chaque changement de sélection, en plus du `ControlValueAccessor`. */
  readonly selectionChange = output<TValue | null>();

  /** Atteinte du bas de liste — pour la pagination incrémentale. */
  readonly scrolledToEnd = output<void>();

  protected readonly ngSelectClasses = computed(() => {
    const classes: string[] = [];
    if (this.small()) classes.push('app-select-sm');
    // `.ng-invalid.ng-touched` est déjà stylé ; cette classe couvre l'invalidité pilotée à la main.
    if (this.invalid()) classes.push('ng-invalid', 'ng-touched');
    return classes.join(' ');
  });

  /** Relaie le terme saisi vers `searched`, sous réserve de `minSearchLength`. */
  protected onSearched(term: string): void {
    const value = term ?? '';
    if (value.length && value.length < this.minSearchLength()) {
      return;
    }
    this.searched.emit(value);
  }

  protected onSelectionChange(value: TValue | null): void {
    this.updateValue(value);
    this.selectionChange.emit(value);
  }
}
