import { Component, inject } from '@angular/core';
import { FormArray, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { HttpResponse } from '@angular/common/http';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { AssureFormStepService } from './assure-form-step.service';
import { IClientTiersPayant } from '../../../shared/model/client-tiers-payant.model';
import { CustomerService } from '../customer.service';
import { ToastModule } from 'primeng/toast';
import { CardModule } from 'primeng/card';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ErrorService } from '../../../shared/error.service';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ICustomer } from '../../../shared/model/customer.model';
import { FormTiersPayantComponent } from '../../tiers-payant/form-tiers-payant/form-tiers-payant.component';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { Select } from 'primeng/select';

@Component({
  selector: 'jhi-complementaire-step',
  providers: [MessageService, ConfirmationService],
  imports: [
    ReactiveFormsModule,
    DropdownModule,
    KeyFilterModule,
    InputTextModule,
    AutoCompleteModule,
    ToastModule,
    CardModule,
    ConfirmDialogModule,
    ButtonModule,
    Select,
  ],
  templateUrl: './complementaire-step.component.html',
  styles: ``,
})
export class ComplementaireStepComponent {
  private fb = inject(UntypedFormBuilder);

  catgories = [
    { label: 'RC1', value: 1 },
    { label: 'RC2', value: 2 },
    { label: 'RC3', value: 3 },
  ];
  minLength = 3;
  tiersPayant!: ITiersPayant | null;
  tiersPayants: ITiersPayant[] = [];
  validSize = true;
  editForm = this.fb.group({
    tiersPayants: this.fb.array([]),
  });
  tiersPayantService = inject(TiersPayantService);
  assureFormStepService = inject(AssureFormStepService);
  customerService = inject(CustomerService);
  messageService = inject(MessageService);
  errorService = inject(ErrorService);
  confirmationService = inject(ConfirmationService);
  dialogService = inject(DialogService);
  ref!: DynamicDialogRef;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  get editFormGroups(): FormArray {
    return this.editForm.get('tiersPayants') as FormArray;
  }

  initForm(current: ICustomer): void {
    if (current.tiersPayants.length > 0) {
      this.buildTiersPayant(current.tiersPayants);
    }
  }

  addTiersPayant(): void {
    const tiersPayants = this.editFormGroups;
    tiersPayants.push(
      this.fb.group({
        taux: [null, [Validators.required, Validators.min(10), Validators.max(100)]],
        tiersPayant: [null, [Validators.required]],
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
    const tiersPayants = this.convertFormAsFormArray();
    this.validSize = tiersPayants.length < 3;
  }

  convertFormAsFormArray(): FormArray {
    return this.editForm.get('tiersPayants') as FormArray;
  }

  onSelectTiersPayant(event: any, index: number): void {
    if (event.value?.id === null) {
      this.addTiersPayantAssurance(index);
    } else {
      this.tiersPayant = event.value;
    }
  }

  addTiersPayantAssurance(index: number): void {
    this.ref = this.dialogService.open(FormTiersPayantComponent, {
      data: { entity: null, type: this.assureFormStepService.typeAssure() },
      header: 'FORMULAIRE DE CREATION DE TIERS-PAYANT',
      width: '80%',
    });
    this.ref.onClose.subscribe((tiersPayant: ITiersPayant) => {
      if (tiersPayant) {
        this.tiersPayants.push(tiersPayant);
        this.convertFormAsFormArray().at(index).patchValue({ tiersPayant });
      }
    });
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
      .subscribe((res: HttpResponse<ITiersPayant[]>) => {
        this.tiersPayants = res.body!;
        if (this.tiersPayants.length === 0) {
          this.tiersPayants.push({ id: null, fullName: 'Ajouter un nouveau tiers-payant' });
        }
      });
  }

  createFromForm(): IClientTiersPayant[] {
    return this.editForm.get(['tiersPayants']).value.flatMap((tiersPayant: any) => [
      {
        id: tiersPayant.id,
        taux: tiersPayant.taux,
        tiersPayant: tiersPayant.tiersPayant,
        tiersPayantId: tiersPayant.tiersPayant?.id,
        num: tiersPayant.num,
        plafondConso: tiersPayant.plafondConso,
        plafondJournalier: tiersPayant.plafondJournalier,
        priorite: tiersPayant.priorite,
        categorie: tiersPayant.priorite,
        plafondAbsolu: tiersPayant.plafondAbsolu,
      },
    ]);
  }

  buildTiersPayant(tierPayant: IClientTiersPayant[]): void {
    tierPayant
      .filter(clt => clt.categorie !== 0)
      .forEach(tp => {
        const complementiare = tp.tiersPayant;
        this.editFormGroups.push(
          this.fb.group({
            id: tp.id,
            num: tp.num,
            tiersPayant: complementiare,
            plafondConso: tp.plafondConso,
            plafondJournalier: tp.plafondJournalier,
            priorite: tp.categorie,
            plafondAbsolu: tp.plafondAbsolu,
            taux: tp.taux,
          }),
        );
      });
  }

  removeTiersPayant(index: number): void {
    const tiersPayants = this.convertFormAsFormArray();
    const tiersPayant = tiersPayants.at(index).value as IClientTiersPayant;
    if (tiersPayant.id) {
      this.customerService.deleteTiersPayant(tiersPayant.id).subscribe({
        next: () => {
          tiersPayants.removeAt(index);
          this.validateTiersPayantSize();
        },
        error: err => this.onSaveError(err),
      });
    } else {
      tiersPayants.removeAt(index);
      this.validateTiersPayantSize();
    }
  }

  onSaveError(error: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: this.errorService.getErrorMessage(error),
    });
  }

  confirmRemove(index: number): void {
    this.confirmationService.confirm({
      message: 'Voulez-vous vraiment ce complémentaire ?',
      header: 'Suppression',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.removeTiersPayant(index);
      },
      key: 'deletecomplementaire',
    });
  }
}
