import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { InputTextModule } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { Toast } from 'primeng/toast';
import { IDelivery } from 'app/shared/model/delevery.model';
import { ICommande } from 'app/shared/model/commande.model';
import { DATE_FORMAT_ISO_DATE } from 'app/shared/util/warehouse-util';
import { DeliveryService } from '../../../../entities/commande/delevery/delivery.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { TauriPrinterService } from '../../../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../../../shared/util/tauri-util';
import { CommandeReceivedComponent } from '../commande-received/commande-received.component';

@Component({
  selector: 'app-commande-received-home',
  templateUrl: './commande-received-home.component.html',
  styleUrls: ['./commande-received-home.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    CommandeReceivedComponent,
    ButtonModule,
    TooltipModule,
    InputTextModule,
    IconField,
    InputIcon,
    DatePicker,
    FloatLabel,
    ProgressSpinnerModule,
    DecimalPipe,
    DatePipe,
    Toast,
  ],
})
export class CommandeReceivedHomeComponent implements OnInit {

  // ── État master/detail ────────────────────────────────────────────────────
  readonly editingCommande = signal<ICommande | null>(null);

  // ── Liste ─────────────────────────────────────────────────────────────────
  readonly deliveries = signal<IDelivery[]>([]);
  readonly loading = signal(false);
  readonly totalItems = signal(0);
  readonly currentPage = signal(0);
  readonly rows = 10;

  protected searchText = '';
  protected dtStart: Date | null = new Date();
  protected dtEnd: Date | null = new Date();

  private readonly deliveryService = inject(DeliveryService);
  private readonly notificationService = inject(NotificationService);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.loadPage(0);
  }

  onSearch(): void {
    this.loadPage(0);
  }

  loadPage(page = 0): void {
    this.loading.set(true);
    const query: any = { page, size: this.rows, search: this.searchText, statut: 'RECEIVED' };
    if (this.dtStart) query.fromDate = DATE_FORMAT_ISO_DATE(this.dtStart);
    if (this.dtEnd) query.toDate = DATE_FORMAT_ISO_DATE(this.dtEnd);
    this.deliveryService
      .query(query)
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res: HttpResponse<IDelivery[]>) => {
          this.deliveries.set(res.body ?? []);
          this.totalItems.set(Number(res.headers.get('X-Total-Count') ?? 0));
          this.currentPage.set(page);
        },
        error: () => this.notificationService.error('Erreur lors du chargement des bons en attente', 'Erreur'),
      });
  }

  // ── Navigation master/detail ──────────────────────────────────────────────

  onEditer(delivery: IDelivery): void {
    this.deliveryService.find(delivery.commandeId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        if (res.body) this.editingCommande.set(res.body as unknown as ICommande);
      },
      error: () => this.notificationService.error('Erreur lors du chargement du bon', 'Erreur'),
    });
  }

  onRetour(): void {
    this.editingCommande.set(null);
    this.loadPage(this.currentPage());
  }

  onCommandeChange(c: ICommande | null): void {
    if (c) {
      this.editingCommande.set(c);
    } else {
      this.onRetour();
    }
  }

  // ── Export PDF ────────────────────────────────────────────────────────────

  exportPdf(delivery: IDelivery, event: MouseEvent): void {
    event.stopPropagation();
    this.deliveryService.exportToPdf(delivery.commandeId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: blob => {
        if (this.tauriPrinter.isRunningInTauri()) {
          handleBlobForTauri(blob, 'bon_en_cours');
        } else {
          window.open(URL.createObjectURL(blob));
        }
      },
      error: () => this.notificationService.error('Erreur export PDF', 'Erreur'),
    });
  }

  // ── Pagination ─────────────────────────────────────────────────────────────

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.totalItems() / this.rows)));

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.loadPage(page);
    }
  }
}
