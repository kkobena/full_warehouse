import { Component, OnInit, inject } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ConfirmationService, LazyLoadEvent } from 'primeng/api';
import { Observable } from 'rxjs';
import { TypeEtiquetteService } from './type-etiquette.service';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { ITypeEtiquette, TypeEtiquette } from '../../shared/model/type-etiquette.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';

@Component({
    selector: 'jhi-type-etiquette',
    templateUrl: './type-etiquette.component.html',
    imports: [
        WarehouseCommonModule,
        FormsModule,
        DialogModule,
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        RippleModule,
        RouterModule,
        TableModule,
    ]
})
export class TypeEtiquetteComponent implements OnInit {
  protected entityService = inject(TypeEtiquetteService);
  protected activatedRoute = inject(ActivatedRoute);
  protected router = inject(Router);
  protected modalService = inject(ConfirmationService);
  private fb = inject(UntypedFormBuilder);

  entites?: ITypeEtiquette[];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page = 0;
  selectedEl?: ITypeEtiquette;
  loading = false;
  isSaving = false;
  displayDialog = false;

  editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
  });

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
      })
      .subscribe(
        (res: HttpResponse<ITypeEtiquette[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        () => this.onError(),
      );
  }

  lazyLoading(event: LazyLoadEvent): void {
    this.page = event.first / event.rows;
    this.loading = true;
    this.entityService
      .query({
        page: this.page,
        size: event.rows,
      })
      .subscribe(
        (res: HttpResponse<ITypeEtiquette[]>) => this.onSuccess(res.body, res.headers, this.page),
        () => this.onError(),
      );
  }

  confirmDialog(id: number): void {
    this.modalService.confirm({
      message: 'Voulez-vous supprimer cet enregistrement ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage(0);
        });
      },
    });
  }

  updateForm(entity: ITypeEtiquette): void {
    this.editForm.patchValue({
      id: entity.id,
      libelle: entity.libelle,
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
  }

  addNewEntity(): void {
    this.updateForm(new TypeEtiquette());
    this.displayDialog = true;
  }

  onEdit(entity: ITypeEtiquette): void {
    this.updateForm(entity);
    this.displayDialog = true;
  }

  delete(entity: ITypeEtiquette): void {
    this.confirmDelete(entity.id);
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  protected onSuccess(data: ITypeEtiquette[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/type-etiquette'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
      },
    });
    this.entites = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.loading = false;
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.displayDialog = false;
    this.loadPage(0);
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ITypeEtiquette>>): void {
    result.subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError(),
    );
  }

  private createFromForm(): ITypeEtiquette {
    return {
      ...new TypeEtiquette(),
      id: this.editForm.get(['id'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
    };
  }
}
