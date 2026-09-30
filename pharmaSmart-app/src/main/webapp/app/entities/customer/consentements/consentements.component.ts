import { ChangeDetectionStrategy, Component, effect, inject, input, signal, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent } from 'app/shared/ui';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { CustomerService } from '../customer.service';
import { CanalConsentement, IConsentement } from '../customer-fiche.model';

const LIBELLES: Record<CanalConsentement, string> = { SMS: 'SMS', WHATSAPP: 'WhatsApp', EMAIL: 'E-mail' };

/**
 * Consentement du client aux messages, canal par canal (docs/PLAN-FICHE-CLIENT.md, lot 4). Un refus
 * bloque la relance des différés et l'avis d'avoir disponible sur ce canal.
 */
@Component({
  selector: 'app-consentements',
  templateUrl: './consentements.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, NgbTooltip, ButtonComponent],
})
export class ConsentementsComponent {
  readonly customerId = input.required<number>();
  readonly canEdit = input<boolean>(false);

  protected readonly consentements = signal<IConsentement[]>([]);
  protected readonly libelles = LIBELLES;

  private readonly customerService = inject(CustomerService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  constructor() {
    effect(() => {
      const id = this.customerId();
      untracked(() =>
        this.customerService.consentements(id).subscribe({
          next: c => this.consentements.set(c),
          error: (err: HttpErrorResponse) =>
            this.notificationService.error(this.errorService.getErrorMessage(err, "Les consentements n'ont pas pu être chargés")),
        }),
      );
    });
  }

  protected enregistrer(canal: CanalConsentement, accorde: boolean): void {
    this.customerService.enregistrerConsentement(this.customerId(), canal, accorde).subscribe({
      next: c => this.consentements.update(liste => liste.map(x => (x.canal === canal ? c : x))),
      error: (err: HttpErrorResponse) =>
        this.notificationService.error(this.errorService.getErrorMessage(err, "Le consentement n'a pas pu être enregistré")),
    });
  }

  protected detail(c: IConsentement): string {
    if (c.accorde === null || c.accorde === undefined) {
      return 'Jamais demandé au client';
    }
    const date = c.date ? new Date(c.date).toLocaleDateString('fr-FR') : '';
    return `${c.accorde ? 'Accepté' : 'Refusé'} le ${date}${c.recueilliPar ? ', recueilli par ' + c.recueilliPar : ''}`;
  }
}
