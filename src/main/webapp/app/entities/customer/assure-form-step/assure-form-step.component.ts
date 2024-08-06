import { Component, inject, OnInit, viewChild } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { StepsModule } from 'primeng/steps';
import { MenuItem, MessageService } from 'primeng/api';
import { ICustomer } from '../../../shared/model/customer.model';
import { AssureFormStepService } from './assure-form-step.service';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import TranslateDirective from '../../../shared/language/translate.directive';
import { StepperModule } from 'primeng/stepper';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { Button } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { AssureStepComponent } from './assure-step.component';
import { AyantDroitStepComponent } from './ayant-droit-step.component';
import { ComplementaireStepComponent } from './complementaire-step.component';
import { ErrorService } from '../../../shared/error.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { CustomerService } from '../customer.service';
import { CommonService } from './common.service';

@Component({
  selector: 'jhi-assure-form-step',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    RouterOutlet,
    StepsModule,
    FaIconComponent,
    TranslateDirective,
    StepperModule,
    Button,
    ToastModule,
    AssureStepComponent,
    AyantDroitStepComponent,
    ComplementaireStepComponent,
  ],
  templateUrl: './assure-form-step.component.html',
  providers: [MessageService],
})
export class AssureFormStepComponent implements OnInit {
  ayantDroitStepComponent = viewChild(AyantDroitStepComponent);
  complementaireStepComponent = viewChild(ComplementaireStepComponent);
  assureStepComponent = viewChild(AssureStepComponent);
  items: MenuItem[];
  entity?: ICustomer;
  active: number | undefined = 0;
  commonService = inject(CommonService);
  assureFormStepService = inject(AssureFormStepService);
  messageService = inject(MessageService);
  errorService = inject(ErrorService);
  customerService = inject(CustomerService);
  isSaving = false;

  constructor(
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
  ) {}

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    const typeVente = this.config.data.typeVente;
    this.commonService.categorieTiersPayant.set(typeVente);
    this.commonService.categorie.set(typeVente);

    if (this.entity) {
      this.assureFormStepService.setEdition(true);
      this.assureFormStepService.setAssure(this.entity);
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
    if (this.complementaireStepComponent()) {
      this.complementaireStepComponent().saveFormState();
    }
  }

  onGoBackFromComplementaire(prevCallback: any): void {
    this.complementaireStepComponent().goBack();
    prevCallback.emit();
  }

  onGoBackFromAyantDroit(prevCallback: any): void {
    this.ayantDroitStepComponent().goBack();
    prevCallback.emit();
  }

  currentCustomerState(): void {
    const currentAssure = this.assureFormStepService.assure();
    const ayantDroits = currentAssure?.ayantDroits;
    const complementaires = currentAssure?.tiersPayants;
    this.assureFormStepService.setAssure({
      ...this.assureStepComponent().createFromForm(),
      ayantDroits: ayantDroits,
      tiersPayants: complementaires,
    });
  }

  onGoAyantDroit(nextCallback: any): void {
    this.currentCustomerState();
    nextCallback.emit();
  }

  cancel(): void {
    this.ref.close();
  }

  onSaveError(error: any): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: this.errorService.getErrorMessage(error),
    });
  }

  onGoComplementaire(nextCallback: any): void {
    this.ayantDroitStepComponent().onNext();
    nextCallback.emit();
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

  subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result.subscribe({
      next: (res: HttpResponse<ICustomer>) => this.onSaveSuccess(res.body),
      error: (error: any) => this.onSaveError(error),
    });
  }

  onSaveSuccess(customer: ICustomer | null): void {
    this.isSaving = false;
    this.assureFormStepService.setAssure(null);
    this.ref.close(customer);
  }
}
