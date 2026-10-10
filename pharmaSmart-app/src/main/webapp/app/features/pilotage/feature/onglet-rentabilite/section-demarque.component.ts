import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { CardComponent, DataTableComponent, KpiItemComponent, KpiStripComponent } from 'app/shared/ui';
import { PilotageApiService } from '../../data-access/pilotage-api.service';
import { RequetePilotage } from '../../models/pilotage.model';
import { ValeurIndicateurPipe } from '../../ui/valeur-indicateur/valeur-indicateur.pipe';
import { VariationComponent } from '../../ui/variation/variation.component';

/** Démarque : valeur perdue (ajustements de sortie), part du CA, par motif face à la référence, produits les plus touchés. */
@Component({
  selector: 'app-section-demarque',
  imports: [KpiStripComponent, KpiItemComponent, CardComponent, DataTableComponent, VariationComponent, ValeurIndicateurPipe],
  templateUrl: './section-demarque.component.html',
  styleUrl: '../../ui/section-onglet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionDemarqueComponent {
  readonly requete = input.required<RequetePilotage>();

  private readonly api = inject(PilotageApiService);

  protected readonly demarque = rxResource({
    params: () => this.requete(),
    stream: ({ params }) => this.api.lireDemarque(params),
  });
}
