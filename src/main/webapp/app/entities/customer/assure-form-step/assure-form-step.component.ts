import { Component, inject, OnInit, viewChild } from '@angular/core';
import { StepsModule } from 'primeng/steps';
import { MenuItem } from 'primeng/api';
import { ICustomer } from '../../../shared/model/customer.model';
import { AssureFormStepService } from './assure-form-step.service';
import { StepperModule } from 'primeng/stepper';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { Button } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { AssureStepComponent } from './assure-step.component';
import { AyantDroitStepComponent } from './ayant-droit-step.component';
import { ErrorService } from '../../../shared/error.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { CustomerService } from '../customer.service';
import { CommonService } from './common.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';

@Component({
  selector: 'jhi-assure-form-step',
  imports: [
    WarehouseCommonModule,
    StepsModule,
    StepperModule,
    Button,
    ToastModule,
    AssureStepComponent,
    AyantDroitStepComponent,
    ToastAlertComponent,
  ],
  templateUrl: './assure-form-step.component.html',
})
export class AssureFormStepComponent implements OnInit {
  header: string;
  entity?: ICustomer;
  active: number | undefined = 0;
  isSaving = false;
  typeAssure: string | undefined;
  activeStep = 1;
  ayantDroitStepComponent = viewChild<AyantDroitStepComponent>('ayantDroitStep');
  assureStepComponent = viewChild<AssureStepComponent>('assureStep');
  protected readonly commonService = inject(CommonService);
  protected items: MenuItem[];
  protected readonly assureFormStepService = inject(AssureFormStepService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly errorService = inject(ErrorService);
  private readonly customerService = inject(CustomerService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');

  ngOnInit(): void {
    this.commonService.categorieTiersPayant.set(this.typeAssure);
    this.commonService.categorie.set(this.typeAssure);
    this.assureFormStepService.setTypeAssure(this.typeAssure);
    this.assureFormStepService.setAssure(this.entity);
    if (this.entity) {
      this.assureFormStepService.setEdition(true);
    } else {
      this.assureFormStepService.setEdition(false);
    }
  }

  onCompleteAssure(): void {
    if (this.assureStepComponent()) {
      this.currentCustomerState();
    }
    if (!this.assureFormStepService.isEdition()) {
      if (this.ayantDroitStepComponent()) {
        this.ayantDroitStepComponent().saveFormState();
      }
    }
  }

  onGoBackFromAyantDroit(index: number): void {
    this.ayantDroitStepComponent().goBack();
    this.activeStep = index;
  }

  currentCustomerState(): void {
    const currentAssure = this.assureFormStepService.assure();
    const ayantDroits = currentAssure ? currentAssure.ayantDroits : [];
    // const complementaires = currentAssure?.tiersPayants;
    this.assureFormStepService.setAssure({
      ...this.assureStepComponent().createFromForm(),
      ayantDroits,
      // tiersPayants: complementaires,
    });
  }

  onGoAyantDroit(index: number): void {
    this.currentCustomerState();
    this.activeStep = index;
    //  nextCallback.emit();
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  onSaveError(error: any): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  save(): void {
    this.isSaving = true;
    this.onCompleteAssure();
    const customer = this.assureFormStepService.assure();
    if (customer.id !== undefined && customer.id) {
      customer.type = 'ASSURE';
      this.subscribeToSaveResponse(this.customerService.update(customer));
    } else {
      this.subscribeToSaveResponse(this.customerService.create(customer));
    }
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    console.warn('subscribeToSaveResponse');
    result.subscribe({
      next: (res: HttpResponse<ICustomer>) => this.onSaveSuccess(res.body),
      error: (error: any) => this.onSaveError(error),
    });
  }

  private onSaveSuccess(customer: ICustomer | null): void {
    this.isSaving = false;
    this.alert().showInfo('sisdisufisuf');
    // this.assureFormStepService.setAssure(null);
    this.activeModal.close(customer);
  }
}
