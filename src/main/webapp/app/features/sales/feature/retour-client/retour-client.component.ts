import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
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

import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import {
  IRetourClient,
  ModeReglementRetour,
  MotifRetourClient,
  RetourClientApiService,
} from '../../data-access/services/retour-client-api.service';
import { RetourClientModalComponent } from '../../ui/retour-client-modal/retour-client-modal.component';

@Component({
  selector: 'app-retour-client',
  templateUrl: './retour-client.component.html',
  styleUrl: './retour-client.component.scss',
  providers: [DatePipe],
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
  ],
})
export class RetourClientComponent implements OnInit {
  private readonly api = inject(RetourClientApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly datePipe = inject(DatePipe);
  private readonly modalService = inject(NgbModal);

  protected loading = signal(false);
  protected retours: IRetourClient[] = [];
  protected totalItems = 0;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;

  protected search = '';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();

  protected readonly motifOptions: { label: string; value: MotifRetourClient }[] = [
    { label: 'Erreur de dispensation', value: 'ERREUR_DISPENSATION' },
    { label: 'Produit défectueux', value: 'PRODUIT_DEFECTUEUX' },
    { label: 'Erreur de quantité', value: 'ERREUR_QUANTITE' },
    { label: 'Insatisfaction client', value: 'INSATISFACTION' },
    { label: 'Autre', value: 'AUTRE' },
  ];

  protected readonly modeReglementOptions: { label: string; value: ModeReglementRetour; icon: string }[] = [
    { label: 'Remboursement espèces', value: 'REMBOURSEMENT_ESPECES', icon: 'pi pi-money-bill' },
    { label: 'Remboursement CB', value: 'REMBOURSEMENT_CB', icon: 'pi pi-credit-card' },
    { label: 'Avoir client', value: 'AVOIR_CLIENT', icon: 'pi pi-ticket' },
  ];

  ngOnInit(): void {
    this.loadPage();
  }

  protected loadPage(page?: number): void {
    const p = page ?? this.page;
    this.loading.set(true);
    this.api.query({
      page: p,
      size: this.itemsPerPage,
      search: this.search || null,
      fromDate: this.datePipe.transform(this.fromDate, 'yyyy-MM-dd'),
      toDate: this.datePipe.transform(this.toDate, 'yyyy-MM-dd'),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          this.totalItems = Number(res.headers.get('X-Total-Count'));
          this.page = p;
          this.retours = res.body ?? [];
        },
        error: () => this.loading.set(false),
      });
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event.first != null && event.rows != null) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.loadPage(this.page);
    }
  }

  protected onSearch(): void {
    this.loadPage(0);
  }

  protected openRetourModal(): void {
    const ref = this.modalService.open(RetourClientModalComponent, { centered: true, size: 'xl', backdrop: 'static' });
    ref.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      if (result) this.loadPage(0);
    });
  }

  protected motifLabelOf(motif?: string): string {
    return this.motifOptions.find(o => o.value === motif)?.label ?? (motif ?? '—');
  }

  protected modeLabelOf(mode?: string): string {
    return this.modeReglementOptions.find(o => o.value === mode)?.label ?? (mode ?? '—');
  }

  protected modeIconOf(mode?: string): string {
    return this.modeReglementOptions.find(o => o.value === mode)?.icon ?? 'pi pi-undo';
  }
}
