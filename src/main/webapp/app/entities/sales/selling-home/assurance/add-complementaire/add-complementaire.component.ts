import { AfterViewInit, Component, ElementRef, inject, viewChild } from '@angular/core';
import { ClientTiersPayant, IClientTiersPayant } from '../../../../../shared/model/client-tiers-payant.model';
import { ICustomer } from '../../../../../shared/model/customer.model';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputMaskModule } from 'primeng/inputmask';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { RadioButtonModule } from 'primeng/radiobutton';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { DecimalPipe } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Select, SelectModule } from 'primeng/select';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-add-complementaire',
  imports: [
    ReactiveFormsModule,
    InputMaskModule,
    InputTextModule,
    KeyFilterModule,
    RadioButtonModule,
    ToastModule,
    ButtonModule,
    DecimalPipe,
    SelectModule,
    Card,
  ],
  templateUrl: './add-complementaire.component.html',
  styleUrls: ['./add-complementaire.component.scss'],
})
export class AddComplementaireComponent implements AfterViewInit {
  tiersPayant = viewChild.required<Select>('tiersPayant');
  numBon = viewChild.required<ElementRef>('numBon');
  assure?: ICustomer | null;
  isSaving = false;
  isValid = true;
  tiersPayantsExisting: IClientTiersPayant[] = [];
  tiersPayants: IClientTiersPayant[] = [];

  protected selectedTiersPayant: IClientTiersPayant | null = null;
  protected fb = inject(FormBuilder);
  protected editForm = this.fb.group({
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

  private readonly activeModal = inject(NgbActiveModal);

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.tiersPayant().focus();
    }, 100);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    this.isSaving = true;
    this.activeModal.close(this.createFromForm());
  }

  onSelect(evt: any): void {
    this.selectedTiersPayant = this.assure.tiersPayants.find(e => e.id === Number(evt.value));
    if (this.selectedTiersPayant) {
      this.updateForm(this.selectedTiersPayant);
      setTimeout(() => {
        this.numBon().nativeElement.focus();
      }, 50);
    } else {
      this.editForm.reset();
    }
  }

  protected getTiersPayants(): IClientTiersPayant[] {
    if (this.tiersPayantsExisting && this.tiersPayantsExisting.length > 0) {
      return this.assure.tiersPayants.filter(e => !this.tiersPayantsExisting.some(i => i.id === e.id));
    } else {
      return this.assure.tiersPayants;
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
      id: this.editForm.get(['id']).value,
      numBon: this.editForm.get(['numBon']).value,
      categorie: this.editForm.get(['categorie']).value,
      taux: this.editForm.get(['taux']).value,
      priorite: this.editForm.get(['categorie']).value,
    };
  }
}
