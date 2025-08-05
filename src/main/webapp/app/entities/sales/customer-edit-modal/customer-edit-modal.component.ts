import { Component, inject, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ICustomer } from '../../../shared/model/customer.model';
import {
  FormArray,
  FormsModule,
  ReactiveFormsModule,
  UntypedFormBuilder,
  Validators
} from '@angular/forms';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { CustomerService } from '../../customer/customer.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { ISales } from '../../../shared/model/sales.model';
import { TableModule } from 'primeng/table';
import { IThirdPartySaleLine } from '../../../shared/model/third-party-sale-line';
import { CardModule } from 'primeng/card';
import { KeyFilter } from 'primeng/keyfilter';

@Component({
  selector: 'jhi-customer-edit-modal',
  templateUrl: './customer-edit-modal.component.html',
  styleUrls: ['./customer-edit-modal.component.scss'],
  imports: [FormsModule, WarehouseCommonModule, ButtonModule, InputTextModule, ReactiveFormsModule, TableModule, CardModule, KeyFilter],
})
export class CustomerEditModalComponent implements OnInit {
  @Input() sale: ISales;
  customer: ICustomer;
  ayantDroit: ICustomer;
  thirdPartySaleLines: IThirdPartySaleLine[];
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    customer: this.fb.group({
      id: [],
      num: [null, [Validators.required]],
      firstName: [null, [Validators.required]],
      lastName: [null, [Validators.required]],
      phone: [],
    }),
    ayantDroit: this.fb.group({
      id: [],
      num: [null, [Validators.required]],
      firstName: [null, [Validators.required]],
      lastName: [null, [Validators.required]],
    }),
    thirdPartySaleLines: this.fb.array([]),
  });

  protected isSaving = false;
  private activeModal = inject(NgbActiveModal);
  private customerService = inject(CustomerService);

  get thirdPartySaleLinesFomArray(): FormArray {
    return this.editForm.get('thirdPartySaleLines') as FormArray;
  }

  ngOnInit(): void {
    this.customer = this.sale.customer;
    this.ayantDroit = this.sale.ayantDroit?.id !== this.customer.id ? this.sale.ayantDroit : null;
    this.thirdPartySaleLines = this.sale.thirdPartySaleLines;
    this.updateForm();
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    this.isSaving = true;
    const customer = this.createFromForm();
    this.subscribeToSaveResponse(this.customerService.update(customer));
  }

  protected createFromForm(): ICustomer {
    const customerFromForm = this.editForm.get('customer').value;
    return {
      ...this.customer,
      id: customerFromForm.id,
      num: customerFromForm.num,
      firstName: customerFromForm.firstName,
      lastName: customerFromForm.lastName,
      phone: customerFromForm.phone,
    };
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  private onSaveSuccess(sales: ISales | null): void {
    this.isSaving = false;
    this.activeModal.close(sales);
  }

  private onSaveError(): void {
    this.isSaving = false;
  }

  private updateForm(): void {
    this.editForm.patchValue({
      customer: {
        id: this.customer.id,
        num: this.sale.tiersPayants[0].num,
        firstName: this.customer.firstName,
        lastName: this.customer.lastName,
        phone: this.customer.phone,
      },
    });
    if (this.ayantDroit) {
      this.editForm.patchValue({
        ayantDroit: {
          id: this.ayantDroit.id,
          num: this.ayantDroit.numAyantDroit,
          firstName: this.ayantDroit.firstName,
          lastName: this.ayantDroit.lastName,
        },
      });
    }
    if (this.thirdPartySaleLines) {
      this.thirdPartySaleLines.forEach(line => {
        this.thirdPartySaleLinesFomArray.push(
          this.fb.group({
            id: [line.id],
            numBon: [line.numBon],
            taux: [line.taux],
            montant: [line.montant],
            tiersPayantFullName: [line.tiersPayantFullName],
          }),
        );
      });
    }
  }
}
