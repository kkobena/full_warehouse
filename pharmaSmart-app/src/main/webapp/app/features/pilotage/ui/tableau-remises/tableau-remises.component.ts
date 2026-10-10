import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { BadgeComponent, DataTableComponent } from 'app/shared/ui';
import { CelluleAnalyse, LigneRemise, SensFavorable, UniteIndicateur } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../valeur-indicateur/valeur-indicateur.pipe';
import { VariationComponent } from '../variation/variation.component';
import { AideComponent } from '../aide/aide.component';

interface Colonne {
  libelle: string;
  aide?: string;
  unite: UniteIndicateur;
  sens: SensFavorable;
  lire: (ligne: LigneRemise) => CelluleAnalyse | null;
}

const COLONNES: readonly Colonne[] = [
  { libelle: 'Ventes', unite: 'NOMBRE', sens: 'HAUSSE', lire: ligne => ligne.nbVentes },
  { libelle: 'CA TTC', unite: 'MONTANT', sens: 'HAUSSE', lire: ligne => ligne.caTtc },
  { libelle: 'Remises', unite: 'MONTANT', sens: 'BAISSE', lire: ligne => ligne.remises },
  { libelle: 'Taux de remise', aide: 'Remises / CA TTC.', unite: 'POURCENTAGE', sens: 'BAISSE', lire: ligne => ligne.tauxRemise },
];
const COLONNE_MARGE: Colonne = { libelle: 'Taux de marge', aide: 'Marge / CA HT des lignes remisées.', unite: 'POURCENTAGE', sens: 'HAUSSE', lire: ligne => ligne.tauxMarge };

/** Remises par mode d'octroi, par tranche ou par vendeur : chaque valeur et sa variation ; marge et alerte quand elles existent. */
@Component({
  selector: 'app-tableau-remises',
  imports: [AideComponent, DataTableComponent, BadgeComponent, VariationComponent, ValeurIndicateurPipe],
  template: `
    <app-data-table [scrollable]="true" scrollHeight="22rem" [stripedRows]="true" [value]="lignes()" emptyMessage="Aucune remise sur la période" size="small">
      <ng-template #header>
        <tr class="pharma-table-head">
          <th scope="col">{{ entete() }}</th>
          @for (colonne of colonnes(); track colonne.libelle) {
            <th class="text-end" scope="col">{{ colonne.libelle }}@if (colonne.aide) {<app-aide [texte]="colonne.aide" />}</th>
          }
          @if (avecAlerte()) {
            <th scope="col"><span class="visually-hidden">Alerte</span></th>
          }
        </tr>
      </ng-template>
      <ng-template #body let-ligne>
        <tr>
          <th scope="row">{{ ligne.libelle }}</th>
          @for (colonne of colonnes(); track colonne.libelle) {
            <td class="text-end">
              @if (colonne.lire(ligne); as cellule) {
                <div>{{ cellule.valeur | valeurIndicateur: colonne.unite : true }}</div>
                @if (cellule.valeurReference !== null) {
                  <app-variation [ecartPct]="cellule.ecartPct" [ecart]="cellule.ecart" [sensFavorable]="colonne.sens" [unite]="colonne.unite" />
                }
              }
            </td>
          }
          @if (avecAlerte()) {
            <td>
              @if (ligne.alerte) {
                <app-badge [label]="'Plus de ' + multipleAlerte() + ' fois le taux de l’équipe'" icon="pi pi-exclamation-triangle" severity="warn" />
              }
            </td>
          }
        </tr>
      </ng-template>
    </app-data-table>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TableauRemisesComponent {
  readonly lignes = input.required<readonly LigneRemise[]>();
  readonly entete = input.required<string>();
  readonly avecAlerte = input(false);
  readonly multipleAlerte = input(2);

  protected readonly colonnes = computed(() => (this.lignes().some(ligne => ligne.tauxMarge !== null) ? [...COLONNES, COLONNE_MARGE] : COLONNES));
}
