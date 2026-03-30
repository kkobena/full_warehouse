import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { DatePickerModule } from 'primeng/datepicker';
import { ICommande } from '../../../../shared/model/commande.model';
import { DeliveryService } from '../../../../entities/commande/delevery/delivery.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';

@Component({
  selector: 'app-reconciliation-facture',
  templateUrl: './reconciliation-facture.component.html',
  styleUrls: ['./reconciliation-facture.component.scss'],
  imports: [CommonModule, FormsModule, ButtonModule, InputNumberModule, InputTextModule, DatePickerModule],
})
export class ReconciliationFactureComponent implements OnInit {
  commande!: ICommande;
  header = 'Rapprochement facture fournisseur';

  protected factureReference = '';
  protected factureDate: Date | null = null;
  protected factureMontantHT: number | null = null;
  protected factureTVA: number | null = null;
  protected saving = false;

  private readonly activeModal = inject(NgbActiveModal);
  private readonly deliveryService = inject(DeliveryService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.factureReference = this.commande.receiptReference ?? '';
    this.factureDate = this.commande.receiptDate ? new Date(this.commande.receiptDate) : null;
    // receiptAmount = ce que le fournisseur a facturé (htAmount en base) ; grossAmount = calculé des lignes
    this.factureMontantHT = this.commande.receiptAmount ?? this.commande.grossAmount ?? null;
    this.factureTVA = this.commande.taxAmount ?? null;
  }

  /** Montant calculé depuis les lignes de commande (référence interne). */
  protected get blMontantHT(): number {
    return this.commande.grossAmount ?? 0;
  }

  /** Écart HT = Facture - BL calculé. */
  protected get ecartHT(): number {
    return (this.factureMontantHT ?? 0) - this.blMontantHT;
  }

  /** TVA BL de référence. */
  protected get blTVA(): number {
    return this.commande.taxAmount ?? 0;
  }

  protected get ecartTVA(): number {
    return (this.factureTVA ?? 0) - this.blTVA;
  }

  protected get totalFacture(): number {
    return (this.factureMontantHT ?? 0) + (this.factureTVA ?? 0);
  }

  protected get totalBL(): number {
    return this.blMontantHT + this.blTVA;
  }

  protected get isReconcilie(): boolean {
    return this.ecartHT === 0 && this.ecartTVA === 0;
  }

  protected get canSave(): boolean {
    return !!this.factureReference?.trim() && this.factureMontantHT !== null;
  }

  protected onSubmit(): void {
    if (!this.canSave) return;

    const payload = {
      ...this.commande,
      receiptReference: this.factureReference.trim(),
      receiptDate: this.factureDate ? this.factureDate.toISOString().split('T')[0] : this.commande.receiptDate,
      receiptAmount: this.factureMontantHT!,
      taxAmount: this.factureTVA ?? 0,
    };

    this.saving = true;
    this.deliveryService.update(payload as any).subscribe({
      next: () => {
        this.saving = false;
        this.notificationService.success(
          this.isReconcilie ? 'Facture rapprochée — aucun écart' : `Facture enregistrée — écart HT : ${this.ecartHT} F`,
          'Réconciliation',
        );
        this.activeModal.close(payload);
      },
      error: err => {
        this.saving = false;
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Réconciliation');
      },
    });
  }

  protected onCancel(): void {
    this.activeModal.dismiss();
  }
}
