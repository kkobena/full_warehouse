import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { CardComponent } from 'app/shared/ui';
import { OngletPilotage } from '../../pilotage-onglets';

/** Contenu provisoire d'un onglet pas encore réalisé : la question à laquelle il répondra et la phase prévue. */
@Component({
  selector: 'app-onglet-a-venir',
  imports: [CardComponent],
  template: `
    <app-card [header]="onglet().question" [icon]="onglet().icone">
      <p class="mb-0">Contenu prévu en phase {{ onglet().phase }} du plan de pilotage.</p>
    </app-card>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OngletAVenirComponent {
  readonly onglet = input.required<OngletPilotage>();
}
