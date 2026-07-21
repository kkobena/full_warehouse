import {ChangeDetectionStrategy, Component, inject, model, OnDestroy} from '@angular/core';
import {FormArray, ReactiveFormsModule, UntypedFormBuilder, Validators} from '@angular/forms';
import {HttpResponse} from '@angular/common/http';
import {IClientTiersPayant, ICustomer, ITiersPayant} from '../../../shared/model';
import {TiersPayantService} from '../../tiers-payant/tierspayant.service';
import {AssureFormStepService} from './assure-form-step.service';
import {CustomerService} from '../customer.service';
import {ErrorService} from '../../../shared/error.service';
import {
  FormTiersPayantComponent
} from '../../tiers-payant/form-tiers-payant/form-tiers-payant.component';
import {showCommonModal} from '../../sales/selling-home/sale-helper';
import {NgbModal, NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';
import {
  NgbConfirmDialogService
} from "../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {NotificationService} from "../../../shared/services/notification.service";
import {
  ButtonComponent,
  CardComponent,
  KeyFilterDirective,
  SelectComponent,
  SelectSearchComponent
} from '../../../shared/ui';

@Component({
  selector: 'jhi-complementaire-step',
  imports: [
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    KeyFilterDirective,
    SelectComponent,
    SelectSearchComponent,
    NgbTooltip
  ],
  templateUrl: './complementaire-step.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./assured-form-step.component.scss'],
})
export class ComplementaireStepComponent implements OnDestroy {
  assureFormStepService = inject(AssureFormStepService);
  customerService = inject(CustomerService);
  tiersPayant: ITiersPayant | null = null;
  tiersPayants: ITiersPayant[] = [];
  tiersPayantAlreadyAdded = model<IClientTiersPayant[]>([]);
  validSize = true;
  protected fb = inject(UntypedFormBuilder);
  protected catgories = [
    {label: 'RC1', value: 1},
    {label: 'RC2', value: 2},
    {label: 'RC3', value: 3},
  ];
  protected minLength = 3;
  protected editForm = this.fb.group({
    tiersPayants: this.fb.array([]),
  });
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
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

  /**
   * `(selectionChange)` d'app-select-search émet la **valeur** sélectionnée, là où le
   * `(onSelect)` de `p-autocomplete` émettait un objet `{ value, originalEvent }`.
   * Lire `event.value` renvoyait donc toujours `undefined`.
   */
  onSelectTiersPayant(tiersPayant: any, index: number): void {
    if (tiersPayant?.id === null) {
      this.addTiersPayantAssurance(index);
    } else if (tiersPayant) {
      this.tiersPayant = tiersPayant;
      this.addToAlreadyAdded(tiersPayant);
    }
  }

  addTiersPayantAssurance(index: number): void {
    showCommonModal(
      this.modalService,
      FormTiersPayantComponent,
      {
        entity: null,
        categorie: this.assureFormStepService.typeAssure(),
        title: 'FORMULAIRE DE CREATION DE TIERS-PAYANT',
      },
      (resp: ITiersPayant) => {
        if (resp) {
          this.tiersPayants.push(resp);
          this.convertFormAsFormArray().at(index).patchValue({tiersPayant: resp});
          this.addToAlreadyAdded(resp);
        }
      },
      'xl',
      'modal-dialog-80',
    );
  }

  searchTiersPayant(query: string): void {
    this.loadTiersPayants(query);
  }

  loadTiersPayants(search?: string): void {
    const query: string = search || '';
    this.tiersPayantService
      .query({
        page: 0,
        size: 10,
        //  type: 'ASSURANCE',
        search: query,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe((res: HttpResponse<ITiersPayant[]>) => {
        const alreadyAddedIds = this.tiersPayantAlreadyAdded().map(tp => tp.tiersPayantId ?? tp.tiersPayant?.id);
        this.tiersPayants = res.body.filter(tp => !alreadyAddedIds.includes(tp.id));
        if (this.tiersPayants.length === 0) {
          this.tiersPayants.push({id: null, fullName: 'Ajouter un nouveau tiers-payant'});
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
    const tiersPayantId = tiersPayant.tiersPayantId ?? tiersPayant.tiersPayant?.id;
    if (tiersPayant.id) {
      this.customerService
        .deleteTiersPayant(tiersPayant.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            tiersPayants.removeAt(index);
            this.removeFromAlreadyAdded(tiersPayantId);
            this.validateTiersPayantSize();
          },
          error: err => this.onSaveError(err),
        });
    } else {
      tiersPayants.removeAt(index);
      this.removeFromAlreadyAdded(tiersPayantId);
      this.validateTiersPayantSize();
    }
  }

  onSaveError(error: any): void {
    this.notificationService.error(this.errorService.getErrorMessage(error));
  }

  confirmRemove(index: number): void {
    if (this.tiersPayant != null) {

      this.confirmDialog.onConfirm(() => this.removeTiersPayant(index),
        ' Suppression', 'Voulez-vous vraiment ce complémentaire ?'
      )
    } else {
      this.removeTiersPayant(index);
    }
  }

  private addToAlreadyAdded(tiersPayant: ITiersPayant): void {
    const current = this.tiersPayantAlreadyAdded();
    if (!current.some(tp => (tp.tiersPayantId ?? tp.tiersPayant?.id) === tiersPayant.id)) {
      this.tiersPayantAlreadyAdded.set([...current, {tiersPayantId: tiersPayant.id, tiersPayant}]);
    }
  }

  private removeFromAlreadyAdded(tiersPayantId: number | undefined): void {
    if (tiersPayantId) {
      const current = this.tiersPayantAlreadyAdded();
      this.tiersPayantAlreadyAdded.set(current.filter(tp => (tp.tiersPayantId ?? tp.tiersPayant?.id) !== tiersPayantId));
    }
  }
}
