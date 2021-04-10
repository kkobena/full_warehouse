import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router } from '@angular/router';
import { combineLatest, Subscription } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { IProduit } from 'app/shared/model/produit.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { ProduitService } from './produit.service';
import { ProduitDeleteDialogComponent } from './produit-delete-dialog.component';
import { ProduitImageDialogueComponent } from './produit-image-dialogue.component';
import { faCut, faFileUpload, faImage, faPlusCircle } from '@fortawesome/free-solid-svg-icons';
import { DetailFormDialogComponent } from './detail-form-dialog.component';
import { DeconditionDialogComponent } from './decondition.dialog.component';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';

@Component({
  selector: 'jhi-produit',
  styles: [
    `
      .table tr th {
        font-size: 0.9rem;
      }

      .btn-sm,
      .btn-group-sm > .btn {
        font-size: 1rem;
      }
    `,
  ],
  templateUrl: './produit.component.html',
})
export class ProduitComponent implements OnInit, OnDestroy {
  faFileUpload = faFileUpload;
  faImage = faImage;
  faCut = faCut;
  faPlusCircle = faPlusCircle;
  produits?: IProduit[];
  eventSubscriber?: Subscription;
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  search!: string;
  package = 'PACKAGE';
  detail = 'DETAIL';

  constructor(
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventManager: JhiEventManager,
    protected modalService: NgbModal
  ) {}

  loadPage(page?: number, dontNavigate?: boolean): void {
    const pageToLoad: number = page || this.page || 1;

    this.produitService
      .query({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        sort: this.sort(),
        withdetail: true,
        search: this.search || '',
      })
      .subscribe(
        (res: HttpResponse<IProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
        () => this.onError()
      );
  }

  ngOnInit(): void {
    this.handleNavigation();
    this.registerChangeInProduits();
  }

  protected handleNavigation(): void {
    combineLatest(this.activatedRoute.data, this.activatedRoute.queryParamMap, (data: Data, params: ParamMap) => {
      const page = params.get('page');
      const pageNumber = page !== null ? +page : 1;
      const sort = (params.get('sort') ?? data['defaultSort']).split(',');
      const predicate = sort[0];
      const ascending = sort[1] === 'asc';
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

  trackId(index: number, item: IProduit): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  registerChangeInProduits(): void {
    this.eventSubscriber = this.eventManager.subscribe('produitListModification', () => this.loadPage());
  }

  delete(produit: IProduit): void {
    const modalRef = this.modalService.open(ProduitDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.produit = produit;
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'libelle') {
      result.push('libelle');
    }
    return result;
  }

  protected onSuccess(data: IProduit[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/produit'], {
        queryParams: {
          page: this.page,
          size: this.itemsPerPage,
          sort: this.predicate + ',' + (this.ascending ? 'asc' : 'desc'),
        },
      });
    }
    this.produits = data || [];
    this.ngbPaginationPage = this.page;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  addImage(produit: IProduit): void {
    const modalRef = this.modalService.open(ProduitImageDialogueComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.produit = produit;
  }

  addDetail(produit: IProduit): void {
    const modalRef = this.modalService.open(DetailFormDialogComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.produit = produit;
  }

  editDetail(produit: IProduit): void {
    const modalRef = this.modalService.open(DetailFormDialogComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.entity = produit;
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, { backdrop: 'static', centered: true });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  decondition(produit: IProduit): void {
    if (produit.produits?.length === 0) {
      this.openInfoDialog("Le produit n'a pas de détail. Vous devriez en ajouter d'abord", 'alert alert-info');
    } else {
      const modalRef = this.modalService.open(DeconditionDialogComponent, {
        size: '60%',
        backdrop: 'static',
        centered: true,
      });
      modalRef.componentInstance.produit = produit;
    }
  }
}
