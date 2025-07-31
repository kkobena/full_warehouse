import { AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, viewChild } from '@angular/core';
import { Customer, ICustomer } from 'app/shared/model/customer.model';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ErrorService } from 'app/shared/error.service';
import { CustomerService } from 'app/entities/customer/customer.service';
import { Observable, Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';
import { RadioButtonModule } from 'primeng/radiobutton';
import { CalendarModule } from 'primeng/calendar';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputMaskModule } from 'primeng/inputmask';
import { DATE_FORMAT_FROM_STRING_FR, FORMAT_ISO_DATE_TO_STRING_FR } from '../../../shared/util/warehouse-util';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';

@Component({
  selector: 'jhi-form-ayant-droit',
  templateUrl: './form-ayant-droit.component.html',
  imports: [
    WarehouseCommonModule,
    ToastModule,
    FormsModule,
    ButtonModule,
    RippleModule,
    InputTextModule,
    RadioButtonModule,
    ReactiveFormsModule,
    CalendarModule,
    KeyFilterModule,
    InputMaskModule,
    ToastAlertComponent,
  ],
})
export class FormAyantDroitComponent implements OnInit, AfterViewInit, OnDestroy {
  header: string;
  entity?: ICustomer;
  assure?: ICustomer;
  protected firstName = viewChild.required<ElementRef>('firstName');
  protected fb = inject(UntypedFormBuilder);
  protected isSaving = false;
  protected isValid = true;
  protected editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required, Validators.min(1)]],
    lastName: [null, [Validators.required, Validators.min(1)]],
    numAyantDroit: [null, [Validators.required]],
    datNaiss: [],
    sexe: [],
  });
  private readonly errorService = inject(ErrorService);
  private readonly customerService = inject(CustomerService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  save(): void {
    this.isSaving = true;
    const customer = this.createFromForm();
    if (customer.id !== undefined && customer.id) {
      this.subscribeToSaveResponse(this.customerService.updateAyantDroit(customer));
    } else {
      this.subscribeToSaveResponse(this.customerService.createAyantDroit(customer));
    }
  }

  updateForm(customer: ICustomer): void {
    this.editForm.patchValue({
      id: customer.id,
      firstName: customer.firstName,
      lastName: customer.lastName,
      datNaiss: customer.datNaiss ? FORMAT_ISO_DATE_TO_STRING_FR(customer.datNaiss) : null,
      sexe: customer.sexe,
      numAyantDroit: customer.numAyantDroit,
    });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.firstName().nativeElement.focus();
    }, 100);
  }

  protected createFromForm(): ICustomer {
    const formValue = this.editForm.value;
    return {
      ...new Customer(),
      id: formValue.id,
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      numAyantDroit: formValue.numAyantDroit,
      datNaiss: DATE_FORMAT_FROM_STRING_FR(formValue.datNaiss),
      sexe: formValue.sexe,
      type: 'ASSURE',
      assureId: this.assure.id,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result.pipe(finalize(() => (this.isSaving = false)), takeUntil(this.destroy$)).subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(customer: ICustomer | null): void {
    this.activeModal.close(customer);
  }

  protected onSaveError(error: any): void {
    if (error.error?.errorKey) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).pipe(takeUntil(this.destroy$)).subscribe(translatedErrorMessage => {
        this.alert().showError(translatedErrorMessage);
      });
    } else {
      this.alert().showError('Erreur interne du serveur.');
    }
  }
}
