import { ChangeDetectionStrategy, Component, computed, input, linkedSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { formatDateFR } from 'app/shared/utils/format-utils';
import { CardComponent, DataTableComponent, KpiItemComponent, KpiStripComponent, PillSelectorComponent } from 'app/shared/ui';
import { SuiviIndicateur, SuiviMois, SuiviObjectifs } from '../../models/pilotage.model';
import { MOIS_COURTS } from '../../ui/graphique-annees/graphique-annees.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { AideComponent } from '../../ui/aide/aide.component';

/** Mois en cours (projection face à l'objectif) puis, pour un indicateur, objectif / réalisé / écart / atteinte mois par mois. */
@Component({
  selector: 'app-suivi-objectifs',
  imports: [AideComponent, FormsModule, CardComponent, DataTableComponent, KpiStripComponent, KpiItemComponent, PillSelectorComponent, ValeurIndicateurPipe],
  templateUrl: './suivi-objectifs.component.html',
  styleUrls: ['../../ui/section-onglet.scss', './objectifs.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SuiviObjectifsComponent {
  readonly suivi = input.required<SuiviObjectifs | null>();
  readonly chargement = input(false);
  readonly erreur = input(false);

  protected readonly moisCourts = MOIS_COURTS;
  protected readonly choixIndicateurs = computed(() =>
    (this.suivi()?.indicateurs ?? []).map(suivi => ({ label: suivi.indicateur.libelle, value: suivi.indicateur.code })),
  );
  protected readonly code = linkedSignal(() => this.suivi()?.indicateurs[0]?.indicateur.code ?? null);
  protected readonly choisi = computed<SuiviIndicateur | null>(() => this.suivi()?.indicateurs.find(suivi => suivi.indicateur.code === this.code()) ?? null);
  protected readonly projections = computed(() => (this.suivi()?.indicateurs ?? []).filter(suivi => suivi.projection));

  /** « Au 10/10/2026, Chiffre d'affaires TTC projeté à 3,3 M pour un objectif de 5 M (67 %). » */
  protected readonly lecture = computed(() => {
    const suivi = this.suivi();
    const premier = suivi?.indicateurs[0];
    const projection = premier?.projection;
    if (!suivi || !premier || !projection || projection.objectif == null || projection.atteinteProjetee == null) {
      return '';
    }
    return `Au ${formatDateFR(suivi.jusquAu)}, ${premier.indicateur.libelle.charAt(0).toLowerCase()}${premier.indicateur.libelle.slice(1)} projeté à ${Math.round(projection.atteinteProjetee)} % de l'objectif du mois.`;
  });

  protected libellerEtat(mois: SuiviMois): string {
    if (mois.enCours) {
      return 'En cours';
    }
    return mois.tenu == null ? '' : mois.tenu ? 'Tenu' : 'Manqué';
  }

  protected classerEtat(tenu: boolean | null): string {
    return tenu == null ? 'objectif-etat' : tenu ? 'objectif-etat objectif-etat--tenu' : 'objectif-etat objectif-etat--manque';
  }
}
