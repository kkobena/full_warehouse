import { Component, DestroyRef, effect, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Button } from 'primeng/button';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Toolbar } from 'primeng/toolbar';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import {
  AvoirClientApiService,
  AvoirClientStatut,
  IAvoirClientDocument,
  ModeClotureAvoir,
} from '../../data-access/services/avoir-client-api.service';
import { SaleToolbarService } from '../../data-access/services/sale-toolbar.service';
import { CloturerAvoirModalComponent } from '../../ui/cloturer-avoir-modal/cloturer-avoir-modal.component';

@Component({
  selector: 'app-avoirs-client-list',
  templateUrl: './avoirs-client-list.component.html',
  styleUrl: './avoirs-client-list.component.scss',
  providers: [DatePipe],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    Button,
    TableModule,
    Toolbar,
    DatePicker,
    FloatLabel,
    IconField,
    InputIcon,
    InputText,
    Select,
    Tag,
    TooltipModule,
  ],
})
export class AvoirsClientListComponent implements OnInit {
  private readonly api = inject(AvoirClientApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly datePipe = inject(DatePipe);
  private readonly modalService = inject(NgbModal);
  private readonly toolbarService = inject(SaleToolbarService);

  protected loading = signal(false);
  protected statut = signal<AvoirClientStatut | null>('OUVERT');

  protected documents = signal<IAvoirClientDocument[]>([]);
  protected documentsTotal = 0;
  protected documentsPage = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;

  protected search = '';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();

  protected readonly statutOptions: { label: string; value: AvoirClientStatut | null }[] = [
    { label: 'Tout', value: null },
    { label: 'Ouverts', value: 'OUVERT' },
    { label: 'Clôturés', value: 'CLOTURE' },
    { label: 'Expirés', value: 'EXPIRE' },
  ];

  protected readonly modeClotureOptions: { label: string; value: ModeClotureAvoir; icon: string }[] = [
    { label: 'Remboursement espèces', value: 'REMBOURSEMENT_ESPECES', icon: 'pi pi-money-bill' },
    { label: 'Remboursement CB', value: 'REMBOURSEMENT_CB', icon: 'pi pi-credit-card' },
    { label: "Bon d'avoir", value: 'BON_AVOIR', icon: 'pi pi-ticket' },
    { label: 'Retour produit', value: 'RETOUR_PRODUIT', icon: 'pi pi-replay' },
    { label: 'Compensation vente', value: 'COMPENSATION_VENTE', icon: 'pi pi-arrow-right-arrow-left' },
  ];

  get totalMontantDocuments(): number {
    return this.documents().reduce((s, d) => {
      const montant = d.statut === 'OUVERT' ? (d.montantRestant ?? d.montant ?? 0) : (d.montant ?? 0);
      return s + montant;
    }, 0);
  }

  constructor() {
    effect(() => {
      const ref = this.toolbarService.avoirSaleRef();
      if (ref) {
        this.search = ref;
        this.statut.set('OUVERT');
        this.loadDocuments(0, 'OUVERT');
        this.toolbarService.clearAvoirSaleRef();
      }
    });
  }

  ngOnInit(): void {
    this.loadDocuments(0, 'OUVERT');
  }

  protected onStatutChange(statut: AvoirClientStatut | null): void {
    this.statut.set(statut);
    this.loadDocuments(0, statut);
  }

  protected loadDocuments(page?: number, statut?: AvoirClientStatut | null): void {
    const pageToLoad = page ?? this.documentsPage;
    const statutFilter = statut !== undefined ? statut : this.statut();
    this.loading.set(true);
    this.api.queryDocuments({
      page: pageToLoad,
      size: this.itemsPerPage,
      search: this.search || null,
      fromDate: this.datePipe.transform(this.fromDate, 'yyyy-MM-dd'),
      toDate: this.datePipe.transform(this.toDate, 'yyyy-MM-dd'),
      statut: statutFilter,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          this.documentsTotal = Number(res.headers.get('X-Total-Count'));
          this.documentsPage = pageToLoad;
          this.documents.set(res.body ?? []);
        },
        error: () => this.loading.set(false),
      });
  }

  protected lazyLoadingDocuments(event: TableLazyLoadEvent): void {
    if (event.first != null && event.rows != null) {
      this.documentsPage = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.loadDocuments(this.documentsPage);
    }
  }

  protected onSearch(): void {
    this.loadDocuments(0);
  }

  protected openCloturerDialog(doc: IAvoirClientDocument): void {
    const ref = this.modalService.open(CloturerAvoirModalComponent, { centered: true, size: 'lg', backdrop: 'static' });
    ref.componentInstance.document = doc;
    ref.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.loadDocuments(0));
  }

  protected modeClotureLabelOf(mode?: string): string {
    return this.modeClotureOptions.find(o => o.value === mode)?.label ?? (mode ?? '—');
  }

  protected modeClotureIconOf(mode?: string): string {
    return this.modeClotureOptions.find(o => o.value === mode)?.icon ?? 'pi pi-check';
  }
}
