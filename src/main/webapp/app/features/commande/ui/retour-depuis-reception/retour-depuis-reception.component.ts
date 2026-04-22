import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { IOrderLine } from '../../../../shared/model/order-line.model';
import { ICommande } from '../../../../shared/model/commande.model';
import { IRetourBon } from '../../../../shared/model/retour-bon.model';
import { IRetourBonItem } from '../../../../shared/model/retour-bon-item.model';
import { RetourBonService } from '../../../../entities/commande/retour_fournisseur/retour-bon.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { Tooltip } from "primeng/tooltip";

interface RetourLine {
  orderLine: IOrderLine;
  qtyRetour: number;
  maxQty: number;
}

@Component({
  selector: 'app-retour-depuis-reception',
  templateUrl: './retour-depuis-reception.component.html',
  styleUrls: ['./retour-depuis-reception.component.scss'],
  imports: [CommonModule, FormsModule, ButtonModule, InputNumberModule, Tooltip]
})
export class RetourDepuisReceptionComponent implements OnInit {
  commande!: ICommande;
  orderLines: IOrderLine[] = [];
  header = 'Retour fournisseur';

  protected retourLines: RetourLine[] = [];
  protected commentaire = '';
  protected saving = false;

  private readonly activeModal = inject(NgbActiveModal);
  private readonly retourBonService = inject(RetourBonService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.retourLines = this.orderLines
      .filter(l => (l.quantityReceived ?? 0) > 0)
      .map(l => ({
        orderLine: l,
        qtyRetour: 0,
        maxQty: l.quantityReceived ?? 0,
      }));
  }

  protected get hasSelection(): boolean {
    return this.retourLines.some(r => r.qtyRetour > 0);
  }

  protected get totalLignes(): number {
    return this.retourLines.filter(r => r.qtyRetour > 0).length;
  }

  protected get totalUnites(): number {
    return this.retourLines.reduce((s, r) => s + r.qtyRetour, 0);
  }

  protected onSelectAll(): void {
    this.retourLines.forEach(r => (r.qtyRetour = r.maxQty));
  }

  protected onReset(): void {
    this.retourLines.forEach(r => (r.qtyRetour = 0));
  }

  protected onSubmit(): void {
    const selected = this.retourLines.filter(r => r.qtyRetour > 0);
    if (selected.length === 0) return;

    const items: IRetourBonItem[] = selected.map(r => ({
      orderLineId: r.orderLine.id,
      orderLineOrderDate: r.orderLine.orderLineId?.orderDate,
      qtyMvt: r.qtyRetour,
      prixAchat: r.orderLine.costAmount ?? r.orderLine.orderCostAmount,
      produitId: r.orderLine.produitId,
      produitLibelle: r.orderLine.produitLibelle,
      produitCip: r.orderLine.produitCip,
    }));

    const retourBon: IRetourBon = {
      commandeId: this.commande.commandeId?.id,
      commandeOrderDate: this.commande.commandeId?.orderDate,
      commandeOrderReference: this.commande.orderReference,
      receiptReference: this.commande.receiptReference,
      fournisseurLibelle: this.commande.fournisseur?.libelle,
      commentaire: this.commentaire || undefined,
      retourBonItems: items,
    };

    this.saving = true;
    this.retourBonService.create(retourBon).subscribe({
      next: res => {
        this.saving = false;
        this.notificationService.success(
          `Avoir #${res.body!.id} créé — ${selected.length} ligne(s), ${this.totalUnites} unité(s)`,
          'Retour fournisseur',
        );
        this.activeModal.close(res.body);
      },
      error: err => {
        this.saving = false;
        this.notificationService.error(this.errorService.getErrorMessage(err), 'Retour fournisseur');
      },
    });
  }

  protected onCancel(): void {
    this.activeModal.dismiss();
  }
}
