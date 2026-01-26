import { Component, inject, OnInit, viewChild } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { combineLatest } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { IProduit } from 'app/shared/model/produit.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { ProduitService } from './produit.service';
import { DetailFormDialogComponent } from './detail-form-dialog.component';
import { DeconditionDialogComponent } from './decondition.dialog.component';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';
import { IResponseDto } from '../../shared/util/response-dto';
import { MenuItem, SelectItem } from 'primeng/api';
import { IProduitCriteria, ProduitCriteria } from '../../shared/model/produit-criteria.model';
import { RayonService } from '../rayon/rayon.service';
import { FamilleProduitService } from '../famille-produit/famille-produit.service';
import { Statut } from '../../shared/model/enumerations/statut.model';
import { TypeProduit } from '../../shared/model/enumerations/type-produit.model';
import { IFournisseurProduit } from '../../shared/model/fournisseur-produit.model';
import { ErrorService } from '../../shared/error.service';
import { FormProduitFournisseurComponent } from './form-produit-fournisseur/form-produit-fournisseur.component';
import { ConfigurationService } from '../../shared/configuration.service';
import { IConfiguration } from '../../shared/model/configuration.model';
import { Params } from '../../shared/model/enumerations/params.model';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ToastModule } from 'primeng/toast';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { FileUploadModule } from 'primeng/fileupload';
import { FormsModule } from '@angular/forms';
import { ToolbarModule } from 'primeng/toolbar';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ImportProduitModalComponent } from './import-produit-modal/import-produit-modal.component';
import { SelectModule } from 'primeng/select';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { EtaProduitComponent } from '../../shared/eta-produit/eta-produit.component';
import { IFamilleProduit } from '../../shared/model/famille-produit.model';
import { IRayon } from '../../shared/model/rayon.model';
import { ButtonGroup } from 'primeng/buttongroup';
import { ListPrixReferenceComponent } from '../prix-reference/list-prix-reference/list-prix-reference.component';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { ImportProduitReponseModalComponent } from './import-produit-reponse-modal/import-produit-reponse-modal.component';
import { CardModule } from 'primeng/card';
import { FloatLabel } from 'primeng/floatlabel';
import { FormTransfertStockComponent } from './form-transfert-stock/form-transfert-stock.component';
import { FormStockProduitComponent } from './form-stock-produit/form-stock-produit.component';
import { IStockProduit } from '../../shared/model/stock-produit.model';

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'jhi-produit',
  templateUrl: './produit.component.html',
  styleUrl: './produit.component.scss',
  imports: [
    WarehouseCommonModule,
    FormsModule,
    SplitButtonModule,
    TableModule,
    ToolbarModule,
    FileUploadModule,
    RouterModule,
    ToastModule,
    ButtonModule,
    TooltipModule,
    InputTextModule,
    SelectModule,
    IconField,
    InputIcon,
    ToggleSwitch,
    EtaProduitComponent,
    ButtonGroup,
    ToastAlertComponent,
    ConfirmDialogComponent,
    CardModule,
    FloatLabel,
  ],
})
export class ProduitComponent implements OnInit {
  protected selectedFamille: number = null;
  protected produits!: IProduit[];
  protected selectedCriteria = 0;
  protected selectedRayon = 0;
  protected filtesProduits: SelectItem[] = [];
  protected rayons: IRayon[] = [];
  protected familles: IFamilleProduit[] = [];
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
  protected splitbuttons: MenuItem[];
  protected criteria: IProduitCriteria;
  protected configuration?: IConfiguration | null;
  protected isMono = true;
  protected rowExpandMode: ExpandMode = 'single';
  protected typeImportation: string | null = null;
  private readonly produitService = inject(ProduitService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly rayonService = inject(RayonService);
  private readonly familleService = inject(FamilleProduitService);
  private readonly errorService = inject(ErrorService);
  private readonly configurationService = inject(ConfigurationService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');

  constructor() {
    this.criteria = new ProduitCriteria();
    this.criteria.status = Statut.ENABLE;
    this.splitbuttons = [
      {
        label: 'Nouvelle installation',
        icon: 'pi pi-file-excel',
        command: () => {
          this.typeImportation = 'NOUVELLE_INSTALLATION';
          this.onOpenImportDialog();
        },
      },
      {
        label: 'Basculement',
        icon: 'pi pi-filter',
        command: () => {
          this.typeImportation = 'BASCULEMENT';
          this.onOpenImportDialog();
        },
      },
      {
        label: 'Basculement de perstige',
        icon: 'pi pi-file-o',
        command: () => {
          this.typeImportation = 'BASCULEMENT_PRESTIGE';
          this.onOpenImportDialog();
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

    this.search = '';
    this.populate();
  }

  protected onOpenImportDialog(): void {
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

  protected populate(): void {
    this.familleService.query({ search: '' }).subscribe({
      next: res => {
        this.familles = res.body;
      },
    });

    this.rayonService
      .query({
        search: '',
        page: 0,
        size: 9999,
      })
      .subscribe({
        next: rayonsResponse => {
          this.rayons = rayonsResponse.body;
        },
      });
  }

  protected loadPage(page?: number, dontNavigate?: boolean): void {
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

  protected registerChangeInProduits(): void {
    this.loadPage();
  }

  protected confirmDelete(produit: IProduit): void {
    this.confimDialog().onConfirm(() => this.delete(produit), 'Suppression', 'Voulez-vous supprimer ce produit ?');
  }

  protected sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'libelle') {
      result.push('libelle');
    }
    return result;
  }

  protected addDetail(produit: IProduit): void {
    showCommonModal(
      this.modalService,
      DetailFormDialogComponent,
      {
        produit,
      },
      resp => {
        this.registerChangeInProduits();
      },
      'lg',
    );
  }

  protected editDetail(produit: IProduit): void {
    showCommonModal(
      this.modalService,
      DetailFormDialogComponent,
      {
        entity: produit,
        produit: produit.parent,
      },
      resp => {
        this.registerChangeInProduits();
      },
      'lg',
    );
  }

  protected openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  protected decondition(produit: IProduit): void {
    if (produit.produits.length === 0) {
      this.openInfoDialog("Le produit n'a pas de détail. Vous devriez en ajouter d'abord", 'alert alert-info');
    } else {
      showCommonModal(
        this.modalService,
        DeconditionDialogComponent,
        {
          produit,
        },
        resp => {
          this.registerChangeInProduits();
        },
        'lg',
      );
    }
  }

  protected onSearch(event: any): void {
    this.search = event.target.value;
    this.loadPage(0);
  }

  protected filtreRayon(event: any): void {
    this.criteria.rayonId = event.value;
    this.loadPage(0);
  }

  protected filtreFamilleProduit(event: any): void {
    this.criteria.familleId = event.value;
    this.loadPage(0);
  }

  protected filtreClik(): void {
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

  protected onChangeDefaultProduitFournisseur(e: any, row: IProduit, four: IFournisseurProduit): void {
    const isChecked = e.checked;
    if (four) {
      this.produitService.updateDefaultFournisseur(four.id, row.id, isChecked).subscribe({
        next: () => {
          if (isChecked) {
            row.fournisseurProduit = four;
          } else {
            row.fournisseurProduit = null;
          }
        },
        error: error => {
          row.fournisseurProduit = four;
          this.onActionError(error);
        },
      });
    }
  }

  protected getToolTip(four: IFournisseurProduit): string {
    return four.principal ? "L'Actuel fournisseur principal" : 'Changer en fournisseur principal';
  }

  protected onDeleteProduitFournisseur(four: IFournisseurProduit, produit: IProduit): void {
    if (four) {
      this.produitService.deleteFournisseur(four.id).subscribe({
        next() {
          if (produit && produit.fournisseurProduits) {
            produit.fournisseurProduits = produit.fournisseurProduits.filter(e => e.id !== four.id);
          }
        },
        error: error => this.onCommonError(error),
      });
    }
  }

  protected addFournisseur(produit: IProduit): void {
    showCommonModal(
      this.modalService,
      FormProduitFournisseurComponent,
      {
        produit,
        header: 'Ajouter un fournisseur au produit ',
      },
      resp => {
        produit.fournisseurProduits.push(resp);
        if (resp.principal) {
          produit.fournisseurProduit = resp;
        }
      },
      'lg',
    );
  }

  protected onTransfererStock(produit: IProduit, stockProduitSrc: IStockProduit): void {
    showCommonModal(
      this.modalService,
      FormTransfertStockComponent,
      {
        produit,
        stockProduitSrc,
      },
      (resp: IStockProduit[]) => {
        produit.stockProduits = resp;
      },
      'xl',
    );
  }

  protected openFormStockProduit(produit: IProduit): void {
    showCommonModal(
      this.modalService,
      FormStockProduitComponent,
      {
        produit,
      },
      (resp: IStockProduit[]) => {
        if (resp && resp.length > 0) {
          produit.stockProduits = [...(produit.stockProduits || []), ...resp];
        }
      },
      'lg',
    );
  }

  protected onEditStock(produit: IProduit, stockProduit: IStockProduit): void {
    showCommonModal(
      this.modalService,
      FormStockProduitComponent,
      {
        produit,
        stockProduit,
      },
      (resp: IStockProduit[]) => {
        if (resp && resp.length > 0) {
          const index = produit.stockProduits?.findIndex(sp => sp.id === resp[0].id);
          if (index !== undefined && index >= 0 && produit.stockProduits) {
            produit.stockProduits[index] = resp[0];
          }
        }
      },
      'lg',
    );
  }

  protected editFournisseur(produit: IProduit, fournisseurProduit: IFournisseurProduit | null): void {
    showCommonModal(
      this.modalService,
      FormProduitFournisseurComponent,
      {
        entity: fournisseurProduit,
        produit,
        header: 'Modification du produit ',
      },
      resp => {
        const newFours = produit.fournisseurProduits.filter(e => e.id !== resp.id);
        if (newFours) {
          newFours.push(resp);
          produit.fournisseurProduits = newFours;
          if (resp.principal) {
            produit.fournisseurProduit = resp;
          }
        }
      },
      'lg',
    );
  }

  protected confirmDeleteProduitFournisseur(four: IFournisseurProduit, produit: IProduit): void {
    this.confimDialog().onConfirm(
      () => this.onDeleteProduitFournisseur(four, produit),
      'Retrait de fournisseur',
      ' Voullez-vous detacher ce fournisseur de ce produit ?',
    );
  }

  protected findConfigStock(): void {
    const stockParam = this.configurationService.getParamByKey(Params.APP_GESTION_STOCK);
    if (stockParam) {
      this.isMono = Number(stockParam.value) === 0;
    }
  }

  protected addPrixReference(produit: IProduit): void {
    const modalRef = this.modalService.open(ListPrixReferenceComponent, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.isFromProduit = true;
    modalRef.componentInstance.produit = produit;
    modalRef.result.then(
      () => {
        this.loadPage();
      },
      () => {
        this.loadPage();
      },
    );
  }

  private showResponse(responsedto: IResponseDto): void {
    showCommonModal(this.modalService, ImportProduitReponseModalComponent, { responsedto }, () => {}, 'lg');
  }

  private delete(produit: IProduit): void {
    this.produitService.delete(produit.id).subscribe(() => {
      this.loadPage();
    });
  }

  private onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  private onActionError(error: HttpErrorResponse): void {
    this.onCommonError(error);
  }

  private onCommonError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private onSuccess(data: IProduit[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
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

  private handleNavigation(): void {
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
}
