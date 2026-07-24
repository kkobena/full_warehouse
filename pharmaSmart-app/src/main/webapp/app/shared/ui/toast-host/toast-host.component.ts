import { Component, inject } from '@angular/core';
import { NgbToast } from '@ng-bootstrap/ng-bootstrap';

import { NotificationSeverity, NotificationService } from 'app/shared/services/notification.service';

/**
 * Rend les notifications de `NotificationService` — remplace `<p-toast>`.
 *
 * À monter **une seule fois** dans le layout racine : le service est global, une pile
 * unique suffit. C'est la différence avec `p-toast`, que chaque écran devait déclarer.
 *
 * L'auto-fermeture est déléguée à `NgbToast` (`[delay]` + `(hidden)`), qui gère aussi
 * la pause au survol.
 */
@Component({
  selector: 'app-toast-host',
  imports: [NgbToast],
  template: `
    <div class="toast-container position-fixed top-50 start-50 translate-middle p-3" aria-live="polite" aria-atomic="true">
      @for (message of notifications.messages(); track message.id) {
        <ngb-toast
          [class]="toastClass(message.severity)"
          [autohide]="true"
          [delay]="message.life"
          (hidden)="notifications.dismiss(message.id)"
        >
          <div class="d-flex align-items-start gap-2">
            <i [class]="iconOf(message.severity)" aria-hidden="true"></i>
            <div>
              @if (message.summary) {
                <div class="fw-semibold">{{ message.summary }}</div>
              }
              <div>{{ message.detail }}</div>
            </div>
          </div>
        </ngb-toast>
      }
    </div>
  `,
  styles: `
    // Au-dessus des modales ngb (z-index 1055) pour rester lisible par-dessus une confirmation.
    .toast-container {
      z-index: 1090;
    }
  `,
})
export class ToastHostComponent {
  protected readonly notifications = inject(NotificationService);

  private static readonly BOOTSTRAP_VARIANT: Record<NotificationSeverity, string> = {
    success: 'success',
    info: 'info',
    warn: 'warning',
    error: 'danger',
  };

  private static readonly ICON: Record<NotificationSeverity, string> = {
    success: 'pi pi-check-circle',
    info: 'pi pi-info-circle',
    warn: 'pi pi-exclamation-triangle',
    error: 'pi pi-times-circle',
  };

  /** `.text-bg-*` colore le fond ET calcule le contraste du texte (Bootstrap 5.3). */
  protected toastClass(severity: NotificationSeverity): string {
    return `text-bg-${ToastHostComponent.BOOTSTRAP_VARIANT[severity]}`;
  }

  protected iconOf(severity: NotificationSeverity): string {
    return ToastHostComponent.ICON[severity];
  }
}
