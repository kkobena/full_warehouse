import {Component, OnInit} from '@angular/core';
import {UntypedFormBuilder, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {ConfirmationService, LazyLoadEvent} from 'primeng/api';
import {HttpHeaders, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {FormeProduitService} from './forme-produit.service';
import {ITEMS_PER_PAGE} from '../../shared/constants/pagination.constants';
import {FormProduit, IFormProduit} from '../../shared/model/form-produit.model';

@Component({
  selector: 'jhi-forme-produit',
  templateUrl: './forme-produit.component.html',
  providers: [ConfirmationService],
})
export class FormeProduitComponent implements OnInit {
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

  constructor(
    protected entityService: FormeProduitService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: ConfirmationService,
    private fb: UntypedFormBuilder
  ) {
  }

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
      .subscribe(
        (res: HttpResponse<IFormProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        () => this.onError()
      );
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first! / event.rows!;
      this.loading = true;
      this.entityService
        .query({
          page: this.page,
          size: event.rows,
        })
        .subscribe(
          (res: HttpResponse<IFormProduit[]>) => this.onSuccess(res.body, res.headers, this.page),
          () => this.onError()
        );
    }
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
      this.confirmDelete(entity.id!);
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
    result.subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError()
    );
  }

  private createFromForm(): IFormProduit {
    return {
      ...new FormProduit(),
      id: this.editForm.get(['id'])!.value,
      libelle: this.editForm.get(['libelle'])!.value,
    };
  }
}
