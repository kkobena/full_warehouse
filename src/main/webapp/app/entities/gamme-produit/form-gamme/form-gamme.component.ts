import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { GammeProduitService } from '../gamme-produit.service';
import { GammeProduit, IGammeProduit } from '../../../shared/model/gamme-produit.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToastModule } from 'primeng/toast';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';

@Component({
  selector: 'jhi-form-gamme',
  templateUrl: './form-gamme.component.html',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ToastModule,
    DropdownModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
  ],
})
export class FormGammeComponent implements OnInit {
  protected entityService = inject(GammeProduitService);
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  private fb = inject(UntypedFormBuilder);
  private messageService = inject(MessageService);

  isSaving = false;
  gamme?: IGammeProduit;
  editForm = this.fb.group({
    id: [],
    code: [],
    libelle: [null, [Validators.required]],
  });

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  ngOnInit(): void {
    this.gamme = this.config.data.gamme;
    if (this.gamme !== null && this.gamme !== undefined) {
      this.updateForm(this.gamme);
    }
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

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IGammeProduit>>): void {
    result.subscribe({
      next: (res: HttpResponse<IGammeProduit>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(response: IGammeProduit | null): void {
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

  private updateForm(entity: IGammeProduit): void {
    this.editForm.patchValue({
      id: entity.id,
      code: entity.code,
      libelle: entity.libelle,
    });
  }

  private createFromForm(): IGammeProduit {
    return {
      ...new GammeProduit(),
      id: this.editForm.get(['id']).value,
      code: this.editForm.get(['code']).value,
      libelle: this.editForm.get(['libelle']).value,
    };
  }
}
