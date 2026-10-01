import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NgbDateStruct, NgbPagination, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, DataTableComponent, DetailFieldComponent, DetailGridComponent, DetailSectionComponent } from 'app/shared/ui';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { RouterLink } from '@angular/router';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';
import { NGB_DATE_OFFSET_MONTHS, NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';
import { IDiffere, IReglementDiffere } from 'app/features/differes/data-access/models/differe.model';
import { IRelanceDiffere, ISituationCredit } from '../customer-fiche.model';
import { CustomerService } from '../customer.service';

/** Onglet « Crédit » de la fiche client : différés non soldés, relances et règlements. */
@Component({
  selector: 'app-customer-credit-tab',
  templateUrl: './customer-credit-tab.component.html',
  styleUrls: ['./customer-credit-tab.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    NgbPagination,
    NgbTooltip,
    RouterLink,
    ButtonComponent,
    DataTableComponent,
    DetailFieldComponent,
    DetailGridComponent,
    DetailSectionComponent,
    PharmaDatePickerComponent,
  ],
})
export class CustomerCreditTabComponent {
  readonly customerId = input.required<number>();
  readonly customerCode = input<string | undefined>(undefined);
  readonly customerPhone = input<string | undefined>(undefined);
  readonly canOpenDifferes = input<boolean>(false);
  readonly devise = input<string>('');

  protected readonly pageSize = 20;
  protected readonly differe = signal<IDiffere | null>(null);
  protected readonly reglements = signal<IReglementDiffere[]>([]);
  protected readonly lignesReglement = computed(() => this.reglements().flatMap(r => r.items ?? []));
  protected readonly reglementsPage = signal(1);
  protected readonly totalReglements = signal(0);
  protected readonly situationCredit = signal<ISituationCredit | null>(null);
  protected readonly relances = signal<IRelanceDiffere[]>([]);
  protected readonly relanceEnCours = signal(false);
  protected readonly releveFrom = signal<NgbDateStruct>(NGB_DATE_OFFSET_MONTHS(-3));
  protected readonly releveTo = signal<NgbDateStruct>(NGB_DATE_OFFSET_MONTHS(0));

  private readonly customerService = inject(CustomerService);
  private readonly notificationService = inject(NotificationService);
  private readonly blobDownloadService = inject(BlobDownloadService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const id = this.customerId();
      untracked(() => this.loadCredit(id));
    });
  }

  /** Relance SMS du solde des différés ; la dernière relance reste visible pour ne pas en abuser. */
  protected relancer(): void {
    this.relanceEnCours.set(true);
    this.customerService
      .relancerDifferes(this.customerId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: relance => {
          this.relanceEnCours.set(false);
          this.relances.update(liste => [relance, ...liste]);
          this.notificationService.success(`Relance envoyée au ${relance.telephone}`);
        },
        error: err => {
          this.relanceEnCours.set(false);
          this.signaler("La relance n'a pas pu être envoyée")(err);
        },
      });
  }

  protected imprimerReleve(): void {
    this.customerService
      .relevePdf(this.customerId(), NGB_DATE_TO_ISO(this.releveFrom()), NGB_DATE_TO_ISO(this.releveTo()))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.blobDownloadService.downloadPdf(blob, `releve-compte-${this.customerCode() ?? this.customerId()}`),
        error: this.signaler("Le relevé n'a pas pu être édité"),
      });
  }

  protected changeReglementsPage(page: number): void {
    this.reglementsPage.set(page);
    this.loadReglements(this.customerId());
  }

  private loadCredit(customerId: number): void {
    this.customerService
      .differes(customerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: differe => this.differe.set(differe), error: this.signaler("Les différés n'ont pas pu être chargés") });
    this.customerService
      .situationCredit(customerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: situation => this.situationCredit.set(situation), error: this.signaler("La limite de crédit n'a pas pu être chargée") });
    this.customerService
      .relancesDifferes(customerId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: relances => this.relances.set(relances), error: this.signaler("Les relances n'ont pas pu être chargées") });
    this.loadReglements(customerId);
  }

  private loadReglements(customerId: number): void {
    this.customerService
      .reglementsDifferes(customerId, { page: this.reglementsPage() - 1, size: this.pageSize })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.totalReglements.set(Number(res.headers.get('X-Total-Count') ?? 0));
          this.reglements.set(res.body ?? []);
        },
        error: this.signaler("Les règlements n'ont pas pu être chargés"),
      });
  }

  private signaler(action: string): (err: HttpErrorResponse) => void {
    return err => this.notificationService.error(this.errorService.getErrorMessage(err, action));
  }
}

