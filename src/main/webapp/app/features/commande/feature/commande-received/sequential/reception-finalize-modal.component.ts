import { Component, computed, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { IOrderLine } from "../../../../../shared/model/order-line.model";

@Component({
  selector: "app-reception-finalize-modal",
  templateUrl: "./reception-finalize-modal.component.html",
  styleUrls: ["./reception-finalize-modal.component.scss"],
  imports: [CommonModule, ButtonModule]
})
export class ReceptionFinalizeModalComponent {
  readonly activeModal = inject(NgbActiveModal);

  // Propriétés publiques — renseignées par l'appelant après modalService.open()
  orderLines: IOrderLine[] = [];
  commandeRef = "";
  fournisseurLibelle = "";

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

  protected get hasSkipped(): boolean { return this.skippedLines > 0; }

  protected onFinalize(): void {
    this.activeModal.close("finalize");
  }

  protected onContinue(): void {
    this.activeModal.dismiss();
  }
}
