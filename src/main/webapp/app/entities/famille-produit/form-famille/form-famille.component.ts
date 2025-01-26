import { HttpResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { MessageService, SelectItem } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { FamilleProduitService } from '../famille-produit.service';
import { FamilleProduit, IFamilleProduit } from '../../../shared/model/famille-produit.model';
import { CategorieService } from '../../categorie/categorie.service';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToastModule } from 'primeng/toast';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { ICategorie } from '../../../shared/model/categorie.model';

@Component({
  selector: 'jhi-form-famille',
  templateUrl: './form-famille.component.html',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ReactiveFormsModule,
    ToastModule,
    DropdownModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
  ],
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
    private fb: UntypedFormBuilder,
    private messageService: MessageService,
    protected categorieProduitService: CategorieService,
  ) {}

  ngOnInit(): void {
    this.familleProduit = this.config.data.familleProduit;
    this.populateAssurrance();
    if (this.familleProduit) {
      this.updateForm(this.familleProduit);
    }
  }

  updateForm(entity: IFamilleProduit): void {
    this.editForm.patchValue({
      id: entity.id,
      code: entity.code,
      libelle: entity.libelle,
      categorieId: entity.categorieId,
    });
  }

  populateAssurrance(): void {
    this.categorieProduitService.query({ search: '' }).subscribe({
      next: (res: HttpResponse<ICategorie[]>) => {
        this.categorieproduits = res.body!.map((item: ICategorie) => {
          return {
            label: item.libelle,
            value: item.id,
          };
        });
      },
    });
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

  cancel(): void {
    this.ref.destroy();
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IFamilleProduit>>): void {
    result.subscribe({
      next: (res: HttpResponse<IFamilleProduit>) => this.onSaveSuccess(res.body),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(response: IFamilleProduit | null): void {
    this.messageService.add({
      severity: 'info',
      summary: 'Information',
      detail: 'Enregistrement effectué avec succès',
    });
    this.ref.close(response);
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
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
}
