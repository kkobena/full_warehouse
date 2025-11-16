import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { InputNumberModule } from 'primeng/inputnumber';
import { ILot } from 'app/shared/model/lot.model';

export interface LotSelection {
  lot: ILot;
  selectedQuantity: number;
  maxQuantity: number;
}

@Component({
  selector: 'jhi-lot-selection-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule, TableModule, InputNumberModule],
  template: `
    <div class="modal-header">
      <h4 class="modal-title">
        <i class="pi pi-tag me-2"></i>
        Sélection des lots à retourner
      </h4>
      <button type="button" class="btn-close" aria-label="Close" (click)="onCancel()"></button>
    </div>

    <div class="modal-body">
      <div class="lot-selection-content">
        <div class="product-info mb-3 p-3 border rounded">
          <div class="mb-2"><strong>Produit:</strong> {{ productLabel }}</div>
          <div>
            <strong>Quantité totale à retourner:</strong>
            <span class="badge bg-primary ms-2">{{ requestedQuantity | number }}</span>
          </div>
        </div>

        <p-table [value]="lotSelections()" class="pharma-table">
          <ng-template #header>
            <tr class="pharma-table-head">
              <th>N° Lot</th>
              <th>Date d'expiration</th>
              <th class="text-end">Disponible</th>
              <th class="text-end" style="width: 150px">Quantité à retourner</th>
            </tr>
          </ng-template>
          <ng-template #body let-selection>
            <tr>
              <td>
                <span class="pharma-code">
                  <i class="pi pi-tag me-1"></i>
                  {{ selection.lot.numLot || 'N/A' }}
                </span>
              </td>
              <td>
                @if (selection.lot.expiryDate) {
                  {{ selection.lot.expiryDate | date: 'dd/MM/yyyy' }}
                } @else {
                  <span class="text-muted">Non définie</span>
                }
              </td>
              <td class="text-end">
                <span class="badge bg-info">{{ selection.maxQuantity | number }}</span>
              </td>
              <td class="text-end">
                <input
                  type="number"
                  [(ngModel)]="selection.selectedQuantity"
                  [min]="0"
                  [max]="selection.maxQuantity"
                  (ngModelChange)="onQuantityChange()"
                  (focus)="$event.target.select()"
                  class="form-control form-control-sm text-end"
                  style="width: 120px; display: inline-block;"
                />
              </td>
            </tr>
          </ng-template>
        </p-table>

        <div
          class="mt-3 p-3 border rounded d-flex justify-content-between align-items-center"
          [class.border-danger]="getTotalSelected() !== requestedQuantity"
          [class.border-success]="getTotalSelected() === requestedQuantity && getTotalSelected() > 0"
        >
          <div>
            <strong>Total sélectionné:</strong>
            <span
              class="ms-2 fs-5"
              [class.text-danger]="getTotalSelected() !== requestedQuantity"
              [class.text-success]="getTotalSelected() === requestedQuantity && getTotalSelected() > 0"
            >
              {{ getTotalSelected() | number }}
            </span>
            <span class="text-muted">/ {{ requestedQuantity }}</span>
          </div>

          @if (getTotalSelected() !== requestedQuantity) {
            <small class="text-warning">
              <i class="pi pi-exclamation-triangle me-1"></i>
              Le total doit correspondre à {{ requestedQuantity }}
            </small>
          } @else {
            <small class="text-success">
              <i class="pi pi-check-circle me-1"></i>
              Répartition correcte
            </small>
          }
        </div>
      </div>
    </div>

    <div class="modal-footer">
      <p-button label="Annuler" severity="secondary" [outlined]="true" icon="pi pi-times" (onClick)="onCancel()" />
      <p-button label="Confirmer" severity="primary" icon="pi pi-check" [disabled]="!isValid()" (onClick)="onConfirm()" />
    </div>
  `,
  styles: [
    `
      .lot-selection-content {
        padding: 0.5rem 0;
      }

      .dialog-footer {
        display: flex;
        gap: 0.5rem;
        justify-content: flex-end;
      }

      .product-info {
        background-color: #f8f9fa;
      }

      .badge {
        padding: 0.35rem 0.65rem;
        font-size: 0.875rem;
        font-weight: 500;
      }
    `,
  ],
})
export class LotSelectionDialogComponent implements OnInit {
  private readonly activeModal = inject(NgbActiveModal);

  @Input() lots: ILot[] = [];
  @Input() requestedQuantity = 0;
  @Input() productLabel = '';

  protected lotSelections = signal<LotSelection[]>([]);

  ngOnInit(): void {
    if (this.lots.length > 0) {
      this.initializeLotSelections();
    }
  }

  private initializeLotSelections(): void {
    const selections: LotSelection[] = this.lots.map(lot => ({
      lot,
      selectedQuantity: 0,
      maxQuantity: lot.quantity || lot.freeQuantity || lot.quantityReceived || 0,
    }));

    // Auto-fill from first lot if possible (FIFO approach)
    if (selections.length > 0 && this.requestedQuantity > 0) {
      let remainingQuantity = this.requestedQuantity;

      // Sort by expiry date (oldest first) for FEFO (First Expired First Out)
      const sortedSelections = [...selections].sort((a, b) => {
        if (!a.lot.expiryDate) return 1;
        if (!b.lot.expiryDate) return -1;
        return new Date(a.lot.expiryDate).getTime() - new Date(b.lot.expiryDate).getTime();
      });

      // Auto-distribute quantities
      for (const selection of sortedSelections) {
        if (remainingQuantity <= 0) break;

        const qtyToAssign = Math.min(remainingQuantity, selection.maxQuantity);
        selection.selectedQuantity = qtyToAssign;
        remainingQuantity -= qtyToAssign;
      }
    }

    this.lotSelections.set(selections);
  }

  protected getTotalSelected(): number {
    return this.lotSelections().reduce((sum, sel) => sum + sel.selectedQuantity, 0);
  }

  protected isValid(): boolean {
    const total = this.getTotalSelected();
    return total === this.requestedQuantity && total > 0;
  }

  protected onQuantityChange(): void {
    // Real-time validation happens through the template bindings
  }

  protected onConfirm(): void {
    if (this.isValid()) {
      const selectedLots = this.lotSelections().filter(sel => sel.selectedQuantity > 0);
      this.activeModal.close(selectedLots);
    }
  }

  protected onCancel(): void {
    this.activeModal.dismiss();
  }
}
