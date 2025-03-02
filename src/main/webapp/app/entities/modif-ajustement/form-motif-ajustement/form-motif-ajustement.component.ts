import { Component, OnInit, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';

import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { ModifAjustementService } from '../motif-ajustement.service';
import { IMotifAjustement, MotifAjustement } from '../../../shared/model/motif-ajustement.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';

@Component({
  selector: 'jhi-form-motif-ajustement',
  templateUrl: './form-motif-ajustement.component.html',
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
  ],
})
export class FormMotifAjustementComponent implements OnInit {
  protected entityService = inject(ModifAjustementService);
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  private fb = inject(UntypedFormBuilder);
  private messageService = inject(MessageService);

  entity?: IMotifAjustement;
  isSaving = false;
  editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
  });

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  updateForm(entity: IMotifAjustement): void {
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

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IMotifAjustement>>): void {
    result.subscribe({
      next: (response: HttpResponse<IMotifAjustement>) => this.onSaveSuccess(response.body),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(response: IMotifAjustement | null): void {
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

  private createFromForm(): IMotifAjustement {
    return {
      ...new MotifAjustement(),
      id: this.editForm.get(['id']).value,
      libelle: this.editForm.get(['libelle']).value,
    };
  }
}
