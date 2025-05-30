import { AfterViewInit, Component, ElementRef, OnInit, viewChild, inject } from '@angular/core';
import { Customer, ICustomer } from 'app/shared/model/customer.model';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ErrorService } from 'app/shared/error.service';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { CustomerService } from 'app/entities/customer/customer.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Observable } from 'rxjs';
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

@Component({
  selector: 'jhi-form-ayant-droit',
  templateUrl: './form-ayant-droit.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
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
  ],
})
export class FormAyantDroitComponent implements OnInit, AfterViewInit {
  protected errorService = inject(ErrorService);
  private fb = inject(UntypedFormBuilder);
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  protected customerService = inject(CustomerService);
  private messageService = inject(MessageService);

  firstName = viewChild.required<ElementRef>('firstName');
  entity?: ICustomer;
  assure?: ICustomer;
  isSaving = false;
  isValid = true;
  editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required, Validators.min(1)]],
    lastName: [null, [Validators.required, Validators.min(1)]],
    numAyantDroit: [null, [Validators.required]],
    datNaiss: [],
    sexe: [],
  });

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    this.assure = this.config.data.assure;
    if (this.entity) {
      this.updateForm(this.entity);
    }
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
      //   datNaiss: customer.datNaiss ? new Date(moment(customer.datNaiss).format('yyyy-MM-DD')) : null,
      datNaiss: customer.datNaiss ? FORMAT_ISO_DATE_TO_STRING_FR(customer.datNaiss) : null,
      sexe: customer.sexe,
      numAyantDroit: customer.numAyantDroit,
    });
  }

  cancel(): void {
    this.ref.close();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.firstName().nativeElement.focus();
    }, 30);
  }

  protected createFromForm(): ICustomer {
    return {
      ...new Customer(),
      id: this.editForm.get(['id']).value,
      firstName: this.editForm.get(['firstName']).value,
      lastName: this.editForm.get(['lastName']).value,
      numAyantDroit: this.editForm.get(['numAyantDroit']).value,
      datNaiss: DATE_FORMAT_FROM_STRING_FR(this.editForm.get(['datNaiss']).value),
      sexe: this.editForm.get(['sexe']).value,
      type: 'ASSURE',
      assureId: this.assure.id,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result.subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(customer: ICustomer | null): void {
    this.isSaving = false;
    this.ref.close(customer);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    if (error.error?.errorKey) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: translatedErrorMessage,
        });
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
