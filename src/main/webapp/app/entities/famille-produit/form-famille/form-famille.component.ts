import { HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MessageService, SelectItem } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { FamilleProduitService } from '../famille-produit.service';
import { FamilleProduit, IFamilleProduit } from '../../../shared/model/famille-produit.model';
import { CategorieService } from '../../categorie/categorie.service';

@Component({
  selector: 'jhi-form-famille',
  templateUrl: './form-famille.component.html',
})
export class FormFamilleComponent implements OnInit {
  isSaving = false;
  familleProduit?: IFamilleProduit;
  categorieproduits: SelectItem[] = [];
  editForm = this.fb.group({
    id: [],
    code: [null, [Validators.required]],
    libelle: [null, [Validators.required]],
    categorieId: [null, [Validators.required]],
  });

  constructor(
    protected entityService: FamilleProduitService,
    public ref: DynamicDialogRef,
    public config: DynamicDialogConfig,
    private fb: FormBuilder,
    private messageService: MessageService,
    protected categorieProduitService: CategorieService
  ) {}

  ngOnInit(): void {
    this.familleProduit = this.config.data.familleProduit;
    this.populateAssurrance();
  }

  updateForm(entity: IFamilleProduit): void {
    this.editForm.patchValue({
      id: entity.id,
      code: entity.code,
      libelle: entity.libelle,
      categorieId: entity.categorieId,
    });
  }

  private createFromForm(): IFamilleProduit {
    return {
      ...new FamilleProduit(),
      id: this.editForm.get(['id'])!.value,
      code: this.editForm.get(['code'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
      categorieId: this.editForm.get(['categorieId'])!.value,
    };
  }
  async populateAssurrance(): Promise<void> {
    const categoriesResponse = await this.categorieProduitService.queryPromise({ search: '' });
    categoriesResponse.forEach(e => {
      this.categorieproduits.push({ label: e.libelle, value: e.id });
    });
    if (this.familleProduit) {
      this.updateForm(this.familleProduit);
    }
  }
  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();
    if (entity.id !== undefined && entity.id !== null) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IFamilleProduit>>): void {
    result.subscribe(
      (res: HttpResponse<IFamilleProduit>) => this.onSaveSuccess(res.body),
      () => this.onSaveError()
    );
  }
  protected onSaveSuccess(response: IFamilleProduit | null): void {
    this.messageService.add({ severity: 'info', summary: 'Information', detail: 'Enregistrement effectué avec succès' });
    this.ref.close(response);
  }
  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Enregistrement a échoué' });
  }
  cancel(): void {
    this.ref.destroy();
  }
}
