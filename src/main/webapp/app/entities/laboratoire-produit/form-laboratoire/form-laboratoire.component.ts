import { HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { LaboratoireProduitService } from '../laboratoire-produit.service';
import { ILaboratoire, Laboratoire } from '../../../shared/model/laboratoire.model';

@Component({
  selector: 'jhi-form-laboratoire',
  templateUrl: './form-laboratoire.component.html',
  styles: [],
})
export class FormLaboratoireComponent implements OnInit {
  laboratoire?: ILaboratoire;
  isSaving = false;
  editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
  });
  constructor(
    protected entityService: LaboratoireProduitService,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    private fb: FormBuilder,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.laboratoire = this.config.data.laboratoire;
    if (this.laboratoire !== null && this.laboratoire !== undefined) {
      this.updateForm(this.laboratoire);
    }
  }
  private createFromForm(): ILaboratoire {
    return {
      ...new Laboratoire(),
      id: this.editForm.get(['id'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
    };
  }
  updateForm(entity: ILaboratoire): void {
    this.editForm.patchValue({
      id: entity.id,
      libelle: entity.libelle,
    });
  }
  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    if (entity.id !== undefined && entity.id !== null) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ILaboratoire>>): void {
    result.subscribe(
      (res: HttpResponse<ILaboratoire>) => this.onSaveSuccess(res.body),
      () => this.onSaveError()
    );
  }
  protected onSaveSuccess(response: ILaboratoire | null): void {
    this.messageService.add({ severity: 'info', summary: 'Information', detail: 'Enregistrement effectu?? avec succ??s' });
    this.ref.close(response);
  }
  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Enregistrement a ??chou??' });
  }
  cancel(): void {
    this.ref.destroy();
  }
}
