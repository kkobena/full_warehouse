import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';

import { ISalesLine, SalesLine } from 'app/shared/model/sales-line.model';
import { SalesLineService } from './sales-line.service';
import { ISales } from 'app/shared/model/sales.model';
import { SalesService } from 'app/entities/sales/sales.service';
import { IProduit } from 'app/shared/model/produit.model';
import { ProduitService } from 'app/entities/produit/produit.service';

type SelectableEntity = ISales | IProduit;

@Component({
  selector: 'jhi-sales-line-update',
  templateUrl: './sales-line-update.component.html',
})
export class SalesLineUpdateComponent implements OnInit {
  isSaving = false;
  sales: ISales[] = [];
  produits: IProduit[] = [];

  editForm = this.fb.group({
    id: [],
    quantitySold: [null, [Validators.required]],
    regularUnitPrice: [null, [Validators.required]],
    discountUnitPrice: [null, [Validators.required]],
    netUnitPrice: [null, [Validators.required]],
    discountAmount: [null, [Validators.required]],
    salesAmount: [null, [Validators.required]],
    grossAmount: [null, [Validators.required]],
    netAmount: [null, [Validators.required]],
    taxAmount: [null, [Validators.required]],
    costAmount: [null, [Validators.required]],
    createdAt: [null, [Validators.required]],
    updatedAt: [null, [Validators.required]],
    sales: [],
    produit: [],
  });

  constructor(
    protected salesLineService: SalesLineService,
    protected salesService: SalesService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ salesLine }) => {
      if (!salesLine.id) {
        const today = moment().startOf('day');
        salesLine.createdAt = today;
        salesLine.updatedAt = today;
      }

      this.updateForm(salesLine);

      this.salesService.query().subscribe((res: HttpResponse<ISales[]>) => (this.sales = res.body || []));

      this.produitService.query().subscribe((res: HttpResponse<IProduit[]>) => (this.produits = res.body || []));
    });
  }

  updateForm(salesLine: ISalesLine): void {
    this.editForm.patchValue({
      id: salesLine.id,
      quantitySold: salesLine.quantitySold,
      regularUnitPrice: salesLine.regularUnitPrice,
      discountUnitPrice: salesLine.discountUnitPrice,
      netUnitPrice: salesLine.netUnitPrice,
      discountAmount: salesLine.discountAmount,
      salesAmount: salesLine.salesAmount,
      grossAmount: salesLine.grossAmount,
      netAmount: salesLine.netAmount,
      taxAmount: salesLine.taxAmount,
      costAmount: salesLine.costAmount,
      createdAt: salesLine.createdAt ? salesLine.createdAt.format(DATE_TIME_FORMAT) : null,
      updatedAt: salesLine.updatedAt ? salesLine.updatedAt.format(DATE_TIME_FORMAT) : null,
      sales: salesLine.sales,
      produit: salesLine.produit,
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const salesLine = this.createFromForm();
    if (salesLine.id !== undefined) {
      this.subscribeToSaveResponse(this.salesLineService.update(salesLine));
    } else {
      this.subscribeToSaveResponse(this.salesLineService.create(salesLine));
    }
  }

  private createFromForm(): ISalesLine {
    return {
      ...new SalesLine(),
      id: this.editForm.get(['id'])!.value,
      quantitySold: this.editForm.get(['quantitySold'])!.value,
      regularUnitPrice: this.editForm.get(['regularUnitPrice'])!.value,
      discountUnitPrice: this.editForm.get(['discountUnitPrice'])!.value,
      netUnitPrice: this.editForm.get(['netUnitPrice'])!.value,
      discountAmount: this.editForm.get(['discountAmount'])!.value,
      salesAmount: this.editForm.get(['salesAmount'])!.value,
      grossAmount: this.editForm.get(['grossAmount'])!.value,
      netAmount: this.editForm.get(['netAmount'])!.value,
      taxAmount: this.editForm.get(['taxAmount'])!.value,
      costAmount: this.editForm.get(['costAmount'])!.value,
      createdAt: this.editForm.get(['createdAt'])!.value ? moment(this.editForm.get(['createdAt'])!.value, DATE_TIME_FORMAT) : undefined,
      updatedAt: this.editForm.get(['updatedAt'])!.value ? moment(this.editForm.get(['updatedAt'])!.value, DATE_TIME_FORMAT) : undefined,
      sales: this.editForm.get(['sales'])!.value,
      produit: this.editForm.get(['produit'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISalesLine>>): void {
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

  trackById(index: number, item: SelectableEntity): any {
    return item.id;
  }
}
