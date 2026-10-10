import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent, DataTableComponent } from 'app/shared/ui';
import { CroiseAnalyse, Indicateur, LigneCroisee } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../valeur-indicateur/valeur-indicateur.pipe';
import { VariationComponent } from '../variation/variation.component';

/**
 * Tableau croisé sur un indicateur (familles × mois, familles × années, heures × jours…) : la valeur et, s'il y a une référence,
 * sa variation. Avec `actionLibelle`, chaque ligne porte un bouton (filtrer sur cette famille…).
 */
@Component({
  selector: 'app-croise-analyse',
  imports: [DataTableComponent, ButtonComponent, VariationComponent, ValeurIndicateurPipe],
  template: `
    <app-data-table [scrollable]="true" scrollHeight="22rem" [showGridlines]="true" [value]="croise().lignes" emptyMessage="Aucune vente sur ce périmètre" size="small">
      <ng-template #header>
        <tr class="pharma-table-head">
          <th scope="col">{{ entete() }}</th>
          @for (colonne of croise().colonnes; track colonne.cle) {
            <th class="text-end" scope="col">{{ colonne.libelle }}</th>
          }
          @if (actionLibelle()) {
            <th scope="col"><span class="visually-hidden">Actions</span></th>
          }
        </tr>
      </ng-template>
      <ng-template #body let-ligne>
        <tr>
          <th scope="row">{{ ligne.libelle }}</th>
          @for (cellule of ligne.cellules; track $index) {
            <td class="text-end">
              <div>{{ cellule.valeur | valeurIndicateur: indicateur().unite : true }}</div>
              @if (avecReference() && cellule.valeurReference !== null) {
                <app-variation [ecartPct]="cellule.ecartPct" [ecart]="cellule.ecart" [sensFavorable]="indicateur().sensFavorable" [unite]="indicateur().unite" />
              }
            </td>
          }
          @if (actionLibelle()) {
            <td>
              <app-button (clicked)="ligneChoisie.emit(ligne)" [ariaLabel]="actionLibelle() + ' : ' + ligne.libelle" [iconOnly]="true" [text]="true"
                          [title]="actionLibelle()" icon="pi pi-filter" size="small" />
            </td>
          }
        </tr>
      </ng-template>
    </app-data-table>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CroiseAnalyseComponent {
  readonly croise = input.required<CroiseAnalyse>();
  readonly indicateur = input.required<Indicateur>();
  /** Coin du tableau : « Famille / Année »… */
  readonly entete = input.required<string>();
  readonly avecReference = input(true);
  readonly actionLibelle = input<string>('');

  readonly ligneChoisie = output<LigneCroisee>();
}
