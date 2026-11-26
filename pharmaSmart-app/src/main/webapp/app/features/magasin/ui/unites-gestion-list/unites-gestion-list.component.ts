import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';

import { IMagasin } from 'app/shared/model/magasin.model';
import { BadgeComponent, ButtonComponent, CardComponent, DataTableComponent } from 'app/shared/ui';

/**
 * Unités de gestion rattachées à l'officine : une parapharmacie, par exemple, qui tient son stock,
 * sa caisse et ses commandes au même endroit. Composant de présentation : la page charge et navigue.
 */
@Component({
  selector: 'app-unites-gestion-list',
  templateUrl: './unites-gestion-list.component.html',
  styleUrl: './unites-gestion-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgbTooltip, BadgeComponent, ButtonComponent, CardComponent, DataTableComponent],
})
export class UnitesGestionListComponent {
  readonly unites = input<IMagasin[]>([]);
  readonly loading = input(false);

  readonly creer = output<void>();
  readonly modifier = output<IMagasin>();
}
