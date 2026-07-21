import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { NgbNav, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';

/**
 * Onglets horizontaux du Design System — conteneur et zone de contenu.
 *
 * **Enveloppe de mise en page, pas de remplacement de `ngbNav`.** La directive `ngbNav`
 * reste chez l'appelant : elle collecte ses onglets par `@ContentChildren(NgbNavItem)`
 * avec `descendants: false`. Des `<ng-container ngbNavItem>` *projetés* appartiennent au
 * template appelant et non au contenu de `ngbNav` — la requête ne les trouverait pas et
 * la barre s'afficherait vide. Ce composant n'apporte donc que les trois conteneurs qui
 * portaient jusqu'ici des noms de classes recopiés d'écran en écran.
 *
 * L'outlet est rendu ici : passer la référence de template du `ngbNav` via `[nav]`.
 *
 * @example
 * <app-nav-tabs [nav]="nav" containerClass="flex-1-overflow">
 *   <div #nav="ngbNav" ngbNav class="nav pharma-nav-tabs" [activeId]="activeTab()" (activeIdChange)="onTabChange($event)">
 *     <ng-container ngbNavItem="synthese">
 *       <a class="pharma-nav-tab-link" ngbNavLink><i class="pi pi-chart-line"></i><span>Synthèse</span></a>
 *       <ng-template ngbNavContent><app-synthese-tab /></ng-template>
 *     </ng-container>
 *   </div>
 * </app-nav-tabs>
 */
@Component({
  selector: 'app-nav-tabs',
  imports: [NgbNavOutlet],
  template: `
    <div [class]="containerClasses()">
      <ng-content />

      <!--
        Un seul écran sur dix-neuf n'a pas d'outlet (semois-config-masse, qui place son
        contenu ailleurs) : il est donc conditionnel plutôt qu'obligatoire.
      -->
      @if (nav()) {
        <div class="pharma-nav-tabs-outlet">
          <div [ngbNavOutlet]="nav()!"></div>
        </div>
      }
    </div>
  `,
  styleUrl: './nav-tabs.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavTabsComponent {
  /**
   * Référence de template du `ngbNav` projeté, ex. `#nav="ngbNav"`. Sans elle, aucun
   * outlet n'est rendu.
   */
  readonly nav = input<NgbNav | undefined>(undefined);

  /** Classes additionnelles sur le conteneur, ex. `flex-1-overflow`. */
  readonly containerClass = input<string>('');

  protected containerClasses(): string {
    return ['pharma-nav-tabs-container', this.containerClass()].filter(Boolean).join(' ');
  }
}
