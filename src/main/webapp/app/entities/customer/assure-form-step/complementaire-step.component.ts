import { Component, inject, OnDestroy, viewChild } from '@angular/core';
import { FormArray, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
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
import { CardModule } from 'primeng/card';
import { ErrorService } from '../../../shared/error.service';
import { ICustomer } from '../../../shared/model/customer.model';
import { FormTiersPayantComponent } from '../../tiers-payant/form-tiers-payant/form-tiers-payant.component';
import { Select, SelectModule } from 'primeng/select';
import { showCommonModal } from '../../sales/selling-home/sale-helper';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Tooltip } from 'primeng/tooltip';

@Component({
  selector: 'jhi-complementaire-step',
  imports: [
    ReactiveFormsModule,
    SelectModule,
    KeyFilterModule,
    InputTextModule,
    AutoCompleteModule,
    CardModule,
    ButtonModule,
    Select,
    ConfirmDialogComponent,
    ToastAlertComponent,
    Tooltip,
  ],
  templateUrl: './complementaire-step.component.html',
})
export class ComplementaireStepComponent implements OnDestroy {
  assureFormStepService = inject(AssureFormStepService);
  customerService = inject(CustomerService);
  tiersPayant!: ITiersPayant | null;
  tiersPayants: ITiersPayant[] = [];
  validSize = true;
  protected fb = inject(UntypedFormBuilder);
  protected catgories = [
    { label: 'RC1', value: 1 },
    { label: 'RC2', value: 2 },
    { label: 'RC3', value: 3 },
  ];
  protected minLength = 3;
  protected editForm = this.fb.group({
    tiersPayants: this.fb.array([]),
  });
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
  private destroy$ = new Subject<void>();

  get editFormGroups(): FormArray {
    return this.editForm.get('tiersPayants') as FormArray;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
          this.convertFormAsFormArray().at(index).patchValue({ resp });
        }
      },
      'xl',
      'modal-dialog-80',
    );
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
      .pipe(takeUntil(this.destroy$))
      .subscribe((res: HttpResponse<ITiersPayant[]>) => {
        this.tiersPayants = res.body!;
        if (this.tiersPayants.length === 0) {
          this.tiersPayants.push({ id: null, fullName: 'Ajouter un nouveau tiers-payant' });
        }
      });
  }

  createFromForm(): IClientTiersPayant[] {
    const formValue = this.editForm.value;
    return formValue.tiersPayants.flatMap((tiersPayant: any) => [
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
      this.customerService
        .deleteTiersPayant(tiersPayant.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
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
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  confirmRemove(index: number): void {
    this.confimDialog().onConfirm(() => this.removeTiersPayant(index), 'Suppression', 'Voulez-vous vraiment ce complémentaire ?');
  }
}
