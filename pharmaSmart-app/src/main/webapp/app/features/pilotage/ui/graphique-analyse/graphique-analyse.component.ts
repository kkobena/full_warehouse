import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ChartComponent } from 'app/shared/chart/chart.component';
import { ChartThemeColors, ChartThemeColorsService } from 'app/shared/chart/chart-theme-colors.service';
import { formatDateFR } from 'app/shared/utils/format-utils';
import { AnalysePilotage } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../valeur-indicateur/valeur-indicateur.pipe';

/** Forme des points, pour distinguer les courbes autrement que par la couleur (WCAG 1.4.1). */
const FORMES = ['circle', 'rect', 'triangle', 'rectRot', 'star', 'crossRot', 'rectRounded', 'line'];
const HAUTEUR_PAR_BARRE = 28;
const HAUTEUR_MIN = 240;

/**
 * Barres classées (référence en fantôme : la teinte de l'accent, pâle, derrière la barre pleine de la période) ou courbes des éléments dans le temps,
 * sur le premier indicateur. Couleurs du thème, relues à chaque changement de thème.
 */
@Component({
  selector: 'app-graphique-analyse',
  imports: [ChartComponent],
  template: `
    @if (configuration(); as config) {
      <app-chart [ariaLabel]="description()" [data]="config.data" [height]="config.hauteur" [options]="config.options" [type]="config.type" />
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GraphiqueAnalyseComponent {
  readonly analyse = input.required<AnalysePilotage>();
  readonly affichage = input.required<'BARRES' | 'COURBE'>();

  private readonly couleurs = inject(ChartThemeColorsService).colors;
  private readonly valeurIndicateur = new ValeurIndicateurPipe();

  protected readonly description = computed(() => {
    const analyse = this.analyse();
    const forme = this.affichage() === 'BARRES' ? 'Barres' : 'Courbes';
    return `${forme} : ${analyse.indicateurs[0]?.libelle} par ${analyse.axe.libelle.toLowerCase()}`;
  });

  protected readonly configuration = computed(() => (this.affichage() === 'BARRES' ? this.configurerBarres() : this.configurerCourbes()));

  private configurerBarres() {
    const analyse = this.analyse();
    const couleurs = this.couleurs();
    const unite = analyse.indicateurs[0].unite;
    return {
      type: 'bar' as const,
      hauteur: String(Math.max(HAUTEUR_MIN, analyse.elements.length * HAUTEUR_PAR_BARRE)),
      data: {
        labels: analyse.elements.map(element => element.libelle),
        datasets: [
          {
            label: `Période : ${formaterPeriode(analyse.comparaison.periode)}`,
            data: analyse.elements.map(element => element.cellules[0].valeur),
            backgroundColor: couleurs.accent,
            barPercentage: 0.55,
            grouped: false,
            order: 1,
          },
          {
            label: analyse.comparaison.reference ? `Comparée à : ${formaterPeriode(analyse.comparaison.reference)}` : 'Sans référence',
            data: analyse.elements.map(element => element.cellules[0].valeurReference),
            backgroundColor: attenuer(couleurs.accent, 0.22),
            borderColor: attenuer(couleurs.accent, 0.55),
            borderWidth: 1,
            barPercentage: 0.9,
            grouped: false,
            order: 2,
          },
        ],
      },
      options: this.options(couleurs, unite, 'y'),
    };
  }

  private configurerCourbes() {
    const analyse = this.analyse();
    const couleurs = this.couleurs();
    const croise = analyse.croise;
    return {
      type: 'line' as const,
      hauteur: '320',
      data: {
        labels: croise?.colonnes.map(colonne => colonne.libelle) ?? [],
        datasets: (croise?.lignes ?? []).map((ligne, rang) => ({
          label: ligne.libelle,
          data: ligne.cellules.map(cellule => cellule.valeur),
          borderColor: couleurs.series[rang % couleurs.series.length],
          backgroundColor: couleurs.series[rang % couleurs.series.length],
          pointStyle: FORMES[rang % FORMES.length],
          pointRadius: 4,
          tension: 0.3,
        })),
      },
      options: this.options(couleurs, analyse.indicateurs[0].unite, 'x'),
    };
  }

  private options(couleurs: ChartThemeColors, unite: AnalysePilotage['indicateurs'][number]['unite'], axeIndex: 'x' | 'y') {
    const axeValeurs = axeIndex === 'y' ? 'x' : 'y';
    const axeLibelles = axeIndex === 'y' ? 'y' : 'x';
    return {
      indexAxis: axeIndex,
      maintainAspectRatio: false,
      plugins: { legend: { labels: { color: couleurs.text, usePointStyle: true } } },
      scales: {
        [axeLibelles]: { ticks: { color: couleurs.textMuted }, grid: { display: false } },
        [axeValeurs]: {
          ticks: { color: couleurs.textMuted, callback: (valeur: number) => this.valeurIndicateur.transform(valeur, unite) },
          grid: { color: couleurs.border },
        },
      },
    };
  }
}

/** L'accent du thème rendu translucide (« #5b89a6 » ou « rgb(…) ») ; une autre écriture reste telle quelle. */
function attenuer(couleur: string, opacite: number): string {
  const hex = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i.exec(couleur)?.[1];
  if (hex) {
    const complet = hex.length === 3 ? [...hex].map(c => c + c).join('') : hex;
    const [r, g, b] = [0, 2, 4].map(i => parseInt(complet.slice(i, i + 2), 16));
    return `rgba(${r}, ${g}, ${b}, ${opacite})`;
  }
  const rgb = /^rgba?\(\s*(\d+)[,\s]+(\d+)[,\s]+(\d+)/i.exec(couleur);
  return rgb ? `rgba(${rgb[1]}, ${rgb[2]}, ${rgb[3]}, ${opacite})` : couleur;
}

/** « du 01/10/2026 au 10/10/2026 » : la légende dit ce que chaque barre mesure. */
function formaterPeriode(periode: { du: string; au: string }): string {
  return `du ${formatDateFR(periode.du)} au ${formatDateFR(periode.au)}`;
}
