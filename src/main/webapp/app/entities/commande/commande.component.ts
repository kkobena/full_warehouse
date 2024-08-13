import { Component, inject, OnInit, viewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NgbModal, NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';
import { ICommande } from 'app/shared/model/commande.model';
import { CommandeService } from './commande.service';
import { ProduitService } from '../produit/produit.service';
import { ConfirmationService } from 'primeng/api';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';
import { ErrorService } from '../../shared/error.service';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { IResponseCommande } from '../../shared/model/response-commande.model';
import { CommandeEnCoursResponseDialogComponent } from './commande-en-cours-response-dialog.component';
import { CommandeImportResponseDialogComponent } from './commande-import-response-dialog.component';
import { ICommandeResponse } from '../../shared/model/commande-response.model';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ImportationNewCommandeComponent } from './importation-new-commande.component';
import { IDelivery } from '../../shared/model/delevery.model';
import { CommandeEnCoursComponent } from './commande-en-cours/commande-en-cours.component';
import { CommandePassesComponent } from './commande-passes/commande-passes.component';
import { CommandeRecusComponent } from './commande-recus/commande-recus.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { FileUploadModule } from 'primeng/fileupload';
import { CardModule } from 'primeng/card';
import { FormsModule } from '@angular/forms';
import { ToolbarModule } from 'primeng/toolbar';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { CommandCommonService } from './command-common.service';

@Component({
  standalone: true,
  selector: 'jhi-commande',
  templateUrl: './commande.component.html',
  providers: [ConfirmationService, DialogService],
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    TableModule,
    NgxSpinnerModule,
    RouterModule,
    RippleModule,
    DynamicDialogModule,
    TooltipModule,
    DialogModule,
    ConfirmDialogModule,
    FileUploadModule,
    CardModule,
    FormsModule,
    ToolbarModule,
    InputTextModule,
    CommandeEnCoursComponent,
    CommandePassesComponent,
    CommandeRecusComponent,
    PanelModule,
  ],
  styles: [
    `
      .commande-gestion .table tr:hover {
        cursor: pointer;
      }

      table .active {
        background-color: #95caf9 !important;
      }
    `,
  ],
})
export class CommandeComponent implements OnInit {
  commandes: ICommande[] = [];
  commandeSelected?: ICommande;
  selectedRowIndex?: number;
  tooltipPosition = 'left';
  selectedFilter = 'REQUESTED';
  loading!: boolean;
  selectedtypeSuggession = 'ALL';
  typeSuggessions: any[] = [];
  fileDialog = false;
  ref!: DynamicDialogRef;
  commandCommonService = inject(CommandCommonService);
  commandeEnCoursComponent = viewChild(CommandeEnCoursComponent);
  commandePasses = viewChild(CommandePassesComponent);
  commandeRecues = viewChild(CommandeRecusComponent);
  protected searchCommande = '';
  protected active = 'REQUESTED';
  protected search = '';
  protected selectionLength: number = 0;

  constructor(
    protected commandeService: CommandeService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal,
    private errorService: ErrorService,
    protected produitService: ProduitService,
    private spinner: NgxSpinnerService,
    private confirmationService: ConfirmationService,
    private dialogService: DialogService,
  ) {}

  ngOnInit(): void {
    this.active = this.commandCommonService.commandPreviousActiveNav();
  }

  onNavChange(evt: NgbNavChangeEvent): void {
    this.active = evt.nextId;
    this.commandCommonService.updateCommandPreviousActiveNav(this.active);
  }

  onSearch(): void {
    switch (this.active) {
      case 'REQUESTED':
        this.commandeEnCoursComponent().onSearch();
        break;
      case 'PASSED':
        this.commandePasses().onSearch();
        break;
      case 'RECEIVED':
        this.commandeRecues().onSearch();
        break;
    }
  }

  onCreatNewCommande(): void {
    this.commandCommonService.updateCommand(null);
    this.commandCommonService.updateCommandPreviousActiveNav(this.active);
    this.router.navigate(['/commande/new']);
  }

  fusionner(): void {
    if (this.active === 'REQUESTED') {
      this.commandeEnCoursComponent().fusionner();
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
    this.commandeService.importerReponseCommande(this.commandeSelected.id, formData).subscribe({
      next: res => {
        this.cancel();
        this.spinner.hide('gestion-commande-spinner');

        this.commandeService.fetchOrderLinesByCommandeId(this.commandeSelected.id).subscribe(ress => {
          this.commandeSelected.orderLines = ress.body!;
        });

        this.openImporterReponseCommandeDialog(res.body);
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
          this.commandeEnCoursComponent().removeAll();
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
          this.commandePasses().rollbackAll();
        }
      },
      key: 'deleteCommande',
    });
  }

  gotoEntreeStockComponent(delivery: IDelivery): void {
    this.commandCommonService.updateCommandPreviousActiveNav(this.active);
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
        error: () => this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger'),
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
