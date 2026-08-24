import { Component, inject, OnInit, signal, ChangeDetectionStrategy, input} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { RemiseRfaApiService } from '../../data-access/services/remise-rfa-api.service';
import { IRemiseRfaFournisseur, IAvoirFournisseur } from '../../data-access/models';
import { formatCurrency } from 'app/shared/utils/format-utils';
import { BadgeComponent, ButtonComponent, DataTableComponent, ToolbarComponent } from '../../../../shared/ui';

import { DeviseDirective, DevisePipe } from 'app/shared/utils/devise';
@Component({
  selector: 'app-remises-rfa',
  imports: [DeviseDirective, DevisePipe, CommonModule, ButtonComponent, DataTableComponent, ToolbarComponent, BadgeComponent, NgbNavModule],
  templateUrl: './remises-rfa.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './remises-rfa.component.scss',
})
export class RemisesRfaComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement. Un écran atteint depuis deux menus affiche donc le nom
   * de celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  rfas = signal<IRemiseRfaFournisseur[]>([]);
  avoirs = signal<IAvoirFournisseur[]>([]);
  isLoading = signal(false);
  activeTab = signal<'rfa' | 'avoirs'>('rfa');

  formatCurrency = formatCurrency;
  Math = Math;

  private readonly api = inject(RemiseRfaApiService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.api.getRfaFournisseurs().subscribe({
      next: res => {
        this.rfas.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
    this.api.getAvoirsFournisseurs().subscribe({
      next: res => this.avoirs.set(res.body ?? []),
    });
  }
}
