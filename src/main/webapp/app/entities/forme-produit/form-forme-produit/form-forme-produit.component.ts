import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { FamilleProduit } from '../../../shared/model/famille-produit.model';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { CommonModule } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { Card } from 'primeng/card';
import { FormeProduitService } from '../forme-produit.service';
import { IFormProduit } from '../../../shared/model/form-produit.model';
import { ErrorService } from '../../../shared/error.service';

@Component({
  selector: 'jhi-form-famille',
  templateUrl: './form-forme-produit.component.html',
  styleUrls: ['../../common-modal.component.scss'],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ButtonModule, InputTextModule, RippleModule, ToastAlertComponent, Card]
})
export class FormFormeProduitComponent implements OnInit,AfterViewInit {

  entity: IFormProduit | null = null;
  header: string = '';
  protected isSaving = false;
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]]
  });

  private readonly entityService = inject(FormeProduitService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);
  private readonly libelle = viewChild.required<ElementRef>('libelle');

  ngOnInit(): void {
    if (this.entity) {
      this.updateForm(this.entity);
    }
  }
  ngAfterViewInit(): void {
    setTimeout(() => {
      this.libelle().nativeElement.focus();
    }, 100);
  }
  protected updateForm(entity: IFormProduit): void {
    this.editForm.patchValue({
      id: entity.id,
      libelle: entity.libelle
    });
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

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IFormProduit>>): void {
    result.subscribe({
      next: (res: HttpResponse<IFormProduit>) => this.onSaveSuccess(res.body),
      error: (err) => this.onSaveError(err)
    });
  }

  private onSaveSuccess(response: IFormProduit | null): void {
    this.activeModal.close(response);
  }

  private createFromForm(): IFormProduit {
    return {
      ...new FamilleProduit(),
      id: this.editForm.get(['id']).value,
      libelle: this.editForm.get(['libelle']).value
    };
  }
}
