import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ICommande } from 'app/shared/model/commande.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { CommandeService } from './commande.service';
import { IOrderLine } from 'app/shared/model/order-line.model';
import { ProduitService } from '../produit/produit.service';
import { ConfirmationService } from 'primeng/api';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';
import { ErrorService } from '../../shared/error.service';
import { NgxSpinnerService } from 'ngx-spinner';
import { IResponseCommande } from '../../shared/model/response-commande.model';
import { CommandeEnCoursResponseDialogComponent } from './commande-en-cours-response-dialog.component';
import { CommandeImportResponseDialogComponent } from './commande-import-response-dialog.component';
import { ICommandeResponse } from '../../shared/model/commande-response.model';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ImportationNewCommandeComponent } from './importation-new-commande.component';
import { IDelivery } from '../../shared/model/delevery.model';
import { CommandeEnCoursComponent } from './commande-en-cours/commande-en-cours.component';
import { CommandePassesComponent } from './commande-passes/commande-passes.component';
import { CommandeRecusComponent } from './commande-recus/commande-recus.component';

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
  providers: [ConfirmationService, DialogService],
})
export class CommandeComponent implements OnInit {
  commandes: ICommande[] = [];
  commandeSelected?: ICommande;
  selectedRowIndex?: number;
  selectedRowOrderLines?: IOrderLine[] = [];
  eventSubscriber?: Subscription;
  totalItems = 0;
  itemsPerPage = ITEMS_PER_PAGE;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  tooltipPosition = 'left';
  index = 0;
  selectedFilter = 'REQUESTED';
  loading!: boolean;
  loadingSelectedFilter = false;
  page = 0;
  selectedtypeSuggession = 'ALL';
  typeSuggessions: any[] = [];
  fileDialog = false;
  ref!: DynamicDialogRef;
  protected searchCommande = '';
  protected active = 'REQUESTED';
  protected search = '';
  protected selectionLength: number = 0;
  @ViewChild(CommandeEnCoursComponent)
  private commandeEnCoursComponent: CommandeEnCoursComponent;
  @ViewChild(CommandePassesComponent)
  private commandePasses: CommandePassesComponent;
  @ViewChild(CommandeRecusComponent)
  private commandeRecues: CommandeRecusComponent;

  constructor(
    protected commandeService: CommandeService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal,
    private errorService: ErrorService,
    protected produitService: ProduitService,
    private spinner: NgxSpinnerService,
    private confirmationService: ConfirmationService,
    private dialogService: DialogService
  ) {}

  ngOnInit(): void {}

  onSearch(): void {
    switch (this.active) {
      case 'REQUESTED':
        this.commandeEnCoursComponent.onSearch();
        break;
      case 'PASSED':
        this.commandePasses.onSearch();
        break;
      case 'RECEIVED':
        this.commandeRecues.onSearch();

        break;
    }
  }

  fusionner(): void {
    if (this.active === 'REQUESTED') {
      this.commandeEnCoursComponent.fusionner();
    }
  }

  cancel(): void {
    this.fileDialog = false;
  }

  onShowFileDialog(commande: ICommande): void {
    this.fileDialog = true;
    this.commandeSelected = commande;
  }

  onImporterReponseCommande(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];

    formData.append('commande', file, file.name);
    this.spinner.show('gestion-commande-spinner');
    this.commandeService.importerReponseCommande(this.commandeSelected?.id!, formData).subscribe({
      next: res => {
        this.cancel();
        this.spinner.hide('gestion-commande-spinner');

        this.commandeService.fetchOrderLinesByCommandeId(this.commandeSelected?.id!).subscribe(ress => {
          this.commandeSelected!.orderLines = ress.body!;
        });

        this.openImporterReponseCommandeDialog(res.body!);
      },
      error: error => {
        this.spinner.hide('gestion-commande-spinner');
        this.onCommonError(error);
      },
    });
  }

  openImportResponseDialogComponent(responseCommande: ICommandeResponse): void {
    const modalRef = this.modalService.open(CommandeImportResponseDialogComponent, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
    });
    modalRef.componentInstance.responseCommande = responseCommande;
  }

  openImporterReponseCommandeDialog(responseCommande: IResponseCommande): void {
    const modalRef = this.modalService.open(CommandeEnCoursResponseDialogComponent, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
    });
    modalRef.componentInstance.responseCommande = responseCommande;
    modalRef.componentInstance.commande = this.commandeSelected;
  }

  onShowNewCommandeDialog(): void {
    this.ref = this.dialogService.open(ImportationNewCommandeComponent, {
      header: 'IMPORTATION DE NOUVELLE COMMANDE',
      width: '40%',
    });
    this.ref.onClose.subscribe((resp: ICommandeResponse) => {
      if (resp) {
        this.active = 'REQUESTED';
        this.onSearch();
        this.openImportResponseDialogComponent(resp);
      }
    });
  }

  confirmDeleteSelectedRows(): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer ces commandes  ?',
      header: ' SUPPRESSION',
      icon: 'pi pi-info-circle',
      accept: () => {
        if (this.active === 'REQUESTED') {
          this.commandeEnCoursComponent.removeAll();
        }
      },
      key: 'deleteCommande',
    });
  }

  confirmRollback(): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous LES retourner dans commande en cours ?',
      header: ' SUPPRESSION',
      icon: 'pi pi-info-circle',
      accept: () => {
        if (this.active === 'PASSED') {
          this.commandePasses.rollbackAll();
        }
      },
      key: 'deleteCommande',
    });
  }

  gotoEntreeStockComponent(delivery: IDelivery): void {
    this.router.navigate(['/commande', delivery.id, 'stock-entry']);
  }

  test(): void {
    this.commandeService.test({ model: 'TEDIS' }).subscribe(() => {
      console.error('===============================');
    });
  }

  protected onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe({
        next: translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        error: () => this.openInfoDialog(error.error.title, 'alert alert-danger'),
      });
    }
  }

  protected openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  protected updateSelectionLength(lgth: number): void {
    this.selectionLength = lgth;
  }
}
