import { AfterViewInit, Component, ElementRef, inject, viewChild } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { DialogModule } from 'primeng/dialog';

import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { StyleClassModule } from 'primeng/styleclass';
import { IRemise, Remise } from '../../../shared/model/remise.model';
import { RemiseService } from '../remise.service';
import { Observable } from 'rxjs';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { Card } from 'primeng/card';
import { ErrorService } from '../../../shared/error.service';

@Component({
  selector: 'jhi-remise-client-form-modal',
  imports: [
    ToastModule,
    DialogModule,
    FormsModule,
    InputTextModule,
    KeyFilterModule,
    ReactiveFormsModule,
    StyleClassModule,
    ButtonModule,
    ToastAlertComponent,
    Card,
  ],
  templateUrl: './remise-client-form-modal.component.html',
  styleUrls: ['../../common-modal.component.scss'],
})
export class RemiseClientFormModalComponent implements AfterViewInit {
  remiseValue = viewChild.required<ElementRef>('remiseValue');
  entity: IRemise | null = null;
  protected fb = inject(FormBuilder);
  protected editForm = this.fb.group({
    id: new FormControl<number | null>(null),
    valeur: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
    remiseValue: new FormControl<number | null>(null, {
      validators: [Validators.min(1), Validators.required],
      nonNullable: true,
    }),
  });
  protected isSaving = false;
  protected title: string | null = null;
  private readonly activeModal = inject(NgbActiveModal);
  private readonly entityService = inject(RemiseService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();

    if (entity.id) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  updateForm(entity: IRemise): void {
    this.editForm.patchValue({
      id: entity.id,
      valeur: entity.valeur,
      remiseValue: entity.remiseValue,
    });
  }

  ngAfterViewInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
    this.remiseValue().nativeElement.focus();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IRemise>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: err => this.onSaveError(err),
    });
  }

  private onSaveSuccess(): void {
    this.isSaving = false;
    this.activeModal.close();
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private createFromForm(): IRemise {
    return {
      ...new Remise(),
      id: this.editForm.get(['id']).value,
      remiseValue: this.editForm.get(['remiseValue']).value,
      valeur: this.editForm.get(['valeur']).value,
      type: 'remiseClient',
    };
  }
}
