import { Component, inject } from '@angular/core';
import { IProduit } from '../../shared/model/produit.model';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { Decondition, IDecondition } from '../../shared/model/decondition.model';
import { DeconditionService } from '../decondition/decondition.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { InputText } from 'primeng/inputtext';

@Component({
  selector: 'jhi-decondition',
  templateUrl: 'decondition-dialog.component.html',
  imports: [WarehouseCommonModule, ReactiveFormsModule, FormsModule, InputText]
})
export class DeconditionDialogComponent {
  activeModal = inject(NgbActiveModal);
  isSaving = false;
  isNotValid = false;
  produit?: IProduit;
  protected deconditionService = inject(DeconditionService);
  private fb = inject(FormBuilder);
  editForm = this.fb.group({
    qtyMvt: [null, [Validators.required, Validators.min(1)]]
  });

  save(): void {
    this.isSaving = true;
    const decondition = this.createFromForm();
    this.subscribeToSaveResponse(this.deconditionService.create(decondition));
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  onQuantitySoldBoxChanged(event: any): void {
    const qty = event.target.value;
    const oldStock = this.produit.totalQuantity;
    this.isNotValid = oldStock < Number(qty);
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IDecondition>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError()
    });
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.activeModal.close();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  private createFromForm(): IDecondition {
    return {
      ...new Decondition(),
      qtyMvt: this.editForm.get(['qtyMvt']).value,
      produitId: this.produit.id
    };
  }
}
