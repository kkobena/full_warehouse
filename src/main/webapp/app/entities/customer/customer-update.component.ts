import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_TIME_FORMAT } from 'app/shared/constants/input.constants';

import { ICustomer, Customer } from 'app/shared/model/customer.model';
import { CustomerService } from './customer.service';
import { IProduit } from 'app/shared/model/produit.model';
import { ProduitService } from 'app/entities/produit/produit.service';

@Component({
  selector: 'jhi-customer-update',
  templateUrl: './customer-update.component.html',
})
export class CustomerUpdateComponent implements OnInit {
  isSaving = false;
  produits: IProduit[] = [];
  editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required]],
    lastName: [null, [Validators.required]],
    phone: [null, [Validators.required]],
    email: [],
    createdAt: [],
    produits: [],
  });

  constructor(
    protected customerService: CustomerService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ customer }) => {
      if (!customer.id) {
        const today = moment().startOf('day');
        customer.createdAt = today;
        customer.updatedAt = today;
      }

      this.updateForm(customer);

      this.produitService.query().subscribe((res: HttpResponse<IProduit[]>) => (this.produits = res.body || []));
    });
  }

  updateForm(customer: ICustomer): void {
    this.editForm.patchValue({
      id: customer.id,
      firstName: customer.firstName,
      lastName: customer.lastName,
      phone: customer.phone,
      email: customer.email,
      createdAt: customer.createdAt ? customer.createdAt.format(DATE_TIME_FORMAT) : null,
      produits: customer.produits,
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const customer = this.createFromForm();
    if (customer.id !== undefined) {
      this.subscribeToSaveResponse(this.customerService.update(customer));
    } else {
      this.subscribeToSaveResponse(this.customerService.create(customer));
    }
  }

  private createFromForm(): ICustomer {
    return {
      ...new Customer(),
      id: this.editForm.get(['id'])!.value,
      firstName: this.editForm.get(['firstName'])!.value,
      lastName: this.editForm.get(['lastName'])!.value,
      phone: this.editForm.get(['phone'])!.value,
      email: this.editForm.get(['email'])!.value,
      createdAt: this.editForm.get(['createdAt'])!.value ? moment(this.editForm.get(['createdAt'])!.value, DATE_TIME_FORMAT) : undefined,
      produits: this.editForm.get(['produits'])!.value,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
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

  trackById(index: number, item: IProduit): any {
    return item.id;
  }

  getSelected(selectedVals: IProduit[], option: IProduit): IProduit {
    if (selectedVals) {
      for (let i = 0; i < selectedVals.length; i++) {
        if (option.id === selectedVals[i].id) {
          return selectedVals[i];
        }
      }
    }
    return option;
  }
}
