import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { DatePickerModule } from 'primeng/datepicker';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { KeyFilter } from 'primeng/keyfilter';
import { ToastModule } from 'primeng/toast';
import { forkJoin } from 'rxjs';
import { IDelivery } from 'app/shared/model/delevery.model';
import { IOrderLine } from 'app/shared/model/order-line.model';
import {
  IReconciliationFactureFournisseur,
  IReconciliationCommand,
} from 'app/shared/model/reconciliation-facture-fournisseur.model';
import { ReconciliationFournisseurService } from 'app/entities/commande/reconciliation/reconciliation-fournisseur.service';
import { DeliveryService } from 'app/entities/commande/delevery/delivery.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { ErrorService } from 'app/shared/error.service';

@Component({
  selector: 'app-reconciliation-workspace',
  templateUrl: './reconciliation-workspace.component.html',
  styleUrls: ['./reconciliation-workspace.component.scss'],
  providers: [DatePipe],
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    InputNumberModule,
    InputTextModule,
    DatePickerModule,
    TableModule,
    TagModule,
    KeyFilter,
    ToastModule,
  ],
})
export class ReconciliationWorkspaceComponent implements OnInit {
  private readonly reconciliationService = inject(ReconciliationFournisseurService);
  private readonly deliveryService = inject(DeliveryService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly datePipe = inject(DatePipe);

  readonly delivery = input.required<IDelivery>();
  readonly done = output<IReconciliationFactureFournisseur>();
  readonly cancelled = output<void>();

  readonly deliveryRef = computed<string>(() => {
    const d = this.delivery() as any;
    return d.receiptReference ?? d.orderReference ?? '';
  });

  protected loading = signal(true);
  protected saving = signal(false);
  protected orderLines = signal<IOrderLine[]>([]);
  protected existing = signal<IReconciliationFactureFournisseur | null>(null);

  protected factureReference = '';
  protected factureDate: Date | null = null;
  protected factureMontantHT: number | null = null;
  protected factureTVA: number | null = null;

  ngOnInit(): void {
    const d = this.delivery();
    const cmdId = d.commandeId ?? { id: d.id!, orderDate: d.orderDate ?? d.receiptDate! };
    forkJoin({
      commande: this.deliveryService.find(d.commandeId),
      recon: this.reconciliationService.find(cmdId.id!, cmdId.orderDate!),
    }).subscribe({
      next: ({ commande, recon }) => {
        this.orderLines.set(((commande.body as any)?.orderLines ?? []) as IOrderLine[]);
        if (recon) {
          this.existing.set(recon);
          this.factureReference = recon.factureReference ?? '';
          this.factureDate = recon.factureDate ? new Date(recon.factureDate) : null;
          this.factureMontantHT = recon.factureMontantHT ?? null;
          this.factureTVA = recon.factureTVA ?? null;
        } else {
          this.factureReference = (d as any).receiptReference ?? '';
          const rawDate = (d as any).receiptDate ?? (d as any).orderDate;
          this.factureDate = rawDate ? new Date(rawDate) : null;
          this.factureMontantHT = null;
          this.factureTVA = (d as any).taxAmount ?? 0;
        }
        this.loading.set(false);
      },
      error: () => {
        this.notificationService.error('Erreur lors du chargement');
        this.loading.set(false);
      },
    });
  }

  protected get blMontantHT(): number {
    return (this.delivery() as any).grossAmount ?? 0;
  }

  protected get blTVA(): number {
    return (this.delivery() as any).taxAmount ?? 0;
  }

  protected get blTTC(): number {
    return this.blMontantHT + this.blTVA;
  }

  protected get factureTTC(): number {
    return (this.factureMontantHT ?? 0) + (this.factureTVA ?? 0);
  }

  protected get ecartHT(): number {
    return (this.factureMontantHT ?? 0) - this.blMontantHT;
  }

  protected get ecartTVA(): number {
    return (this.factureTVA ?? 0) - this.blTVA;
  }

  protected get ecartTTC(): number {
    return this.factureTTC - this.blTTC;
  }

  protected get isReconcilie(): boolean {
    return this.ecartHT === 0 && this.ecartTVA === 0;
  }

  protected get canSave(): boolean {
    return !!this.factureReference?.trim() && this.factureMontantHT !== null;
  }

  protected onSubmit(): void {
    if (!this.canSave) return;
    const d = this.delivery();
    const cmdId = d.commandeId ?? { id: d.id!, orderDate: d.orderDate ?? d.receiptDate! };
    const cmd: IReconciliationCommand = {
      factureReference: this.factureReference.trim(),
      factureDate: this.factureDate
        ? this.datePipe.transform(this.factureDate, 'yyyy-MM-dd')
        : null,
      factureMontantHT: this.factureMontantHT!,
      factureTVA: this.factureTVA ?? 0,
    };
    this.saving.set(true);
    this.reconciliationService.save(cmdId.id!, cmdId.orderDate!, cmd).subscribe({
      next: result => {
        this.saving.set(false);
        this.notificationService.success(
          this.isReconcilie
            ? 'Facture réconciliée — aucun écart'
            : `Facture enregistrée avec écart HT : ${this.ecartHT} F`,
          'Réconciliation',
        );
        this.done.emit(result);
      },
      error: err => {
        this.saving.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Réconciliation');
      },
    });
  }

  protected onCancel(): void {
    this.cancelled.emit();
  }

  protected statutSeverity(statut?: string): string {
    switch (statut) {
      case 'RECONCILIEE': return 'success';
      case 'ECART': return 'warn';
      case 'LITIGE': return 'danger';
      default: return 'secondary';
    }
  }

  protected statutLabel(statut?: string): string {
    switch (statut) {
      case 'RECONCILIEE': return 'Réconciliée';
      case 'ECART': return 'Écart';
      case 'LITIGE': return 'Litige';
      default: return 'En attente';
    }
  }
}
