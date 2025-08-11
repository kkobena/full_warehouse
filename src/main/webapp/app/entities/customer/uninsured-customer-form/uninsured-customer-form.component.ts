import { AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, viewChild } from '@angular/core';
import { MessageService } from 'primeng/api';
import { Customer, ICustomer } from 'app/shared/model/customer.model';
import { ErrorService } from 'app/shared/error.service';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { CustomerService } from 'app/entities/customer/customer.service';
import { Observable, Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { KeyFilterModule } from 'primeng/keyfilter';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-uninsured-customer-form',
  templateUrl: './uninsured-customer-form.component.html',
  styleUrls: ['./uninsured-customer-component.scss'],
  providers: [MessageService],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ToastModule,
    ButtonModule,
    RippleModule,
    InputTextModule,
    KeyFilterModule,
    Card,
  ],
})
export class UninsuredCustomerFormComponent implements OnInit, AfterViewInit, OnDestroy {
  header: string | null = null;
  entity?: ICustomer;
  protected isSaving = false;
  protected isValid = true;
  protected firstName = viewChild.required<ElementRef>('firstName');
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required, Validators.min(1)]],
    lastName: [null, [Validators.required, Validators.min(1)]],
    phone: [null, [Validators.required, Validators.min(1)]],
    email: [],
  });
  private readonly messageService = inject(MessageService);
  private destroy$ = new Subject<void>();
  private readonly errorService = inject(ErrorService);
  private readonly customerService = inject(CustomerService);
  private readonly activeModal = inject(NgbActiveModal);

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.firstName().nativeElement.focus();
    }, 100);
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
    this.activeModal.dismiss();
  }

  private createFromForm(): ICustomer {
    const formValue = this.editForm.value;
    return {
      ...new Customer(),
      id: formValue.id,
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      email: formValue.email,
      phone: formValue.phone,
      type: 'STANDARD',
    };
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result
      .pipe(
        finalize(() => (this.isSaving = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (res: HttpResponse<ICustomer>) => this.onSaveSuccess(res.body),
        error: (error: any) => this.onSaveError(error),
      });
  }

  private onSaveSuccess(customer: ICustomer | null): void {
    this.activeModal.close(customer);
  }

  private onSaveError(error: any): void {
    if (error.error?.errorKey) {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: this.errorService.getErrorMessage(error),
      });
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Erreur interne du serveur.',
      });
    }
  }
}
