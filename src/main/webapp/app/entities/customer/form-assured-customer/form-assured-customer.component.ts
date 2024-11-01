import { AfterViewInit, Component, ElementRef, OnInit, viewChild } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ErrorService } from 'app/shared/error.service';
import { FormArray, FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { TiersPayantService } from 'app/entities/tiers-payant/tierspayant.service';
import { CustomerService } from 'app/entities/customer/customer.service';
import { ITiersPayant } from 'app/shared/model/tierspayant.model';
import { Customer, ICustomer } from 'app/shared/model/customer.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { IClientTiersPayant } from 'app/shared/model/client-tiers-payant.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { ToastModule } from 'primeng/toast';
import { KeyFilterModule } from 'primeng/keyfilter';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { SelectButtonModule } from 'primeng/selectbutton';
import { RadioButtonModule } from 'primeng/radiobutton';
import { CalendarModule } from 'primeng/calendar';
import { DividerModule } from 'primeng/divider';
import { DATE_FORMAT_FROM_STRING_FR, FORMAT_ISO_DATE_TO_STRING_FR } from '../../../shared/util/warehouse-util';
import { InputMaskModule } from 'primeng/inputmask';

@Component({
  selector: 'jhi-form-assured-customer',
  templateUrl: './form-assured-customer.component.html',
  providers: [MessageService, DialogService, ConfirmationService],
  standalone: true,
  imports: [
    WarehouseCommonModule,
    ToastModule,
    FormsModule,
    ButtonModule,
    RippleModule,
    ConfirmDialogModule,
    InputTextModule,
    AutoCompleteModule,
    SelectButtonModule,
    RadioButtonModule,
    DropdownModule,
    ReactiveFormsModule,
    CalendarModule,
    DividerModule,
    KeyFilterModule,
    InputMaskModule,
  ],
})
export class FormAssuredCustomerComponent implements OnInit, AfterViewInit {
  entity?: ICustomer;
  catgories = [
    { label: 'RC1', value: 1 },
    { label: 'RC2', value: 2 },
    { label: 'RC3', value: 3 },
  ];

  isSaving = false;
  isValid = true;
  validSize = true;
  ayantDroitSize = true;
  minLength = 3;
  tiersPayant!: ITiersPayant | null;
  tiersPayants: ITiersPayant[] = [];
  plafonds = [
    { label: 'Non', value: false },
    { label: 'Oui', value: true },
  ];
  firstName = viewChild.required<ElementRef>('firstName');
  editForm = this.fb.group({
    id: [],
    firstName: [null, [Validators.required]],
    lastName: [null, [Validators.required]],
    tiersPayantId: [null, [Validators.required]],
    taux: [null, [Validators.required, Validators.min(5), Validators.max(100)]],
    num: [null, [Validators.required]],
    phone: [],
    email: [],
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
    private messageService: MessageService,
  ) {}

  get editFormGroups(): FormArray {
    return this.editForm.get('tiersPayants') as FormArray;
  }

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    if (this.entity) {
      this.updateForm(this.entity);
      this.buildTiersPayant(this.entity.tiersPayants);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.firstName().nativeElement.focus();
    }, 30);
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

  cancel(): void {
    this.ref.close();
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
      }),
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

  searchTiersPayant(event: any): void {
    this.loadTiersPayants(event.query);
  }

  loadTiersPayants(search?: string): void {
    const query: string = search || '';
    this.tiersPayantService
      .query({
        page: 0,
        size: 10,
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
          }),
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
      datNaiss: customer.datNaiss ? FORMAT_ISO_DATE_TO_STRING_FR(customer.datNaiss) : null,
      sexe: customer.sexe,
      tiersPayantId: customer.tiersPayant,
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
      datNaiss: DATE_FORMAT_FROM_STRING_FR(this.editForm.get(['datNaiss'])!.value),
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
    result.subscribe({
      next: (res: HttpResponse<ICustomer>) => this.onSaveSuccess(res.body),
      error: (error: any) => this.onSaveError(error),
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
