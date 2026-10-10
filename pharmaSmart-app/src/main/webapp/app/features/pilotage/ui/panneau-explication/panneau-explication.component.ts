import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core';

import { ButtonComponent, OffcanvasComponent } from 'app/shared/ui';
import { formatDecimal } from 'app/shared/utils/format-utils';
import { AxeAnalyse, ExplicationEcart } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../valeur-indicateur/valeur-indicateur.pipe';

/** « Expliquer l'écart » : les axes du plus au moins explicatif ; on peut ventiler l'élément par l'axe proposé. */
@Component({
  selector: 'app-panneau-explication',
  imports: [OffcanvasComponent, ButtonComponent, ValeurIndicateurPipe],
  templateUrl: './panneau-explication.component.html',
  styleUrl: './panneau-explication.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PanneauExplicationComponent {
  readonly visible = model.required<boolean>();
  readonly libelleElement = input.required<string>();
  readonly explication = input<ExplicationEcart | undefined>();
  readonly loading = input(false);
  readonly erreur = input(false);

  readonly ventilerPar = output<AxeAnalyse>();

  private readonly valeurIndicateur = new ValeurIndicateurPipe();

  protected readonly sens = computed(() => ((this.explication()?.ecart ?? 0) < 0 ? 'baisse' : 'hausse'));

  protected formaterPart(part: number): string {
    return `${formatDecimal(part, 0)} %`;
  }

  protected formaterEcart(valeur: number | null): string {
    return valeur === null ? '—' : `${valeur > 0 ? '+' : ''}${this.valeurIndicateur.transform(valeur, this.explication()!.indicateur.unite, true)}`;
  }
}
