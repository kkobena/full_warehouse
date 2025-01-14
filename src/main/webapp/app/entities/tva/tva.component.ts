import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { ConfirmationService, LazyLoadEvent } from 'primeng/api';
import { TvaService } from './tva.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule, UntypedFormBuilder, Validators } from '@angular/forms';
import { ITva, Tva } from '../../shared/model/tva.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TableModule } from 'primeng/table';
import { KeyFilterModule } from 'primeng/keyfilter';

@Component({
  selector: 'jhi-tva',
  templateUrl: './tva.component.html',
  providers: [ConfirmationService],
  standalone: true,
  imports: [
    WarehouseCommonModule,
    FormsModule,
    ConfirmDialogModule,
    DialogModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    RouterModule,
    TableModule,
    KeyFilterModule,
  ],
})
export class TvaComponent implements OnInit {
  tvas?: ITva[];
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page = 0;
  predicate!: string;
  ascending!: boolean;
  selectedTva?: ITva;
  loading!: boolean;
  isSaving = false;
  displayDialog?: boolean;
  editForm = this.fb.group({
    id: [],
    taux: [null, [Validators.required]],
  });

  constructor(
    protected tvaService: TvaService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: ConfirmationService,
    private fb: UntypedFormBuilder,
  ) {}

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.tvaService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
      })
      .subscribe({
        next: (res: HttpResponse<ITva[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.tvaService
        .query({
          page: this.page,
          size: event.rows,
        })
        .subscribe({
          next: (res: HttpResponse<ITva[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(() => {
      this.loadPage();
    });
  }

  delete(tva: ITva): void {
    if (tva) {
      this.confirmDelete(tva.id);
    }
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }

  confirmDelete(id: number): void {
    this.confirmDialog(id);
  }

  confirmDialog(id: number): void {
    this.modalService.confirm({
      message: 'Voulez-vous supprimer cet enregistrement ?',
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.tvaService.delete(id).subscribe(() => {
          this.loadPage(0);
        });
      },
    });
  }

  updateForm(tva: ITva): void {
    this.editForm.patchValue({
      id: tva.id,
      taux: tva.taux,
    });
  }

  save(): void {
    this.isSaving = true;
    const tva = this.createFromForm();
    this.subscribeToSaveResponse(this.tvaService.create(tva));
  }

  cancel(): void {
    this.displayDialog = false;
  }

  addNewEntity(): void {
    this.updateForm(new Tva());
    this.displayDialog = true;
  }

  protected onSuccess(data: ITva[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/tva'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
      },
    });
    this.tvas = data || [];
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

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ITva>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  private createFromForm(): ITva {
    return {
      ...new Tva(),
      id: this.editForm.get(['id'])!.value,
      taux: this.editForm.get(['taux'])!.value,
    };
  }
}
