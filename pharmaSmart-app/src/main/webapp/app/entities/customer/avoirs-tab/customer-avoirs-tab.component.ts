import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonComponent, DataTableComponent, DetailGridComponent, DetailSectionComponent } from 'app/shared/ui';
import { DeviseDirective } from 'app/shared/utils/devise';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { IAvoirClientDocument } from 'app/shared/model/avoir-client-document.model';
import { CustomerService } from '../customer.service';

/** Onglet « Avoirs » de la fiche client : avoirs clients et leur statut (ouvert, clôturé, expiré…). */
@Component({
  selector: 'app-customer-avoirs-tab',
  templateUrl: './customer-avoirs-tab.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ButtonComponent, DataTableComponent, DetailGridComponent, DetailSectionComponent, DeviseDirective],
})
export class CustomerAvoirsTabComponent {
  readonly customerId = input.required<number>();

  protected readonly avoirs = signal<IAvoirClientDocument[]>([]);
  protected readonly avoirsOuverts = computed(() => this.avoirs().filter(a => a.statut === 'OUVERT'));
  protected readonly soldeTotalAvoirs = computed(() => this.avoirsOuverts().reduce((sum, a) => sum + (a.montant ?? 0), 0));

  private readonly customerService = inject(CustomerService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const id = this.customerId();
      untracked(() => this.loadAvoirs(id));
    });
  }

  protected openAvoirPdf(avoirId: number): void {
    window.open(`/api/sales/retours/avoirs/${avoirId}/pdf`, '_blank');
  }

  private loadAvoirs(customerId: number): void {
    if (!customerId) {
      return;
    }
    this.customerService
      .avoirsByCustomer(customerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: avoirs => this.avoirs.set(avoirs),
        error: (err: HttpErrorResponse) => this.notificationService.error(this.errorService.getErrorMessage(err, "Les avoirs n'ont pas pu être chargés")),
      });
  }
}

