import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { RequetePilotage } from '../../models/pilotage.model';
import { AncresSectionsComponent } from '../../ui/ancres-sections/ancres-sections.component';
import { SectionClientsComponent } from './section-clients.component';
import { SectionEquipeComponent } from './section-equipe.component';
import { SectionFrequentationComponent } from './section-frequentation.component';

/** Onglet « Clients & équipe » : qui vient, qui vend ? */
@Component({
  selector: 'app-onglet-clients-equipe',
  imports: [AncresSectionsComponent, SectionFrequentationComponent, SectionClientsComponent, SectionEquipeComponent],
  template: `
    <div class="onglet-sections">
      <h2 class="visually-hidden">Clients et équipe</h2>
      <app-ancres-sections [ancres]="ancres" />
      <app-section-frequentation [requete]="requete()" />
      <app-section-clients [requete]="requete()" />
      <app-section-equipe [requete]="requete()" />
    </div>
  `,
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletClientsEquipeComponent {
  readonly requete = input.required<RequetePilotage>();

  protected readonly ancres = [
    { id: 'clients-equipe-frequentation', libelle: 'Fréquentation' },
    { id: 'clients-equipe-clients', libelle: 'Clients' },
    { id: 'clients-equipe-equipe', libelle: 'Équipe' },
  ];
}
