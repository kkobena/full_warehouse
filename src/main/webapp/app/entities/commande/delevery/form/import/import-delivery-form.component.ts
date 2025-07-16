import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { DeliveryService } from '../../delivery.service';
import { DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { IDelivery } from '../../../../../shared/model/delevery.model';
import { IFournisseur } from '../../../../../shared/model/fournisseur.model';
import moment from 'moment/moment';
import { HttpResponse } from '@angular/common/http';
import { FournisseurService } from '../../../../fournisseur/fournisseur.service';
import { ICommandeResponse } from '../../../../../shared/model/commande-response.model';
import { WarehouseCommonModule } from '../../../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { ToastModule } from 'primeng/toast';
import { CardModule } from 'primeng/card';
import { KeyFilterModule } from 'primeng/keyfilter';
import { FileUploadModule } from 'primeng/fileupload';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { PrimeNG } from 'primeng/config';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { DATE_FORMAT } from '../../../../../shared/constants/input.constants';

type UploadDeleiveryReceipt = { model: string; fournisseurId: number; deliveryReceipt: IDelivery };
type ModelFichier = { label: string; value: string };

@Component({
  selector: 'jhi-import-delivery-form',
  templateUrl: './import-delivery-form.component.html',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RouterModule,
    RippleModule,
    DynamicDialogModule,
    FormsModule,
    ReactiveFormsModule,
    InputTextModule,
    DropdownModule,
    ToastModule,
    NgxSpinnerModule,
    CardModule,
    KeyFilterModule,
    FileUploadModule,
    Select,
    DatePicker,
  ],
})
export class ImportDeliveryFormComponent implements OnInit {
  appendTo = 'body';
  fournisseurs: IFournisseur[] = [];
  models: ModelFichier[];
  file: any;
  isSaving = false;
  entity?: IDelivery;
  maxDate = new Date();
  primeNGConfig = inject(PrimeNG);
  translate = inject(TranslateService);
  primngtranslate: Subscription;
  private entityService = inject(DeliveryService);
  private ref = inject(DynamicDialogRef);
  private fb = inject(FormBuilder);
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
      receiptReference: new FormControl<string | null>(null, {
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
    }),
  });
  private readonly messageService = inject(MessageService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly spinner = inject(NgxSpinnerService);

  constructor() {
    this.translate.use('fr');
    this.primngtranslate = this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
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
    const body = new Blob([JSON.stringify(deliveryReceipt)], {
      type: 'application/json',
    });
    formData.append('deliveryReceipt', body);
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
    const receiptDate = this.editForm.get('receiptDate').value;
    return {
      model: this.editForm.get(['model']).value.value,
      fournisseurId: this.editForm.get(['fournisseur']).value.id,
      deliveryReceipt: {
        receiptReference: deliveryReceiptCtl.get('receiptReference').value,
        receiptDate: receiptDate ? moment(receiptDate).format(DATE_FORMAT) : null,
        receiptAmount: deliveryReceiptCtl.get('receiptAmount').value,
        taxAmount: deliveryReceiptCtl.get('taxAmount').value,
      },
    };
  }
}
