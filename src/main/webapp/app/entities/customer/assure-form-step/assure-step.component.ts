import { AfterViewInit, Component, ElementRef, OnInit, viewChild } from '@angular/core';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonDirective } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { InputMaskModule } from 'primeng/inputmask';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { RadioButtonModule } from 'primeng/radiobutton';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { SelectButtonModule } from 'primeng/selectbutton';
import TranslateDirective from '../../../shared/language/translate.directive';
import { Customer, ICustomer } from '../../../shared/model/customer.model';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { HttpResponse } from '@angular/common/http';
import { DATE_FORMAT_FROM_STRING_FR, FORMAT_ISO_DATE_TO_STRING_FR } from '../../../shared/util/warehouse-util';
import { CardModule } from 'primeng/card';
import { AssureFormStepService } from './assure-form-step.service';

@Component({
  selector: 'jhi-assure-step',
  standalone: true,
  imports: [
    AutoCompleteModule,
    ButtonDirective,
    DividerModule,
    DropdownModule,
    FaIconComponent,
    InputMaskModule,
    InputTextModule,
    KeyFilterModule,
    RadioButtonModule,
    ReactiveFormsModule,
    SelectButtonModule,
    TranslateDirective,
    CardModule,
  ],
  templateUrl: './assure-step.component.html',
  styles: ``,
})
export class AssureStepComponent implements OnInit, AfterViewInit {
  entity?: ICustomer;

  isSaving = false;
  isValid = true;
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
  });

  constructor(
    private fb: UntypedFormBuilder,
    private tiersPayantService: TiersPayantService,
    private assureFormStepService: AssureFormStepService,
  ) {}

  ngOnInit(): void {
    const entity = this.assureFormStepService.assure();
    if (entity) {
      this.updateForm(entity);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.firstName().nativeElement.focus();
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
        type: 'ASSURANCE',
        search: query,
      })
      .subscribe((res: HttpResponse<ITiersPayant[]>) => (this.tiersPayants = res.body!));
  }

  onSelectTiersPayant(event: any): void {
    this.tiersPayant = event;
  }

  createFromForm(): ICustomer {
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
      tiersPayant: this.editForm.get(['tiersPayantId'])!.value,
    };
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
}
