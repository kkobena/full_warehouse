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
import { CommandeEnCoursComponent } from './commande-en-cours/commande-en-cours.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { FileUploadModule } from 'primeng/fileupload';
import { CardModule } from 'primeng/card';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ToolbarModule } from 'primeng/toolbar';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { CommandCommonService } from './command-common.service';
import { acceptButtonProps, rejectButtonProps } from '../../shared/util/modal-button-props';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { BonEnCoursComponent } from './delevery/bon-en-cours/bon-en-cours.component';
import { ListBonsComponent } from './delevery/list-bons/list-bons.component';
import { Divider } from 'primeng/divider';
import { SuggestionComponent } from './suggestion/suggestion.component';
import { FournisseurService } from '../fournisseur/fournisseur.service';
import { HttpResponse } from '@angular/common/http';
import { IFournisseur } from '../../shared/model/fournisseur.model';
import { Select } from 'primeng/select';

@Component({
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
    PanelModule,
    IconField,
    InputIcon,
    BonEnCoursComponent,
    ListBonsComponent,
    Divider,
    SuggestionComponent,
    ReactiveFormsModule,
    Select,
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
  protected commandeService = inject(CommandeService);
  protected activatedRoute = inject(ActivatedRoute);
  protected router = inject(Router);
  protected modalService = inject(NgbModal);
  private errorService = inject(ErrorService);
  protected produitService = inject(ProduitService);
  private spinner = inject(NgxSpinnerService);
  private confirmationService = inject(ConfirmationService);
  private dialogService = inject(DialogService);

  commandes: ICommande[] = [];
  commandeSelected?: ICommande;
  selectedFilter = 'REQUESTED';
  loading!: boolean;
  protected selectedtypeSuggession: string = null;
  fileDialog = false;
  ref!: DynamicDialogRef;
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly commandeEnCoursComponent = viewChild(CommandeEnCoursComponent);
  private readonly suggestion = viewChild(SuggestionComponent);
  private readonly enCoursComponent = viewChild(BonEnCoursComponent);
  private readonly listBonsComponent = viewChild(ListBonsComponent);
  protected fournisseurService = inject(FournisseurService);
  protected searchCommande = '';
  protected active = 'REQUESTED';
  protected search = '';
  protected selectionLength = 0;
  protected readonly display = false;
  protected selectFournisseurId: number | null = null;
  protected fournisseurs: IFournisseur[] = [];
  protected typeSuggessions = [
    // { label: 'Tous', value: 'ALL' },
    { label: 'Auto', value: 'AUTO' },
    { label: 'Manuelle', value: 'MANUELLE' },
  ];

  ngOnInit(): void {
    this.fournisseurService
      .query({
        page: 0,
        size: 9999,
      })
      .subscribe((res: HttpResponse<IFournisseur[]>) => {
        this.fournisseurs = res.body || [];
      });
    this.active = this.commandCommonService.commandPreviousActiveNav();
  }

  onNavChange(evt: NgbNavChangeEvent): void {
    this.active = evt.nextId;
    this.commandCommonService.updateCommandPreviousActiveNav(this.active);
  }
  onTypeSuggestionChange(event: any): void {
    this.selectedtypeSuggession = event.value;
    setTimeout(() => {
      this.suggestion().onSearch();
    }, 50);
  }
  onFournisseurChange(event: any): void {
    this.selectFournisseurId = event.value;
    setTimeout(() => {
      this.suggestion().onSearch();
    }, 50);
  }

  onSearch(): void {
    switch (this.active) {
      case 'REQUESTED':
        this.commandeEnCoursComponent().onSearch();
        break;
      case 'SUGGESTIONS':
        this.suggestion().onSearch();
        break;
      case 'BONS_EN_COURS':
        this.enCoursComponent().onSearch();
        break;
      case 'LIST_BONS':
        this.listBonsComponent().onSearch();
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
    } else if (this.active === 'SUGGESTIONS') {
      this.suggestion().fusionner();
    }
  }

  cancel(): void {
    this.fileDialog = false;
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
    this.openCommandeResponseDialog(CommandeImportResponseDialogComponent, responseCommande);
  }

  openImporterReponseCommandeDialog(responseCommande: IResponseCommande): void {
    this.openCommandeResponseDialog(CommandeEnCoursResponseDialogComponent, responseCommande, this.commandeSelected);
  }

  private openCommandeResponseDialog(component: any, responseCommande: any, commande?: ICommande): void {
    const modalRef = this.modalService.open(component, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
    });
    modalRef.componentInstance.responseCommande = responseCommande;
    if (commande) {
      modalRef.componentInstance.commande = commande;
    }
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
      message: ' Voullez-vous supprimer ces lignes  ?',
      header: ' SUPPRESSION',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      icon: 'pi pi-info-circle',
      accept: () => {
        if (this.active === 'REQUESTED') {
          this.commandeEnCoursComponent().removeAll();
        } else if (this.active === 'SUGGESTIONS') {
          this.suggestion().deleteAll();
        }
      },
      key: 'deleteCommande',
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
