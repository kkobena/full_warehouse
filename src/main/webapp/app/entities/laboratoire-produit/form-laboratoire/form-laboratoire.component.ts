import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { LaboratoireProduitService } from '../laboratoire-produit.service';
import { ILaboratoire, Laboratoire } from '../../../shared/model/laboratoire.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';

@Component({
    selector: 'jhi-form-laboratoire',
    templateUrl: './form-laboratoire.component.html',
    styles: [],
    imports: [
        WarehouseCommonModule,
        FormsModule,
        ReactiveFormsModule,
        ToastModule,
        ButtonModule,
        InputTextModule,
        RippleModule,
        DynamicDialogModule,
    ]
})
export class FormLaboratoireComponent implements OnInit {
  protected entityService = inject(LaboratoireProduitService);
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  private fb = inject(UntypedFormBuilder);
  private messageService = inject(MessageService);

  laboratoire?: ILaboratoire;
  isSaving = false;
  editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
  });

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  ngOnInit(): void {
    this.laboratoire = this.config.data.laboratoire;
    if (this.laboratoire !== null && this.laboratoire !== undefined) {
      this.updateForm(this.laboratoire);
    }
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

  cancel(): void {
    this.ref.destroy();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ILaboratoire>>): void {
    result.subscribe(
      (res: HttpResponse<ILaboratoire>) => this.onSaveSuccess(res.body),
      () => this.onSaveError(),
    );
  }

  protected onSaveSuccess(response: ILaboratoire | null): void {
    this.messageService.add({
      severity: 'info',
      summary: 'Information',
      detail: 'Enregistrement effectué avec succès',
    });
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

  private createFromForm(): ILaboratoire {
    return {
      ...new Laboratoire(),
      id: this.editForm.get(['id'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
    };
  }
}
