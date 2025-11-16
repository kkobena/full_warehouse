import { AfterViewInit, Component, inject, viewChild } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ProduitService } from '../../produit/produit.service';
import { RemiseService } from '../remise.service';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { IProduit } from '../../../shared/model/produit.model';
import { IRayon } from '../../../shared/model/rayon.model';
import { CodeRemise } from '../../../shared/model/remise.model';
import { RayonService } from '../../rayon/rayon.service';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { TableHeaderCheckbox, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { SpinerService } from '../../../shared/spiner.service';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { Select } from 'primeng/select';
import { Card } from 'primeng/card';
import { ErrorService } from '../../../shared/error.service';

@Component({
  selector: 'jhi-code-remise-produits-modal',
  imports: [
    FormsModule,
    InputTextModule,
    TagModule,
    ToolbarModule,
    TableModule,
    TooltipModule,
    ButtonModule,
    ToastAlertComponent,
    Select,
    Card,
  ],
  templateUrl: './code-remise-produits-modal.component.html',
  styleUrls: ['../../common-modal.component.scss'],
})
export class CodeRemiseProduitsModalComponent implements AfterViewInit {
  checkbox = viewChild<TableHeaderCheckbox>('checkbox');
  protected codeRemise: CodeRemise | null = null;
  protected selectedRayon: IRayon | null = null;
  protected produits: IProduit[] = [];
  protected selectedProduits: IProduit[] = [];
  protected rayons: IRayon[] = [];
  protected isSaving = false;
  protected totalItems = 0;
  protected page = 0;
  protected loading!: boolean;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected search: string | null = null;
  private readonly activeModal = inject(NgbActiveModal);
  private spinner = inject(SpinerService);
  private readonly produitService = inject(ProduitService);
  private readonly entityService = inject(RemiseService);
  private readonly rayonService = inject(RayonService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly errorService = inject(ErrorService);

  loadData(): void {
    if (this.selectedRayon || this.search) {
      const pageToLoad: number = this.page;
      this.loading = true;
      this.produitService
        .query({
          search: this.search,
          page: pageToLoad,
          size: this.itemsPerPage,
          rayonId: this.selectedRayon,
        })
        .subscribe({
          next: (res: HttpResponse<IProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
          error: err => this.onSaveError(err),
        });
    } else {
      this.produits = [];
    }
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  ngAfterViewInit(): void {
    this.loadRayons();
  }

  protected loadRayons(): void {
    this.rayonService
      .query({
        page: 0,
        size: 9999,
      })
      .subscribe({
        next: (res: HttpResponse<IRayon[]>) => {
          this.rayons = res.body || [];
        },
      });
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if ((this.selectedRayon || this.search) && event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.produitService
        .query({
          search: this.search,
          page: this.page,
          size: event.rows,
          rayonId: this.selectedRayon,
        })
        .subscribe({
          next: (res: HttpResponse<IProduit[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: err => this.onSaveError(err),
        });
    }
  }

  protected onSave(): void {
    this.isSaving = true;
    this.spinner.show();
    this.entityService.addProduitsToCodeRemise(this.buildParams()).subscribe({
      next: () => {
        this.isSaving = false;
        this.spinner.hide();
        this.activeModal.close();
      },
      error: error => {
        this.onSaveError(error);
      },
    });
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.isSaving = false;
    this.spinner.hide();
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private onSuccess(data: IProduit[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.produits = data || [];
    this.loading = false;
  }

  private buildParams(): any {
    const params: any = {};
    params.codeRemise = this.codeRemise.value;
    if (this.selectedRayon) {
      params.rayonId = this.selectedRayon;
    }
    if (this.search) {
      params.search = this.search;
    }
    const all = this.checkbox().checked;
    params.all = all;
    if (!all) {
      params.produitIds = this.selectedProduits.map(produit => produit.id);
    }
    return params;
  }
}
