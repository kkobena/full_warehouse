import { Component, inject, OnInit, viewChild, AfterViewInit, ElementRef } from '@angular/core';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { InputText } from 'primeng/inputtext';
import { KeyFilter } from 'primeng/keyfilter';
import { Checkbox } from 'primeng/checkbox';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../../shared/error.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { IProduit } from '../../../shared/model/produit.model';
import { IStockProduit } from '../../../shared/model/stock-produit.model';
import { StockProduitService } from './stock-produit.service';
import { RepartitionStockService } from '../../repartition-stock/repartition-stock.service';

@Component({
  selector: 'jhi-form-stock-produit',
  imports: [FormsModule, ReactiveFormsModule, Button, Card, InputText, KeyFilter, Checkbox, ToastAlertComponent],
  templateUrl: './form-stock-produit.component.html',
  styleUrl: './form-stock-produit.component.scss',
})
export class FormStockProduitComponent implements OnInit, AfterViewInit {
  produit?: IProduit;
  stockProduit?: IStockProduit;
  isSaving = false;
  isEditMode = false;
  withTransfer = false;
  maxTransferableQty = 0;
  stockRayonProduit?: IStockProduit;

  fb = inject(UntypedFormBuilder);
  editForm = this.fb.group({
    seuilMini: [null, [Validators.required, Validators.min(0)]],
    stockReassort: [null, [Validators.min(0)]],
    stockMaxi: [null],
    withTransfer: [false],
    transferQuantity: [null, [Validators.min(1)]],
  });

  private readonly activeModal = inject(NgbActiveModal);
  private readonly stockProduitService = inject(StockProduitService);
  private readonly repartitionStockService = inject(RepartitionStockService);
  private readonly errorService = inject(ErrorService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly seuilMiniInput = viewChild<ElementRef>('seuilMiniInput');

  //  private readonly stockReassortInput = viewChild<ElementRef>('stockReassortInput');

  ngOnInit(): void {
    this.isEditMode = !!this.stockProduit?.id;

    if (this.isEditMode) {
      this.editForm.patchValue({
        seuilMini: this.stockProduit!.seuilMini,
        stockReassort: this.stockProduit!.stockReassort,
        stockMaxi: this.stockProduit!.stockMaxi,
      });
      this.editForm.get('withTransfer')!.disable();
      this.editForm.get('transferQuantity')!.disable();
    } else {
      this.findStockRayon();
      this.setupTransferWatcher();
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.seuilMiniInput()) {
        this.seuilMiniInput()!.nativeElement.focus();
      }
    }, 100);
  }

  get title(): string {
    if (this.isEditMode) {
      const stockType = this.stockProduit?.storageType || 'Stock';
      return `Modifier ${stockType}`;
    }
    return 'Créer Stock Réserve';
  }

  get isStockRayon(): boolean {
    return this.stockProduit?.type === 'PRINCIPAL';
  }

  isFormValid(): boolean {
    if (this.isSaving || !this.editForm.valid) {
      return false;
    }

    const seuilMini = this.editForm.get('seuilMini')!.value;
    if (seuilMini === null || seuilMini < 0) {
      return false;
    }

    if (!this.isEditMode && this.withTransfer) {
      const transferQuantity = this.editForm.get('transferQuantity')!.value;
      return transferQuantity > 0 && transferQuantity <= this.maxTransferableQty;
    }

    return true;
  }

  save(): void {
    if (!this.isFormValid()) {
      return;
    }

    this.isSaving = true;

    if (this.isEditMode) {
      this.updateStockProduit();
    } else {
      this.createStockProduit();
    }
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  private setupTransferWatcher(): void {
    this.editForm.get('withTransfer')!.valueChanges.subscribe(value => {
      this.withTransfer = value;
      if (value) {
        this.editForm
          .get('transferQuantity')!
          .setValidators([Validators.required, Validators.min(1), Validators.max(this.maxTransferableQty)]);
        this.editForm.get('transferQuantity')!.enable();
      } else {
        this.editForm.get('transferQuantity')!.clearValidators();
        this.editForm.get('transferQuantity')!.setValue(null);
        this.editForm.get('transferQuantity')!.disable();
      }
      this.editForm.get('transferQuantity')!.updateValueAndValidity();
    });
  }

  private findStockRayon(): void {
    const stockProduits = this.produit?.stockProduits || [];
    this.stockRayonProduit = stockProduits.find(sp => sp.type === 'PRINCIPAL');

    if (this.stockRayonProduit) {
      const qtyStock = this.stockRayonProduit.qtyStock || 0;
      const seuilMini = this.stockRayonProduit.seuilMini || 0;
      this.maxTransferableQty = Math.max(0, qtyStock - seuilMini);
    }
  }

  private createStockProduit(): void {
    const seuilMini = this.editForm.get('seuilMini')!.value;
    const stockReassort = this.editForm.get('stockReassort')!.value;
    let transferQuantity = 0;
    if (this.withTransfer && this.stockRayonProduit) {
      transferQuantity = this.editForm.get('transferQuantity')!.value;
    }

    const newStock: IStockProduit = {
      produitId: this.produit!.id,
      seuilMini,
      stockReassort: stockReassort || 0,
      qtyStock: transferQuantity || 0,
      qtyVirtual: 0,
      qtyUG: 0,
      withTransfer: this.withTransfer && this.stockRayonProduit && transferQuantity > 0,
    };

    this.stockProduitService
      .create(newStock)
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: response => {
          const createdStock = response.body!;

          this.onSaveSuccess([createdStock]);
        },
        error: (error: HttpErrorResponse) => this.onSaveError(error),
      });
  }

  private executeTransfer(newReserveStock: IStockProduit): void {
    const transferQuantity = this.editForm.get('transferQuantity')!.value;

    const transferRequest = {
      stockSourceId: this.stockRayonProduit!.id!,
      stockDestinationId: newReserveStock.id!,
      quantity: transferQuantity,
    };

    this.repartitionStockService.processManualRepartition(transferRequest).subscribe({
      next: () => {
        this.onSaveSuccess();
      },
      error: (error: HttpErrorResponse) => {
        this.alert().showError(`Stock créé mais transfert échoué: ${this.errorService.getErrorMessage(error)}`);
        this.onSaveSuccess([newReserveStock]);
      },
    });
  }

  private updateStockProduit(): void {
    const updatedStock: IStockProduit = {
      ...this.stockProduit,
      seuilMini: this.editForm.get('seuilMini')!.value,
      stockReassort: this.editForm.get('stockReassort')!.value,
      stockMaxi: this.editForm.get('stockMaxi')!.value,
    };

    this.stockProduitService
      .update(updatedStock)
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: response => this.onSaveSuccess([response.body!]),
        error: (error: HttpErrorResponse) => this.onSaveError(error),
      });
  }

  private onSaveSuccess(updatedStocks?: IStockProduit[]): void {
    this.activeModal.close(updatedStocks || 'saved');
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
  }
}
