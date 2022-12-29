import {Component, OnInit} from '@angular/core';
import {ConfirmationService, MessageService} from 'primeng/api';
import {DialogService, DynamicDialogConfig, DynamicDialogRef} from 'primeng/dynamicdialog';
import {ErrorService} from 'app/shared/error.service';
import {FormArray, UntypedFormBuilder, Validators} from '@angular/forms';
import {TiersPayantService} from 'app/entities/tiers-payant/tierspayant.service';
import {CustomerService} from 'app/entities/customer/customer.service';
import {ITiersPayant} from 'app/shared/model/tierspayant.model';
import {Customer, ICustomer} from 'app/shared/model/customer.model';
import {Observable} from 'rxjs';
import {HttpResponse} from '@angular/common/http';
import moment from 'moment';
import {IClientTiersPayant} from 'app/shared/model/client-tiers-payant.model';

@Component({
  selector: 'jhi-form-assured-customer',
  templateUrl: './form-assured-customer.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
})
export class FormAssuredCustomerComponent implements OnInit {
  entity?: ICustomer;
  catgories = [
    {label: 'RC1', value: 1},
    {label: 'RC2', value: 2},
    {label: 'RC3', value: 3},
  ];

  categoriesComplementaires = [
    {name: 'T0', value: 0},
    {name: 'T1', value: 1},
    {name: 'T2', value: 2},
    {
      name: 'T3',
      value: 3,
    },
  ];
  isSaving = false;
  isValid = true;
  validSize = true;
  ayantDroitSize = true;
  minLength = 2;
  selectedTiersPayant!: ITiersPayant | null;
  tiersPayant!: ITiersPayant | null;
  tiersPayants: ITiersPayant[] = [];
  maxDate = new Date();
  plafonds = [
    {label: 'Non', value: false},
    {label: 'Oui', value: true},
  ];
  editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required]],
    lastName: [null, [Validators.required]],
    tiersPayantId: [null, [Validators.required]],
    taux: [null, [Validators.required, Validators.min(10), Validators.max(100)]],
    num: [null, [Validators.required]],
    phone: [],
    email: [],
    telephone: [],
    adresse: [],
    sexe: [],
    datNaiss: [],
    remiseId: [],
    plafondConso: [],
    plafondJournalier: [],
    plafondAbsolu: [],
    tiersPayants: this.fb.array([]),
  });

  constructor(
    protected errorService: ErrorService,
    private fb: UntypedFormBuilder,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    protected tiersPayantService: TiersPayantService,
    protected customerService: CustomerService,
    private messageService: MessageService
  ) {
  }

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    if (this.entity) {
      this.updateForm(this.entity);
      this.buildTiersPayant(this.entity.tiersPayants!);
    }
    this.populate();
  }

  save(): void {
    this.isSaving = true;
    const customer = this.createFromForm();
    if (customer.id !== undefined && customer.id) {
      customer.type = 'ASSURE';
      this.subscribeToSaveResponse(this.customerService.update(customer));
    } else {
      this.subscribeToSaveResponse(this.customerService.create(customer));
    }
  }

  get editFormGroups(): FormArray {
    return this.editForm.get('tiersPayants') as FormArray;
  }

  cancel(): void {
    this.ref.close();
  }

  populate(): void {
    this.tiersPayantService
      .query({
        size: 9999,
        page: 0,
      })
      .subscribe((res: HttpResponse<ITiersPayant[]>) => {
        this.tiersPayants = res.body || [];
        if (this.entity) {
          this.selectedTiersPayant = this.tiersPayants.find(e => e.id === this.entity?.tiersPayantId) || null;
        }
      });
  }

  addAyantDroit(): void {
    const ayantDroits = this.editForm.get('ayantDroits') as FormArray;
    ayantDroits.push(
      this.fb.group({
        num: [null, [Validators.required]],
        firstName: [null, [Validators.required]],
        lastName: [null, [Validators.required]],
        sexe: [],
        datNaiss: [],
        phone: [],
        id: [],
      })
    );
    this.valideAyantDroitSize();
  }

  addTiersPayant(): void {
    const tiersPayants = this.editForm.get('tiersPayants') as FormArray;
    tiersPayants.push(
      this.fb.group({
        taux: [null, [Validators.required, Validators.min(10), Validators.max(100)]],
        tiersPayantId: [null, [Validators.required]],
        num: [null, [Validators.required]],
        id: [],
        plafondConso: [],
        plafondJournalier: [],
        plafondAbsolu: [],
        priorite: tiersPayants.length + 1,
      })
    );
    this.validateTiersPayantSize();
  }

  validateTiersPayantSize(): void {
    const tiersPayants = this.editForm.get('tiersPayants') as FormArray;
    this.validSize = tiersPayants.length < 3;
  }

  removeTiersPayant(index: number): void {
    const tiersPayants = this.editForm.get('tiersPayants') as FormArray;
    tiersPayants.removeAt(index);
    this.validateTiersPayantSize();
  }

  removeAyantDroit(index: number): void {
    const ayantDroits = this.editForm.get('ayantDroits') as FormArray;
    ayantDroits.removeAt(index);
    this.valideAyantDroitSize();
  }

  valideAyantDroitSize(): void {
    const ayantDroits = this.editForm.get('ayantDroits') as FormArray;
    this.ayantDroitSize = ayantDroits.length < 1;
  }

  searchTiersPayant(event: any): void {
    this.loadTiersPayants(event.query);
  }

  loadTiersPayants(search?: string): void {
    const query: String = search || '';
    this.tiersPayantService
      .query({
        page: 0,
        size: 99999,
        type: 'ASSURANCE',
        search: query,
      })
      .subscribe((res: HttpResponse<ITiersPayant[]>) => (this.tiersPayants = res.body!));
  }

  buildTiersPayant(tierPayant: IClientTiersPayant[]): IClientTiersPayant[] {
    const newArray: IClientTiersPayant[] = [];
    tierPayant
      .filter(clt => clt.categorie !== 0)
      .forEach(tp => {
        const complementiare = tp.tiersPayant;
        newArray.push({
          id: tp.id,
          num: tp.num,
          tiersPayantId: complementiare,
          plafondConso: tp.plafondConso,
          plafondJournalier: tp.plafondJournalier,
          priorite: tp.categorie,
          plafondAbsolu: tp.plafondAbsolu,
          taux: tp.taux,
        });
        this.editFormGroups.push(
          this.fb.group({
            id: tp.id,
            num: tp.num,
            tiersPayantId: complementiare,
            plafondConso: tp.plafondConso,
            plafondJournalier: tp.plafondJournalier,
            priorite: tp.categorie,
            plafondAbsolu: tp.plafondAbsolu,
            taux: tp.taux,
          })
        );
      });

    return newArray;
  }

  onSelectTiersPayant(event: any): void {
    this.tiersPayant = event;
  }

  protected updateForm(customer: ICustomer): void {
    this.editForm.patchValue({
      id: customer.id,
      firstName: customer.firstName,
      lastName: customer.lastName,
      email: customer.email,
      phone: customer.phone,
      num: customer.num,
      datNaiss: customer.datNaiss ? new Date(moment(customer.datNaiss).format('yyyy-MM-DD')) : null,
      sexe: customer.sexe,
      tiersPayantId: customer.tiersPayantId,
      plafondConso: customer.plafondConso,
      plafondJournalier: customer.plafondJournalier,
      plafondAbsolu: customer.plafondAbsolu,
      taux: customer.taux,
    });
  }

  protected createFromForm(): ICustomer {
    return {
      ...new Customer(),
      id: this.editForm.get(['id'])!.value,
      firstName: this.editForm.get(['firstName'])!.value,
      lastName: this.editForm.get(['lastName'])!.value,
      email: this.editForm.get(['email'])!.value,
      phone: this.editForm.get(['phone'])!.value,
      type: 'ASSURE',
      num: this.editForm.get(['num'])!.value,
      datNaiss: moment(this.editForm.get(['datNaiss'])!.value),
      sexe: this.editForm.get(['sexe'])!.value,
      tiersPayantId: this.editForm.get(['tiersPayantId'])!.value.id,
      plafondConso: this.editForm.get(['plafondConso'])!.value,
      plafondJournalier: this.editForm.get(['plafondJournalier'])!.value,
      plafondAbsolu: this.editForm.get(['plafondAbsolu'])!.value,
      taux: this.editForm.get(['taux'])!.value,
      tiersPayants: this.editForm.get(['tiersPayants'])!.value.flatMap((tiersPayant: any) => [
        {
          taux: tiersPayant.taux,
          tiersPayantId: tiersPayant.tiersPayantId?.id,
          num: tiersPayant.num,
          plafondConso: tiersPayant.plafondConso,
          plafondJournalier: tiersPayant.plafondJournalier,
          priorite: tiersPayant.priorite,
          plafondAbsolu: tiersPayant.plafondAbsolu,
          id: tiersPayant.id,
        },
      ]),
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ICustomer>>): void {
    result.subscribe(
      res => this.onSaveSuccess(res.body),
      error => this.onSaveError(error)
    );
  }

  protected onSaveSuccess(customer: ICustomer | null): void {
    this.isSaving = false;
    this.ref.close(customer);
  }

  protected onSaveError(error: any): void {
    this.isSaving = false;
    if (error.error && error.error.errorKey) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(translatedErrorMessage => {
        this.messageService.add({severity: 'error', summary: 'Erreur', detail: translatedErrorMessage});
      });
    } else {
      this.messageService.add({severity: 'error', summary: 'Erreur', detail: 'Erreur interne du serveur.'});
    }
  }
}
