import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { InputMaskModule } from 'primeng/inputmask';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { RadioButton, RadioButtonModule } from 'primeng/radiobutton';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { SelectButtonModule } from 'primeng/selectbutton';
import TranslateDirective from '../../../shared/language/translate.directive';
import { Customer, ICustomer } from '../../../shared/model/customer.model';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { HttpResponse } from '@angular/common/http';
import { CardModule } from 'primeng/card';
import { AssureFormStepService } from './assure-form-step.service';
import { CommonService } from './common.service';
import { ComplementaireStepComponent } from './complementaire-step.component';
import { FormTiersPayantComponent } from '../../tiers-payant/form-tiers-payant/form-tiers-payant.component';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
import { DateNaissDirective } from '../../../shared/date-naiss.directive';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'jhi-assure-step',
  imports: [
    CommonModule,
    AutoCompleteModule,
    RadioButton,
    RadioButtonModule,
    DividerModule,
    DropdownModule,
    InputMaskModule,
    InputTextModule,
    KeyFilterModule,
    RadioButtonModule,
    ReactiveFormsModule,
    SelectButtonModule,
    TranslateDirective,
    CardModule,
    ComplementaireStepComponent,
    DateNaissDirective,
  ],
  templateUrl: './assure-step.component.html',
})
export class AssureStepComponent implements OnInit, AfterViewInit {
  entity?: ICustomer;
  ref!: DynamicDialogRef;
  isSaving = false;
  isValid = true;
  minLength = 2;
  tiersPayant!: ITiersPayant | null;
  tiersPayants: ITiersPayant[] = [];
  fb = inject(UntypedFormBuilder);
  commonService = inject(CommonService);

  assureFormStepService = inject(AssureFormStepService);
  firstName = viewChild.required<ElementRef>('firstName');
  complementaireStepComponent = viewChild(ComplementaireStepComponent);
  editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required]],
    lastName: [null, [Validators.required]],
    tiersPayantId: [null, [Validators.required]],
    taux: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
    num: [null, [Validators.required]],
    phone: [],
    email: [],
    adresse: [],
    sexe: [],
    datNaiss: [],
    remiseId: [],
  });
  private readonly dialogService = inject(DialogService);
  private readonly tiersPayantService = inject(TiersPayantService);

  ngOnInit(): void {
    const entity = this.assureFormStepService.assure();
    if (entity) {
      this.updateForm(entity);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.firstName().nativeElement.focus();
      const entity = this.assureFormStepService.assure();
      if (this.complementaireStepComponent()) {
        this.complementaireStepComponent().initForm(entity);
      }
    }, 30);
  }

  searchTiersPayant(event: any): void {
    this.loadTiersPayants(event.query);
  }

  loadTiersPayants(search?: string): void {
    const query: string = search || '';

    this.tiersPayantService
      .query({
        page: 0,
        size: 10,
        type: this.commonService.categorie(),
        search: query,
      })
      .subscribe((res: HttpResponse<ITiersPayant[]>) => {
        this.tiersPayants = res.body!;
        if (this.tiersPayants.length === 0) {
          this.tiersPayants.push({ id: null, fullName: 'Ajouter un nouveau tiers-payant' });
        }
      });
  }

  onSelectTiersPayant(event: any): void {
    if (event.value?.id === null) {
      this.addTiersPayantAssurance();
    } else {
      this.tiersPayant = event.value;
      this.commonService.setCategorieTiersPayant(this.tiersPayant.categorie);
    }
  }

  createFromForm(): ICustomer {
    return {
      ...new Customer(),
      id: this.editForm.get(['id']).value,
      firstName: this.editForm.get(['firstName']).value,
      lastName: this.editForm.get(['lastName']).value,
      email: this.editForm.get(['email']).value,
      phone: this.editForm.get(['phone']).value,
      type: 'ASSURE',
      num: this.editForm.get(['num']).value,
      datNaiss: this.editForm.get(['datNaiss']).value,
      sexe: this.editForm.get(['sexe']).value,
      tiersPayantId: this.editForm.get(['tiersPayantId']).value.id,
      taux: this.editForm.get(['taux']).value,
      tiersPayant: this.editForm.get(['tiersPayantId']).value,
      tiersPayants: this.buildComplementaires(),
    };
  }

  addTiersPayantAssurance(): void {
    this.ref = this.dialogService.open(FormTiersPayantComponent, {
      data: { entity: null, type: this.assureFormStepService.typeAssure() },
      header: 'FORMULAIRE DE CREATION DE TIERS-PAYANT',
      width: '80%',
    });
    this.ref.onClose.subscribe((tiersPayant: ITiersPayant) => {
      if (tiersPayant) {
        this.tiersPayants.push(tiersPayant);
        this.editForm.patchValue({ tiersPayantId: tiersPayant });
      }
    });
  }

  protected updateForm(customer: ICustomer): void {
    this.editForm.patchValue({
      id: customer.id,
      firstName: customer.firstName,
      lastName: customer.lastName,
      email: customer.email,
      phone: customer.phone,
      num: customer.num,
      datNaiss: customer.datNaiss,
      sexe: customer.sexe,
      tiersPayantId: customer.tiersPayant,
      taux: customer.taux,
    });
  }

  private buildComplementaires(): IClientTiersPayant[] {
    if (this.complementaireStepComponent()) {
      return this.complementaireStepComponent().createFromForm();
    }
    return [];
  }
}
