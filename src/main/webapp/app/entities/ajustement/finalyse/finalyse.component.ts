import { Component, OnInit } from '@angular/core';
import { AjustementService } from '../ajustement.service';
import { IAjust } from '../../../shared/model/ajust.model';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { BLOCK_SPACE } from '../../../shared/util/warehouse-util';

@Component({
  selector: 'jhi-finalyse',
  templateUrl: './finalyse.component.html',
  providers: [MessageService],
})
export class FinalyseComponent implements OnInit {
  protected isSaving = false;
  protected entity?: IAjust;
  protected editForm = this.fb.group({
    commentaire: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
  });
  protected readonly BLOCK_SPACE = BLOCK_SPACE;

  constructor(
    protected ajustementService: AjustementService,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    private fb: FormBuilder,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.entity = this.config.data.entity;
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
      commentaire: this.editForm.get(['commentaire'])!.value,
    };
  }
}
