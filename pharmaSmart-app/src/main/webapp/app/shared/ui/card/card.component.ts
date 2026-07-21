import {ChangeDetectionStrategy, Component, input} from '@angular/core';
import {NgStyle} from '@angular/common';

/**
 * Gabarit visuel de la carte.
 *
 * ⚠ Passer la valeur en liaison — `[variant]="'form'"` — et non en attribut statique.
 * IntelliJ ne résout pas l'attribut statique vers cette entrée et signale
 * « Attribute variant is not allowed here » (vérifié : le passage à un type nommé n'y
 * change rien, seule la liaison lève l'avertissement). Angular accepte les deux formes.
 * Même précaution que pour `appKeyFilter`.
 */
export type AppCardVariant = 'data' | 'form';

/**
 * Carte du Design System — rend du Bootstrap 5 natif habillé en `.data-card`.
 *
 * Remplace `p-card`. L'implémentation précédente encapsulait `p-card` de PrimeNG : elle
 * ne retirait donc aucune dépendance et ne survivait pas à la Phase 4 du plan de
 * migration. Celle-ci n'a plus aucun lien avec PrimeNG.
 *
 * L'en-tête n'est rendu que si `header` ou `icon` est renseigné, ou si du contenu est
 * projeté dans `[cardActions]`. Pour un en-tête entièrement libre, projeter dans
 * `[cardHeader]` : il remplace alors titre et icône.
 *
 * ⚠ Les emplacements se désignent par `ngProjectAs` (ex. `ngProjectAs="[cardHeader]"`), jamais
 * par l'attribut nu : IntelliJ ne reconnaît pas ce dernier et signale
 * « Attribute cardHeader is not allowed here » sur chaque usage.
 *
 * @example
 * <app-card header="Total par mode de règlement" icon="pi pi-credit-card text-info">
 *   <ul class="list-group list-group-flush">…</ul>
 * </app-card>
 *
 * @example Actions à droite de l'en-tête
 * <app-card header="Ventes par tiers-payant" icon="pi pi-chart-pie text-success" bodyClass="py-4">
 *   <app-select ngProjectAs="[cardActions]" [items]="tops" bindLabel="label" />
 *   <ul>…</ul>
 * </app-card>
 */
@Component({
  selector: 'app-card',
  imports: [NgStyle],
  template: `
    <div [class]="cardClasses()" [ngStyle]="style()">
      @if (showHeader()) {
        <div class="card-header">
          <!--
            Le titre reste dans un <h5 class="card-title"> pour conserver la hiérarchie
            documentaire et le style de .data-card. Un en-tête libre projeté le remplace
            entièrement.
          -->
          <ng-content select="[cardHeader]">
            <h5 class="card-title">
              @if (icon()) {
                <i [class]="icon()" aria-hidden="true"></i>
              }
              {{ header() }}
            </h5>
          </ng-content>

          <ng-content select="[cardActions]" />
        </div>
      }

      @if (subheader()) {
        <div class="card-subtitle text-muted px-3 pt-2">{{ subheader() }}</div>
      }

      <div class="card-body" [class]="bodyClass()">
        <ng-content />
      </div>

      @if (showFooter()) {
        <div class="card-footer">
          <ng-content select="[cardFooter]" />
        </div>
      }
    </div>
  `,
  styleUrl: './card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardComponent {
  /** Titre affiché dans l'en-tête. */
  readonly header = input<string>('');

  /** Classe d'icône précédant le titre, ex. `pi pi-credit-card text-info`. */
  readonly icon = input<string>('');

  /** Ligne secondaire sous l'en-tête. */
  readonly subheader = input<string>('');

  /**
   * Force l'affichage de l'en-tête même sans `header` ni `icon` — nécessaire quand seul
   * `[cardHeader]` ou `[cardActions]` est projeté, Angular n'offrant pas de détection
   * fiable de la présence d'un contenu projeté.
   */
  readonly withHeader = input<boolean>(false);

  /** Affiche le pied de carte, à remplir via `[cardFooter]`. */
  readonly withFooter = input<boolean>(false);

  /** Classes du corps, ex. `py-4` ou `p-0` — toutes deux gérées par `.data-card`. */
  readonly bodyClass = input<string>('');

  /**
   * Gabarit visuel.
   *
   * - `data` : habillage `.data-card` des tableaux de bord — fond blanc, coins 16px,
   *   ombre au survol, en-tête en dégradé.
   * - `form` : cartes de regroupement des modales — fond `--p-surface-50`, corps
   *   transparent, en-tête sans rembourrage pour laisser un `.section-header` projeté
   *   occuper toute la largeur.
   *
   * Cette distinction existe parce que `_modal-theme.scss` stylait ces cartes sans
   * `::ng-deep` : ses règles ne franchissent pas l'encapsulation du composant, qui doit
   * donc porter lui-même l'habillage des formulaires.
   */
  readonly variant = input<AppCardVariant>('data');

  /** Classes additionnelles posées sur la carte. */
  readonly customClass = input<string>('');

  readonly style = input<Record<string, unknown>>({});

  protected cardClasses(): string {
    const variantClass = this.variant() === 'form' ? 'card-form' : 'data-card';
    return ['card', 'h-100', variantClass, this.customClass()].filter(Boolean).join(' ');
  }

  protected showHeader(): boolean {
    return this.withHeader() || !!this.header() || !!this.icon();
  }

  protected showFooter(): boolean {
    return this.withFooter();
  }
}
