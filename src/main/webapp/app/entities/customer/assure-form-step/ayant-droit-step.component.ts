import { Component, inject, OnInit } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { Customer, ICustomer } from '../../../shared/model/customer.model';
import { InputMaskModule } from 'primeng/inputmask';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { RadioButtonModule } from 'primeng/radiobutton';
import TranslateDirective from '../../../shared/language/translate.directive';
import { CardModule } from 'primeng/card';
import { AssureFormStepService } from './assure-form-step.service';
import { DividerModule } from 'primeng/divider';
import { DateNaissDirective } from '../../../shared/date-naiss.directive';

@Component({
  selector: 'jhi-ayant-droit-step',
  imports: [
    InputMaskModule,
    InputTextModule,
    KeyFilterModule,
    RadioButtonModule,
    ReactiveFormsModule,
    TranslateDirective,
    CardModule,
    DividerModule,
    DateNaissDirective,
  ],
  templateUrl: './ayant-droit-step.component.html',
  styleUrls: ['./assured-form-step-component.scss'],
})
export class AyantDroitStepComponent implements OnInit {
  assure?: ICustomer;
  ayantDroit: ICustomer;
  isSaving = false;
  isValid = true;
  assureFormStepService = inject(AssureFormStepService);
  fb = inject(UntypedFormBuilder);
  editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required, Validators.min(1)]],
    lastName: [null, [Validators.required, Validators.min(1)]],
    numAyantDroit: [null, [Validators.required, Validators.min(1)]],
    datNaiss: [],
    sexe: [],
  });

  ngOnInit(): void {
    const currentAssure = this.assureFormStepService.assure();
    if (currentAssure.ayantDroits.length > 0) {
      this.updateForm(currentAssure.ayantDroits[0]);
    }
  }

  // onDbleClick(customer: ICustomer): void {
  //   this.onSelect(customer);
  // }

  // onSelect(customer: ICustomer): void {
  //   this.ayantDroit = customer;
  //   this.updateForm(customer);
  // }

  updateForm(customer: ICustomer): void {
    this.editForm.patchValue({
      id: customer.id,
      firstName: customer.firstName,
      lastName: customer.lastName,
      datNaiss: customer.datNaiss,
      sexe: customer.sexe,
      numAyantDroit: customer.numAyantDroit,
    });
  }

  createFromForm(): ICustomer {
    const formValue = this.editForm.value;
    return {
      ...new Customer(),
      id: formValue.id,
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      numAyantDroit: formValue.numAyantDroit,
      datNaiss: formValue.datNaiss,
      sexe: formValue.sexe,
      type: 'ASSURE',
    };
  }

  onNext(): void {
    this.saveFormState();
  }

  isValidForm(): boolean {
    const formValue = this.editForm.value;
    const hasAnyValue = formValue.firstName || formValue.lastName || formValue.numAyantDroit;
    const allEmpty = !formValue.firstName && !formValue.lastName && !formValue.numAyantDroit;

    return allEmpty || (hasAnyValue && this.editForm.valid);
  }

  saveFormState(): void {
    const currentAssure = this.assureFormStepService.assure();
    if (this.editForm.valid) {
      const ayantDroits = this.createFromForm();
      currentAssure.ayantDroits = [ayantDroits];
      this.assureFormStepService.setAssure(currentAssure);
    }
  }

  goBack(): void {
    this.saveFormState();
  }
}
