import {
  Component,
  DestroyRef,
  ElementRef,
  inject,
  OnInit,
  AfterViewInit,
  viewChild,
  ChangeDetectionStrategy
} from '@angular/core';
import {
  ClientTiersPayant,
  IClientTiersPayant
} from '../../../../../shared/model/client-tiers-payant.model';
import {ICustomer} from '../../../../../shared/model';
import {
  FormBuilder,
  FormControl,
  FormsModule,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {CustomerService} from '../../../../customer/customer.service';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize} from 'rxjs/operators';
import {ButtonComponent, CardComponent, KeyFilterDirective, SelectComponent, SwitchComponent} from '../../../../../shared/ui';

@Component({
  selector: 'jhi-add-complementaire',
  imports: [
    ReactiveFormsModule,
    FormsModule,
    ButtonComponent,
    CardComponent,
    KeyFilterDirective,
    SelectComponent,
    SwitchComponent,
  ],
  templateUrl: './add-complementaire.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./add-complementaire.component.scss'],
})
export class AddComplementaireComponent implements OnInit, AfterViewInit {
  tiersPayant = viewChild.required('tiersPayant', { read: ElementRef<HTMLElement> });
  numBon = viewChild.required<ElementRef>('numBon');
  assure?: ICustomer | null;
  isSaving = false;
  isValid = true;
  tiersPayantsExisting: IClientTiersPayant[] = [];
  tiersPayants: IClientTiersPayant[] = [];

  protected selectedTiersPayant: IClientTiersPayant | null = null;
  protected originalTiersPayant: IClientTiersPayant | null = null;
  protected showPrioriteSwitch = false;
  protected isPrioriteEnabled = false;
  protected prioriteValue = 0;

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
  private readonly customerService = inject(CustomerService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.editForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.checkForChanges();
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.tiersPayant().nativeElement.querySelector('input')?.focus();
    }, 100);
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    if (this.hasEntityChanges()) {
      // If there are entity changes (not just numBon), make backend call
      this.saveWithBackendCall();
    } else {
      // If only numBon changed, just close with data
      this.isSaving = true;
      this.activeModal.close(this.createFromForm());
    }
  }

  onSelect(tiersPayantId: number): void {
    this.selectedTiersPayant = this.assure.tiersPayants.find(e => e.id === Number(tiersPayantId));
    if (this.selectedTiersPayant) {
      // Store original state for change detection
      this.originalTiersPayant = {...this.selectedTiersPayant};
      this.updateForm(this.selectedTiersPayant);

      // Show priorite switch if priorite is not 0, but keep it unchecked by default
      this.showPrioriteSwitch = this.selectedTiersPayant.categorie !== 0;
      this.isPrioriteEnabled = false;

      setTimeout(() => {
        this.numBon().nativeElement.focus();
      }, 50);
    } else {
      this.editForm.reset();
      this.originalTiersPayant = null;
      this.showPrioriteSwitch = false;
      this.isPrioriteEnabled = false;
    }
    this.prioriteValue = this.selectedTiersPayant.categorie;
  }

  onPrioriteToggle(checked: boolean): void {
    this.prioriteValue = checked ? 0 : this.selectedTiersPayant?.categorie;
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
    const formValue = this.editForm.value;
    return {
      ...new ClientTiersPayant(),
      id: formValue.id,
      numBon: formValue.numBon,
      categorie: formValue.categorie,
      taux: formValue.taux,
      priorite: this.prioriteValue,
      customerId: this.assure?.id,
      tiersPayantId: this.selectedTiersPayant?.tiersPayantId,
      num: formValue.num,
      tiersPayantFullName: formValue.tiersPayantFullName,
    };
  }

  private hasEntityChanges(): boolean {
    if (!this.originalTiersPayant) {
      return false;
    }

    const currentValues = this.editForm.value;
    return (
      this.originalTiersPayant.taux !== currentValues.taux ||
      this.originalTiersPayant.num !== currentValues.num ||
      this.originalTiersPayant.categorie != this.prioriteValue ||
      this.originalTiersPayant.categorie !== currentValues.categorie
    );
  }

  private checkForChanges(): void {
    // This can be used for real-time validation or UI updates
  }

  private saveWithBackendCall(): void {
    this.isSaving = true;
    const clientTiersPayant = this.createFromForm();

    this.customerService
      .updateTiersPayant(clientTiersPayant)
      .pipe(
        finalize(() => (this.isSaving = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: response => {
          if (response.body) {
            this.activeModal.close(clientTiersPayant);
          }
        },
        error: () => {
          this.isValid = false;
          this.isSaving = false;
        },
      });
  }
}
