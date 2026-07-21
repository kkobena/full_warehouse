import { Component, computed, inject, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonComponent } from "../../../../../shared/ui";
import { IOrderLine } from "../../../../../shared/model/order-line.model";

@Component({
  selector: "app-reception-finalize-modal",
  templateUrl: "./reception-finalize-modal.component.html",
  styleUrls: ["./reception-finalize-modal.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, ButtonComponent]
})
export class ReceptionFinalizeModalComponent {
  readonly activeModal = inject(NgbActiveModal);
  orderLines: IOrderLine[] = [];
  commandeRef = "";
  fournisseurLibelle = "";
  /** AX-23g — Lignes dont le CIP a été mis à jour pendant cette réception. */
  updatedCipLines: IOrderLine[] = [];

  protected get totalLines(): number { return this.orderLines.length; }

  protected get processedLines(): number {
    return this.orderLines.filter(l => {
      const rec = l.quantityReceivedTmp ?? l.quantityReceived ?? 0;
      return rec > 0 && rec >= (l.quantityRequested ?? 0);
    }).length;
  }

  protected get skippedLines(): number { return this.totalLines - this.processedLines; }

  protected get totalAmount(): number {
    return this.orderLines.reduce((sum, l) => {
      const qty = l.quantityReceivedTmp ?? l.quantityReceived ?? 0;
      return sum + qty * (l.orderCostAmount ?? 0);
    }, 0);
  }

  protected get totalUg(): number {
    return this.orderLines.reduce((sum, l) => sum + (l.freeQty ?? 0), 0);
  }

  // AX-20 — Récapitulatif TVA par taux
  protected get tvaGroups(): { taux: number; montantHT: number; montantTVA: number }[] {
    const map = new Map<number, { montantHT: number; montantTVA: number }>();
    for (const l of this.orderLines) {
      const taux = l.tva ?? 0;
      const qty = l.quantityReceivedTmp ?? l.quantityReceived ?? 0;
      const ht = qty * (l.orderCostAmount ?? 0);
      const tva = l.taxAmount ?? 0;
      const cur = map.get(taux) ?? { montantHT: 0, montantTVA: 0 };
      map.set(taux, { montantHT: cur.montantHT + ht, montantTVA: cur.montantTVA + tva });
    }
    return Array.from(map.entries())
      .map(([taux, vals]) => ({ taux, ...vals }))
      .sort((a, b) => a.taux - b.taux);
  }

  protected get ecartColisage(): number {
    return this.orderLines.filter(l => {
      const pcb = l.qteColis;
      if (!pcb || pcb <= 1) return false;
      const qty = l.quantityReceivedTmp ?? l.quantityReceived ?? 0;
      return qty > 0 && qty % pcb !== 0;
    }).length;
  }

  protected get hasSkipped(): boolean { return this.skippedLines > 0; }

  protected onFinalize(): void {
    this.activeModal.close("finalize");
  }

  protected onContinue(): void {
    this.activeModal.dismiss();
  }
}
