import { Component, inject, viewChild } from '@angular/core';
import { IProduit } from '../../../shared/model/produit.model';
import { ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ProduitService } from '../produit.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Button } from 'primeng/button';
import { DateNaissDirective } from '../../../shared/date-naiss.directive';
import { InputText } from 'primeng/inputtext';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { Card } from 'primeng/card';
import { ErrorService } from '../../../shared/error.service';

@Component({
  selector: 'jhi-date-peremption-form',
  imports: [Button, ReactiveFormsModule, DateNaissDirective, InputText, ToastAlertComponent, Card],
  templateUrl: './date-peremption-form.component.html',
  styleUrls: ['../../common-modal.component.scss'],
})
export class DatePeremptionFormComponent {
  produit?: IProduit;
  protected isSaving = false;
  protected minDate = new Date().toISOString().split('T')[0];
  protected maxDate = '2100-12-31';
  protected isValid = true;
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    datePeremption: [null, [Validators.required]],
  });
  private readonly produitService = inject(ProduitService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly activeModal = inject(NgbActiveModal);

  private createFrom(): any {
    return {
      datePeremption: this.editForm.get(['datePeremption']).value,
    };
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    this.isSaving = true;
    const datePeremtion = this.createFrom();
    this.produitService.updatePeremptionDate(this.produit.id, datePeremtion).subscribe({
      next: () => {
        this.isSaving = false;
        this.alert().showInfo('Date de péremption mise à jour avec succès.');
        this.cancel();
      },
      error: error => {
        this.isSaving = false;
        this.isValid = false;
        this.alert().showError(this.errorService.getErrorMessage(error));
      },
    });
  }
}
