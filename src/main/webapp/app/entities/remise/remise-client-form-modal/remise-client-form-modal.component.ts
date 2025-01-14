import { AfterViewInit, Component, ElementRef, inject, viewChild } from '@angular/core';
import { ButtonDirective } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ProduitService } from '../../produit/produit.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { FormBuilder, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { KeyFilterModule } from 'primeng/keyfilter';
import { Ripple } from 'primeng/ripple';
import { StyleClassModule } from 'primeng/styleclass';
import { IRemise, Remise } from '../../../shared/model/remise.model';
import { RemiseService } from '../remise.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';

@Component({
  selector: 'jhi-remise-client-form-modal',
  providers: [MessageService, ConfirmationService],
  standalone: true,
  imports: [
    ButtonDirective,
    ToastModule,
    DialogModule,
    DropdownModule,
    FormsModule,
    InputTextModule,
    KeyFilterModule,
    ReactiveFormsModule,
    Ripple,
    StyleClassModule,
  ],
  templateUrl: './remise-client-form-modal.component.html',
})
export class RemiseClientFormModalComponent implements AfterViewInit {
  modalService = inject(NgbModal);
  activeModal = inject(NgbActiveModal);
  produitService = inject(ProduitService);
  messageService = inject(MessageService);
  entityService = inject(RemiseService);
  remiseValue = viewChild.required<ElementRef>('remiseValue');
  fb = inject(FormBuilder);
  editForm = this.fb.group({
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
  entity: IRemise | null = null;
  protected isSaving = false;
  protected title: string | null = null;

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
      error: () => this.onSaveError(),
    });
  }

  private onSaveSuccess(): void {
    this.isSaving = false;
    this.activeModal.close();
  }

  private onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: "L'enregistrement n'a pas été effectué!",
    });
  }

  private createFromForm(): IRemise {
    return {
      ...new Remise(),
      id: this.editForm.get(['id'])!.value,
      remiseValue: this.editForm.get(['remiseValue'])!.value,
      valeur: this.editForm.get(['valeur'])!.value,
      type: 'remiseClient',
    };
  }
}
