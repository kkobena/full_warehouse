import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TableauProduitService } from './tableau-produit.service';
import { IResponseDto } from '../../shared/util/response-dto';
import { ITableau, Tableau } from '../../shared/model/tableau.model';

@Component({
  selector: 'jhi-groupe-fournisseur',
  templateUrl: './tableau-produit.component.html',
  providers: [MessageService, ConfirmationService],
})
export class TableauProduitComponent implements OnInit {
  fileDialog?: boolean;
  responsedto!: IResponseDto;
  responseDialog?: boolean;
  entites?: ITableau[];
  selectedEl?: ITableau;
  loading = false;
  isSaving = false;
  displayDialog?: boolean;

  editForm = this.fb.group({
    id: new FormControl<number | null>(null, {}),
    code: new FormControl<string | null>(null, {
      validators: [Validators.required],
      nonNullable: true,
    }),

    value: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.required],
      nonNullable: true,
    }),
  });

  constructor(
    protected entityService: TableauProduitService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    private messageService: MessageService,
    protected modalService: ConfirmationService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage(): void {
    this.loading = true;
    this.entityService.query().subscribe({
      next: (res: HttpResponse<ITableau[]>) => this.onSuccess(res.body),
      error: () => this.onError(),
    });
  }

  lazyLoading(): void {
    this.loadPage();
  }

  confirmDialog(id: number): void {
    this.modalService.confirm({
      message: 'Voulez-vous supprimer cet enregistrement ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage();
        });
      },
    });
  }

  updateForm(entity: ITableau): void {
    this.editForm.patchValue({
      id: entity.id,
      code: entity.code,
      value: entity.value,
    });
  }

  save(): void {
    this.isSaving = true;
    const entity = this.createFromForm();

    if (entity.id !== undefined) {
      this.subscribeToSaveResponse(this.entityService.update(entity));
    } else {
      this.subscribeToSaveResponse(this.entityService.create(entity));
    }
  }

  cancel(): void {
    this.displayDialog = false;
    this.fileDialog = false;
  }

  addNewEntity(): void {
    this.updateForm(new Tableau());
    this.displayDialog = true;
  }

  onEdit(entity: ITableau): void {
    this.updateForm(entity);
    this.displayDialog = true;
  }

  delete(entity: ITableau): void {
    if (entity && entity.id) {
      this.confirmDelete(entity.id);
    }
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  showFileDialog(): void {
    this.fileDialog = true;
  }

  protected onSuccess(data: ITableau[] | null): void {
    this.router.navigate(['/tableaux']);
    this.entites = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.loading = false;
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.displayDialog = false;
    this.loadPage();
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'info',
      summary: 'Enregistrement',
      detail: 'Opération effectuée avec succès',
    });
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ITableau>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  private createFromForm(): ITableau {
    return {
      ...new Tableau(),
      id: this.editForm.get(['id'])!.value,
      code: this.editForm.get(['code'])!.value,
      value: this.editForm.get(['value'])!.value,
    };
  }
}
