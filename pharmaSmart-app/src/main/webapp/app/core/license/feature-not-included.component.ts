import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { LicenseService } from 'app/core/license/license.service';

/**
 * Page affichée lorsqu'un module n'est pas couvert par l'abonnement.
 *
 * <p>Une page de refus n'a d'intérêt que si elle indique **quoi faire ensuite** : elle nomme le
 * module manquant et affiche les coordonnées du revendeur, seul interlocuteur capable de l'ajouter.
 *
 * <p>Le nom du module arrive par `queryParams` grâce à `withComponentInputBinding()`.
 */
@Component({
  selector: 'app-feature-not-included',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="container py-5" style="max-width: 40rem">
      <div class="text-center mb-4">
        <i class="pi pi-lock text-muted" style="font-size: 3rem" aria-hidden="true"></i>
        <h2 class="mt-3">Module non souscrit</h2>
        <p class="text-muted mb-0">
          Le module <strong>{{ moduleLabel() }}</strong> n'est pas inclus dans votre abonnement.
        </p>
      </div>

      @if (support(); as contacts) {
        @if (contacts.phones.length || contacts.emails.length) {
          <div class="card">
            <div class="card-body">
              <h5 class="card-title">{{ contacts.resellerName ?? 'Votre revendeur' }}</h5>
              <p class="text-muted small">Contactez-le pour ajouter ce module à votre abonnement.</p>
              <ul class="list-unstyled mb-0 d-flex flex-column gap-2">
                @for (phone of contacts.phones; track phone) {
                  <li><i class="pi pi-phone me-2" aria-hidden="true"></i><a [href]="'tel:' + phone">{{ phone }}</a></li>
                }
                @for (email of contacts.emails; track email) {
                  <li><i class="pi pi-envelope me-2" aria-hidden="true"></i><a [href]="'mailto:' + email">{{ email }}</a></li>
                }
              </ul>
            </div>
          </div>
        }
      }
    </div>
  `,
})
export default class FeatureNotIncludedComponent {
  /** Nom technique du module, transmis en `queryParam` par `licenseFeatureGuard`. */
  readonly module = input<string>('');

  private readonly licenseService = inject(LicenseService);

  protected readonly support = this.licenseService.support;

  protected readonly moduleLabel = computed(() => {
    const code = this.module();
    return this.licenseService.features().find(feature => feature.code === code)?.label ?? (code || 'demandé');
  });
}
