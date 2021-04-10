import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';

import { IProduit, Produit } from 'app/shared/model/produit.model';
import { ProduitService } from './produit.service';
import { ICategorie } from 'app/shared/model/categorie.model';
import { CategorieService } from 'app/entities/categorie/categorie.service';
import { TypeProduit } from '../../shared/model/enumerations/type-produit.model';

@Component({
  selector: 'jhi-produit-update',
  templateUrl: './produit-update.component.html',
})
export class ProduitUpdateComponent implements OnInit {
  isSaving = false;
  isValid = true;
  categories: ICategorie[] = [];
  editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
    costAmount: [null, [Validators.required]],
    regularUnitPrice: [null, [Validators.required]],
    createdAt: [],
    itemQty: [null, [Validators.required, Validators.min(0)]],
    itemCostAmount: [null, [Validators.required, Validators.min(0)]],
    itemRegularUnitPrice: [null, [Validators.required, Validators.min(0)]],
  });

  constructor(
    protected produitService: ProduitService,
    protected categorieService: CategorieService,
    protected activatedRoute: ActivatedRoute,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ produit }) => {
      if (!produit.id) {
        const today = moment().startOf('day');
        produit.createdAt = today;
        produit.updatedAt = today;
      }

      this.updateForm(produit);

      this.categorieService.query().subscribe((res: HttpResponse<ICategorie[]>) => (this.categories = res.body || []));
    });
  }

  updateForm(produit: IProduit): void {
    this.editForm.patchValue({
      id: produit.id,
      libelle: produit.libelle,
      costAmount: produit.costAmount,
      regularUnitPrice: produit.regularUnitPrice,
      createdAt: produit.createdAt ? produit.createdAt.format(DATE_TIME_FORMAT) : null,
      itemQty: produit.itemQty,
      itemCostAmount: produit.itemCostAmount,
      itemRegularUnitPrice: produit.itemRegularUnitPrice,
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const produit = this.createFromForm();
    if (produit.id !== undefined) {
      this.subscribeToSaveResponse(this.produitService.update(produit));
    } else {
      this.subscribeToSaveResponse(this.produitService.create(produit));
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
      itemQty: this.editForm.get(['itemQty'])!.value,
      itemCostAmount: this.editForm.get(['itemCostAmount'])!.value,
      itemRegularUnitPrice: this.editForm.get(['itemRegularUnitPrice'])!.value,
      typeProduit: TypeProduit.PACKAGE,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IProduit>>): void {
    result.subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError()
    );
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  trackById(index: number, item: ICategorie): any {
    return item.id;
  }

  handleCostInput(event: any): void {
    const value = Number(event.target.value);
    const unitPrice = Number(this.editForm.get(['regularUnitPrice'])!.value);
    if (value >= unitPrice) {
      this.isValid = false;
    } else {
      this.isValid = true;
    }
  }

  handleUnitPriceInput(event: any): void {
    const value = Number(event.target.value);
    const costAmount = Number(this.editForm.get(['costAmount'])!.value);
    if (costAmount >= value) {
      this.isValid = false;
    } else {
      this.isValid = true;
    }
  }

  handleItemQty(event: any): void {
    const itemQty = event.target.value;
    const costAmount = Number(this.editForm.get(['costAmount'])!.value);
    const regularUnitPrice = Number(this.editForm.get(['regularUnitPrice'])!.value);
    const itemCostAmount = costAmount / itemQty;
    const itemRegularUnitPrice = regularUnitPrice / itemQty;
    this.editForm.get(['itemCostAmount'])!.setValue(itemCostAmount.toFixed());
    this.editForm.get(['itemRegularUnitPrice'])!.setValue(itemRegularUnitPrice.toFixed());
  }

  handleItemCost(event: any): void {
    const value = Number(event.target.value);
    const itemRegularUnitPrice = Number(this.editForm.get(['itemRegularUnitPrice'])!.value);
    if (value >= itemRegularUnitPrice) {
      this.isValid = false;
    } else {
      this.isValid = true;
    }
  }

  handleItemPrice(event: any): void {
    const value = event.target.value;
    const itemCostAmount = Number(this.editForm.get(['itemCostAmount'])!.value);
    if (itemCostAmount >= Number(value)) {
      this.isValid = false;
    } else {
      this.isValid = true;
    }
  }
}
