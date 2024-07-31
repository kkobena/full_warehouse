import { Component, inject, OnInit } from '@angular/core';
import { FormArray, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonDirective } from 'primeng/button';
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
import { MessageService } from 'primeng/api';
import { ErrorService } from '../../../shared/error.service';

@Component({
  selector: 'jhi-complementaire-step',
  standalone: true,
  providers: [MessageService],
  imports: [
    ReactiveFormsModule,
    DropdownModule,
    ButtonDirective,
    KeyFilterModule,
    InputTextModule,
    AutoCompleteModule,
    ToastModule,
    CardModule,
  ],
  templateUrl: './complementaire-step.component.html',
  styles: ``,
})
export class ComplementaireStepComponent implements OnInit {
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

  constructor(private fb: UntypedFormBuilder) {}

  get editFormGroups(): FormArray {
    return this.editForm.get('tiersPayants') as FormArray;
  }

  ngOnInit(): void {
    const currentAssure = this.assureFormStepService.assure();
    if (currentAssure?.tiersPayants?.length > 0) {
      this.buildTiersPayant(currentAssure.tiersPayants);
    } else {
      this.addTiersPayant();
    }
  }

  addTiersPayant(): void {
    const tiersPayants = this.editForm.get('tiersPayants') as FormArray;
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

  convertFormAsFormArray(): FormArray<any> {
    return this.editForm.get('tiersPayants') as FormArray;
  }

  onSelectTiersPayant(event: any): void {
    this.tiersPayant = event;
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

  createFromForm(): IClientTiersPayant[] {
    return this.editForm.get(['tiersPayants'])!.value.flatMap((tiersPayant: any) => [
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
            //  tiersPayantId: complementiare,
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

  goBack(): void {
    const customer = this.assureFormStepService.assure();
    if (this.editForm.get(['tiersPayants']).valid) {
      customer.tiersPayants = this.createFromForm();

      this.assureFormStepService.setAssure(customer);
    }
  }

  saveFormState(): void {
    const customer = this.assureFormStepService.assure();
    if (this.editForm.get(['tiersPayants']).valid) {
      customer.tiersPayants = this.createFromForm();
      this.assureFormStepService.setAssure(customer);
    }
  }

  removeTiersPayant(index: number): void {
    const tiersPayants = this.convertFormAsFormArray();
    const tiersPayant = tiersPayants.at(index).value as IClientTiersPayant;
    console.log('tiersPayant', tiersPayant);
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
}
