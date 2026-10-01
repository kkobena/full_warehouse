import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NgbDateStruct, NgbPagination } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, DataTableComponent, DetailGridComponent, DetailSectionComponent } from 'app/shared/ui';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { DeviseDirective } from 'app/shared/utils/devise';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { NGB_DATE_OFFSET_MONTHS, NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';
import { IProduitDelivre } from '../customer-fiche.model';
import { CustomerService } from '../customer.service';

/** Onglet « Produits délivrés » de la fiche client : « le même que la dernière fois ». */
@Component({
  selector: 'app-customer-produits-tab',
  templateUrl: './customer-produits-tab.component.html',
  styleUrls: ['./customer-produits-tab.component.scss'],
  styles: `
    .fiche-recherche {
      max-width: 260px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, NgbPagination, ButtonComponent, DataTableComponent, DetailGridComponent, DetailSectionComponent, PharmaDatePickerComponent, DeviseDirective],
})
export class CustomerProduitsTabComponent {
  readonly customerId = input.required<number>();

  protected readonly pageSize = 20;
  protected readonly produits = signal<IProduitDelivre[]>([]);
  protected readonly produitsFrom = signal<NgbDateStruct>(NGB_DATE_OFFSET_MONTHS(-12));
  protected readonly produitsTo = signal<NgbDateStruct>(NGB_DATE_OFFSET_MONTHS(0));
  protected readonly produitsSearch = signal('');
  protected readonly produitsPage = signal(1);
  protected readonly totalProduits = signal(0);

  private readonly customerService = inject(CustomerService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const id = this.customerId();
      untracked(() => this.loadProduits(id));
    });
  }

  protected searchProduits(): void {
    this.produitsPage.set(1);
    this.loadProduits(this.customerId());
  }

  protected changeProduitsPage(page: number): void {
    this.produitsPage.set(page);
    this.loadProduits(this.customerId());
  }

  private loadProduits(customerId: number): void {
    const req: Record<string, unknown> = {
      fromDate: NGB_DATE_TO_ISO(this.produitsFrom()),
      toDate: NGB_DATE_TO_ISO(this.produitsTo()),
      page: this.produitsPage() - 1,
      size: this.pageSize,
    };
    // createRequestOption envoie « undefined » en toutes lettres : la recherche ne part que si elle est saisie.
    if (this.produitsSearch().trim()) {
      req['search'] = this.produitsSearch().trim();
    }
    this.customerService
      .produitsDelivres(customerId, req)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.totalProduits.set(Number(res.headers.get('X-Total-Count') ?? 0));
          this.produits.set(res.body ?? []);
        },
        error: this.signaler("Les produits délivrés n'ont pas pu être chargés"),
      });
  }

  private signaler(action: string): (err: HttpErrorResponse) => void {
    return err => this.notificationService.error(this.errorService.getErrorMessage(err, action));
  }
}

