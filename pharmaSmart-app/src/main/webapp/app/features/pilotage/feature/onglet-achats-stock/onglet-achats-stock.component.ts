import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { RequetePilotage } from '../../models/pilotage.model';
import { AncresSectionsComponent } from '../../ui/ancres-sections/ancres-sections.component';
import { SectionAchatsVentesComponent } from './section-achats-ventes.component';
import { SectionAchatsComponent } from './section-achats.component';
import { SectionRupturesComponent } from './section-ruptures.component';
import { SectionStockComponent } from './section-stock.component';

/** Onglet « Achats & stock » : mes achats et mon stock sont-ils maîtrisés ? Quatre sous-sections, chacune avec ses données. */
@Component({
  selector: 'app-onglet-achats-stock',
  imports: [AncresSectionsComponent, SectionAchatsComponent, SectionAchatsVentesComponent, SectionStockComponent, SectionRupturesComponent],
  template: `
    <div class="onglet-sections">
      <h2 class="visually-hidden">Achats et stock</h2>
      <app-ancres-sections [ancres]="ancres" />
      <app-section-achats [requete]="requete()" />
      <app-section-achats-ventes [requete]="requete()" />
      <app-section-stock [requete]="requete()" />
      <app-section-ruptures [requete]="requete()" />
    </div>
  `,
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletAchatsStockComponent {
  readonly requete = input.required<RequetePilotage>();

  protected readonly ancres = [
    { id: 'achats-stock-achats', libelle: 'Achats' },
    { id: 'achats-stock-achats-ventes', libelle: 'Achats / ventes' },
    { id: 'achats-stock-stock', libelle: 'Stock' },
    { id: 'achats-stock-ruptures', libelle: 'Ruptures & péremptions' },
  ];
}
