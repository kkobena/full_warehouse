import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { RayonService } from '../rayon.service';
import { IRayon, Rayon } from '../../../shared/model/rayon.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { KeyFilter } from 'primeng/keyfilter';

@Component({
  selector: 'jhi-form-rayon',
  templateUrl: './form-rayon.component.html',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    DynamicDialogModule,
    KeyFilter,
  ],
})
export class FormRayonComponent implements OnInit {
  protected entityService = inject(RayonService);
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  private fb = inject(UntypedFormBuilder);
  private messageService = inject(MessageService);

  isSaving = false;
  entity?: IRayon;
  editForm = this.fb.group({
    id: [],
    code: [null, [Validators.required]],
    libelle: [null, [Validators.required]],
  });

  ngOnInit(): void {
    this.entity = this.config.data;
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  updateForm(entity: IRayon): void {
    this.editForm.patchValue({
      id: entity.id,
      code: entity.code,
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

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IRayon>>): void {
    result.subscribe({
      next: (res: HttpResponse<IRayon>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(response: IRayon | null): void {
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

  private createFromForm(): IRayon {
    return {
      ...new Rayon(),
      id: this.editForm.get(['id']).value,
      code: this.editForm.get(['code']).value,
      libelle: this.editForm.get(['libelle']).value,
    };
  }
}
