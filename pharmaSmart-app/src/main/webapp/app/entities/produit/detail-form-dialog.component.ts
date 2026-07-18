import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ProduitService } from './produit.service';
import { IProduit, Produit } from '../../shared/model/produit.model';
import { Observable } from 'rxjs';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { TypeProduit } from '../../shared/model/enumerations/type-produit.model';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { ErrorService } from '../../shared/error.service';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { CommonModule } from "@angular/common";

@Component({
  selector: 'jhi-detail-form-dialog',
  templateUrl: './detail-form-dialog.component.html',
  styleUrls: ['./detail-form-dialog.scss'],
  imports: [CommonModule, ReactiveFormsModule, FormsModule, InputText, InputNumber, Button, Card, ToastAlertComponent],
})
export class DetailFormDialogComponent implements OnInit, AfterViewInit {
  produit?: Produit;
  entity?: Produit;
  protected activeModal = inject(NgbActiveModal);
  protected isSaving = false;
  protected isValid = true;
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
    itemQty: [null, [Validators.required, Validators.min(2)]],
    costAmount: [null, [Validators.required, Validators.min(0)]],
    regularUnitPrice: [null, [Validators.required, Validators.min(0)]],
  });
  private readonly produitService = inject(ProduitService);
  private itemQty = viewChild.required<InputNumber>('itemQty');
  private readonly errorService = inject(ErrorService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');

  ngOnInit(): void {
    if (this.entity) {
      this.updateDetailForm(this.entity);
    } else {
      this.updateForm(this.produit);
    }

  }

  ngAfterViewInit(): void {
    if (!this.entity) {
      this.handleInitAmount(null);
    }
    setTimeout(() => {
      this.itemQty()!.input()?.nativeElement.focus();
    }, 100);

    this.editForm.get('itemQty').valueChanges.subscribe(value => {
      if (value) {
        this.handleInitAmount(value);
      }
    });
  }

  protected save(): void {
    this.isSaving = true;
    const produit = this.createFromForm();
    if (produit.id !== undefined && produit.id !== null) {
      this.subscribeToSaveResponse(this.produitService.updateDetail(produit));
    } else {
      this.subscribeToSaveResponse(this.produitService.create(produit));
    }
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IProduit>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: (error: HttpErrorResponse) => this.onSaveError(error),
    });
  }

  private onSaveSuccess(): void {
    this.isSaving = false;
    this.activeModal.close('saved');
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): IProduit {
    return {
      ...new Produit(),
      id: this.editForm.get(['id']).value,
      itemQty: this.editForm.get(['itemQty']).value,
      libelle: this.editForm.get(['libelle']).value,
      costAmount: this.editForm.get(['costAmount']).value,
      regularUnitPrice: this.editForm.get(['regularUnitPrice']).value,
      produitId: this.produit.id,
      typeProduit: TypeProduit.DETAIL,
      quantity: 0,
      netUnitPrice: 0,
      itemCostAmount: 0,
      itemRegularUnitPrice: 0,
    };
  }

  private handleInitAmount(qty: number): void {
    const itemQty = qty || this.produit.itemQty;
    if (Number(itemQty) > 0) {
      const itemCostAmount = this.produit.costAmount / itemQty;
      const itemRegularUnitPrice = this.produit.regularUnitPrice / itemQty;
      this.editForm.get(['costAmount']).setValue(itemCostAmount.toFixed());
      this.editForm.get(['regularUnitPrice']).setValue(itemRegularUnitPrice.toFixed());
    } else {
      this.editForm.get(['costAmount']).setValue(null);
      this.editForm.get(['regularUnitPrice']).setValue(null);
    }
  }

  private updateDetailForm(produit: IProduit): void {
    this.editForm.patchValue({
      id: produit.id,
      libelle: produit.libelle,
      costAmount: produit.costAmount,
      regularUnitPrice: produit.regularUnitPrice,
      itemQty: this.produit?.itemQty,
    });
  }

  private updateForm(produit: IProduit): void {
    this.editForm.patchValue({
      libelle: produit.libelle + '-DET',
      costAmount: produit.itemCostAmount,
      regularUnitPrice: produit.itemRegularUnitPrice,
      itemQty: produit.itemQty,
    });
  }
}
