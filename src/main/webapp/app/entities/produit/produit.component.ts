import { Component, inject, OnInit, viewChild } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { combineLatest, Observable } from 'rxjs';
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
import { InputSwitchModule } from 'primeng/inputswitch';
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
import { DatePeremptionFormComponent } from './date-peremption-form/date-peremption-form.component';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { finalize } from 'rxjs/operators';
import { FileUploadDialogComponent } from '../groupe-tiers-payant/file-upload-dialog/file-upload-dialog.component';
import {
  ImportProduitReponseModalComponent
} from './import-produit-reponse-modal/import-produit-reponse-modal.component';
import { Panel } from 'primeng/panel';
import { CardModule } from 'primeng/card';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'jhi-produit',
  styles: [
    `
      .expanded-content {
        background-color: #f8f9fa;
      }

      :host ::ng-deep .p-card .info-title {
        background-color: var(--p-sky-400);

      }

      :host ::ng-deep .p-card .warn-title {
        background-color: var(--p-orange-400);

      }

      :host ::ng-deep .p-card .secondary-title {
        background-color: var(--p-purple-400);

      }

      :host ::ng-deep .p-card .primary-title {
        background-color: var(--p-primary-color);


      }

      :host ::ng-deep .p-card .p-card-title .card-title {
        color: #ffffff;
        padding: 0.5rem;
        margin: -1.25rem -1.25rem 1.25rem -1.25rem;
        border-top-left-radius: var(--border-radius);
        border-top-right-radius: var(--border-radius);
      }

      .card-title {
        font-size: 1.2rem;
        font-weight: 600;
        display: flex;
        align-items: center;
      }

      .field {
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        align-items: center;
        padding: 0.5rem 0;
        border-bottom: 1px solid #dfe7ef;
      }

      .field:last-child {
        border-bottom: none;
      }

      .field label {
        font-weight: 600;
        color: #6c757d;
      }

      .field span {
        color: #495057;
        text-align: right;
        font-weight: 600;
      }


    `
  ],
  templateUrl: './produit.component.html',
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
    InputSwitchModule,
    InputTextModule,
    SelectModule,
    IconField,
    InputIcon,
    ToggleSwitch,
    EtaProduitComponent,
    ButtonGroup,
    ToastAlertComponent,
    ConfirmDialogComponent,
    Panel,
    CardModule,
    SpinnerComponent
  ]
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
  protected fileDialog = false;
  protected jsonDialog = false;
  protected displayDialog = false;
  protected responsedto!: IResponseDto;
  protected isSaving = false;
  protected splitbuttons: MenuItem[];
  protected criteria: IProduitCriteria;
  protected onErrorOccur = false;
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
   private readonly spinner = viewChild.required<SpinnerComponent>('spinner');

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
        }
      },
      {
        label: 'Basculement',
        icon: 'pi pi-filter',
        command: () => {
          this.typeImportation = 'BASCULEMENT';
          this.onOpenImportDialog();
        }
      },
      {
        label: 'Basculement de perstige',
        icon: 'pi pi-file-o',
        command: () => {
          this.typeImportation = 'BASCULEMENT_PRESTIGE';
          this.onOpenImportDialog();
        }
      }
    ];
    this.filtesProduits = [
      { label: 'Produits actifs', value: 0 },
      { label: 'Produits désactifs', value: 1 },
      { label: 'Déconditionnables', value: 2 },
      { label: 'Déconditionnés', value: 3 },
      { label: 'Tous', value: 10 }
    ];

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

  private showResponse(responsedto: IResponseDto): void {
    showCommonModal(
      this.modalService,
      ImportProduitReponseModalComponent,
      { responsedto },
      () => {
      },
      'lg'
    );
  }

  populate(): void {
    this.familleService.query({ search: '' }).subscribe({
      next: res => {
        this.familles = res.body;
      }
    });

    this.rayonService
      .query({
        search: '',
        page: 0,
        size: 9999
      })
      .subscribe({
        next: rayonsResponse => {
          this.rayons = rayonsResponse.body;
        }
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
        familleId: this.criteria.familleId
      })
      .subscribe({
        next: (res: HttpResponse<IProduit[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
        error: () => this.onError()
      });
  }

  ngOnInit(): void {
    this.findConfigStock();
    this.handleNavigation();
    this.registerChangeInProduits();
  }

  registerChangeInProduits(): void {
    this.loadPage();
  }

  confirmDelete(produit: IProduit): void {
    this.confimDialog().onConfirm(() => this.delete(produit), 'Suppression', 'Voulez-vous supprimer ce produit ?');
  }

  private delete(produit: IProduit): void {
    this.produitService.delete(produit.id).subscribe(() => {
      this.loadPage();
    });
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
      centered: true
    });
    modalRef.componentInstance.produit = produit;
  }

  editDetail(produit: IProduit): void {
    const modalRef = this.modalService.open(DetailFormDialogComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true
    });
    modalRef.componentInstance.entity = produit;
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  decondition(produit: IProduit): void {
    if (produit.produits.length === 0) {
      this.openInfoDialog('Le produit n\'a pas de détail. Vous devriez en ajouter d\'abord', 'alert alert-info');
    } else {
      const modalRef = this.modalService.open(DeconditionDialogComponent, {
        size: '60%',
        backdrop: 'static',
        centered: true
      });
      modalRef.componentInstance.produit = produit;
    }
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

  protected onImportJsonFile(): void {
    showCommonModal(
      this.modalService,
      FileUploadDialogComponent,
      { accept: '.json' },
      (result) => {
        this.spinner().show();
        this.uploadJsonDataResponse(this.produitService.uploadJsonData(result));
      },
      'lg'
    );
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

  protected onChangeDefaultProduitFournisseur(e: any, four: IFournisseurProduit): void {
    const isChecked = e.checked;
    if (four) {
      this.produitService.updateDefaultFournisseur(four.id, isChecked).subscribe({
        error: error => this.onActionError(four, error)
      });
    }
  }

  protected getToolTip(four: IFournisseurProduit): string {
    return four.principal ? 'L\'Actuel fournisseur principal' : 'Changer en fournisseur principal';
  }

  protected onDeleteProduitFournisseur(four: IFournisseurProduit, produit: IProduit): void {
    if (four) {
      this.produitService.deleteFournisseur(four.id).subscribe({
        next() {
          if (produit && produit.fournisseurProduits) {
            produit.fournisseurProduits = produit.fournisseurProduits.filter(e => e.id !== four.id);
          }
        },
        error: error => this.onCommonError(error)
      });
    }
  }

  protected addFournisseur(produit: IProduit): void {
    showCommonModal(
      this.modalService,
      FormProduitFournisseurComponent,
      {
        produit,
        header: 'Ajouter un fournisseur au produit ' + produit.libelle
      },
      (resp) => {
        produit.fournisseurProduits.push(resp);
      },
      'lg'
    );
  }

  protected editFournisseur(produit: IProduit, fournisseurProduit: IFournisseurProduit | null): void {

    showCommonModal(
      this.modalService,
      FormProduitFournisseurComponent,
      {
        entity: fournisseurProduit,
        produit,
        header: 'Modification du produit ' + produit.libelle
      },
      (resp) => {
        const newFours = produit.fournisseurProduits.filter(e => e.id !== resp.id);
        if (newFours) {
          newFours.push(resp);
          produit.fournisseurProduits = newFours;
        }
      },
      'lg'
    );
  }

  protected confirmDeleteProduitFournisseur(four: IFournisseurProduit, produit: IProduit): void {
    console.log(four);
    this.confimDialog().onConfirm(() => this.onDeleteProduitFournisseur(four, produit), 'Retrait de fournisseur', ' Voullez-vous detacher ce fournisseur de ce produit ?');
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
      centered: true
    });
    modalRef.componentInstance.isFromProduit = true;
    modalRef.componentInstance.produit = produit;
    modalRef.result.then(
      () => {
        this.loadPage();
      },
      () => {
        this.loadPage();
      }
    );
  }

  protected addPeremptionDate(produit: IProduit): void {
    const modalRef = this.modalService.open(DatePeremptionFormComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true
    });
    modalRef.componentInstance.produit = produit;
    modalRef.result.then(
      () => {
        this.loadPage();
      },
      () => {
        this.loadPage();
      }
    );
  }

  private onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  private onSaveError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private onActionError(el: IFournisseurProduit, error: HttpErrorResponse): void {
    el.principal = false;
    this.onCommonError(error);
  }

  private onCommonError(error: HttpErrorResponse): void {
    this.alert().showError(this.errorService.getErrorMessage(error));
  }

  private uploadJsonDataResponse(result: Observable<HttpResponse<void>>): void {
    result.pipe(finalize(() => {
      this.spinner().hide();
      this.isSaving = false;
    })).subscribe({
      next: () => this.onPocesJsonSuccess(),
      error: (err) => this.onSaveError(err)
    });
  }

  private onPocesJsonSuccess(): void {

    const interval = setInterval(() => {
      this.produitService.findImortation().subscribe({
        next: res => {
          if (res.body) {
            this.responsedto = res.body;
            if (this.responsedto.completed) {
              setTimeout(() => {
              }, 5000);
              clearInterval(interval);
            }
          }
        },
        error() {
          setTimeout(() => {
          }, 5000);
          clearInterval(interval);
        }
      });
    }, 10000);
  }

  private onSuccess(data: IProduit[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/produit'], {
        queryParams: {
          page: this.page,
          size: this.itemsPerPage,
          sort: this.predicate + ',' + (this.ascending ? 'asc' : 'desc')
        }
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
