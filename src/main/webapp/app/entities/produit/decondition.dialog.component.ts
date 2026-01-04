import { AfterViewInit, Component, ElementRef, inject, viewChild } from '@angular/core';
import { IProduit } from '../../shared/model/produit.model';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { Decondition, IDecondition } from '../../shared/model/decondition.model';
import { DeconditionService } from '../decondition/decondition.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { InputText } from 'primeng/inputtext';
import { Card } from 'primeng/card';
import { Button } from 'primeng/button';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { InputNumber } from 'primeng/inputnumber';

@Component({
  selector: 'jhi-decondition',
  templateUrl: 'decondition-dialog.component.html',
  styleUrls: ['./detail-form-dialog.scss'],
  imports: [WarehouseCommonModule, ReactiveFormsModule, FormsModule, InputText, Card, Button, ToastAlertComponent],
})
export class DeconditionDialogComponent implements AfterViewInit {
  activeModal = inject(NgbActiveModal);
  isSaving = false;
  isNotValid = false;
  produit?: IProduit;
  protected deconditionService = inject(DeconditionService);
  private fb = inject(FormBuilder);
  editForm = this.fb.group({
    qtyMvt: [1, [Validators.required, Validators.min(1)]],
  });
  private itemQty = viewChild.required<ElementRef>('qtyMvt');

  save(): void {
    this.isSaving = true;
    const decondition = this.createFromForm();
    this.subscribeToSaveResponse(this.deconditionService.create(decondition));
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      const input = this.itemQty()?.nativeElement;
      if (input) {
        input.focus();
        input.select();
      }
    }, 100);
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
      qtyMvt: this.editForm.get(['qtyMvt']).value,
      produitId: this.produit.id,
    };
  }
}
