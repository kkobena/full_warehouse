import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { formatDecimal } from 'app/shared/utils/format-utils';
import { SensFavorable, UniteIndicateur } from '../../models/pilotage.model';

/**
 * Variation d'un indicateur : flèche, valeur et sens en toutes lettres pour les lecteurs d'écran (la couleur n'est jamais le
 * seul porteur d'information). Un taux varie en points, le reste en %. Couleur selon le sens favorable : une hausse des
 * remises est défavorable.
 */
@Component({
  selector: 'app-variation',
  template: `
    @if (texte(); as libelle) {
      <span [class]="classes()">
        <span aria-hidden="true">{{ fleche() }}</span>
        <span class="visually-hidden">{{ sens() }}</span>
        {{ libelle }}
      </span>
    } @else {
      <span class="variation variation--neutre" title="Non significatif : pas de référence comparable">n.s.</span>
    }
  `,
  styleUrl: './variation.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VariationComponent {
  readonly ecart = input<number | null>(null);
  readonly ecartPct = input<number | null>(null);
  readonly unite = input.required<UniteIndicateur>();
  readonly sensFavorable = input.required<SensFavorable>();

  private readonly enPoints = computed(() => this.unite() === 'POURCENTAGE');
  private readonly valeur = computed(() => (this.enPoints() ? this.ecart() : this.ecartPct()));

  protected readonly texte = computed(() => {
    const valeur = this.valeur();
    if (valeur == null) {
      return null;
    }
    return `${formatDecimal(Math.abs(valeur), 1)} ${this.enPoints() ? 'pt' : '%'}`;
  });

  protected readonly fleche = computed(() => {
    const valeur = this.valeur() ?? 0;
    return valeur > 0 ? '▲' : valeur < 0 ? '▼' : '=';
  });

  protected readonly sens = computed(() => {
    const valeur = this.valeur() ?? 0;
    return valeur > 0 ? 'en hausse de' : valeur < 0 ? 'en baisse de' : 'stable,';
  });

  protected readonly classes = computed(() => `variation variation--${this.appreciation()}`);

  private appreciation(): 'favorable' | 'defavorable' | 'neutre' {
    const valeur = this.valeur() ?? 0;
    if (valeur === 0 || this.sensFavorable() === 'NEUTRE') {
      return 'neutre';
    }
    return (valeur > 0) === (this.sensFavorable() === 'HAUSSE') ? 'favorable' : 'defavorable';
  }
}
