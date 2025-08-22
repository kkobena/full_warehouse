import { AfterViewInit, Component, ElementRef, inject, viewChild } from '@angular/core';
import { AjustementService } from '../ajustement.service';
import { IAjust } from '../../../shared/model/ajust.model';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';

import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { finalize } from 'rxjs/operators';
import { SpinnerComponent } from '../../../shared/spinner/spinner.component';
import { Card } from 'primeng/card';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { InputText } from 'primeng/inputtext';

@Component({
  selector: 'jhi-finalyse',
  templateUrl: './finalyse.component.html',
  styleUrls: ['../../common-modal.component.scss'],

  imports: [WarehouseCommonModule, RouterModule, ButtonModule, FormsModule, ReactiveFormsModule, ToastAlertComponent, SpinnerComponent, Card, InputText]
})
export class FinalyseComponent implements AfterViewInit {
  header: string | null = null;
  entity?: IAjust;
  protected isSaving = false;
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
  private readonly activeModal = inject(NgbActiveModal);


  ngAfterViewInit(): void {
    setTimeout(() => {
      this.commentaire().nativeElement.focus();
    }, 100);
  }

  save(): void {
    this.isSaving = true;
    this.spinner().show();
    this.subscribeToSaveResponse(this.ajustementService.saveAjustement(this.createFromForm()));
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<{}>>): void {
    result.pipe(finalize(() => this.spinner().hide())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError()
    });
  }

  protected onSaveSuccess(): void {
    this.activeModal.close('Ajustement finalisé avec succès');
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
