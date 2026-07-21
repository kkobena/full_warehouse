import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Barre d'actions secondaire — filtres à gauche, actions à droite.
 *
 * Destinée aux composants enfants montés dans un parent qui porte déjà l'en-tête sombre
 * (`app-toolbar`) : fond gris clair, séparateur bas, rembourrage compact. À ne pas
 * confondre avec `app-toolbar`, qui est la barre principale d'un écran et porte le titre.
 *
 * Encapsule la structure `.su-action-bar` / `__filters` / `__actions` recopiée sur cinq
 * écrans du domaine commande.
 *
 * ⚠ Les emplacements se désignent par `ngProjectAs`, jamais par l'attribut nu : IntelliJ
 * ne reconnaît pas ce dernier et signale « Attribute actionBarFilters is not allowed
 * here » sur chaque usage.
 *
 * @example
 * <app-action-bar>
 *   <ng-container ngProjectAs="[actionBarFilters]">
 *     <app-icon-field icon="pi pi-search"><input class="form-control" /></app-icon-field>
 *   </ng-container>
 *
 *   <ng-container ngProjectAs="[actionBarActions]">
 *     <app-button (clicked)="onSearch()" icon="pi pi-search" label="Rechercher" severity="info" size="small" />
 *   </ng-container>
 * </app-action-bar>
 */
@Component({
  selector: 'app-action-bar',
  template: `
    <div [class]="barClasses()">
      <div class="su-action-bar__filters">
        <ng-content select="[actionBarFilters]" />
      </div>

      <div class="su-action-bar__actions">
        <ng-content select="[actionBarActions]" />
      </div>
    </div>
  `,
  styleUrl: './action-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActionBarComponent {
  /** Classes additionnelles posées sur la barre. */
  readonly barClass = input<string>('');

  protected barClasses(): string {
    return ['su-action-bar', this.barClass()].filter(Boolean).join(' ');
  }
}
