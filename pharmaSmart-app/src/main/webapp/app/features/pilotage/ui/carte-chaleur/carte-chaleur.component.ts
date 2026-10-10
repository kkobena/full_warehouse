import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CroiseAnalyse, UniteIndicateur } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../valeur-indicateur/valeur-indicateur.pipe';

/** Intensité maximale du fond : au-delà, le texte du thème perdrait du contraste sur l'accent. */
const INTENSITE_MAX = 45;

/**
 * Carte de chaleur d'un croisé (heures × jours de la semaine…) : chaque case porte sa valeur en chiffres — la couleur n'est jamais
 * seule à porter l'information (WCAG 1.4.1) — sur un fond d'accent du thème d'autant plus soutenu que la valeur est forte.
 */
@Component({
  selector: 'app-carte-chaleur',
  imports: [ValeurIndicateurPipe],
  template: `
    <div class="carte-chaleur-defilement">
      <table class="carte-chaleur">
        <caption class="visually-hidden">{{ legende() }}</caption>
        <thead>
          <tr>
            <th scope="col">{{ entete() }}</th>
            @for (colonne of croise().colonnes; track colonne.cle) {
              <th scope="col">{{ colonne.libelle }}</th>
            }
          </tr>
        </thead>
        <tbody>
          @for (ligne of croise().lignes; track ligne.cle) {
            <tr>
              <th scope="row">{{ ligne.libelle }}</th>
              @for (cellule of ligne.cellules; track $index) {
                <td [style.--intensite]="intensite(cellule.valeur) + '%'">{{ cellule.valeur ? (cellule.valeur | valeurIndicateur: unite()) : '' }}</td>
              }
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: `
    .carte-chaleur-defilement {
      overflow-x: auto;
    }

    .carte-chaleur {
      width: 100%;
      border-collapse: separate;
      border-spacing: 2px;
      font-size: 0.8rem;

      th {
        padding: 0.2rem 0.4rem;
        font-weight: 600;
        text-align: center;
        white-space: nowrap;
      }

      td {
        min-width: 2.5rem;
        padding: 0.3rem;
        text-align: center;
        border-radius: 0.25rem;
        background: color-mix(in srgb, var(--p-primary-color) var(--intensite, 0%), var(--bs-body-bg));
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CarteChaleurComponent {
  readonly croise = input.required<CroiseAnalyse>();
  readonly entete = input.required<string>();
  readonly legende = input.required<string>();
  readonly unite = input<UniteIndicateur>('NOMBRE');

  private readonly maximum = computed(() => {
    let maximum = 0;
    for (const ligne of this.croise().lignes) {
      for (const cellule of ligne.cellules) {
        maximum = Math.max(maximum, cellule.valeur ?? 0);
      }
    }
    return maximum;
  });

  protected intensite(valeur: number | null): number {
    const maximum = this.maximum();
    return maximum === 0 || !valeur ? 0 : Math.round((valeur / maximum) * INTENSITE_MAX);
  }
}
