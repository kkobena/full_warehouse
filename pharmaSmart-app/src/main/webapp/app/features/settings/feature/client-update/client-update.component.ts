import { ChangeDetectionStrategy, Component, computed, inject, OnInit } from '@angular/core';
import { ClientUpdateService } from 'app/core/tauri/client-update.service';
import { ButtonComponent, CardComponent } from '../../../../shared/ui';

/**
 * Écran de mise à jour du poste client.
 *
 * Un poste client n'est pas installé : on y a déposé `pharmasmart.exe`. Cet écran
 * automatise ce qui se faisait à la main — récupérer le nouvel exécutable auprès du
 * poste serveur de l'officine et remplacer celui du poste.
 *
 * Le parcours est délibérément en deux temps : **installer**, puis **redémarrer**.
 * Redémarrer d'office fermerait l'application sans prévenir, éventuellement en pleine
 * vente. C'est l'opérateur qui choisit le moment.
 */
@Component({
  selector: 'app-client-update',
  templateUrl: './client-update.component.html',
  styleUrls: ['./client-update.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonComponent, CardComponent],
})
export class ClientUpdateComponent implements OnInit {
  private readonly updateService = inject(ClientUpdateService);

  readonly phase = this.updateService.phase;
  readonly status = this.updateService.status;
  readonly errorMessage = this.updateService.errorMessage;

  readonly supported = this.updateService.isSupported();

  readonly busy = computed(() => this.phase() === 'checking' || this.phase() === 'applying');

  ngOnInit(): void {
    // Vérification au chargement : c'est une simple consultation, l'opérateur
    // arrive donc sur une information déjà à jour plutôt que sur un bouton à cliquer.
    void this.updateService.check();
  }

  check(): void {
    void this.updateService.check();
  }

  apply(): void {
    void this.updateService.apply();
  }

  restart(): void {
    void this.updateService.restart();
  }
}
