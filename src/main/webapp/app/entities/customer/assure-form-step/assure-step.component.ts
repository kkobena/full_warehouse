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
import { IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
import { DateNaissDirective } from '../../../shared/date-naiss.directive';
import { CommonModule } from '@angular/common';
import { showCommonModal } from '../../sales/selling-home/sale-helper';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FormTiersPayantComponent } from '../../tiers-payant/form-tiers-payant/form-tiers-payant.component';

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
  header: string | null = null;
  entity?: ICustomer;
  isSaving = false;
  isValid = true;
  minLength = 2;
  tiersPayant!: ITiersPayant | null;
  tiersPayants: ITiersPayant[] = [];
  commonService = inject(CommonService);
  assureFormStepService = inject(AssureFormStepService);
  firstName = viewChild.required<ElementRef>('firstName');
  complementaireStepComponent = viewChild<ComplementaireStepComponent>('complementaireStep');
  fb = inject(UntypedFormBuilder);
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
  readonly tiersPayantService = inject(TiersPayantService);
  readonly modalService = inject(NgbModal);

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
    }, 100);
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
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: null,
        categorie: this.assureFormStepService.typeAssure(),
        header: 'FORMULAIRE DE CREATION DE TIERS-PAYANT',
      },
      (resp: ITiersPayant) => {
        if (resp) {
          this.tiersPayants.push(resp);
          this.editForm.patchValue({ tiersPayantId: resp });
        }
      },
      'xl',
      'modal-dialog-80',
    );
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
