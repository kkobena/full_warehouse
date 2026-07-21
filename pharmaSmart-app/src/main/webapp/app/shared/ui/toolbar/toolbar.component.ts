import { ChangeDetectionStrategy, Component, input } from '@angular/core';

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
@Component({
  selector: 'app-toolbar',
  template: `
    <div [class]="toolbarClasses()">
      <div class="pharma-toolbar-header">
        @if (icon()) {
          <i [class]="icon()" aria-hidden="true"></i>
        }

        <span class="pharma-toolbar-title">{{ title() }}</span>

        @if (subtitle()) {
          <span class="pharma-toolbar-subtitle">{{ subtitle() }}</span>
        }

        <ng-content select="[toolbarHeaderExtra]" />
      </div>

      <div class="pharma-toolbar-content">
        <div class="pharma-toolbar-filters">
          <ng-content select="[toolbarFilters]" />
        </div>

        <div class="pharma-toolbar-actions">
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
  /** Titre affiché dans l'en-tête. */
  readonly title = input<string>('');

  /** Classe d'icône précédant le titre, ex. `pi pi-shield`. */
  readonly icon = input<string>('');

  /** Ligne secondaire dans l'en-tête. Rare — un seul écran l'utilise aujourd'hui. */
  readonly subtitle = input<string>('');

  /** Variante resserrée, pour les barres à quatre filtres ou plus. */
  readonly compact = input<boolean>(false);

  /** Classes additionnelles posées sur la barre. */
  readonly toolbarClass = input<string>('');

  protected toolbarClasses(): string {
    return ['pharma-toolbar', this.compact() ? 'pharma-toolbar-compact' : '', this.toolbarClass()].filter(Boolean).join(' ');
  }
}
