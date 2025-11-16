import { Component, EventEmitter, Input, OnChanges, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { ILot } from 'app/shared/model/lot.model';
import { Tooltip } from 'primeng/tooltip';

export interface InlineLotSelection {
  lot: ILot;
  selectedQuantity: number;
  maxQuantity: number;
}

@Component({
  selector: 'jhi-inline-lot-selection',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule, Tooltip],
  template: `
    <div class="inline-lot-selection">
      <!-- Summary Header -->
      <div class="lot-summary-header" (click)="toggleExpanded()">
        <div class="d-flex align-items-center gap-2">
          <i class="pi" [class.pi-chevron-right]="!isExpanded()" [class.pi-chevron-down]="isExpanded()"></i>
          <i class="pi pi-tag text-primary"></i>
          <span class="fw-bold">{{ lots.length }} lot(s) disponible(s)</span>
        </div>

        @if (!isExpanded() && hasSelection()) {
          <div class="selection-preview">
            <span class="badge bg-success">
              <i class="pi pi-check me-1"></i>
              {{ getTotalSelected() }} / {{ requestedQuantity }} sélectionné
            </span>
          </div>
        }
      </div>

      <!-- Expanded Lot Details -->
      @if (isExpanded()) {
        <div class="lot-details-container">
          <div class="lot-info-banner">
            <div>
              <strong>Quantité totale à retourner:</strong>
              <span class="badge bg-primary ms-2">{{ requestedQuantity | number }}</span>
            </div>
            <div
              class="total-indicator"
              [class.text-danger]="getTotalSelected() !== requestedQuantity"
              [class.text-success]="getTotalSelected() === requestedQuantity && getTotalSelected() > 0"
            >
              <strong>Total:</strong>
              <span class="fs-6 ms-1">{{ getTotalSelected() | number }}</span>
              @if (getTotalSelected() !== requestedQuantity) {
                <i class="pi pi-exclamation-triangle ms-2"></i>
              } @else if (getTotalSelected() > 0) {
                <i class="pi pi-check-circle ms-2"></i>
              }
            </div>
          </div>

          <div class="lots-grid">
            @for (selection of lotSelections(); track selection.lot.id) {
              <div class="lot-card" [class.lot-selected]="selection.selectedQuantity > 0">
                <div class="lot-card-header">
                  <div class="lot-number">
                    <i class="pi pi-tag me-1"></i>
                    <strong>{{ selection.lot.numLot || 'N/A' }}</strong>
                  </div>
                  @if (selection.selectedQuantity > 0) {
                    <span class="badge bg-success">Sélectionné</span>
                  }
                </div>

                <div class="lot-card-body">
                  <div class="lot-info-row">
                    <span class="label">
                      <i class="pi pi-calendar me-1"></i>
                      Expiration:
                    </span>
                    <span class="value">
                      @if (selection.lot.expiryDate) {
                        {{ selection.lot.expiryDate | date: 'dd/MM/yyyy' }}
                      } @else {
                        <span class="text-muted">N/A</span>
                      }
                    </span>
                  </div>

                  <div class="lot-info-row">
                    <span class="label">
                      <i class="pi pi-box me-1"></i>
                      Disponible:
                    </span>
                    <span class="value">
                      <span class="badge bg-info">{{ selection.maxQuantity | number }}</span>
                    </span>
                  </div>

                  <div class="quantity-input-row">
                    <label class="form-label mb-1">Quantité à retourner:</label>
                    <div class="quantity-controls">
                      <p-button
                        icon="pi pi-minus"
                        [rounded]="true"
                        [text]="true"
                        size="small"
                        [disabled]="selection.selectedQuantity <= 0"
                        (onClick)="decrementQuantity(selection)"
                        pTooltip="Diminuer"
                      />
                      <input
                        type="number"
                        [(ngModel)]="selection.selectedQuantity"
                        [min]="0"
                        [max]="selection.maxQuantity"
                        (ngModelChange)="onQuantityChange()"
                        (focus)="$event.target.select()"
                        class="form-control form-control-sm text-center quantity-input"
                      />
                      <p-button
                        icon="pi pi-plus"
                        [rounded]="true"
                        [text]="true"
                        size="small"
                        [disabled]="selection.selectedQuantity >= selection.maxQuantity"
                        (onClick)="incrementQuantity(selection)"
                        pTooltip="Augmenter"
                      />
                    </div>
                  </div>
                </div>

                <div class="lot-card-footer">
                  <p-button
                    label="Tout sélectionner"
                    [text]="true"
                    size="small"
                    severity="secondary"
                    icon="pi pi-check-square"
                    [disabled]="selection.selectedQuantity === selection.maxQuantity"
                    (onClick)="selectAll(selection)"
                  />
                  <p-button
                    label="Effacer"
                    [text]="true"
                    size="small"
                    severity="danger"
                    icon="pi pi-times"
                    [disabled]="selection.selectedQuantity === 0"
                    (onClick)="clearSelection(selection)"
                  />
                </div>
              </div>
            }
          </div>

          <div class="action-buttons">
            <p-button
              label="Auto-répartir (FEFO)"
              icon="pi pi-sync"
              [outlined]="true"
              size="small"
              severity="secondary"
              (onClick)="autoDistribute()"
              pTooltip="Répartition automatique - Premier Expiré Premier Sorti"
            />
            <div class="flex-grow-1"></div>
            <p-button label="Annuler" icon="pi pi-times" [outlined]="true" size="small" severity="secondary" (onClick)="onCancel()" />
            <p-button
              label="Confirmer la sélection"
              icon="pi pi-check"
              size="small"
              severity="primary"
              [disabled]="!isValid()"
              (onClick)="onConfirm()"
            />
          </div>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .inline-lot-selection {
        border: 1px solid #dee2e6;
        border-radius: 0.375rem;
        margin: 0.5rem 0;
        background: white;
      }

      .lot-summary-header {
        padding: 0.75rem 1rem;
        cursor: pointer;
        display: flex;
        justify-content: space-between;
        align-items: center;
        background: #f8f9fa;
        border-radius: 0.375rem 0.375rem 0 0;
        transition: background-color 0.2s;

        &:hover {
          background: #e9ecef;
        }
      }

      .selection-preview {
        .badge {
          font-size: 0.875rem;
          padding: 0.35rem 0.65rem;
        }
      }

      .lot-details-container {
        padding: 1rem;
        border-top: 1px solid #dee2e6;
      }

      .lot-info-banner {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem 1rem;
        background: #f8f9fa;
        border-radius: 0.375rem;
        margin-bottom: 1rem;
      }

      .total-indicator {
        font-size: 1.1rem;
      }

      .lots-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
        gap: 1rem;
        margin-bottom: 1rem;
      }

      .lot-card {
        border: 2px solid #dee2e6;
        border-radius: 0.5rem;
        overflow: hidden;
        transition: all 0.2s;
        background: white;

        &.lot-selected {
          border-color: #198754;
          box-shadow: 0 0 0 0.2rem rgba(25, 135, 84, 0.1);
        }

        &:hover {
          box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
        }
      }

      .lot-card-header {
        padding: 0.75rem 1rem;
        background: #f8f9fa;
        border-bottom: 1px solid #dee2e6;
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .lot-number {
        font-size: 1rem;
        color: #0d6efd;
      }

      .lot-card-body {
        padding: 1rem;
      }

      .lot-info-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.5rem;
        font-size: 0.875rem;

        .label {
          color: #6c757d;
        }

        .value {
          font-weight: 500;
        }
      }

      .quantity-input-row {
        margin-top: 1rem;
        padding-top: 1rem;
        border-top: 1px solid #dee2e6;

        .form-label {
          font-size: 0.875rem;
          font-weight: 500;
        }
      }

      .quantity-controls {
        display: flex;
        align-items: center;
        gap: 0.5rem;

        .quantity-input {
          width: 80px;
          font-size: 1rem;
          font-weight: 600;
        }
      }

      .lot-card-footer {
        padding: 0.75rem 1rem;
        background: #f8f9fa;
        border-top: 1px solid #dee2e6;
        display: flex;
        gap: 0.5rem;
        justify-content: space-between;
      }

      .action-buttons {
        display: flex;
        gap: 0.5rem;
        align-items: center;
        padding-top: 1rem;
        border-top: 1px solid #dee2e6;
      }

      .badge {
        padding: 0.35rem 0.65rem;
        font-size: 0.875rem;
        font-weight: 500;
      }
    `,
  ],
})
export class InlineLotSelectionComponent implements OnChanges {
  @Input() lots: ILot[] = [];
  @Input() requestedQuantity = 0;
  @Input() productLabel = '';
  @Input() expanded = false;

  @Output() lotsConfirmed = new EventEmitter<InlineLotSelection[]>();
  @Output() cancelled = new EventEmitter<void>();

  protected lotSelections = signal<InlineLotSelection[]>([]);
  protected isExpanded = signal<boolean>(false);

  ngOnChanges(): void {
    if (this.lots.length > 0) {
      this.initializeLotSelections();
    }
    this.isExpanded.set(this.expanded);
  }

  private initializeLotSelections(): void {
    const selections: InlineLotSelection[] = this.lots.map(lot => ({
      lot,
      selectedQuantity: 0,
      maxQuantity: lot.quantity || lot.freeQuantity || lot.quantityReceived || 0,
    }));

    this.lotSelections.set(selections);
  }

  protected toggleExpanded(): void {
    this.isExpanded.update(val => !val);
  }

  protected getTotalSelected(): number {
    return this.lotSelections().reduce((sum, sel) => sum + sel.selectedQuantity, 0);
  }

  protected hasSelection(): boolean {
    return this.getTotalSelected() > 0;
  }

  protected isValid(): boolean {
    const total = this.getTotalSelected();
    return total === this.requestedQuantity && total > 0;
  }

  protected onQuantityChange(): void {
    // Real-time validation
  }

  protected incrementQuantity(selection: InlineLotSelection): void {
    if (selection.selectedQuantity < selection.maxQuantity) {
      selection.selectedQuantity++;
      this.onQuantityChange();
    }
  }

  protected decrementQuantity(selection: InlineLotSelection): void {
    if (selection.selectedQuantity > 0) {
      selection.selectedQuantity--;
      this.onQuantityChange();
    }
  }

  protected selectAll(selection: InlineLotSelection): void {
    selection.selectedQuantity = selection.maxQuantity;
    this.onQuantityChange();
  }

  protected clearSelection(selection: InlineLotSelection): void {
    selection.selectedQuantity = 0;
    this.onQuantityChange();
  }

  protected autoDistribute(): void {
    // Reset all selections
    this.lotSelections().forEach(sel => (sel.selectedQuantity = 0));

    let remainingQuantity = this.requestedQuantity;

    // Sort by expiry date (FEFO - First Expired First Out)
    const sortedSelections = [...this.lotSelections()].sort((a, b) => {
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

    this.onQuantityChange();
  }

  protected onConfirm(): void {
    if (this.isValid()) {
      const selectedLots = this.lotSelections().filter(sel => sel.selectedQuantity > 0);
      this.lotsConfirmed.emit(selectedLots);
    }
  }

  protected onCancel(): void {
    this.cancelled.emit();
  }
}
