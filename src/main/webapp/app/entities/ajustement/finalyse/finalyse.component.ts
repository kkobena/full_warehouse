import { Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { AjustementService } from '../ajustement.service';
import { IAjust } from '../../../shared/model/ajust.model';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { BLOCK_SPACE } from '../../../shared/util/warehouse-util';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { TextareaModule } from 'primeng/textarea';

@Component({
  selector: 'jhi-finalyse',
  templateUrl: './finalyse.component.html',
  providers: [MessageService],
  imports: [WarehouseCommonModule, RouterModule, ToastModule, ButtonModule, FormsModule, ReactiveFormsModule, TextareaModule],
})
export class FinalyseComponent implements OnInit {
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  commentaire = viewChild.required<ElementRef>('commentaire');
  protected ajustementService = inject(AjustementService);
  protected isSaving = false;
  protected entity?: IAjust;
  protected readonly BLOCK_SPACE = BLOCK_SPACE;
  private fb = inject(FormBuilder);
  protected editForm = this.fb.group({
    commentaire: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
  });
  private messageService = inject(MessageService);

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    this.commentaire().nativeElement.focus();
  }

  save(): void {
    this.isSaving = true;
    this.subscribeToSaveResponse(this.ajustementService.saveAjustement(this.createFromForm()));
  }

  cancel(): void {
    this.ref.destroy();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.ref.close();
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
    });
  }

  private createFromForm(): IAjust {
    return {
      ...this.entity,
      commentaire: this.editForm.get(['commentaire']).value,
    };
  }
}
