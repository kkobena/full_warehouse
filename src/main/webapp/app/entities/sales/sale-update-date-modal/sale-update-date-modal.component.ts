import { Component, inject } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { ISales } from 'app/shared/model/sales.model';
import { SalesService } from '../sales.service';
import { WarehouseCommonModule } from 'app/shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { DatePickerComponent } from 'app/shared/date-picker/date-picker.component';
import moment from 'moment/moment';

@Component({
  selector: 'jhi-sale-update-date-modal',
  template: `
    <div class="modal-header">
      <h4 class="modal-title" id="modal-basic-title">Modifier la date de vente</h4>
      <button type="button" class="btn-close" aria-label="Close" (click)="cancel()"></button>
    </div>
    <form [formGroup]="editForm" (ngSubmit)="save()">
      <div class="modal-body">
        <div class="form-group">
          <jhi-date-picker id="field_updatedAt" label="warehouseApp.sales.updatedAt" formControlName="updatedAt"></jhi-date-picker>
        </div>
      </div>
    </form>
    <div class="modal-footer">
      <div class="d-flex justify-content-end mt-3">
        <p-button type="button" label="Annuler" severity="secondary" (click)="cancel()"></p-button>
        <p-button type="submit" severity="primary" label="Enregistrer" [disabled]="editForm.invalid || isSaving"></p-button>
      </div>
    </div>
  `,
  standalone: true,
  imports: [WarehouseCommonModule, ReactiveFormsModule, ButtonModule, DatePickerComponent],
})
export class SaleUpdateDateModalComponent {
  sale: ISales | null = null; ///*const modalData = (this as any).sale;
  protected activeModal = inject(NgbActiveModal);
  protected fb = inject(FormBuilder);

  protected isSaving = false;

  protected editForm = this.fb.group({
    updatedAt: new FormControl<Date | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),
  });
  private readonly salesService = inject(SalesService);

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    this.isSaving = true;
    const formDate = this.editForm.get('updatedAt')?.value;
    const updatedSale = { ...this.sale, updatedAt: moment(formDate) };
    this.subscribeToSaveResponse(this.salesService.updateDate(updatedSale as ISales));
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(updatedSale: ISales | null): void {
    this.activeModal.close(updatedSale);
  }

  protected onSaveError(): void {
    console.error('Error saving sale updated date');
  }
}
