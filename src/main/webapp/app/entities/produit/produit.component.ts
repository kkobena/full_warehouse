import { Component, OnInit } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router } from '@angular/router';
import { combineLatest, Observable, Subscription } from 'rxjs';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { IProduit } from 'app/shared/model/produit.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { ProduitService } from './produit.service';
import { ProduitDeleteDialogComponent } from './produit-delete-dialog.component';
import { faCut, faFileUpload, faImage, faPlusCircle } from '@fortawesome/free-solid-svg-icons';
import { DetailFormDialogComponent } from './detail-form-dialog.component';
import { DeconditionDialogComponent } from './decondition.dialog.component';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';
import { IResponseDto } from '../../shared/util/response-dto';
import { ConfirmationService, MenuItem, MessageService, SelectItem } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { IProduitCriteria, ProduitCriteria } from '../../shared/model/produit-criteria.model';
import { RayonService } from '../rayon/rayon.service';
import { FamilleProduitService } from '../famille-produit/famille-produit.service';
import { SERVER_API_URL } from '../../app.constants';
import { Statut } from '../../shared/model/enumerations/statut.model';
import { TypeProduit } from '../../shared/model/enumerations/type-produit.model';
import { IFournisseurProduit } from '../../shared/model/fournisseur-produit.model';
import { ErrorService } from '../../shared/error.service';
import { FormProduitFournisseurComponent } from './form-produit-fournisseur/form-produit-fournisseur.component';
import { ConfigurationService } from '../../shared/configuration.service';
import { IConfiguration } from '../../shared/model/configuration.model';
import { Params } from '../../shared/model/enumerations/params.model';

@Component({
  selector: 'jhi-produit',
  styles: [
    `
      .p-datatable td {
        font-size: 0.6rem;
      }

      .table tr th {
        font-size: 0.9rem;
      }

      .btn-sm,
      .btn-group-sm > .btn {
        font-size: 1rem;
      }

      .secondColumn {
        color: blue;
      }

      .invoice-table {
        width: 100%;
        border-collapse: collapse;
      }

      .invoice-table tr {
        border-bottom: 1px solid #dee2e6;
      }

      .invoice-table td:first-child {
        text-align: left;
      }

      .invoice-table td {
        padding: 0.1rem;
      }

      .p-datatable .p-datatable-header {
        text-align: center;
      }

      table .number {
        text-align: right !important;
      }
    `,
  ],
  templateUrl: './produit.component.html',
  providers: [MessageService, DialogService, ConfirmationService, NgbActiveModal],
})
export class ProduitComponent implements OnInit {
  faFileUpload = faFileUpload;
  faImage = faImage;
  faCut = faCut;
  faPlusCircle = faPlusCircle;
  produits!: IProduit[];
  selectedCriteria = 0;
  selectedRayon = 0;
  selectedFamille = 0;
  filtesProduits: SelectItem[] = [];
  rayons: SelectItem[] = [];
  familles: SelectItem[] = [];
  eventSubscriber?: Subscription;
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  page!: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  search: string;
  package = TypeProduit.PACKAGE;
  detail = TypeProduit.DETAIL;
  fileDialog = false;
  jsonDialog = false;
  responseDialog = false;
  displayDialog = false;
  responsedto!: IResponseDto;
  isSaving = false;
  stockFileJsonDialog = false;
  splitbuttons: MenuItem[];
  criteria: IProduitCriteria;
  onErrorOccur = false;
  public resourceUrl = SERVER_API_URL;
  ref!: DynamicDialogRef;
  configuration?: IConfiguration | null;
  isMono = true;
  rowExpandMode = 'single';

  //            <td><span [class]="'product-badge status-' + product.inventoryStatus.toLowerCase()">{{product.inventoryStatus}}</span></td>
  constructor(
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal,
    protected confirmationService: ConfirmationService,
    private dialogService: DialogService,
    private messageService: MessageService,
    protected rayonService: RayonService,
    protected familleService: FamilleProduitService,
    protected errorService: ErrorService,
    protected configurationService: ConfigurationService
  ) {
    this.criteria = new ProduitCriteria();
    this.criteria.status = Statut.ENABLE;
    this.splitbuttons = [
      {
        label: 'Fiche à partir csv',
        icon: 'pi pi-file-excel',
        command: () => {
          this.fileDialog = true;
        },
      },
      {
        label: 'Fiche à partir json',
        icon: 'pi pi-file-o',
        command: () => {
          this.jsonDialog = true;
        },
      },
      {
        label: 'Mise à jour du stock à partir json',
        icon: 'pi pi-filter',
        command: () => {
          this.stockFileJsonDialog = true;
        },
      },
    ];
    this.filtesProduits = [
      { label: 'Produits actifs', value: 0 },
      { label: 'Produits désactifs', value: 1 },
      { label: 'Déconditionnables', value: 2 },
      { label: 'Déconditionnés', value: 3 },
      { label: 'Tous', value: 10 },
    ];
    this.familles.push({ label: 'TOUT', value: null });
    this.rayons.push({ label: 'TOUT', value: null });
    this.search = '';
    this.populate();
  }

  showFileDialog(): void {
    this.fileDialog = true;
  }

  async populate(): Promise<void> {
    const familleProduitsResponse = await this.familleService.queryPromise({ search: '' });
    familleProduitsResponse.forEach(e => {
      this.familles.push({ label: e.libelle, value: e.id });
    });
    const rayonsResponse = await this.rayonService.queryPromise({ search: '' });
    rayonsResponse.forEach(e => {
      this.rayons.push({ label: e.libelle, value: e.id });
    });
  }

  loadPage(page?: number, dontNavigate?: boolean): void {
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

    this.produitService
      .query({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        sort: this.sort(),
        search: this.search || '',
        storageId: this.criteria.storageId,
        rayonId: this.criteria.rayonId,
        deconditionne: this.criteria.deconditionne,
        deconditionnable: this.criteria.deconditionnable,
        status: statut,
        familleId: this.criteria.familleId,
      })
      .subscribe({
        next: (res: HttpResponse<IProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
        error: () => this.onError(),
      });
  }

  ngOnInit(): void {
    this.findConfigStock();
    this.handleNavigation();
    this.registerChangeInProduits();
  }

  trackId(index: number, item: IProduit): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  registerChangeInProduits(): void {
    this.loadPage();
  }

  delete(produit: IProduit): void {
    const modalRef = this.modalService.open(ProduitDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    modalRef.componentInstance.produit = produit;
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'libelle') {
      result.push('libelle');
    }
    return result;
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
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
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

  onUpload(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importcsv', file, file.name);
    this.uploadFileResponse(this.produitService.uploadFile(formData));
  }

  cancel(): void {
    this.displayDialog = false;
    this.fileDialog = false;
    this.jsonDialog = false;
    this.onErrorOccur = false;
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

  onUploadJson(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('importjson', file, file.name);
    this.uploadJsonDataResponse(this.produitService.uploadJsonData(formData));
  }

  filtreClik(): void {
    if (this.selectedCriteria === 2) {
      this.criteria.deconditionnable = true;
      this.criteria.deconditionne = undefined;
      this.criteria.status = Statut.ENABLE;
    } else if (this.selectedCriteria === 3) {
      this.criteria.deconditionnable = undefined;
      this.criteria.deconditionne = true;
      this.criteria.status = Statut.ENABLE;
    } else if (this.selectedCriteria === 1) {
      this.criteria.status = Statut.DISABLE;
      this.criteria.deconditionnable = undefined;
      this.criteria.deconditionne = undefined;
    } else if (this.selectedCriteria === 0) {
      this.criteria.status = Statut.ENABLE;
      this.criteria.deconditionnable = undefined;
      this.criteria.deconditionne = undefined;
    } else if (this.selectedCriteria === 10) {
      this.criteria = {};
    }
    this.loadPage(0);
  }

  onChangeDefaultProduitFournisseur(e: any, four: IFournisseurProduit): void {
    const isChecked = e.checked;
    if (four) {
      this.produitService.updateDefaultFournisseur(four.id!, isChecked).subscribe(
        () => {},
        error => this.onActionError(four, error)
      );
    }
  }

  onDeleteProduitFournisseur(four: IFournisseurProduit, produit: IProduit): void {
    if (four) {
      this.produitService.deleteFournisseur(four.id!).subscribe(
        () => {
          if (produit && produit.fournisseurProduits) {
            produit.fournisseurProduits = produit.fournisseurProduits.filter(e => e.id !== four.id);
          }
        },
        error => this.onCommonError(error)
      );
    }
  }

  addFournisseur(produit: IProduit): void {
    this.ref = this.dialogService.open(FormProduitFournisseurComponent, {
      data: {
        produit,
      },
      header: 'Ajouter un fournisseur au produit ' + produit.libelle,
      width: '40%',
    });
    this.ref.onClose.subscribe((resp: IFournisseurProduit) => {
      if (resp) {
        produit.fournisseurProduits?.push(resp);
      }
    });
  }

  editFournisseur(produit: IProduit, fournisseurProduit: IFournisseurProduit | null): void {
    this.ref = this.dialogService.open(FormProduitFournisseurComponent, {
      data: {
        produit,
        entity: fournisseurProduit,
      },
      header: 'Modification du produit ' + produit.libelle,
      width: '40%',
    });
    this.ref.onClose.subscribe((resp: IFournisseurProduit) => {
      if (resp) {
        const newFours = produit.fournisseurProduits?.filter(e => e.id !== resp.id);
        if (newFours) {
          newFours.push(resp);
          produit.fournisseurProduits = newFours;
        }
      }
    });
  }

  confirmDeleteProduitFournisseur(four: IFournisseurProduit, produit: IProduit): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous detacher ce fournisseur de ce produit ?',
      header: 'Retrait de fournisseur ',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.onDeleteProduitFournisseur(four, produit);
      },
      key: 'deleteItem',
    });
  }

  findConfigStock(): void {
    const stockParam = this.configurationService.getParamByKey(Params.APP_GESTION_STOCK);
    if (stockParam) {
      this.isMono = Number(stockParam.value) === 0;
    }
  }

  protected uploadFileResponse(result: Observable<HttpResponse<IResponseDto>>): void {
    result.subscribe(
      (res: HttpResponse<IResponseDto>) => this.onPocesCsvSuccess(res.body),
      () => this.onSaveError()
    );
  }

  protected onPocesCsvSuccess(responseDto: IResponseDto | null): void {
    if (responseDto) this.responsedto = responseDto;
    this.responseDialog = true;
    this.fileDialog = false;
    this.loadPage(0);
  }

  protected onSaveError(): void {
    this.isSaving = false;
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur',
      detail: 'Enregistrement a échoué',
    });
  }

  protected onActionError(el: IFournisseurProduit, error: any): void {
    if (error.error) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(
        translatedErrorMessage => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: translatedErrorMessage,
          });
        },
        () => {
          this.onErrorOccur = true;
        }
      );
    }
    el.principal = false;
  }

  protected onCommonError(error: any): void {
    if (error.error) {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(
        translatedErrorMessage => {
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: translatedErrorMessage,
          });
        },
        () => {
          this.messageService.add({ severity: 'error', summary: 'Erreur', detail: error.title });
        }
      );
    }
  }

  protected uploadJsonDataResponse(result: Observable<HttpResponse<void>>): void {
    result.subscribe(
      () => this.onPocesJsonSuccess(),
      () => this.onSaveError()
    );
  }

  protected onPocesJsonSuccess(): void {
    this.jsonDialog = false;
    this.responseDialog = true;
    const interval = setInterval(() => {
      this.produitService.findImortation().subscribe(
        res => {
          if (res.body) {
            this.responsedto = res.body;
            if (this.responsedto.completed) {
              setTimeout(() => {}, 5000);
              clearInterval(interval);
            }
          }
        },
        () => {
          setTimeout(() => {}, 5000);
          clearInterval(interval);
        }
      );
    }, 10000);
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

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
