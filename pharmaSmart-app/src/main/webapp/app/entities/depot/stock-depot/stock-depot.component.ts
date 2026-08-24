import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbModal, NgbPagination } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, DataTableComponent, IconFieldComponent, SelectComponent, ToolbarComponent } from '../../../shared/ui';
import TranslateDirective from '../../../shared/language/translate.directive';
import { IMagasin, IProduit } from '../../../shared/model';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { IResponseDto } from '../../../shared/util/response-dto';
import { IProduitCriteria, ProduitCriteria } from '../../../shared/model/produit-criteria.model';
import { ActivatedRoute, Data, ParamMap, RouterLink } from '@angular/router';
import { Statut } from '../../../shared/model/enumerations/statut.model';
import { ImportProduitModalComponent } from '../../produit/import-produit-modal/import-produit-modal.component';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { showCommonModal } from '../../sales/selling-home/sale-helper';
import { ImportProduitReponseModalComponent } from '../../produit/import-produit-reponse-modal/import-produit-reponse-modal.component';
import { combineLatest } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { StockDepotService } from './stock-depot.service';
import { MagasinService } from '../../magasin/magasin.service';

@Component({
  selector: 'app-stock-depot',
  imports: [
    CommonModule,
    ButtonComponent,
    IconFieldComponent,
    NgbPagination,
    SelectComponent,
    DataTableComponent,
    ToolbarComponent,
    TranslateDirective,
    FormsModule,
    RouterLink,
  ],
  templateUrl: './stock-depot.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './stock-depot.component.scss',
})
export class StockDepotComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement. Un écran atteint depuis deux menus affiche donc le nom
   * de celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  protected readonly produits = signal<IProduit[] | undefined>(undefined);
  protected readonly selectedDepot = signal<IMagasin | null>(null);
  protected readonly depots = signal<IMagasin[]>([]);

  /**
   * Options du sélecteur de dépôt, avec l'adresse ajoutée au libellé.
   *
   * `computed` et non un accesseur : `items` est un `model()` signal côté `ng-select`, donc
   * une nouvelle référence de tableau à chaque cycle de détection lui fait reconstruire toute
   * sa liste d'options — et le `track` du `@for` porte sur l'objet option, si bien que chaque
   * `<div>` de la liste est détruit puis recréé sous le curseur, ce qui rend les options
   * inclicquables : `mousedown` et `mouseup` ne tombent plus sur le même nœud.
   */
  protected readonly depotOptions = computed<(IMagasin & { displayLabel: string })[]>(() =>
    this.depots().map(depot => ({
      ...depot,
      displayLabel: depot.address ? `${depot.name} — ${depot.address}` : depot.name,
    })),
  );
  protected readonly totalItems = signal(0);
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected readonly page = signal<number | undefined>(undefined);
  protected predicate!: string;
  protected ascending!: boolean;
  protected readonly ngbPaginationPage = signal(1);
  protected readonly search = signal<string | undefined>(undefined);
  protected readonly criteria = signal<IProduitCriteria | undefined>(undefined);
  protected typeImportation: string | null = null;
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly modalService = inject(NgbModal);
  private readonly stockDepotService = inject(StockDepotService);
  private readonly magasinService = inject(MagasinService);

  constructor() {
    this.criteria.set(new ProduitCriteria());
    this.criteria().status = Statut.ENABLE;

    this.search.set('');
    this.populate();
  }

  onOpenImportDialog(): void {
    const modalRef = this.modalService.open(ImportProduitModalComponent, {
      backdrop: 'static',
      size: 'lg',
      centered: true,
    });
    modalRef.componentInstance.type = this.typeImportation;
    modalRef.closed.subscribe(reason => {
      if (reason) {
        this.showResponse(reason);
        this.loadPage(0);
      }
    });
  }

  populate(): void {
    this.magasinService.fetchAllDepots().subscribe((res: HttpResponse<IMagasin[]>) => {
      this.depots.set(res.body || []);
    });
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
    if (this.selectedDepot() !== null) {
      const pageToLoad: number = page || this.page() || 1;
      let statut = 'ENABLE';
      if (this.criteria()) {
        if (this.criteria().status) {
          if (this.criteria().status === Statut.DISABLE) {
            statut = 'DISABLE';
          } else if (this.criteria().status === Statut.DELETED) {
            statut = 'DELETED';
          }
        }
      }

      this.stockDepotService
        .query({
          page: pageToLoad - 1,
          size: this.itemsPerPage,
          sort: this.sort(),
          search: this.search() || '',
          deconditionne: this.criteria().deconditionne,
          deconditionnable: this.criteria().deconditionnable,
          status: statut,
          magasinId: this.selectedDepot()?.id ?? undefined,
        })
        .subscribe({
          next: (res: HttpResponse<IProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
          error: () => this.onError(),
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
    this.search.set(event.target.value);
    this.loadPage(0);
  }

  protected onSelectDepot(): void {
    this.loadPage(0);
  }

  private showResponse(responsedto: IResponseDto): void {
    showCommonModal(this.modalService, ImportProduitReponseModalComponent, { responsedto }, () => {}, 'lg');
  }

  private onError(): void {
    this.ngbPaginationPage.set(this.page() ?? 1);
  }

  private onSuccess(data: IProduit[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')));
    this.page.set(page);

    this.produits.set(data || []);
    this.ngbPaginationPage.set(this.page());
  }

  private handleNavigation(): void {
    combineLatest(this.activatedRoute.data, this.activatedRoute.queryParamMap, (data: Data, params: ParamMap) => {
      const page = params.get('page');
      const pageNumber = page !== null ? +page : 1;

      if (pageNumber !== this.page()) {
        this.loadPage(pageNumber, true);
      }
    }).subscribe();
  }
}
