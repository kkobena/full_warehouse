import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { NavStore } from 'app/core/store/nav.store';

/**
 * Barre d'outils d'écran — en-tête coloré, filtres à gauche, actions à droite.
 *
 * Encapsule la structure recopiée sur 71 écrans (`.pharma-toolbar`, `-header`, `-title`,
 * `-content`, `-filters`, `-actions`) et remplace l'usage résiduel de `<p-toolbar>`.
 *
 * Les conteneurs de filtres et d'actions sont rendus inconditionnellement : c'est déjà le
 * cas dans le markup d'origine, où `.pharma-toolbar-filters` apparaît souvent vide pour
 * pousser les actions à droite. Les supprimer quand ils sont vides casserait cet
 * alignement.
 *
 * ⚠ Les emplacements se désignent par `ngProjectAs`, jamais par l'attribut nu
 * (`<ng-container toolbarFilters>`). Les deux fonctionnent dans Angular, mais IntelliJ ne
 * reconnaît pas l'attribut nu et signale « Attribute toolbarFilters is not allowed here »
 * sur chaque usage. `ngProjectAs` est un attribut Angular officiel, donc résolu.
 *
 * @example
 * <app-toolbar icon="pi pi-shield" title="Liste des tiers payants" [compact]="true">
 *   <ng-container ngProjectAs="[toolbarFilters]">
 *     <app-select [items]="type" [(ngModel)]="typeSelected" />
 *   </ng-container>
 *
 *   <ng-container ngProjectAs="[toolbarActions]">
 *     <div class="pharma-button-group">
 *       <app-button (clicked)="loadPage()" icon="pi pi-search" label="Rechercher" severity="info" />
 *     </div>
 *   </ng-container>
 * </app-toolbar>
 *
 * @example Badge ou action dans l'en-tête
 * <app-toolbar icon="pi pi-file" title="Factures">
 *   <span ngProjectAs="[toolbarHeaderExtra]" class="pharma-badge">{{ total() }}</span>
 * </app-toolbar>
 */
const CONTROLES_FOCALISABLES = 'button:not([disabled]), a[href], [tabindex]:not([tabindex="-1"]):not(button):not(a)';

@Component({
  selector: 'app-toolbar',
  template: `
    <div [class]="toolbarClasses()">
      <div class="pharma-toolbar-header">
        @if (icon()) {
          <i [class]="icon()" aria-hidden="true"></i>
        }

        <!--
          Un TITRE, pas un simple texte en gras : la barre d'outils nomme l'écran, et c'était
          jusqu'ici un \`<span>\` — aucune page de l'application n'offrait donc de titre à la
          navigation par en-têtes, sur laquelle repose la lecture d'écran. Le style suit la
          classe, il ne change pas.
        -->
        <h1 class="pharma-toolbar-title">{{ resolvedTitle() }}</h1>

        @if (subtitle()) {
          <span class="pharma-toolbar-subtitle">{{ subtitle() }}</span>
        }

        <ng-content select="[toolbarHeaderExtra]" />
      </div>

      <div class="pharma-toolbar-content">
        <div class="pharma-toolbar-filters">
          <ng-content select="[toolbarFilters]" />
        </div>

        <!-- Flèches gauche / droite, Début et Fin d'un bouton à l'autre ; Tab passe toujours par chacun. -->
        <div (keydown)="naviguerAuxFleches($event)" [attr.aria-label]="'Actions : ' + resolvedTitle()" class="pharma-toolbar-actions" role="toolbar">
          <ng-content select="[toolbarActions]" />
        </div>
      </div>

      <!-- Contenu libre sous la barre : bandeau de progression, message d'état… -->
      <ng-content />
    </div>
  `,
  styleUrl: './toolbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToolbarComponent {
  /** Titre affiché dans l'en-tête, quand aucun {@link code} ne le fournit. */
  readonly title = input<string>('');

  /**
   * Code de l'entrée de navigation dont cet écran est le contenu, ex. `declaration-ca.ponction`.
   *
   * <p>Renseigné, le titre vient de la base : `titre_long` s'il existe, le libellé du menu sinon.
   * L'administrateur qui renomme une entrée voit donc le changement ici comme dans le menu, au lieu
   * de deux valeurs qui divergent en silence.
   *
   * <p>`titre_long` existe parce que les deux libellés n'ont pas le même métier : le menu vit dans
   * une colonne repliable et doit rester court, la barre dispose de toute la largeur et peut nommer
   * précisément — « Ponction » d'un côté, « Ponction du chiffre d'affaires » de l'autre.
   */
  readonly code = input<string>('');

  private readonly navStore = inject(NavStore);

  /**
   * Le titre effectif : la base d'abord, l'entrée `title` en dernier recours.
   *
   * <p>Le repli n'est pas une commodité : un code absent de l'arbre — item désactivé, installation
   * plus ancienne — laisserait sinon un écran sans titre.
   */
  protected readonly resolvedTitle = computed(() => {
    const noeud = this.code() ? this.navStore.node(this.code()) : undefined;
    return noeud?.titreLong || noeud?.libelle || this.title();
  });

  /** Classe d'icône précédant le titre, ex. `pi pi-shield`. */
  readonly icon = input<string>('');

  /** Ligne secondaire dans l'en-tête. Rare — un seul écran l'utilise aujourd'hui. */
  readonly subtitle = input<string>('');

  /** Variante resserrée, pour les barres à quatre filtres ou plus. */
  readonly compact = input<boolean>(false);

  /** Classes additionnelles posées sur la barre. */
  readonly toolbarClass = input<string>('');

  /** Déplace le focus entre les contrôles de la zone d'actions (patron « toolbar » de l'APG), sauf depuis un champ de saisie. */
  protected naviguerAuxFleches(event: KeyboardEvent): void {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key) || event.altKey || event.ctrlKey || event.metaKey) {
      return;
    }
    const cible = event.target as HTMLElement;
    if (cible.closest('input, textarea, select, [contenteditable="true"], ng-select')) {
      return;
    }
    const barre = event.currentTarget as HTMLElement;
    const controles = Array.from(barre.querySelectorAll<HTMLElement>(CONTROLES_FOCALISABLES))
      // Les boutons masqués par CSS (`d-none` responsive) sont sautés ; sans `checkVisibility` (jsdom), tous comptent.
      .filter(el => el.checkVisibility?.() ?? true);
    const courant = controles.findIndex(el => el === cible || el.contains(cible));
    if (controles.length < 2 || courant < 0) {
      return;
    }
    const dernier = controles.length - 1;
    const suivant: Record<string, number> = {
      ArrowLeft: courant === 0 ? dernier : courant - 1,
      ArrowRight: courant === dernier ? 0 : courant + 1,
      Home: 0,
      End: dernier,
    };
    event.preventDefault();
    controles[suivant[event.key]].focus();
  }

  protected toolbarClasses(): string {
    return ['pharma-toolbar', this.compact() ? 'pharma-toolbar-compact' : '', this.toolbarClass()].filter(Boolean).join(' ');
  }
}
