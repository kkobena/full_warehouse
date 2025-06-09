import { Component, inject, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ProduitService } from './produit.service';
import { IProduit, Produit } from '../../shared/model/produit.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { TypeProduit } from '../../shared/model/enumerations/type-produit.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Button } from 'primeng/button';

@Component({
  selector: 'jhi-detail-form-dialog',
  templateUrl: './detail-form-dialog.component.html',
  imports: [WarehouseCommonModule, ReactiveFormsModule, FormsModule, InputText, InputNumber, Button],
})
export class DetailFormDialogComponent implements OnInit {
  produit?: Produit;
  entity?: Produit;
  protected activeModal = inject(NgbActiveModal);
  protected isSaving = false;
  protected isValid = true;
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
    costAmount: [null, [Validators.required]],
    regularUnitPrice: [null, [Validators.required]],
  });
  private readonly produitService = inject(ProduitService);

  ngOnInit(): void {
    if (this.entity !== null && this.entity !== undefined) {
      this.updateForm(this.entity);
    }
  }

  updateForm(produit: IProduit): void {
    this.editForm.patchValue({
      id: produit.id,
      libelle: produit.libelle,
      costAmount: produit.costAmount,
      regularUnitPrice: produit.regularUnitPrice,
    });
  }

  save(): void {
    this.isSaving = true;
    const produit = this.createFromForm();
    if (produit.id !== undefined && produit.id !== null) {
      this.subscribeToSaveResponse(this.produitService.updateDetail(produit));
    } else {
      this.subscribeToSaveResponse(this.produitService.create(produit));
    }
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IProduit>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;

    this.activeModal.close();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  private createFromForm(): IProduit {
    return {
      ...new Produit(),
      id: this.editForm.get(['id']).value,
      libelle: this.editForm.get(['libelle']).value,
      costAmount: this.editForm.get(['costAmount']).value,
      regularUnitPrice: this.editForm.get(['regularUnitPrice']).value,
      produitId: this.produit.id,
      typeProduit: TypeProduit.DETAIL,
      quantity: 0,
      netUnitPrice: 0,
      itemQty: 0,
      itemCostAmount: 0,
      itemRegularUnitPrice: 0,
    };
  }
}
