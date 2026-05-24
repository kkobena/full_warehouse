import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { IRetourBon } from 'app/shared/model/retour-bon.model';
import { IRetourBonItem } from 'app/shared/model/retour-bon-item.model';
import { AvoirFournisseurService } from 'app/entities/commande/retour_fournisseur/avoir-fournisseur.service';
import { IAvoirLigneCommand } from 'app/shared/model/avoir-fournisseur.model';
import { Tooltip } from 'primeng/tooltip';

interface ResponseLine {
  item: IRetourBonItem;
  qtyAcceptee: number;
  prixAchat: number;
  alreadyProcessed: boolean;
}

@Component({
  selector: 'jhi-supplier-response-modal',
  imports: [CommonModule, FormsModule, ButtonModule, TableModule, InputNumberModule, ToastModule, WarehouseCommonModule, Tooltip],
  providers: [MessageService],
  templateUrl: './supplier-response-modal.component.html',
  styleUrl: './supplier-response-modal.component.scss',
})
export class SupplierResponseModalComponent implements OnInit {
  protected readonly activeModal = inject(NgbActiveModal);
  protected readonly messageService = inject(MessageService);
  private readonly avoirFournisseurService = inject(AvoirFournisseurService);

  retourBon: IRetourBon | null = null;
  title = 'Saisir la réponse fournisseur';

  protected lines = signal<ResponseLine[]>([]);
  protected isSaving = signal(false);

  ngOnInit(): void {
    if (this.retourBon?.retourBonItems) {
      this.lines.set(
        this.retourBon.retourBonItems.map(item => ({
          item,
          qtyAcceptee: item.acceptedQty ?? item.qtyMvt ?? 0,
          prixAchat: item.prixAchat ?? 0,
          alreadyProcessed: (item.acceptedQty ?? 0) > 0 && item.acceptedQty === item.qtyMvt,
        })),
      );
    }
  }

  protected onQuantityChange(line: ResponseLine): void {
    const max = line.item.qtyMvt ?? 0;
    if (line.qtyAcceptee > max) {
      line.qtyAcceptee = max;
      this.messageService.add({
        severity: 'warn',
        summary: 'Attention',
        detail: `La quantité acceptée ne peut pas dépasser ${max}`,
      });
    }
    if (line.qtyAcceptee < 0) {
      line.qtyAcceptee = 0;
    }
  }

  protected canSave(): boolean {
    return this.lines().length > 0 && !this.isSaving();
  }

  protected getTotalRequested(): number {
    return this.lines().reduce((s, l) => s + (l.item.qtyMvt ?? 0), 0);
  }

  protected getTotalAccepted(): number {
    return this.lines().reduce((s, l) => s + l.qtyAcceptee, 0);
  }

  protected getRowClass(line: ResponseLine): string {
    if (line.qtyAcceptee === 0) return 'rejected-row';
    if (line.item.qtyMvt && line.qtyAcceptee < line.item.qtyMvt) return 'partial-row';
    return 'accepted-row';
  }

  protected save(): void {
    if (!this.canSave() || !this.retourBon?.id) return;
    this.isSaving.set(true);

    const lignes: IAvoirLigneCommand[] = this.lines()
      .filter(l => !l.alreadyProcessed)
      .map(l => ({
        retourBonItemId: l.item.id!,
        qtyAcceptee: l.qtyAcceptee,
        prixAchat: l.prixAchat || undefined,
      }));

    this.avoirFournisseurService
      .create({ retourBonId: this.retourBon.id, lignes })
      .subscribe({
        next: avoir => {
          this.isSaving.set(false);
          this.activeModal.close(avoir);
        },
        error: () => {
          this.isSaving.set(false);
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: "Erreur lors de la création de l'avoir" });
        },
      });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
