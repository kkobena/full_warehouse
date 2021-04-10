import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ProduitService } from './produit.service';
import { IProduit, Produit } from '../../shared/model/produit.model';
import { DATE_TIME_FORMAT } from '../../shared/constants/input.constants';
import * as moment from 'moment';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';
import { TypeProduit } from '../../shared/model/enumerations/type-produit.model';

@Component({
  selector: 'jhi-detail-form-dialog',
  templateUrl: './detail-form-dialog.component.html',
})
export class DetailFormDialogComponent implements OnInit {
  isSaving = false;
  isValid = true;
  produit?: Produit;
  entity?: Produit;
  editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
    costAmount: [null, [Validators.required]],
    regularUnitPrice: [null, [Validators.required]],
    createdAt: [],
  });

  constructor(
    private fb: FormBuilder,
    protected produitService: ProduitService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager
  ) {}

  ngOnInit(): void {
    if (this.entity !== null && this.entity !== undefined) {
      this.updateForm(this.entity);
    } else {
      this.updateFormWithParentData(this.produit!);
    }
  }

  private createFromForm(): IProduit {
    return {
      ...new Produit(),
      id: this.editForm.get(['id'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
      costAmount: this.editForm.get(['costAmount'])!.value,
      regularUnitPrice: this.editForm.get(['regularUnitPrice'])!.value,
      createdAt: this.editForm.get(['createdAt'])!.value ? moment(this.editForm.get(['createdAt'])!.value, DATE_TIME_FORMAT) : undefined,
      produitId: this.produit?.id,
      typeProduit: TypeProduit.DETAIL,
      quantity: 0,
      netUnitPrice: 0,
      itemQty: 0,
      itemCostAmount: 0,
      itemRegularUnitPrice: 0,
    };
  }

  updateFormWithParentData(parent: IProduit): void {
    this.editForm.patchValue({
      costAmount: parent.itemCostAmount,
      regularUnitPrice: parent.itemRegularUnitPrice,
    });
  }

  updateForm(produit: IProduit): void {
    this.editForm.patchValue({
      id: produit.id,
      libelle: produit.libelle,
      costAmount: produit.costAmount,
      regularUnitPrice: produit.regularUnitPrice,
      createdAt: produit.createdAt ? produit.createdAt.format(DATE_TIME_FORMAT) : null,
    });
  }

  save(): void {
    this.isSaving = true;
    const produit = this.createFromForm();
    if (produit.id !== undefined && produit.id !== null) {
      this.subscribeToSaveResponse(this.produitService.update(produit));
    } else {
      this.subscribeToSaveResponse(this.produitService.create(produit));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IProduit>>): void {
    result.subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError()
    );
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.eventManager.broadcast('produitListModification');
    this.activeModal.close();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}
