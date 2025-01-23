import { Component, OnInit } from '@angular/core';
import { IProduit } from '../../shared/model/produit.model';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { Decondition, IDecondition } from '../../shared/model/decondition.model';
import { DeconditionService } from '../decondition/decondition.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
    selector: 'jhi-decondition',
    templateUrl: 'decondition-dialog.component.html',
    imports: [WarehouseCommonModule, ReactiveFormsModule, FormsModule]
})
export class DeconditionDialogComponent implements OnInit {
  isSaving = false;
  isNotValid = false;
  produit?: IProduit;
  editForm = this.fb.group({
    qtyMvt: [null, [Validators.required, Validators.min(1)]],
  });

  constructor(
    private fb: FormBuilder,
    public activeModal: NgbActiveModal,
    protected deconditionService: DeconditionService,
  ) {}

  ngOnInit(): void {}

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
    if (oldStock < Number(qty)) {
      this.isNotValid = true;
    } else {
      this.isNotValid = false;
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IDecondition>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
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
      qtyMvt: this.editForm.get(['qtyMvt'])!.value,
      produitId: this.produit.id,
    };
  }
}
