import { ChangeDetectionStrategy, Component, inject, OnInit, signal, input} from '@angular/core';

import { CardComponent } from 'app/shared/ui/card/card.component';
import { ToolbarComponent } from 'app/shared/ui/toolbar/toolbar.component';
import { NotificationService } from 'app/shared/services/notification.service';
import { DeclarationCaApiService } from '../../data-access/services/declaration-ca-api.service';

/**
 * Paramètre d'officine : exclure ou non les unités gratuites du chiffre d'affaires à déclarer.
 *
 * <p>Un seul interrupteur, mais il change le chiffre de trois écrans de comptabilité — d'où
 * l'explication chiffrée à côté plutôt qu'un simple libellé.
 */
@Component({
  selector: 'app-exclusion-parametres',
  imports: [CardComponent, ToolbarComponent],
  templateUrl: './exclusion-parametres.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExclusionParametresComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit alors le libellé du menu — ou son
   * `titre_long` quand la barre nomme plus longuement — au lieu d'une valeur figée dans le gabarit.
   */
  readonly navCode = input<string>('');

  private readonly api = inject(DeclarationCaApiService);
  private readonly notification = inject(NotificationService);

  protected readonly excludeFreeUnit = signal(false);
  protected readonly chargement = signal(false);

  ngOnInit(): void {
    this.chargement.set(true);
    this.api.lireParametres().subscribe({
      next: ({ excludeFreeUnit }) => {
        this.excludeFreeUnit.set(excludeFreeUnit);
        this.chargement.set(false);
      },
      error: () => {
        this.chargement.set(false);
        this.notification.error('Impossible de lire le paramètre');
      },
    });
  }

  protected enregistrer(valeur: boolean): void {
    this.chargement.set(true);
    this.api.majParametres(valeur).subscribe({
      next: ({ excludeFreeUnit }) => {
        this.excludeFreeUnit.set(excludeFreeUnit);
        this.chargement.set(false);
        this.notification.success(
          excludeFreeUnit
            ? 'Les unités gratuites sont désormais exclues du CA à déclarer'
            : 'Les unités gratuites sont de nouveau déclarées',
        );
      },
      error: () => {
        this.chargement.set(false);
        this.notification.error('Enregistrement impossible');
      },
    });
  }
}
