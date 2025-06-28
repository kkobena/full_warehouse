import { Component, inject, OnInit } from '@angular/core';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';

import { ErrorService } from '../../../shared/error.service';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { GroupeFournisseur, IGroupeFournisseur } from '../../../shared/model/groupe-fournisseur.model';
import { GroupeFournisseurService } from '../groupe-fournisseur.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToastModule } from 'primeng/toast';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { KeyFilterModule } from 'primeng/keyfilter';

@Component({
  selector: 'jhi-form-groupe-fournisseur',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ToastModule,
    DropdownModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    KeyFilterModule,
    DynamicDialogModule,
  ],
  templateUrl: './form-groupe-fournisseur.component.html',
})
export class FormGroupeFournisseurComponent implements OnInit {
  protected ref = inject(DynamicDialogRef);
  protected config = inject(DynamicDialogConfig);
  protected entity?: IGroupeFournisseur;
  protected blockSpace: RegExp = /[^s]/;
  protected isSaving = false;
  protected isValid = true;
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
    addresspostale: [],
    numFaxe: [],
    email: [],
    tel: [],
    odre: [],
    codeRecepteurPharmaMl: [],
    codeOfficePharmaMl: [],
    urlPharmaMl: [],
  });
  private readonly messageService = inject(MessageService);
  private readonly errorService = inject(ErrorService);
  private readonly entityService = inject(GroupeFournisseurService);

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  cancel(): void {
    this.ref.close();
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();

    if (entity.id !== undefined) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IGroupeFournisseur>>): void {
    result.subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
    });
  }

  private onSaveSuccess(entity: IGroupeFournisseur | null): void {
    this.isSaving = false;
    this.ref.close(entity);
  }

  private updateForm(entity: IGroupeFournisseur): void {
    this.editForm.patchValue({
      id: entity.id,
      libelle: entity.libelle,
      addresspostale: entity.addresspostale,
      numFaxe: entity.numFaxe,
      email: entity.email,
      tel: entity.tel,
      odre: entity.odre,
      urlPharmaMl: entity.urlPharmaMl,
      codeRecepteurPharmaMl: entity.codeRecepteurPharmaMl,
      codeOfficePharmaMl: entity.codeOfficePharmaMl,
    });
  }

  private onSaveError(error: any): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: this.errorService.getErrorMessage(error),
    });
  }

  private createFromForm(): IGroupeFournisseur {
    return {
      ...new GroupeFournisseur(),
      id: this.editForm.get(['id']).value,
      libelle: this.editForm.get(['libelle']).value,
      addresspostale: this.editForm.get(['addresspostale']).value,
      numFaxe: this.editForm.get(['numFaxe']).value,
      email: this.editForm.get(['email']).value,
      tel: this.editForm.get(['tel']).value,
      odre: this.editForm.get(['odre']).value,
      codeRecepteurPharmaMl: this.editForm.get(['codeRecepteurPharmaMl']).value,
      codeOfficePharmaMl: this.editForm.get(['codeOfficePharmaMl']).value,
      urlPharmaMl: this.editForm.get(['urlPharmaMl']).value,
    };
  }
}
