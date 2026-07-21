import { Component, inject, OnInit, viewChild, AfterViewInit, ElementRef, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { NotificationService } from '../../../shared/services/notification.service';
import { ErrorService } from '../../../shared/error.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { IProduit } from "../../../shared/model";
import { IStockProduit } from '../../../shared/model/stock-produit.model';
import { RepartitionStockService } from '../../repartition-stock/repartition-stock.service';
import { ButtonComponent, CardComponent, KeyFilterDirective } from '../../../shared/ui';

@Component({
  selector: 'jhi-form-transfert-stock',
  imports: [FormsModule, ReactiveFormsModule, ButtonComponent, CardComponent, KeyFilterDirective],
  templateUrl: './form-transfert-stock.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './form-transfert-stock.component.scss',
})
export class FormTransfertStockComponent implements OnInit, AfterViewInit {
  produit?: IProduit;
  stockProduitSrc?: IStockProduit;
  stockProduitDest?: IStockProduit;
  isSaving = false;
  maxTransferableQty = 0;
  isCreatingNewReserve = false;

  fb = inject(UntypedFormBuilder);
  editForm = this.fb.group({
    quantity: [null, [Validators.required, Validators.min(1)]],
    seuilMini: [null, [Validators.min(0)]],
  });

  private readonly activeModal = inject(NgbActiveModal);
  private readonly repartitionStockService = inject(RepartitionStockService);
  private readonly errorService = inject(ErrorService);
  private readonly notificationService = inject(NotificationService);
  private readonly seuilMiniInput = viewChild<ElementRef>('seuilMiniInput');
  private readonly quantityInput = viewChild<ElementRef>('quantityInput');

  ngOnInit(): void {
    this.initDestination();
    this.calculateMaxTransferableQty();

    // Update validators based on context
    this.editForm.get('quantity')!.setValidators([Validators.required, Validators.min(1), Validators.max(this.maxTransferableQty)]);

    // Make seuilMini required if creating new reserve
    if (this.isCreatingNewReserve) {
      this.editForm.get('seuilMini')!.setValidators([Validators.required, Validators.min(0)]);
    }
  }

  ngAfterViewInit(): void {
    // Set focus on the appropriate field
    setTimeout(() => {
      if (this.isCreatingNewReserve && this.seuilMiniInput()) {
        this.seuilMiniInput()!.nativeElement.focus();
      } else if (this.quantityInput()) {
        this.quantityInput()!.nativeElement.focus();
      }
    }, 100);
  }

  get title(): string {
    const sourceLabel = this.stockProduitSrc ? this.getStorageDisplayName(this.stockProduitSrc) : 'Source inconnue';
    let destLabel = '';

    if (this.isCreatingNewReserve) {
      destLabel = 'Nouvelle Réserve';
    } else if (this.stockProduitDest) {
      destLabel = this.getStorageDisplayName(this.stockProduitDest);
    } else {
      destLabel = 'Destination inconnue';
    }

    return `Transfert de stock : ${sourceLabel} → ${destLabel}`;
  }

  isFormValid(): boolean {
    if (this.isSaving || !this.editForm.valid) {
      return false;
    }

    const quantity = this.editForm.get('quantity')!.value;
    const hasValidQuantity = quantity > 0 && quantity <= this.maxTransferableQty;

    if (this.isCreatingNewReserve) {
      const seuilMini = this.editForm.get('seuilMini')!.value;
      return hasValidQuantity && seuilMini !== null && seuilMini >= 0;
    }

    return hasValidQuantity;
  }

  save(): void {
    if (!this.isFormValid()) {
      return;
    }

    this.isSaving = true;
    const quantity = this.editForm.get('quantity')!.value;
    const seuilMini = this.isCreatingNewReserve ? this.editForm.get('seuilMini')!.value : undefined;

    const request = {
      stockSourceId: this.stockProduitSrc ? this.stockProduitSrc.id : undefined,
      stockDestinationId: this.isCreatingNewReserve ? null : this.stockProduitDest ? this.stockProduitDest.id : null,
      quantity,
      seuilMini,
    };

    this.repartitionStockService
      .processManualRepartition(request)
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: () => this.onSaveSuccess(),
        error: (error: HttpErrorResponse) => this.onSaveError(error),
      });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  getStorageDisplayName(stockProduit: IStockProduit): string {
    return stockProduit.storageName || stockProduit.storageType || 'Inconnu';
  }

  private initDestination(): void {
    const stockProduits = this.produit ? this.produit.stockProduits : undefined;
    if (stockProduits && this.stockProduitSrc) {
      const srcId = this.stockProduitSrc.id;

      // Si 2 stocks: la destination est automatiquement l'autre stock
      if (stockProduits.length === 2) {
        this.stockProduitDest = stockProduits.find(sp => sp.id !== srcId);
        this.isCreatingNewReserve = false;
      } else if (stockProduits.length === 1) {
        // Si 1 seul stock: on crée une nouvelle réserve
        this.stockProduitDest = undefined;
        this.isCreatingNewReserve = true;
      }
    }
  }

  private calculateMaxTransferableQty(): void {
    if (this.stockProduitSrc) {
      const qtyStock = this.stockProduitSrc.qtyStock || 0;
      const type = this.stockProduitSrc.type;

      // Si la source est SAFETY_STOCK (Réserve), on peut transférer tout le stock
      // Si la source est PRINCIPAL (Rayon), on doit respecter le seuil mini
      if (type === 'SAFETY_STOCK') {
        this.maxTransferableQty = qtyStock;
      } else {
        const seuilMini = this.stockProduitSrc.seuilMini || 0;
        this.maxTransferableQty = Math.max(0, qtyStock - seuilMini);
      }
    }
  }

  private onSaveSuccess(): void {
    this.activeModal.close('saved');
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }
}
