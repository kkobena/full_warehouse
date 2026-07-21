import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, inject, OnInit, signal, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { ILot, Lot } from 'app/shared/model/lot.model';
import { LotService } from '../../../../../entities/commande/lot/lot.service';
import { NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';
import { CommonModule } from '@angular/common';
import { AbstractOrderItem } from 'app/shared/model/abstract-order-item.model';
import { ErrorService } from 'app/shared/error.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import type { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { NotificationService } from 'app/shared/services/notification.service';
import { ButtonComponent, CardComponent, KeyFilterDirective } from 'app/shared/ui';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';

@Component({
  selector: 'jhi-form-lot',
  templateUrl: './form-lot.component.html',
  styleUrls: ['form-lot.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    ButtonComponent,
    FormsModule,
    ReactiveFormsModule,
    KeyFilterDirective,
    PharmaDatePickerComponent,
    CardComponent,
  ],
})
export class FormLotComponent implements OnInit, AfterViewInit {
  header = 'Ajout de lot';
  entity?: ILot;
  deliveryItem?: AbstractOrderItem;
  commandeId?: number;
  protected fb = inject(FormBuilder);
  protected isSaving = false;
  protected numLotAlreadyExist = false;
  protected maxDate: NgbDateStruct | null = null;
  protected minDate: NgbDateStruct | null = null;
  protected showUgControl = false;
  protected expiryWarning = signal<'none' | 'soon' | 'critical'>('none');
  protected editForm = this.fb.group({
    id: new FormControl<number | null>(null, {}),
    numLot: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    expiryDate: new FormControl<NgbDateStruct | null>(null, {
      validators: [Validators.required],
    }),
    ugQuantityReceived: new FormControl<number | null>(null, {
      validators: [Validators.min(0)],
    }),
    quantityReceived: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0)],
      nonNullable: true,
    }),
    manufacturingDate: new FormControl<NgbDateStruct | null>(null),
  });
  private readonly entityService = inject(LotService);
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly notificationService = inject(NotificationService);
  private numLotInput = viewChild.required<ElementRef>('numLotInput');

  private static toNgbDate(date: Date): NgbDateStruct {
    return { year: date.getFullYear(), month: date.getMonth() + 1, day: date.getDate() };
  }

  ngOnInit(): void {
    this.maxDate = FormLotComponent.toNgbDate(new Date());
    this.minDate = FormLotComponent.toNgbDate(new Date());
    this.showUgControl = Number(this.deliveryItem.freeQty) > 0 || this.getLotUgQuantity() < Number(this.deliveryItem.freeQty);
    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.editForm.get('ugQuantityReceived').setValidators([Validators.min(0), Validators.max(this.getValidLotUgQuantity())]);
    this.editForm.get('ugQuantityReceived').updateValueAndValidity();
    this.editForm
      .get('quantityReceived')
      .setValidators([Validators.required, Validators.min(1), Validators.max(this.getValidLotQuantity())]);
    this.editForm.get('quantityReceived').updateValueAndValidity();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.numLotInput().nativeElement.focus();
    }, 100);
  }

  updateForm(entity: ILot): void {
    const expiryDate = entity.expiryDate ? FormLotComponent.toNgbDate(new Date(entity.expiryDate)) : null;
    this.editForm.patchValue({
      numLot: entity.numLot,
      id: entity.id,
      quantityReceived: entity.quantityReceived,
      expiryDate,
      manufacturingDate: entity.manufacturingDate ? FormLotComponent.toNgbDate(new Date(entity.manufacturingDate)) : null,
      ugQuantityReceived: entity.ugQuantityReceived,
    });
    this.onExpiryDateSelected(expiryDate);
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    if (entity.id != undefined) {
      this.subscribeToSaveResponse(this.entityService.editLot(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.addLot(entity));
    }
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  onExpiryDateSelected(date: NgbDateStruct | null): void {
    if (!date) {
      this.expiryWarning.set('none');
      return;
    }
    const dateAsDate = new Date(date.year, date.month - 1, date.day);
    const monthsToExpiry = (dateAsDate.getTime() - Date.now()) / (1000 * 60 * 60 * 24 * 30.44);
    if (monthsToExpiry < 3) {
      this.expiryWarning.set('critical');
    } else if (monthsToExpiry < 6) {
      this.expiryWarning.set('soon');
    } else {
      this.expiryWarning.set('none');
    }
  }

  onValidateQuantity(event: any): void {
    const quantity = Number(event.target.value);
    const maxQuantity = this.getValidLotQuantity();
    if (quantity > maxQuantity) {
      this.notificationService.error(`La quantité saisie ne peut être supérieure à ${maxQuantity}`, 'Erreur');
    }
  }

  onValidateNumLot(event: any): void {
    const numLot = event.target.value;
    this.numLotAlreadyExist = this.deliveryItem.lots.some(lot => lot.numLot === numLot && lot.id !== this.entity.id);
    if (this.numLotAlreadyExist) {
      this.notificationService.error(`Le numero de lot [ ${numLot} ] est déjà enregistré pour cette ligne de commande`, 'Erreur');
    }
  }

  onValidateUgQuantity(event: any): void {
    const quantity = Number(event.target.value);
    const maxQuantity = this.getValidLotUgQuantity();
    if (quantity > maxQuantity) {
      this.notificationService.error(`La quantité ug saisie ne peut être supérieure à ${maxQuantity}`, 'Erreur');
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ILot>>): void {
    result.subscribe({
      next: (res: HttpResponse<ILot>) => this.onSaveSuccess(res.body),
      error: err => this.onSaveError(err),
    });
  }

  protected onSaveSuccess(response: ILot | null): void {
    this.activeModal.close(response);
  }

  protected onSaveError(err: HttpErrorResponse): void {
    this.isSaving = false;
    this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
  }

  private createFromForm(): ILot {
    return {
      ...new Lot(),
      id: this.editForm.get(['id']).value,
      numLot: this.editForm.get(['numLot']).value,
      expiryDate: NGB_DATE_TO_ISO(this.editForm.get(['expiryDate']).value),
      manufacturingDate: NGB_DATE_TO_ISO(this.editForm.get(['manufacturingDate']).value),
      quantityReceived: this.editForm.get(['quantityReceived']).value,
      ugQuantityReceived: this.editForm.get(['ugQuantityReceived']).value,
      receiptItemId: this.deliveryItem?.orderLineId,
    };
  }

  private getValidLotQuantity(): number {
    const quantityReceived = this.deliveryItem.quantityReceived || this.deliveryItem.quantityRequested;
    if (this.entity) {
      return quantityReceived - (this.getLotQuantity() - this.entity.quantityReceived);
    } else {
      return quantityReceived - this.getLotQuantity();
    }
  }

  private getValidLotUgQuantity(): number {
    const ugQuantity = this.deliveryItem.freeQty;
    if (this.entity && ugQuantity) {
      return ugQuantity - (this.getLotUgQuantity() - this.entity.ugQuantityReceived);
    }
    if (ugQuantity) {
      return ugQuantity - this.getLotUgQuantity();
    }
    return 0;
  }

  private getLotQuantity(): number {
    return this.deliveryItem.lots.reduce((first, second) => first + second.quantityReceived, 0);
  }

  private getLotUgQuantity(): number {
    return this.deliveryItem.lots.reduce((first, second) => first + second.ugQuantityReceived, 0);
  }
}
