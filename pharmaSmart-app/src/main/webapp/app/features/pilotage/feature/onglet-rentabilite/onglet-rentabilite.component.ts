import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { RequetePilotage } from '../../models/pilotage.model';
import { AncresSectionsComponent } from '../../ui/ancres-sections/ancres-sections.component';
import { SectionDemarqueComponent } from './section-demarque.component';
import { SectionMargeComponent } from './section-marge.component';
import { SectionRemisesComponent } from './section-remises.component';

/** Onglet « Rentabilité & remises » : où se gagne et se perd l'argent. Trois sous-sections, chacune avec ses propres données. */
@Component({
  selector: 'app-onglet-rentabilite',
  imports: [AncresSectionsComponent, SectionMargeComponent, SectionRemisesComponent, SectionDemarqueComponent],
  template: `
    <div class="onglet-sections">
      <h2 class="visually-hidden">Rentabilité et remises</h2>
      <app-ancres-sections [ancres]="ancres" />
      <app-section-marge [requete]="requete()" />
      <app-section-remises [requete]="requete()" />
      <app-section-demarque [requete]="requete()" />
    </div>
  `,
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletRentabiliteComponent {
  readonly requete = input.required<RequetePilotage>();

  protected readonly ancres = [
    { id: 'rentabilite-marge', libelle: 'Marge' },
    { id: 'rentabilite-remises', libelle: 'Remises' },
    { id: 'rentabilite-demarque', libelle: 'Démarque' },
  ];
}
