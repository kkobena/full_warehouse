import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { ClientTiersPayant, IClientTiersPayant } from '../../../../../shared/model/client-tiers-payant.model';
import { ICustomer } from '../../../../../shared/model/customer.model';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { CustomerService } from '../../../../customer/customer.service';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { InputMaskModule } from 'primeng/inputmask';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { RadioButtonModule } from 'primeng/radiobutton';
import { ToastModule } from 'primeng/toast';
import TranslateDirective from '../../../../../shared/language/translate.directive';

@Component({
    selector: 'jhi-add-complementaire',
    imports: [
        ReactiveFormsModule,
        FaIconComponent,
        InputMaskModule,
        InputTextModule,
        KeyFilterModule,
        RadioButtonModule,
        ToastModule,
        TranslateDirective,
    ],
    templateUrl: './add-complementaire.component.html',
    styles: ``
})
export class AddComplementaireComponent implements OnInit, AfterViewInit {
  tiersPayant = viewChild.required<ElementRef>('tiersPayant');
  numBon = viewChild.required<ElementRef>('numBon');
  assure?: ICustomer | null;
  isSaving = false;
  isValid = true;
  tiersPayantsExisting: IClientTiersPayant[] = [];
  customerService = inject(CustomerService);
  editForm = this.fb.group({
    id: new FormControl<number | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    taux: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(5), Validators.max(100)],
      nonNullable: true,
    }),
    categorie: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0), Validators.max(5)],
      nonNullable: true,
    }),
    tiersPayant: new FormControl<IClientTiersPayant | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    numBon: new FormControl<string | null>(null),
    tiersPayantFullName: new FormControl<string | null>(null),
    num: new FormControl<string | null>(null),
  });

  constructor(
    private fb: FormBuilder,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
  ) {}

  getTiersPayants(): IClientTiersPayant[] {
    if (this.tiersPayantsExisting && this.tiersPayantsExisting.length > 0) {
      return this.assure?.tiersPayants.filter(e => !this.tiersPayantsExisting.some(i => i.id === e.id));
    }
    return this.assure?.tiersPayants;
  }

  ngOnInit(): void {
    this.assure = this.config.data.assure;
    this.tiersPayantsExisting = this.config.data.tiersPayantsExisting;
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.tiersPayant().nativeElement.focus();
    }, 30);
  }

  cancel(): void {
    this.ref.close();
  }

  save(): void {
    this.isSaving = true;
    const tiersPayant = this.createFromForm();
    this.ref.close(tiersPayant);
  }

  onSelect(evt: any): void {
    const tiersPayant = this.assure.tiersPayants.find(e => e.id === Number(evt.value));
    if (tiersPayant) {
      this.updateForm(tiersPayant);
      this.numBon().nativeElement.focus();
    } else {
      this.editForm.reset();
    }
  }

  private updateForm(tp: IClientTiersPayant): void {
    this.editForm.patchValue({
      id: tp.id,
      taux: tp.taux,
      num: tp.num,
      tiersPayantFullName: tp.tiersPayantFullName,
      categorie: tp.categorie,
    });
  }

  private createFromForm(): IClientTiersPayant {
    return {
      ...new ClientTiersPayant(),
      id: this.editForm.get(['id'])!.value,
      numBon: this.editForm.get(['numBon'])!.value,
      categorie: this.editForm.get(['categorie'])!.value,
      taux: this.editForm.get(['taux'])!.value,
      priorite: this.editForm.get(['categorie'])!.value,
    };
  }
}
