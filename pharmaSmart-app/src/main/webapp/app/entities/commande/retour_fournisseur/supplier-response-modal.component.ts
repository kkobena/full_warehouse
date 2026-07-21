import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbActiveModal, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { IRetourBon } from "app/shared/model/retour-bon.model";
import { IRetourBonItem } from "app/shared/model/retour-bon-item.model";
import { AvoirFournisseurService } from "app/entities/commande/retour_fournisseur/avoir-fournisseur.service";
import { IAvoirLigneCommand } from "app/shared/model/avoir-fournisseur.model";
import { NotificationService } from "../../../shared/services/notification.service";
import { ButtonComponent, DataTableComponent, EditableCellComponent, InputNumberComponent } from "../../../shared/ui";

interface ResponseLine {
  item: IRetourBonItem;
  qtyAcceptee: number;
  prixAchat: number;
  alreadyProcessed: boolean;
}

@Component({
  selector: "app-supplier-response-modal",
  imports: [CommonModule, FormsModule, ButtonComponent, DataTableComponent, InputNumberComponent, EditableCellComponent, NgbTooltip],
  templateUrl: "./supplier-response-modal.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./supplier-response-modal.component.scss"
})
export class SupplierResponseModalComponent implements OnInit {
  protected readonly activeModal = inject(NgbActiveModal);
  private readonly notificationService = inject(NotificationService);
  private readonly avoirFournisseurService = inject(AvoirFournisseurService);

  retourBon: IRetourBon | null = null;
  title = "Saisir la réponse fournisseur";

  protected lines = signal<ResponseLine[]>([]);
  protected isSaving = signal(false);

  ngOnInit(): void {
    if (this.retourBon?.retourBonItems) {
      this.lines.set(
        this.retourBon.retourBonItems.map(item => ({
          item,
          qtyAcceptee: item.acceptedQty ?? item.qtyMvt ?? 0,
          prixAchat: item.prixAchat ?? 0,
          alreadyProcessed: (item.acceptedQty ?? 0) > 0 && item.acceptedQty === item.qtyMvt
        }))
      );
    }
  }

  protected onQuantityChange(line: ResponseLine): void {
    const max = line.item.qtyMvt ?? 0;
    if (line.qtyAcceptee > max) {
      line.qtyAcceptee = max;
      this.notificationService.warning("Attention", `La quantité acceptée ne peut pas dépasser ${max}`);
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
    if (line.qtyAcceptee === 0) return "rejected-row";
    if (line.item.qtyMvt && line.qtyAcceptee < line.item.qtyMvt) return "partial-row";
    return "accepted-row";
  }

  protected save(): void {
    if (!this.canSave() || !this.retourBon?.id) return;
    this.isSaving.set(true);

    const lignes: IAvoirLigneCommand[] = this.lines()
      .filter(l => !l.alreadyProcessed)
      .map(l => ({
        retourBonItemId: l.item.id!,
        qtyAcceptee: l.qtyAcceptee,
        prixAchat: l.prixAchat || undefined
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
          this.notificationService.error("Une erreur est survenue lors de la création de l'avoir fournisseur.");
        }
      });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
