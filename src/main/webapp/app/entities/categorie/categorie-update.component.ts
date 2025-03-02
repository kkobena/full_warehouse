import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';

import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';

import { Categorie, ICategorie } from 'app/shared/model/categorie.model';
import { CategorieService } from './categorie.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-categorie-update',
  templateUrl: './categorie-update.component.html',
  imports: [WarehouseCommonModule, FormsModule, ReactiveFormsModule],
})
export class CategorieUpdateComponent implements OnInit {
  protected categorieService = inject(CategorieService);
  protected activatedRoute = inject(ActivatedRoute);
  private fb = inject(UntypedFormBuilder);

  isSaving = false;

  editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
  });

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ categorie }) => {
      this.updateForm(categorie);
    });
  }

  updateForm(categorie: ICategorie): void {
    this.editForm.patchValue({
      id: categorie.id,
      libelle: categorie.libelle,
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const categorie = this.createFromForm();
    if (categorie.id !== undefined) {
      this.subscribeToSaveResponse(this.categorieService.update(categorie));
    } else {
      this.subscribeToSaveResponse(this.categorieService.create(categorie));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ICategorie>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  private createFromForm(): ICategorie {
    return {
      ...new Categorie(),
      id: this.editForm.get(['id']).value,
      libelle: this.editForm.get(['libelle']).value,
    };
  }
}
