import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CardComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { ReglageAnalyse, RequetePilotage } from '../../models/pilotage.model';
import { CarteChaleurComponent } from '../../ui/carte-chaleur/carte-chaleur.component';
import { CourbeIndicateurComponent } from '../../ui/courbe-indicateur/courbe-indicateur.component';
import { REGLAGE_PAR_DEFAUT } from '../onglet-analyser/reglage-analyse';

const HEURES_PAR_JOUR: ReglageAnalyse = { ...REGLAGE_PAR_DEFAUT, indicateurs: ['NB_VENTES'], axe: 'HEURE', axe2: 'JOUR_SEMAINE', top: 0, affichage: 'CROISE' };
const CRENEAUX = 3;

/** Fréquentation : ventes par heure et jour de la semaine (l'analyse heure × jour), les créneaux les plus chargés, les ventes par jour. */
@Component({
  selector: 'app-section-frequentation',
  imports: [CardComponent, CarteChaleurComponent, CourbeIndicateurComponent],
  templateUrl: './section-frequentation.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionFrequentationComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly chaleur = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.analyser(params, HEURES_PAR_JOUR) });
  protected readonly parJour = rxResource({
    params: () => ({ ...this.requete(), granularite: 'JOUR' as const }),
    stream: ({ params }) => this.api.lireSeries(params, ['NB_VENTES']),
  });

  protected readonly croise = computed(() => this.chaleur.value()?.croise ?? null);
  protected readonly serie = computed(() => this.parJour.value()?.series[0]);

  /** « Créneaux les plus chargés : mardi 10 h (42 ventes), … » ; vide sans vente. */
  protected readonly lectureCreneaux = computed(() => {
    const croise = this.croise();
    if (!croise) {
      return '';
    }
    const cases = croise.lignes.flatMap(ligne =>
      ligne.cellules.map((cellule, rang) => ({ libelle: `${croise.colonnes[rang].libelle.toLowerCase()} ${ligne.libelle}`, valeur: cellule.valeur ?? 0 })),
    );
    const plusCharges = cases
      .filter(creneau => creneau.valeur > 0)
      .sort((a, b) => b.valeur - a.valeur)
      .slice(0, CRENEAUX)
      .map(creneau => `${creneau.libelle} (${creneau.valeur} ventes)`);
    return plusCharges.length ? `Créneaux les plus chargés : ${plusCharges.join(', ')}.` : '';
  });
}
