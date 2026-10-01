import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';

import { formatCurrencyWithUnit, formatDateFR, formatDecimal, formatNumber } from 'app/shared/utils/format-utils';

export type AppDetailFieldFormat = 'texte' | 'nombre' | 'montant' | 'date';

/**
 * Teinte de la valeur, pour signaler un seuil franchi.
 *
 * ⚠ Passer la valeur en liaison — `[tone]="'danger'"` — et non en attribut statique : IntelliJ ne
 * résout pas l'attribut statique vers un type nommé. Même précaution que `variant` d'`app-card`.
 */
export type AppDetailFieldTone = 'success' | 'warning' | 'danger' | 'none';

/**
 * Une ligne « libellé … valeur » d'une {@link DetailSectionComponent}.
 *
 * <p>La valeur absente s'affiche `—` : le repli n'est plus à écrire en `@if` à chaque champ.
 * Un contenu projeté (interrupteur, badge) remplace la valeur formatée.
 *
 * @example
 * <app-detail-field label="CMM" hint="Consommation mensuelle moyenne" [value]="cmm" format="nombre" [decimals]="1" unit="u/m" />
 * <app-detail-field label="Thermosensible"><app-switch … /></app-detail-field>
 */
@Component({
  selector: 'app-detail-field',
  imports: [NgbTooltip],
  template: `
    <span class="detail-field-label">
      {{ label() }}
      @if (hint()) {
        <i class="pi pi-info-circle detail-field-hint" [ngbTooltip]="hint()" container="body" aria-hidden="true"></i>
      }
    </span>
    <span [class]="valueClasses()" [attr.title]="titre()">
      <ng-content>{{ texte() }}</ng-content>
    </span>
  `,
  styleUrl: './detail-field.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailFieldComponent {
  readonly label = input.required<string>();

  /** Explication affichée au survol de l'icône ⓘ. */
  readonly hint = input<string>('');

  readonly value = input<unknown>(null);

  readonly format = input<AppDetailFieldFormat>('texte');

  /** Décimales du format `nombre`. */
  readonly decimals = input<number>(0);

  /** Unité accolée à la valeur, ex. `u`, `j`, `%`. Jamais affichée derrière un `—`. */
  readonly unit = input<string>('');

  readonly tone = input<AppDetailFieldTone>('none');

  protected readonly texte = computed(() => {
    const v = this.value();
    if (v == null || v === '' || (typeof v === 'number' && isNaN(v))) {
      return '—';
    }
    const brut = this.formater(v);
    return this.unit() ? `${brut} ${this.unit()}` : brut;
  });

  /** Libellés longs tronqués : le survol rend la valeur entière. */
  protected readonly titre = computed(() => (this.format() === 'texte' && this.texte() !== '—' ? this.texte() : null));

  protected readonly valueClasses = computed(() =>
    ['detail-field-value', this.tone() !== 'none' ? `detail-field-value--${this.tone()}` : ''].filter(Boolean).join(' '),
  );

  private formater(v: unknown): string {
    switch (this.format()) {
      case 'nombre':
        return this.decimals() > 0 ? formatDecimal(Number(v), this.decimals()) : formatNumber(Number(v));
      case 'montant':
        return formatCurrencyWithUnit(Number(v));
      case 'date':
        return formatDateFR(v as string | Date);
      default:
        return String(v);
    }
  }
}
