import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable, Subscription } from 'rxjs';
import { ILot, Lot } from '../../../shared/model/lot.model';
import { LotService } from './lot.service';
import { BLOCK_SPACE, DATE_FORMAT_YYYY_MM_DD } from '../../../shared/util/warehouse-util';
import { IDeliveryItem } from '../../../shared/model/delivery-item';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { RippleModule } from 'primeng/ripple';
import { KeyFilterModule } from 'primeng/keyfilter';
import { InputTextModule } from 'primeng/inputtext';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';
import { DatePicker } from 'primeng/datepicker';

@Component({
  selector: 'jhi-form-lot',
  templateUrl: './form-lot.component.html',
  providers: [MessageService],
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    TooltipModule,
    ToastModule,
    RippleModule,
    DynamicDialogModule,
    FormsModule,
    ReactiveFormsModule,
    KeyFilterModule,
    InputTextModule,
    DatePicker,
  ],
})
export class FormLotComponent implements OnInit {
  protected entityService = inject(LotService);
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  private fb = inject(FormBuilder);
  private messageService = inject(MessageService);
  primeNGConfig = inject(PrimeNG);
  translate = inject(TranslateService);

  primngtranslate: Subscription;
  isSaving = false;
  entity?: ILot;
  deliveryItem?: IDeliveryItem;
  commandeId?: number;
  numLotAlreadyExist = false;
  maxDate = new Date();
  minDate = new Date();
  showUgControl = false;
  blockSpace = BLOCK_SPACE;
  editForm = this.fb.group({
    id: new FormControl<number | null>(null, {}),
    numLot: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    expiryDate: new FormControl<Date | null>(null, {
      validators: [Validators.required],
    }),
    ugQuantityReceived: new FormControl<number | null>(null, {
      validators: [Validators.min(0)],
    }),
    quantityReceived: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0)],
      nonNullable: true,
    }),

    manufacturingDate: new FormControl<Date | null>(null),
  });

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
    this.translate.use('fr');
    this.primngtranslate = this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  ngOnInit(): void {
    this.maxDate = new Date();
    this.minDate = new Date();
    this.entity = this.config.data.entity;
    this.deliveryItem = this.config.data.deliveryItem;
    this.commandeId = this.config.data.commandeId;
    this.showUgControl = Number(this.deliveryItem.ugQuantity) > 0 || this.getLotUgQuantity() < Number(this.deliveryItem.ugQuantity);

    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.editForm.get('ugQuantityReceived')!.setValidators([Validators.min(0), Validators.max(this.getValidLotUgQuantity())]);
    this.editForm.get('ugQuantityReceived')!.updateValueAndValidity();
    this.editForm
      .get('quantityReceived')!
      .setValidators([Validators.required, Validators.min(1), Validators.max(this.getValidLotQuantity())]);
    this.editForm.get('quantityReceived')!.updateValueAndValidity();
  }

  updateForm(entity: ILot): void {
    this.editForm.patchValue({
      numLot: entity.numLot,
      id: entity.id,
      quantityReceived: entity.quantityReceived,
      expiryDate: entity.expiryDate ? new Date(entity.expiryDate) : null,
      manufacturingDate: entity.manufacturingDate ? new Date(entity.manufacturingDate) : null,
      ugQuantityReceived: entity.ugQuantityReceived,
    });
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
    this.ref.destroy();
  }

  onValidateQuantity(event: any): void {
    const quantity = Number(event.target.value);
    const maxQuantity = this.getValidLotQuantity();
    if (quantity > maxQuantity) {
      this.messageService.add({
        severity: 'error',
        summary: 'ERROR DE VALIDATION',
        detail: `La quantité saisie ne peut être supérieure à ${maxQuantity}`,
      });
    }
  }

  onValidateNumLot(event: any): void {
    const numLot = event.target.value;
    this.numLotAlreadyExist = this.deliveryItem.lots.some(lot => lot.numLot === numLot && lot.id !== this.entity.id);
    if (this.numLotAlreadyExist) {
      this.messageService.add({
        severity: 'error',
        summary: 'ERROR DE VALIDATION',
        detail: `Le numero de lot [ ${numLot} ] est déjà enregistré pour cette ligne de commande`,
      });
    }
  }

  onValidateUgQuantity(event: any): void {
    const quantity = Number(event.target.value);
    const maxQuantity = this.getValidLotUgQuantity();
    if (quantity > maxQuantity) {
      this.messageService.add({
        severity: 'error',
        summary: 'ERROR DE VALIDATION',
        detail: `La quantité ug saisie ne peut être supérieure à ${maxQuantity}`,
      });
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ILot>>): void {
    result.subscribe({
      next: (res: HttpResponse<ILot>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(response: ILot | null): void {
    this.ref.close(response);
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
    });
  }

  private createFromForm(): ILot {
    return {
      ...new Lot(),
      id: this.editForm.get(['id'])!.value,
      numLot: this.editForm.get(['numLot'])!.value,
      expiryDate: this.editForm.get(['expiryDate'])!.value
        ? DATE_FORMAT_YYYY_MM_DD(new Date(this.editForm.get(['expiryDate'])!.value))
        : null,
      manufacturingDate: this.editForm.get(['manufacturingDate'])!.value
        ? DATE_FORMAT_YYYY_MM_DD(new Date(this.editForm.get(['manufacturingDate'])!.value))
        : null,
      quantityReceived: this.editForm.get(['quantityReceived'])!.value,
      ugQuantityReceived: this.editForm.get(['ugQuantityReceived'])!.value,
      commandeId: this.commandeId,
      receiptItemId: this.deliveryItem.id,
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
    const ugQuantity = this.deliveryItem.ugQuantity;
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
