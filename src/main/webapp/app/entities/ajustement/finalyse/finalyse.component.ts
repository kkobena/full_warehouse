import { Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { AjustementService } from '../ajustement.service';
import { IAjust } from '../../../shared/model/ajust.model';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TextareaModule } from 'primeng/textarea';

import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { finalize } from 'rxjs/operators';
import { SpinnerComponent } from '../../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-finalyse',
  templateUrl: './finalyse.component.html',

  imports: [WarehouseCommonModule, RouterModule, ButtonModule, FormsModule, ReactiveFormsModule, TextareaModule, ToastAlertComponent, SpinnerComponent]
})
export class FinalyseComponent implements OnInit {
  ref = inject(DynamicDialogRef);
  config = inject(DynamicDialogConfig);
  protected isSaving = false;
  protected entity?: IAjust;

  protected fb = inject(FormBuilder);
  protected editForm = this.fb.group({
    commentaire: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true
    })
  });
  private commentaire = viewChild.required<ElementRef>('commentaire');
  private readonly ajustementService = inject(AjustementService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private alert = viewChild.required<ToastAlertComponent>('alert');

  ngOnInit(): void {
    this.entity = this.config.data.entity;
    this.commentaire().nativeElement.focus();
  }

  save(): void {
    this.isSaving = true;
    this.spinner().show();
    this.subscribeToSaveResponse(this.ajustementService.saveAjustement(this.createFromForm()));
  }

  cancel(): void {
    this.ref.destroy();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<{}>>): void {
    result.pipe(finalize(() => this.spinner().hide())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError()
    });
  }

  protected onSaveSuccess(): void {
    this.ref.close();
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.alert().showError('Erreur d\'enregistrement', 'Erreur d\'enregistrement');
  }

  private createFromForm(): IAjust {
    return {
      ...this.entity,
      commentaire: this.editForm.get(['commentaire']).value
    };
  }
}
