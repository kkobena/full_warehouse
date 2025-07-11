import { Component, inject, OnInit } from '@angular/core';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { Customer, ICustomer } from '../../../shared/model/customer.model';
import { InputMaskModule } from 'primeng/inputmask';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { RadioButtonModule } from 'primeng/radiobutton';
import { ToastModule } from 'primeng/toast';
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
    ToastModule,
    TranslateDirective,
    CardModule,
    DividerModule,
    DateNaissDirective,
  ],
  templateUrl: './ayant-droit-step.component.html',
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
    return {
      ...new Customer(),
      id: this.editForm.get(['id']).value,
      firstName: this.editForm.get(['firstName']).value,
      lastName: this.editForm.get(['lastName']).value,
      numAyantDroit: this.editForm.get(['numAyantDroit']).value,
      datNaiss: this.editForm.get(['datNaiss']).value,
      sexe: this.editForm.get(['sexe']).value,
      type: 'ASSURE',
    };
  }

  onNext(): void {
    this.saveFormState();
  }

  isValidForm(): boolean {
    const ayantDroits = this.createFromForm();
    const formIsSet: boolean = ayantDroits.firstName !== null && ayantDroits.lastName !== null && ayantDroits.numAyantDroit !== null;
    const formNotSet: boolean = ayantDroits.firstName === null && ayantDroits.lastName === null && ayantDroits.numAyantDroit === null;
    const firstNameIsEmpty: boolean = ayantDroits.firstName === '' && ayantDroits.lastName === null && ayantDroits.numAyantDroit === null;
    const lastNameAnFirstNameIsEmpty: boolean =
      ayantDroits.firstName === '' && ayantDroits.lastName === '' && ayantDroits.numAyantDroit === null;
    const lastNameIsEmpty: boolean = ayantDroits.firstName === null && ayantDroits.lastName === '' && ayantDroits.numAyantDroit === null;
    const allEmpty: boolean = ayantDroits.firstName === '' && ayantDroits.lastName === '' && ayantDroits.numAyantDroit === '';
    const numAyantDroitEmpty: boolean = ayantDroits.firstName === null && ayantDroits.lastName === null && ayantDroits.numAyantDroit === '';
    return formNotSet || formIsSet || firstNameIsEmpty || lastNameAnFirstNameIsEmpty || allEmpty || lastNameIsEmpty || numAyantDroitEmpty;
  }

  saveFormState(): void {
    const currentAssure = this.assureFormStepService.assure();
    const isValideForm = this.editForm.valid;
    if (isValideForm) {
      const ayantDroits = this.createFromForm();
      currentAssure.ayantDroits = [ayantDroits];
      // const complementaires = currentAssure?.tiersPayants;
      this.assureFormStepService.setAssure(currentAssure);
    }
  }

  goBack(): void {
    this.saveFormState();
  }
}
