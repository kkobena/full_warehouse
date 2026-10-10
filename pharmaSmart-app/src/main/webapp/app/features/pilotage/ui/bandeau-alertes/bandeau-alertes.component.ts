import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AlertePilotage } from '../../models/pilotage.model';

const ALERTES_MONTREES = 5;
const CLE_OUVERT = 'pilotage.alertes.ouvert';

/**
 * Alertes actives, les plus graves d'abord : au plus cinq, repliées par défaut (le dernier choix est retenu par le navigateur) ; chacune mène à l'onglet où creuser. La gravité est
 * écrite (« Grave », « À surveiller ») ; la couleur du thème ne fait que la souligner.
 */
@Component({
  selector: 'app-bandeau-alertes',
  imports: [RouterLink],
  template: `
    @if (alertes().length) {
      <section aria-labelledby="pilotage-alertes" class="bandeau-alertes">
        <div class="bandeau-alertes-entete">
          <h3 class="bandeau-alertes-titre" id="pilotage-alertes">
            <i aria-hidden="true" class="pi pi-exclamation-triangle"></i>
            {{ alertes().length === 1 ? '1 alerte' : alertes().length + ' alertes' }}
          </h3>
          <button (click)="basculer()" [attr.aria-expanded]="ouvert()" aria-controls="pilotage-alertes-liste" class="btn btn-link btn-sm" type="button">
            {{ ouvert() ? 'Replier' : 'Afficher' }}
          </button>
        </div>
        @if (ouvert()) {
          <ul class="bandeau-alertes-liste" id="pilotage-alertes-liste">
            @for (alerte of montrees(); track alerte.code) {
              <li [class]="'bandeau-alerte bandeau-alerte--' + alerte.gravite.toLowerCase()">
                <a [queryParams]="{ onglet: alerte.onglet }" queryParamsHandling="merge" routerLink=".">
                  <span class="bandeau-alerte-gravite">{{ alerte.gravite === 'HAUTE' ? 'Grave' : 'À surveiller' }}</span>
                  <span class="visually-hidden"> : </span>
                  <strong>{{ alerte.titre }}</strong>
                  <span class="visually-hidden">. </span>
                  <span class="bandeau-alerte-detail">{{ alerte.detail }}</span>
                </a>
              </li>
            }
          </ul>
          @if (alertes().length > montrees().length) {
            <p class="bandeau-alertes-note">Et {{ alertes().length - montrees().length }} autre(s), moins graves.</p>
          }
        }
      </section>
    }
  `,
  styleUrl: './bandeau-alertes.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BandeauAlertesComponent {
  readonly alertes = input.required<readonly AlertePilotage[]>();

  protected readonly ouvert = signal(lireOuvert());
  protected readonly montrees = computed(() => this.alertes().slice(0, ALERTES_MONTREES));

  protected basculer(): void {
    this.ouvert.update(ouvert => !ouvert);
    ecrireOuvert(this.ouvert());
  }
}

function lireOuvert(): boolean {
  try {
    return localStorage.getItem(CLE_OUVERT) === 'true';
  } catch {
    return false;
  }
}

function ecrireOuvert(ouvert: boolean): void {
  try {
    localStorage.setItem(CLE_OUVERT, String(ouvert));
  } catch {
    // Stockage indisponible (navigation privée) : le choix vaut pour la session.
  }
}
