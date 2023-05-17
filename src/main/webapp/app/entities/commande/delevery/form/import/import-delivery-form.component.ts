import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { DeliveryService } from '../../delivery.service';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { NgxSpinnerService } from 'ngx-spinner';
import { IDelivery } from '../../../../../shared/model/delevery.model';
import { IFournisseur } from '../../../../../shared/model/fournisseur.model';
import moment, { Moment } from 'moment/moment';
import { HttpResponse } from '@angular/common/http';
import { FournisseurService } from '../../../../fournisseur/fournisseur.service';
import { ICommandeResponse } from '../../../../../shared/model/commande-response.model';

type UploadDeleiveryReceipt = { model: string; fournisseurId: number; deliveryReceipt: IDelivery };
type ModelFichier = { label: string; value: string };

@Component({
  selector: 'jhi-import-delivery-form',
  templateUrl: './import-delivery-form.component.html',
  styleUrls: ['./import-delivery-form.component.scss'],
})
export class ImportDeliveryFormComponent implements OnInit {
  appendTo = 'body';
  fournisseurs: IFournisseur[] = [];
  models: ModelFichier[];
  file: any;
  isSaving = false;
  entity?: IDelivery;
  maxDate = new Date();
  minDate = new Date();

  editForm = this.fb.group({
    model: new FormControl<ModelFichier | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    fournisseur: new FormControl<IFournisseur | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    deliveryReceipt: this.fb.group({
      receiptRefernce: new FormControl<string | null>(null, {
        validators: [Validators.required],
        nonNullable: true,
      }),
      receiptDate: new FormControl<Date | null>(null, {
        validators: [Validators.required],
      }),
      receiptAmount: new FormControl<number | null>(null, {
        validators: [Validators.min(0), Validators.required],
        nonNullable: true,
      }),
      taxAmount: new FormControl<number | null>(null, {
        validators: [Validators.required, Validators.min(0)],
        nonNullable: true,
      }),
      sequenceBon: new FormControl<string | null>(null, {}),
    }),
  });

  constructor(
    protected entityService: DeliveryService,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    private fb: FormBuilder,
    private messageService: MessageService,
    private fournisseurService: FournisseurService,
    private spinner: NgxSpinnerService
  ) {
    this.models = [
      { label: 'LABOREX', value: 'LABOREX' },
      { label: 'COPHARMED', value: 'COPHARMED' },
      { label: 'DPCI', value: 'DPCI' },
      { label: 'TEDIS', value: 'TEDIS' },
    ];
  }

  ngOnInit(): void {
    this.populate();
  }

  populate(): void {
    this.fournisseurService.query({ size: 99999 }).subscribe((res: HttpResponse<IFournisseur[]>) => {
      this.fournisseurs = res.body || [];
    });
  }

  uploadHandler(event: any, fileUpload: any): void {
    this.file = event.files[0];
    console.log(this.file);
    fileUpload.clear();
  }

  cancel(): void {
    this.ref.close();
  }

  isValidForm(): boolean {
    return !!this.file;
  }

  save(): void {
    this.isSaving = true;

    const deliveryReceipt: UploadDeleiveryReceipt = this.createUploadDeleiveryReceipt();
    const formData: FormData = new FormData();
    formData.append('deliveryReceipt', JSON.stringify(deliveryReceipt));
    formData.append('fichier', this.file, this.file.name);
    this.spinner.show('gestion-commande-spinner');
    this.entityService.uploadNew(formData).subscribe({
      next: (res: HttpResponse<ICommandeResponse>) => {
        if (res) {
          this.ref.close(res.body);
        }
      },
      error: () => this.onSaveError(),
      complete: () => {
        this.isSaving = false;
        this.spinner.hide('gestion-commande-spinner');
      },
    });
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.spinner.hide('gestion-commande-spinner');
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: "L'importation n'a pas abouti",
    });
  }

  private createUploadDeleiveryReceipt(): UploadDeleiveryReceipt {
    const deliveryReceiptCtl = this.editForm.get('deliveryReceipt');
    return {
      model: this.editForm.get(['model'])!.value.value,
      fournisseurId: this.editForm.get(['fournisseur'])!.value.id,
      deliveryReceipt: {
        receiptRefernce: deliveryReceiptCtl.get('receiptRefernce')!.value,
        receiptFullDate: this.buildDate(deliveryReceiptCtl.get('receiptDate')!.value),
        sequenceBon: deliveryReceiptCtl.get('sequenceBon')!.value,
        receiptAmount: deliveryReceiptCtl.get('receiptAmount')!.value,
        taxAmount: deliveryReceiptCtl.get('taxAmount')!.value,
      },
    };
  }

  private buildDate(param: any): Moment {
    const receiptDate = new Date(param);
    return moment(receiptDate);
  }
}
