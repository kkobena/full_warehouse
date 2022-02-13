import { Component, OnInit } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Customer, ICustomer } from 'app/shared/model/customer.model';
import { ErrorService } from 'app/shared/error.service';
import { FormBuilder, Validators } from '@angular/forms';
import { CustomerService } from 'app/entities/customer/customer.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';

@Component({
  selector: 'jhi-uninsured-customer-form',
  templateUrl: './uninsured-customer-form.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
})
export class UninsuredCustomerFormComponent implements OnInit {
  entity?: ICustomer;
  isSaving = false;
  isValid = true;
  editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required, Validators.min(1)]],
    lastName: [null, [Validators.required, Validators.min(1)]],
    phone: [null, [Validators.required, Validators.min(1)]],
    email: [],
  });

  constructor(
    protected errorService: ErrorService,
    private fb: FormBuilder,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    protected customerService: CustomerService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  save(): void {
    this.isSaving = true;
    const customer = this.createFromForm();
    if (customer.id !== undefined && customer.id) {
      customer.type = 'STANDARD';
      this.subscribeToSaveResponse(this.customerService.updateUninsuredCustomer(customer));
    } else {
      this.subscribeToSaveResponse(this.customerService.createUninsuredCustomer(customer));
    }
  }

  updateForm(customer: ICustomer): void {
    this.editForm.patchValue({
      id: customer.id,
      firstName: customer.firstName,
      lastName: customer.lastName,
      email: customer.email,
      phone: customer.phone,
    });
  }

  cancel(): void {
    this.ref.close();
  }

  protected createFromForm(): ICustomer {
    return {
      ...new Customer(),
      id: this.editForm.get(['id'])!.value,
      firstName: this.editForm.get(['firstName'])!.value,
      lastName: this.editForm.get(['lastName'])!.value,
      email: this.editForm.get(['email'])!.value,
      phone: this.editForm.get(['phone'])!.value,
      type: 'STANDARD',
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result.subscribe(
      res => this.onSaveSuccess(res.body),
      error => this.onSaveError(error)
    );
  }

  protected onSaveSuccess(customer: ICustomer | null): void {
    this.isSaving = false;
    this.ref.close(customer);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    if (error.error && error.error.errorKey) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: translatedErrorMessage });
      });
    } else {
      this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Erreur interne du serveur.' });
    }
  }
}
