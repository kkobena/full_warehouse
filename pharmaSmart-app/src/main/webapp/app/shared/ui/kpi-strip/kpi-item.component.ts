import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { AppKpiAccent } from './kpi-strip.component';

/**
 * Un indicateur du bandeau `app-kpi-strip`.
 *
 * `value` couvre le cas courant ; pour une valeur composée, projeter dans `[kpiValue]`.
 * Le sous-texte passe toujours par la projection `[kpiSub]`, parce qu'il porte le plus
 * souvent une flèche d'évolution conditionnelle.
 *
 * ⚠ Les emplacements se désignent par `ngProjectAs` (ex. `ngProjectAs="[kpiSub]"`), jamais
 * par l'attribut nu : IntelliJ ne reconnaît pas ce dernier et signale
 * « Attribute kpiSub is not allowed here » sur chaque usage.
 *
 * @example
 * <app-kpi-item [accent]="'success'" icon="pi pi-percentage" iconClass="text-success"
 *               label="Marge brute" [value]="marge | number" valueClass="text-success">
 *   <ng-container ngProjectAs="[kpiSub]">{{ taux | number:'1.1-1' }} %</ng-container>
 * </app-kpi-item>
 */
@Component({
  selector: 'app-kpi-item',
  // Les classes sont portées par l'hôte, pas par un `<div>` interne : `.kpi-strip` est un
  // conteneur flex et `app-kpi-item` en est l'enfant direct. Un div intermédiaire
  // s'interposerait, et c'est l'hôte — non stylé — qui recevrait le `flex: 1`.
  host: {
    '[class]': 'itemClasses()',
  },
  template: `
    @if (icon()) {
      <i [class]="iconClasses()" aria-hidden="true"></i>
    }

    <div class="kpi-strip-body">
      <span class="kpi-strip-label">{{ label() }}</span>

      <!--
        value et suffix sont interpolés séparément, jamais concaténés côté appelant :
        l'interpolation rend une valeur nulle comme chaîne vide, alors qu'une
        concaténation TypeScript produit littéralement « null FCFA ».
      -->
      <span [class]="valueClasses()">
        {{ value() }}@if (suffix()) {<span class="kpi-strip-suffix">{{ suffix() }}</span>}
        <ng-content select="[kpiValue]" />
      </span>

      <span class="kpi-strip-sub">
        <ng-content select="[kpiSub]" />
      </span>
    </div>
  `,
  styleUrl: './kpi-item.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KpiItemComponent {
  readonly label = input<string>('');

  /** Valeur principale. Pour un contenu composé, projeter dans `[kpiValue]`. */
  readonly value = input<string | number | null | undefined>('');

  /**
   * Unité affichée après la valeur, ex. `FCFA` ou `%`.
   *
   * À préférer à une concaténation dans l'appelant : `(x | number) + ' FCFA'` affiche
   * « null FCFA » dès que `x` est nul, là où l'interpolation ne rend rien.
   */
  readonly suffix = input<string>('');

  /** Classe d'icône, ex. `pi pi-shopping-cart`. */
  readonly icon = input<string>('');

  /** Couleur de l'icône, ex. `text-primary`, ou une classe utilitaire du projet. */
  readonly iconClass = input<string>('');

  /** Couleur de la valeur, ex. `text-success`. */
  readonly valueClass = input<string>('');

  /** Barre verticale colorée à gauche de l'item. */
  readonly accent = input<AppKpiAccent>('none');

  protected itemClasses(): string {
    const accent = this.accent();
    return ['kpi-strip-item', accent === 'none' ? '' : `${accent}-accent`].filter(Boolean).join(' ');
  }

  protected iconClasses(): string {
    return [this.icon(), this.iconClass()].filter(Boolean).join(' ');
  }

  protected valueClasses(): string {
    return ['kpi-strip-value', this.valueClass()].filter(Boolean).join(' ');
  }
}
