import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CardComponent, DataTableComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { RequetePilotage } from '../../models/pilotage.model';
import { BarresComponent, SerieBarres } from '../../ui/barres/barres.component';
import { TuileCellule, TuilesCellulesComponent, celluleSeule } from '../../ui/tuiles-cellules/tuiles-cellules.component';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { VariationComponent } from '../../ui/variation/variation.component';
import { CourbeIndicateurComponent } from '../../ui/courbe-indicateur/courbe-indicateur.component';
import { OrganismeTresorerie } from '../../models/pilotage.model';
import { AideComponent } from '../../ui/aide/aide.component';
import { elargirAuxTreizeTranches, libellerHorizon } from '../../data-access/horizon-courbe';

const ORIGINES: Record<NonNullable<OrganismeTresorerie['origineDelai']>, string> = {
  OBSERVE: 'délai observé sur ses règlements',
  GROUPE: 'délai contractuel de son groupe',
  DEFAUT: 'délai par défaut de l’officine',
};

/** Tiers payant : facturé et réglé sur la période ; encours, DSO, vieillissement, échéancier et concentration à date. */
@Component({
  selector: 'app-section-tiers-payant',
  imports: [AideComponent, CardComponent, DataTableComponent, TuilesCellulesComponent, BarresComponent, CourbeIndicateurComponent, ValeurIndicateurPipe],
  templateUrl: './section-tiers-payant.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionTiersPayantComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly tiersPayant = rxResource({ params: () => this.requete(), stream: ({ params }) => this.api.lireTiersPayant(params) });
  protected readonly part = rxResource({
    params: () => elargirAuxTreizeTranches(this.requete()),
    stream: ({ params }) => this.api.lireSeries(params, ['PART_TIERS_PAYANT']),
  });

  protected readonly tuiles = computed<TuileCellule[]>(() => {
    const tp = this.tiersPayant.value();
    if (!tp) {
      return [];
    }
    return [
      { libelle: 'Facturé', definition: 'Factures émises sur la période.', unite: 'MONTANT', sens: 'NEUTRE', cellule: tp.facture },
      { libelle: 'Réglé', definition: 'Réglé sur les factures de la période.', unite: 'MONTANT', sens: 'HAUSSE', cellule: tp.regle },
      { libelle: 'Encours', definition: 'Reste dû des factures non soldées, à date.', unite: 'MONTANT', sens: 'BAISSE', cellule: celluleSeule(tp.encours) },
      { libelle: 'DSO', definition: 'Âge moyen de l’encours, pondéré par les montants.', unite: 'JOURS', sens: 'BAISSE', cellule: celluleSeule(tp.dso) },
      { libelle: 'Concentration (3 premiers)', definition: 'Part de l’encours portée par les 3 premiers organismes.', unite: 'POURCENTAGE', sens: 'BAISSE', cellule: celluleSeule(tp.concentrationTrois) },
    ];
  });

  protected readonly serie = computed(() => this.part.value()?.series[0]);
  protected readonly horizon = computed(() => libellerHorizon(this.requete().granularite));

  protected readonly vieillissement = computed<SerieBarres[]>(() => [{ libelle: 'Encours', valeurs: (this.tiersPayant.value()?.vieillissement ?? []).map(t => t.montant) }]);
  protected readonly libellesVieillissement = computed(() => (this.tiersPayant.value()?.vieillissement ?? []).map(t => t.libelle));
  protected readonly echeancier = computed<SerieBarres[]>(() => [{ libelle: 'Attendu', valeurs: (this.tiersPayant.value()?.encaissementsAttendus ?? []).map(t => t.montant) }]);
  protected readonly libellesEcheancier = computed(() => (this.tiersPayant.value()?.encaissementsAttendus ?? []).map(t => t.libelle));

  protected expliquerDelai(organisme: OrganismeTresorerie): string {
    return organisme.origineDelai ? `${organisme.delaiRetenu} j : ${ORIGINES[organisme.origineDelai]}` : '';
  }
}
