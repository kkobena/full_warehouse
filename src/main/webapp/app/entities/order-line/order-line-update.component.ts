import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';

import { IOrderLine, OrderLine } from 'app/shared/model/order-line.model';
import { OrderLineService } from './order-line.service';
import { ICommande } from 'app/shared/model/commande.model';
import { CommandeService } from 'app/entities/commande/commande.service';
import { IProduit } from 'app/shared/model/produit.model';
import { ProduitService } from 'app/entities/produit/produit.service';

type SelectableEntity = ICommande | IProduit;

@Component({
  selector: 'jhi-order-line-update',
  templateUrl: './order-line-update.component.html',
})
export class OrderLineUpdateComponent implements OnInit {
  isSaving = false;
  commandes: ICommande[] = [];
  produits: IProduit[] = [];
  receiptDateDp: any;

  editForm = this.fb.group({
    id: [],
    receiptDate: [null, [Validators.required]],
    quantityReceived: [null, [Validators.required]],
    quantityRequested: [null, [Validators.required]],
    quantityReturned: [null, [Validators.required]],
    discountAmount: [null, [Validators.required]],
    orderAmount: [null, [Validators.required]],
    grossAmount: [null, [Validators.required]],
    netAmount: [null, [Validators.required]],
    taxAmount: [null, [Validators.required]],
    createdAt: [null, [Validators.required]],
    updatedAt: [null, [Validators.required]],
    costAmount: [null, [Validators.required]],
    commande: [],
    produit: [],
  });

  constructor(
    protected orderLineService: OrderLineService,
    protected commandeService: CommandeService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ orderLine }) => {
      if (!orderLine.id) {
        const today = moment().startOf('day');
        orderLine.createdAt = today;
        orderLine.updatedAt = today;
      }

      this.updateForm(orderLine);

      this.commandeService.query().subscribe((res: HttpResponse<ICommande[]>) => (this.commandes = res.body || []));

      this.produitService.query().subscribe((res: HttpResponse<IProduit[]>) => (this.produits = res.body || []));
    });
  }

  updateForm(orderLine: IOrderLine): void {
    this.editForm.patchValue({
      id: orderLine.id,
      receiptDate: orderLine.receiptDate,
      quantityReceived: orderLine.quantityReceived,
      quantityRequested: orderLine.quantityRequested,
      quantityReturned: orderLine.quantityReturned,
      discountAmount: orderLine.discountAmount,
      orderAmount: orderLine.orderAmount,
      grossAmount: orderLine.grossAmount,
      netAmount: orderLine.netAmount,
      taxAmount: orderLine.taxAmount,
      createdAt: orderLine.createdAt ? orderLine.createdAt.format(DATE_TIME_FORMAT) : null,
      updatedAt: orderLine.updatedAt ? orderLine.updatedAt.format(DATE_TIME_FORMAT) : null,
      costAmount: orderLine.costAmount,
      commande: orderLine.commande,
      produit: orderLine.produit,
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const orderLine = this.createFromForm();
    if (orderLine.id !== undefined) {
      this.subscribeToSaveResponse(this.orderLineService.update(orderLine));
    } else {
      this.subscribeToSaveResponse(this.orderLineService.create(orderLine));
    }
  }

  private createFromForm(): IOrderLine {
    return {
      ...new OrderLine(),
      id: this.editForm.get(['id'])!.value,
      receiptDate: this.editForm.get(['receiptDate'])!.value,
      quantityReceived: this.editForm.get(['quantityReceived'])!.value,
      quantityRequested: this.editForm.get(['quantityRequested'])!.value,
      quantityReturned: this.editForm.get(['quantityReturned'])!.value,
      discountAmount: this.editForm.get(['discountAmount'])!.value,
      orderAmount: this.editForm.get(['orderAmount'])!.value,
      grossAmount: this.editForm.get(['grossAmount'])!.value,
      netAmount: this.editForm.get(['netAmount'])!.value,
      taxAmount: this.editForm.get(['taxAmount'])!.value,
      createdAt: this.editForm.get(['createdAt'])!.value ? moment(this.editForm.get(['createdAt'])!.value, DATE_TIME_FORMAT) : undefined,
      updatedAt: this.editForm.get(['updatedAt'])!.value ? moment(this.editForm.get(['updatedAt'])!.value, DATE_TIME_FORMAT) : undefined,
      costAmount: this.editForm.get(['costAmount'])!.value,
      commande: this.editForm.get(['commande'])!.value,
      produit: this.editForm.get(['produit'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IOrderLine>>): void {
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
