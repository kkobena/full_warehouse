import { Component, inject, OnInit, viewChild } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { NgbModal, NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';
import { ICommande } from 'app/shared/model/commande.model';
import { CommandeImportResponseDialogComponent } from './commande-import-response-dialog.component';
import { ICommandeResponse } from '../../shared/model/commande-response.model';
import { ImportationNewCommandeComponent } from './importation-new-commande.component';
import { CommandeEnCoursComponent } from './commande-en-cours/commande-en-cours.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { FileUploadModule } from 'primeng/fileupload';
import { CardModule } from 'primeng/card';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ToolbarModule } from 'primeng/toolbar';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { CommandCommonService } from './command-common.service';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { BonEnCoursComponent } from './delevery/bon-en-cours/bon-en-cours.component';
import { ListBonsComponent } from './delevery/list-bons/list-bons.component';
import { SuggestionComponent } from './suggestion/suggestion.component';
import { FournisseurService } from '../fournisseur/fournisseur.service';
import { HttpResponse } from '@angular/common/http';
import { IFournisseur } from '../../shared/model/fournisseur.model';
import { Select } from 'primeng/select';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'jhi-commande',
  templateUrl: './commande.component.html',

  imports: [
    WarehouseCommonModule,
    ButtonModule,
    TableModule,
    RouterModule,
    TooltipModule,
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
    SuggestionComponent,
    ReactiveFormsModule,
    Select,
    ConfirmDialogComponent
  ],
  styleUrl: './commande.component.scss',
})
export class CommandeComponent implements OnInit {
  protected fournisseurService = inject(FournisseurService);
  protected searchCommande = '';
  protected active = 'REQUESTED';
  protected search = '';
  protected selectionLength = 0;
  protected readonly display = false;
  protected selectFournisseurId: number | null = null;
  protected fournisseurs: IFournisseur[] = [];
  protected selectedtypeSuggession: string = null;
  protected typeSuggessions = [
    // { label: 'Tous', value: 'ALL' },
    { label: 'Auto', value: 'AUTO' },
    { label: 'Manuelle', value: 'MANUELLE' }
  ];

  protected commandes: ICommande[] = [];
  protected selectedFilter = 'REQUESTED';
  protected loading!: boolean;
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly commandeEnCoursComponent = viewChild(CommandeEnCoursComponent);
  private readonly suggestion = viewChild(SuggestionComponent);
  private readonly enCoursComponent = viewChild(BonEnCoursComponent);
  private readonly listBonsComponent = viewChild(ListBonsComponent);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

  ngOnInit(): void {
    this.fournisseurService
      .query({
        page: 0,
        size: 9999
      })
      .subscribe((res: HttpResponse<IFournisseur[]>) => {
        this.fournisseurs = res.body || [];
      });
    this.active = this.commandCommonService.commandPreviousActiveNav();
  }

  protected onNavChange(evt: NgbNavChangeEvent): void {
    this.active = evt.nextId;
    this.commandCommonService.updateCommandPreviousActiveNav(this.active);
  }

  protected onTypeSuggestionChange(event: any): void {
    this.selectedtypeSuggession = event.value;
    setTimeout(() => {
      this.suggestion().onSearch();
    }, 100);
  }

  protected onFournisseurChange(event: any): void {
    this.selectFournisseurId = event.value;
    setTimeout(() => {
      this.suggestion().onSearch();
    }, 50);
  }

  protected onSearch(): void {
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

  protected onCreatNewCommande(): void {
    this.commandCommonService.updateCommand(null);
    this.commandCommonService.updateCommandPreviousActiveNav(this.active);
    this.router.navigate(['/commande/new']);
  }

  protected fusionner(): void {
    if (this.active === 'REQUESTED') {
      this.commandeEnCoursComponent().fusionner();
    } else if (this.active === 'SUGGESTIONS') {
      this.suggestion().fusionner();
    }
  }


  private openImportResponseDialogComponent(responseCommande: ICommandeResponse): void {
    this.openCommandeResponseDialog(CommandeImportResponseDialogComponent, responseCommande);
  }

  private openCommandeResponseDialog(component: any, responseCommande: any, commande?: ICommande): void {
    const modalRef = this.modalService.open(component, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static'
    });
    modalRef.componentInstance.responseCommande = responseCommande;
    if (commande) {
      modalRef.componentInstance.commande = commande;
    }
  }

  onShowNewCommandeDialog(): void {
    showCommonModal(
      this.modalService,
      ImportationNewCommandeComponent,
      {
        header: 'IMPORTATION DE NOUVELLE COMMANDE'
      },
      (reason) => {
        this.active = 'REQUESTED';
        this.onSearch();
        this.openImportResponseDialogComponent(reason);
      },
      'lg'
    );
  }

  confirmDeleteSelectedRows(): void {
    this.confimDialog().onConfirm(() => {
      if (this.active === 'REQUESTED') {
        this.commandeEnCoursComponent().removeAll();
      } else if (this.active === 'SUGGESTIONS') {
        this.suggestion().deleteAll();
      }
    }, 'Suppression', 'Êtes-vous sûr de vouloir supprimer ?');

  }


  protected updateSelectionLength(lgth: number): void {
    this.selectionLength = lgth;
  }
}
