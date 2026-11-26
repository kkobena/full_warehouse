import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { IMagasin } from 'app/shared/model/magasin.model';
import { BadgeComponent, CardComponent, DetailFieldComponent } from 'app/shared/ui';
import { porteIdentiteFne } from '../../models/magasin.model';

/** Fiche d'un magasin en lecture : quatre cartes, dans l'ordre où les documents imprimés les reprennent. */
@Component({
  selector: 'app-magasin-detail',
  templateUrl: './magasin-detail.component.html',
  styleUrl: './magasin-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BadgeComponent, CardComponent, DetailFieldComponent],
})
export class MagasinDetailComponent {
  readonly magasin = input.required<IMagasin>();

  protected readonly avecIdentiteFne = computed(() => porteIdentiteFne(this.magasin()));
}
