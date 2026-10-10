import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { RequetePilotage } from '../../models/pilotage.model';
import { AncresSectionsComponent } from '../../ui/ancres-sections/ancres-sections.component';
import { SectionDifferesComponent } from './section-differes.component';
import { SectionEncaissementsComponent } from './section-encaissements.component';
import { SectionTiersPayantComponent } from './section-tiers-payant.component';

/** Onglet « Trésorerie & tiers payant » : mon argent rentre-t-il ? */
@Component({
  selector: 'app-onglet-tresorerie',
  imports: [AncresSectionsComponent, SectionEncaissementsComponent, SectionTiersPayantComponent, SectionDifferesComponent],
  template: `
    <div class="onglet-sections">
      <h2 class="visually-hidden">Trésorerie et tiers payant</h2>
      <app-ancres-sections [ancres]="ancres" />
      <app-section-encaissements [requete]="requete()" />
      <app-section-tiers-payant [requete]="requete()" />
      <app-section-differes [requete]="requete()" />
    </div>
  `,
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletTresorerieComponent {
  readonly requete = input.required<RequetePilotage>();

  protected readonly ancres = [
    { id: 'tresorerie-encaissements', libelle: 'Encaissements & caisse' },
    { id: 'tresorerie-tiers-payant', libelle: 'Tiers payant' },
    { id: 'tresorerie-differes', libelle: 'Différés & crédit' },
  ];
}
