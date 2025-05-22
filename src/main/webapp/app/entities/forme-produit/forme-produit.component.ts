import { Component, OnInit, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ConfirmationService, LazyLoadEvent } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FormeProduitService } from './forme-produit.service';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { FormProduit, IFormProduit } from '../../shared/model/form-produit.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { DialogModule } from 'primeng/dialog';
import { FileUploadModule } from 'primeng/fileupload';
import { ToolbarModule } from 'primeng/toolbar';
import { TableModule } from 'primeng/table';
import { TextareaModule } from 'primeng/textarea';
import { acceptButtonProps, rejectButtonProps } from '../../shared/util/modal-button-props';
import {Tooltip} from "primeng/tooltip";

@Component({
  selector: 'jhi-forme-produit',
  templateUrl: './forme-produit.component.html',
  providers: [ConfirmationService],
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    RippleModule,
    ConfirmDialogModule,
    ToastModule,
    DialogModule,
    FileUploadModule,
    ToolbarModule,
    TableModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    TextareaModule,
    Tooltip,
  ],
})
export class FormeProduitComponent implements OnInit {
  protected entityService = inject(FormeProduitService);
  protected activatedRoute = inject(ActivatedRoute);
  protected router = inject(Router);
  protected modalService = inject(ConfirmationService);
  private fb = inject(UntypedFormBuilder);

  entites?: IFormProduit[];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page = 0;
  selectedEl?: IFormProduit;
  loading!: boolean;
  isSaving = false;
  displayDialog?: boolean;

  editForm = this.fb.group({
    id: [],
    libelle: [null, [Validators.required]],
  });

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(() => {
      this.loadPage();
    });
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.entityService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
      })
      .subscribe({
        next: (res: HttpResponse<IFormProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.entityService
        .query({
          page: this.page,
          size: event.rows,
        })
        .subscribe({
          next: (res: HttpResponse<IFormProduit[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  confirmDialog(id: number): void {
    this.modalService.confirm({
      message: 'Voulez-vous supprimer cet enregistrement ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.entityService.delete(id).subscribe(() => {
          this.loadPage(0);
        });
      },
    });
  }

  updateForm(entity: IFormProduit): void {
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
    this.updateForm(new FormProduit());
    this.displayDialog = true;
  }

  onEdit(entity: IFormProduit): void {
    this.updateForm(entity);
    this.displayDialog = true;
  }

  delete(entity: IFormProduit): void {
    if (entity) {
      this.confirmDelete(entity.id);
    }
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  protected onSuccess(data: IFormProduit[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/forme-produit'], {
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

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IFormProduit>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  private createFromForm(): IFormProduit {
    return {
      ...new FormProduit(),
      id: this.editForm.get(['id']).value,
      libelle: this.editForm.get(['libelle']).value,
    };
  }
}
