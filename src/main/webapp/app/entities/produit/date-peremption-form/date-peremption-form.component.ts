import {Component, inject} from '@angular/core';
import {IProduit} from "../../../shared/model/produit.model";
import {ReactiveFormsModule, UntypedFormBuilder, Validators} from "@angular/forms";
import {ProduitService} from "../produit.service";
import {MessageService} from "primeng/api";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {Button} from "primeng/button";
import {Toast} from "primeng/toast";
import {DateNaissDirective} from "../../../shared/date-naiss.directive";
import {InputText} from "primeng/inputtext";

@Component({
  selector: 'jhi-date-peremption-form',
  imports: [
    Button,
    ReactiveFormsModule,
    Toast,
    DateNaissDirective,
    InputText
  ],
  providers: [MessageService],
  templateUrl: './date-peremption-form.component.html',
})
export class DatePeremptionFormComponent {
  produit?: IProduit;
  protected isSaving = false;
  protected minDate =new Date().toISOString().split('T')[0];
  protected maxDate ='2100-12-31';
  protected isValid = true;
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    datePeremption: [null, [Validators.required]],
  });
  private readonly produitService = inject(ProduitService);
  private readonly messageService = inject(MessageService);
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
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Date de péremption mise à jour avec succès.',
        });
        this.cancel();
      },
      error: (error) => {
        this.isSaving = false;
        this.isValid = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: error.error.message || 'Une erreur est survenue lors de la mise à jour de la date de péremption.',
        });
      },
    });
  }

}
