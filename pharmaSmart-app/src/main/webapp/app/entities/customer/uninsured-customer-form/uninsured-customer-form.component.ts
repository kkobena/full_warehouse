import { AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { Customer, ICustomer } from 'app/shared/model/customer.model';
import { ErrorService } from 'app/shared/error.service';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { CustomerService } from 'app/entities/customer/customer.service';
import { Observable, Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { NotificationService } from "../../../shared/services/notification.service";
import {
  ButtonComponent,
  CardComponent,
  KeyFilterDirective
} from '../../../shared/ui';

@Component({
  selector: 'app-uninsured-customer-form',
  templateUrl: './uninsured-customer-form.component.html',
  styleUrls: ['./uninsured-customer-component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    KeyFilterDirective
  ],
})
export class UninsuredCustomerFormComponent implements OnInit, AfterViewInit, OnDestroy {
  title: string | null = null;
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
  private destroy$ = new Subject<void>();
  private readonly errorService = inject(ErrorService);
  private readonly customerService = inject(CustomerService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notificationService = inject(NotificationService);
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
      this.notificationService.error(this.errorService.getErrorMessage(error), 'Erreur');
    } else {
      this.notificationService.error('Erreur interne du serveur.', 'Erreur');
    }
  }
}
