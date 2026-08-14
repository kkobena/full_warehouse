import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LicenseService } from 'app/core/license/license.service';

/**
 * Exigence **B3** — bannière permanente d'alerte de licence.
 *
 * **Volontairement non masquable** : aucun bouton de fermeture, aucun `localStorage` de masquage.
 * Une bannière qu'on peut faire disparaître serait fermée le premier jour et oubliée jusqu'à la
 * coupure — ce qui est exactement la situation qu'elle existe pour éviter. La seule façon de la
 * faire disparaître est de renouveler la licence.
 *
 * Elle porte toujours un accès direct à l'écran d'activation : un utilisateur alerté doit trouver
 * la sortie en un clic, sans avoir à explorer les menus.
 *
 * Cf. docs/PLAN-GESTION-LICENCE.md §5.3.
 */
@Component({
  selector: 'app-license-banner',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (licenseService.showBanner()) {
      <div class="license-banner" [class]="variantClass()" role="alert" aria-live="assertive">
        <div class="license-banner__content">
          <i [class]="icon()" aria-hidden="true"></i>

          <div class="license-banner__text">
            <strong>{{ title() }}</strong>
            <span>{{ licenseService.license()?.message }}</span>
          </div>

          @if (phones().length || emails().length) {
            <div class="license-banner__support">
              @for (phone of phones(); track phone) {
                <a [href]="'tel:' + phone" class="link-dark text-decoration-none">
                  <i class="pi pi-phone" aria-hidden="true"></i> {{ phone }}
                </a>
              }
              @for (email of emails(); track email) {
                <a [href]="'mailto:' + email" class="link-dark text-decoration-none">
                  <i class="pi pi-envelope" aria-hidden="true"></i> {{ email }}
                </a>
              }
            </div>
          }

          <a routerLink="/licence" class="btn btn-sm btn-dark license-banner__action"> Gérer ma licence </a>
        </div>
      </div>
    }
  `,
  styles: `
    .license-banner {
      position: fixed;
      right: 0;
      bottom: 0;
      left: 0;
      // Sous les toasts (1090) pour ne pas les masquer, au-dessus des modales ngb (1055)
      // afin de rester visible pendant une confirmation.
      z-index: 1080;
      padding: 0.5rem 1rem;
      border-top: 1px solid rgba(0, 0, 0, 0.15);
      font-size: 0.875rem;
    }

    .license-banner__content {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      align-items: center;
      justify-content: center;
    }

    .license-banner__text {
      display: flex;
      flex-wrap: wrap;
      gap: 0.35rem;
      align-items: baseline;
    }

    .license-banner__support {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
    }

    .license-banner__action {
      white-space: nowrap;
    }
  `,
})
export class LicenseBannerComponent {
  protected readonly licenseService = inject(LicenseService);

  protected readonly phones = computed(() => this.licenseService.support().phones ?? []);

  protected readonly emails = computed(() => this.licenseService.support().emails ?? []);

  /**
   * Trois niveaux seulement : information (démo), avertissement (échéance proche) et blocage
   * (lecture seule). Multiplier les nuances de couleur rendrait l'urgence illisible.
   */
  protected readonly variantClass = computed(() => {
    const info = this.licenseService.license();
    if (info?.readOnly) {
      return 'alert alert-danger mb-0';
    }
    return info?.demo && info.status === 'VALID' ? 'alert alert-info mb-0' : 'alert alert-warning mb-0';
  });

  protected readonly icon = computed(() =>
    this.licenseService.license()?.readOnly ? 'pi pi-ban' : 'pi pi-exclamation-triangle',
  );

  protected readonly title = computed(() => {
    const info = this.licenseService.license();
    if (!info) {
      return '';
    }
    if (info.demo) {
      return 'VERSION DE DÉMONSTRATION — sans valeur légale.';
    }
    switch (info.status) {
      case 'DEMO_QUOTA_REACHED':
        return 'Limite de la version de démonstration atteinte.';
      case 'MISSING':
        return 'Aucune licence installée.';
      case 'INVALID':
        return 'Licence non valide.';
      case 'CLOCK_TAMPERED':
        return 'Horloge du poste incohérente.';
      case 'EXPIRED':
        return 'Abonnement expiré — application en lecture seule.';
      case 'GRACE':
        return 'Abonnement expiré — délai de renouvellement en cours.';
      default:
        return 'Abonnement bientôt expiré.';
    }
  });
}
