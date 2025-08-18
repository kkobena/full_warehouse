import { HttpResponse } from '@angular/common/http';
import { Component, inject, OnInit, viewChild } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { FamilleProduitService } from '../famille-produit.service';
import { FamilleProduit, IFamilleProduit } from '../../../shared/model/famille-produit.model';
import { CategorieService } from '../../categorie/categorie.service';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { ICategorie } from '../../../shared/model/categorie.model';
import { CommonModule } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { Select } from 'primeng/select';
import { Card } from 'primeng/card';

@Component({
  selector: 'jhi-form-famille',
  templateUrl: './form-famille.component.html',
  styleUrls: ['../../common-modal.component.scss'],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ButtonModule, InputTextModule, RippleModule, ToastAlertComponent, Select, Card]
})
export class FormFamilleComponent implements OnInit {
  familleProduit?: IFamilleProduit;
  header: string = '';
  protected isSaving = false;
  protected categorieproduits: ICategorie[] = [];
  protected fb = inject(UntypedFormBuilder);
  protected editForm = this.fb.group({
    id: [],
    code: [null, [Validators.required]],
    libelle: [null, [Validators.required]],
    categorieId: [null, [Validators.required]]
  });
  protected categorieProduitService = inject(CategorieService);
  private readonly entityService = inject(FamilleProduitService);
  private readonly activeModal = inject(NgbActiveModal);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');

  ngOnInit(): void {
    this.populateAssurrance();
    if (this.familleProduit) {
      this.updateForm(this.familleProduit);
    }
  }

  protected updateForm(entity: IFamilleProduit): void {
    this.editForm.patchValue({
      id: entity.id,
      code: entity.code,
      libelle: entity.libelle,
      categorieId: entity.categorieId
    });
  }

  protected populateAssurrance(): void {
    this.categorieProduitService.query({ search: '' }).subscribe({
      next: (res: HttpResponse<ICategorie[]>) => {
        this.categorieproduits = res.body;
      }
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

  protected onSaveError(): void {
    this.isSaving = false;
    this.alert().showError();
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<IFamilleProduit>>): void {
    result.subscribe({
      next: (res: HttpResponse<IFamilleProduit>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError()
    });
  }

  private onSaveSuccess(response: IFamilleProduit | null): void {
    this.activeModal.close(response);
  }

  private createFromForm(): IFamilleProduit {
    return {
      ...new FamilleProduit(),
      id: this.editForm.get(['id']).value,
      code: this.editForm.get(['code']).value,
      libelle: this.editForm.get(['libelle']).value,
      categorieId: this.editForm.get(['categorieId']).value
    };
  }
}
