import { HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ICustomer } from 'app/shared/model/customer.model';
import { IProduit } from 'app/shared/model/produit.model';
import { ISalesLine, SalesLine } from 'app/shared/model/sales-line.model';
import { ISales, Sales } from 'app/shared/model/sales.model';
import { Observable } from 'rxjs';
import { SalesLineService } from '../sales-line/sales-line.service';
import { SalesService } from './sales.service';

@Component({
  selector: 'jhi-pack-dialogue',
  templateUrl: './pack-dialogue.component.html',
})
export class PackDialogueComponent implements OnInit {
  produit?: IProduit;
  isSaving = false;
  sale?: ISales;
  customer?: ICustomer;
  detail = 'DETAIL';
  isNotValid = false;
  editForm = this.fb.group({
    quantitySold: [null, [Validators.required, Validators.min(1)]],
  });

  constructor(
    public activeModal: NgbActiveModal,
    private fb: FormBuilder,
    private salesService: SalesService,
    private saleItemService: SalesLineService
  ) {}

  ngOnInit(): void {}
  cancel(): void {
    this.activeModal.dismiss();
  }
  private createFromForm(): ISalesLine {
    return {
      ...new SalesLine(),
      produitId: this.produit?.id,
      regularUnitPrice: this.produit?.regularUnitPrice,
      costAmount: this.produit?.costAmount,
      quantitySold: this.editForm.get(['quantitySold'])!.value,
      saleId: this.sale?.id,
    };
  }
  private createSaleFromForm(): ISales {
    return {
      ...new Sales(),
      customerId: this.customer?.id,
      salesLines: [this.createFromForm()],
    };
  }
  save(): void {
    this.isSaving = true;
    if (this.sale === null || this.sale === undefined) {
      this.subscribeToSaveResponse(this.salesService.create(this.createSaleFromForm()));
    } else {
      const saline = this.createFromForm();
      this.subscribeToSaveLineResponse(this.saleItemService.create(saline));
    }
  }
  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe(
      (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body!),
      () => this.onSaveError()
    );
  }
  protected onSaveSuccess(sale: ISales): void {
    this.isSaving = false;
    this.activeModal.close(sale);
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }
  protected subscribeToSaveLineResponse(result: Observable<HttpResponse<ISalesLine>>): void {
    result.subscribe(
      (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body!),
      () => this.onSaveError()
    );
  }

  onQuantitySoldBoxChanged(event: any): void {
    const qty = event.target.value;
    const oldStock = this.produit!.quantity;
    if (oldStock! < Number(qty)) {
      this.isNotValid = true;
    } else {
      this.isNotValid = false;
    }
  }
}
