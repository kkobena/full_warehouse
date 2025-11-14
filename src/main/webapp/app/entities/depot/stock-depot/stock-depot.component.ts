import { Component, inject, OnInit } from '@angular/core';
import { Button } from 'primeng/button';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { CommonModule, DecimalPipe } from '@angular/common';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { NgbModal, NgbPagination } from '@ng-bootstrap/ng-bootstrap';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { Toolbar } from 'primeng/toolbar';
import TranslateDirective from '../../../shared/language/translate.directive';
import { IProduit } from '../../../shared/model/produit.model';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { TypeProduit } from '../../../shared/model/enumerations/type-produit.model';
import { IResponseDto } from '../../../shared/util/response-dto';
import { IProduitCriteria, ProduitCriteria } from '../../../shared/model/produit-criteria.model';
import { IConfiguration } from '../../../shared/model/configuration.model';
import { ActivatedRoute, Data, ParamMap } from '@angular/router';
import { Statut } from '../../../shared/model/enumerations/statut.model';
import { ImportProduitModalComponent } from '../../produit/import-produit-modal/import-produit-modal.component';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { showCommonModal } from '../../sales/selling-home/sale-helper';
import {
  ImportProduitReponseModalComponent
} from '../../produit/import-produit-reponse-modal/import-produit-reponse-modal.component';
import { combineLatest } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { StockDepotService } from './stock-depot.service';
import { MagasinService } from '../../magasin/magasin.service';
import { IMagasin } from '../../../shared/model/magasin.model';


@Component({
  selector: 'jhi-stock-depot',
  imports: [
    CommonModule,
    Button,
    ConfirmDialogComponent,
    DecimalPipe,
    IconField,
    InputIcon,
    InputText,
    NgbPagination,
    Select,
    TableModule,
    ToastAlertComponent,
    Toolbar,
    TranslateDirective,
    FormsModule
  ],
  templateUrl: './stock-depot.component.html',
  styleUrl: './stock-depot.component.scss'
})
export class StockDepotComponent implements OnInit {
  protected produits!: IProduit[];
  protected selectedDepot: IMagasin | null = null;
  protected depots: IMagasin[] = [];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected search: string;
  protected package = TypeProduit.PACKAGE;
  protected detail = TypeProduit.DETAIL;
  protected responsedto!: IResponseDto;
  protected isSaving = false;
  protected criteria: IProduitCriteria;
  protected configuration?: IConfiguration | null;
  protected typeImportation: string | null = null;
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly modalService = inject(NgbModal);
  private readonly stockDepotService = inject(StockDepotService);
  private readonly magasinService = inject(MagasinService);


  constructor() {
    this.criteria = new ProduitCriteria();
    this.criteria.status = Statut.ENABLE;


    this.search = '';
    this.populate();
  }

  onOpenImportDialog(): void {
    const modalRef = this.modalService.open(ImportProduitModalComponent, {
      backdrop: 'static',
      size: 'lg',
      centered: true
    });
    modalRef.componentInstance.type = this.typeImportation;
    modalRef.closed.subscribe(reason => {
      if (reason) {
        this.showResponse(reason);
        this.loadPage(0);
      }
    });
  }

  protected onSelectDepot(): void {
    this.loadPage(0);
  }

  populate(): void {
    this.magasinService.fetchAllDepots().subscribe((res: HttpResponse<IMagasin[]>) => {
      this.depots = res.body || [];

    });
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    if (this.selectedDepot !== null) {
      const pageToLoad: number = page || this.page || 1;
      let statut = 'ENABLE';
      if (this.criteria) {
        if (this.criteria.status) {
          if (this.criteria.status === Statut.DISABLE) {
            statut = 'DISABLE';
          } else if (this.criteria.status === Statut.DELETED) {
            statut = 'DELETED';
          }
        }
      }

      this.stockDepotService
        .query({
          page: pageToLoad - 1,
          size: this.itemsPerPage,
          sort: this.sort(),
          search: this.search || '',
          deconditionne: this.criteria.deconditionne,
          deconditionnable: this.criteria.deconditionnable,
          status: statut,
          magasinId: this.selectedDepot ? this.selectedDepot.id : undefined

        })
        .subscribe({
          next: (res: HttpResponse<IProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
          error: () => this.onError()
        });
    }
  }

  ngOnInit(): void {
    this.handleNavigation();
    this.registerChangeInProduits();
  }

  registerChangeInProduits(): void {
    this.loadPage();
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'libelle') {
      result.push('libelle');
    }
    return result;
  }

  onSearch(event: any): void {
    this.search = event.target.value;
    this.loadPage(0);
  }

  filtreRayon(event: any): void {
    this.criteria.rayonId = event.value;
    this.loadPage(0);
  }

  filtreFamilleProduit(event: any): void {
    this.criteria.familleId = event.value;
    this.loadPage(0);
  }


  private showResponse(responsedto: IResponseDto): void {
    showCommonModal(this.modalService, ImportProduitReponseModalComponent, { responsedto }, () => {
    }, 'lg');
  }

  private onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  private onSuccess(data: IProduit[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;

    this.produits = data || [];
    this.ngbPaginationPage = this.page;
  }

  private handleNavigation(): void {
    combineLatest(this.activatedRoute.data, this.activatedRoute.queryParamMap, (data: Data, params: ParamMap) => {
      const page = params.get('page');
      const pageNumber = page !== null ? +page : 1;


      if (pageNumber !== this.page) {

        this.loadPage(pageNumber, true);
      }
    }).subscribe();
  }
}
