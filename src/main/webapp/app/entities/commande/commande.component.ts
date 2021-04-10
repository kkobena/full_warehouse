import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, ParamMap, Router, Data } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ICommande } from 'app/shared/model/commande.model';
import { ITEMS_PER_PAGE, DETAIL_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { CommandeService } from './commande.service';
import { CommandeDeleteDialogComponent } from './commande-delete-dialog.component';
import { IOrderLine } from 'app/shared/model/order-line.model';
import { OrderLineService } from '../order-line/order-line.service';
import { IProduit } from 'app/shared/model/produit.model';
import { AgGridAngular } from 'ag-grid-angular';
import { ProduitService } from '../produit/produit.service';
import { AllCommunityModules } from '@ag-grid-community/all-modules';

@Component({
  selector: 'jhi-commande',
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
      }
      .active {
        background-color: #95caf9 !important;
      }
      .master {
        padding: 14px 12px;
        border-radius: 12px;
        box-shadow: 0 4px 8px rgb(0 0 0 / 16%);
        justify-content: space-between;
      }
      .ag-theme-alpine {
        max-height: 700px;
        height: 600px;
        min-height: 500px;
      }
    `,
  ],
  templateUrl: './commande.component.html',
})
export class CommandeComponent implements OnInit, OnDestroy {
  commandes?: ICommande[];
  commandeSelected?: ICommande;
  selectedRowIndex?: number;
  selectedRowOrderLines?: IOrderLine[] = [];
  eventSubscriber?: Subscription;
  totalItems = 0;
  totalDetail = 0;
  detaimPerPage = DETAIL_PER_PAGE;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  pageItem!: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  public columnDefs: any[];
  rowData: any = [];

  @ViewChild('productGrid') productGrid!: AgGridAngular;
  public modules: any[] = AllCommunityModules;

  constructor(
    protected commandeService: CommandeService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventManager: JhiEventManager,
    protected modalService: NgbModal,
    protected orderLineService: OrderLineService,
    protected produitService: ProduitService
  ) {
    this.columnDefs = [
      { headerName: 'Libellé', field: 'produit.libelle', minWidth: 200, flex: 2 },

      {
        headerName: 'Prix achat',
        width: 150,
        field: 'costAmount',
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Montant achat',
        width: 150,
        field: 'orderAmount',
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Quantité reçue',
        width: 150,
        field: 'quantityReceived',
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: this.formatNumber,
      },
    ];
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    const pageToLoad: number = page || this.page || 1;
    this.commandeService
      .query({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        sort: this.sort(),
      })
      .subscribe(
        (res: HttpResponse<ICommande[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
        () => this.onError()
      );
  }

  ngOnInit(): void {
    this.handleNavigation();
    this.registerChangeInCommandes();
    this.loadProduits();
  }

  protected handleNavigation(): void {
    combineLatest(this.activatedRoute.data, this.activatedRoute.queryParamMap, (data: Data, params: ParamMap) => {
      const page = params.get('page');
      const pageNumber = page !== null ? +page : 1;
      const sort = (params.get('sort') ?? data['defaultSort']).split(',');
      const predicate = sort[0];
      const ascending = sort[1] === 'desc';
      if (pageNumber !== this.page || predicate !== this.predicate || ascending !== this.ascending) {
        this.predicate = predicate;
        this.ascending = ascending;
        this.loadPage(pageNumber, true);
      }
    }).subscribe();
  }

  ngOnDestroy(): void {
    if (this.eventSubscriber) {
      this.eventManager.destroy(this.eventSubscriber);
    }
  }

  trackId(index: number, item: ICommande): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  registerChangeInCommandes(): void {
    this.eventSubscriber = this.eventManager.subscribe('commandeListModification', () => this.loadPage());
  }

  delete(commande: ICommande): void {
    const modalRef = this.modalService.open(CommandeDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.commande = commande;
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'updatedAt') {
      result.push('updatedAt');
    }
    return result;
  }

  protected onSuccess(data: ICommande[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/commande'], {
        queryParams: {
          page: this.page,
          size: this.itemsPerPage,
          sort: this.predicate + ',' + (this.ascending ? 'asc' : 'desc'),
        },
      });
    }
    this.commandes = data || [];
    this.ngbPaginationPage = this.page;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  clickRow(commande: ICommande): void {
    this.selectedRowIndex = commande.id;
    this.commandeSelected = commande;

    this.loadOrderLine();
  }
  protected onOrderLineSuccess(data: IOrderLine[] | null): void {
    this.rowData = data || [];
  }

  protected onOrderLineError(): void {}
  loadOrderLine(): void {
    this.orderLineService.query(this.commandeSelected?.id).subscribe(
      (res: HttpResponse<IOrderLine[]>) => this.onOrderLineSuccess(res.body),
      () => this.onError()
    );
  }

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.rowData = data || [];
  }

  loadProduits(): void {
    this.produitService
      .query({
        page: 0,
        size: 9999,
        sort: this.sortProduits(),
        withdetail: false,
      })
      .subscribe(
        (res: HttpResponse<any[]>) => this.onProduitSuccess(res.body),
        () => this.onError()
      );
  }

  sortProduits(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'libelle') {
      result.push('libelle');
    }
    return result;
  }
  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1,');
  }
}
