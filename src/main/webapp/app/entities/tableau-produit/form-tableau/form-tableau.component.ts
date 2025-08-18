import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ITableau, Tableau } from '../../../shared/model/tableau.model';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { ErrorService } from '../../../shared/error.service';
import { TableauProduitService } from '../tableau-produit.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { KeyFilter } from 'primeng/keyfilter';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-form-tableau',
  imports: [ToastAlertComponent, Button, FormsModule, InputText, ReactiveFormsModule, KeyFilter, Card],
  templateUrl: './form-tableau.component.html',
  styleUrls: ['../../common-modal.component.scss']
})
export class FormTableauComponent implements OnInit {
  entity: ITableau | null = null;
  header: string = '';
  protected fb = inject(FormBuilder);
  protected editForm = this.fb.group({
    id: new FormControl<number | null>(null, {}),
    code: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true
    }),

    value: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.required],
      nonNullable: true
    })
  });
  protected isSaving = false;
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly entityService = inject(TableauProduitService);
  private readonly activeModal = inject(NgbActiveModal);

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();

    if (entity.id !== undefined && entity.id !== null) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  private updateForm(entity: ITableau): void {
    this.editForm.patchValue({
      id: entity.id,
      code: entity.code,
      value: entity.value
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

  private subscribeToSaveResponse(result: Observable<HttpResponse<ITableau>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: error => this.onSaveError(error)
    });
  }

  private createFromForm(): ITableau {
    return {
      ...new Tableau(),
      id: this.editForm.get(['id']).value,
      code: this.editForm.get(['code']).value,
      value: this.editForm.get(['value']).value
    };
  }
}
