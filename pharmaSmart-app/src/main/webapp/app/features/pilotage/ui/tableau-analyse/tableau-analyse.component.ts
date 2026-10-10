import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';

import { ButtonComponent, DataTableComponent } from 'app/shared/ui';
import { formatDecimal } from 'app/shared/utils/format-utils';
import { AnalysePilotage, ElementAnalyse } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../valeur-indicateur/valeur-indicateur.pipe';
import { VariationComponent } from '../variation/variation.component';
import { AideComponent } from '../aide/aide.component';

/**
 * Tableau à variations : valeur complète et variation de chaque indicateur, part du total et contribution à l'écart (premier
 * indicateur, s'il est additif) ; ligne « autres » puis total. Sur un élément : descendre, voir ses ventes (produit), expliquer.
 */
@Component({
  selector: 'app-tableau-analyse',
  imports: [NgbTooltip, AideComponent, DataTableComponent, ButtonComponent, VariationComponent, ValeurIndicateurPipe],
  templateUrl: './tableau-analyse.component.html',
  styles: `
    .tableau-analyse-actions {
      display: flex;
      justify-content: flex-end;
      gap: 0.25rem;
    }

    .tableau-analyse-valeur {
      font-variant-numeric: tabular-nums;
    }

    // Le pied global (tfoot td : 1rem) décalerait les totaux de leurs colonnes : même marge horizontale que les cellules.
    .tableau-analyse-total > th,
    .tableau-analyse-total > td {
      padding: 0.5rem 0.25rem;
      border-top: 2px solid var(--pharma-chrome-rule);
      font-weight: 600;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableauAnalyseComponent {
  readonly analyse = input.required<AnalysePilotage>();
  readonly loading = input(false);
  /** Le détail des ventes d'un produit passe par l'historique produit, soumis à ses propres droits. */
  readonly peutVoirVentes = input(false);

  readonly descendre = output<ElementAnalyse>();
  readonly voirVentes = output<ElementAnalyse>();
  readonly expliquer = output<ElementAnalyse>();

  protected readonly lignes = computed(() => {
    const analyse = this.analyse();
    return analyse.autres ? [...analyse.elements, analyse.autres] : analyse.elements;
  });

  protected readonly avecPart = computed(() => this.analyse().elements.some(element => element.part !== null));
  protected readonly avecContribution = computed(() => this.analyse().elements.some(element => element.contribution !== null));
  protected readonly avecReference = computed(() => this.analyse().comparaison.reference !== null);

  protected readonly axeSuivant = computed(() => this.analyse().axe.suivant);
  protected readonly estProduit = computed(() => this.analyse().axe.code === 'PRODUIT');
  protected readonly filtrable = computed(() => this.analyse().axe.filtrable);

  protected formaterPart(valeur: number | null): string {
    return valeur === null ? '—' : `${formatDecimal(valeur, 1)} %`;
  }
}
