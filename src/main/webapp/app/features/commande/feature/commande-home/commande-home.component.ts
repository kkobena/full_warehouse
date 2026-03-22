import { Component, inject, OnInit, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { NgbModal, NgbNavChangeEvent, NgbNav, NgbNavItem, NgbNavLink, NgbNavContent, NgbNavOutlet } from '@ng-bootstrap/ng-bootstrap';
import { ICommande } from 'app/shared/model/commande.model';
import { CommandeImportResponseDialogComponent } from '../../../../entities/commande/commande-import-response-dialog.component';
import { ICommandeResponse } from 'app/shared/model/commande-response.model';
import { ImportationNewCommandeComponent } from '../../../../entities/commande/importation-new-commande.component';
import { CommandeEnCoursComponent } from '../../ui/commande-en-cours/commande-en-cours.component';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { FileUploadModule } from 'primeng/fileupload';
import { CardModule } from 'primeng/card';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ToolbarModule } from 'primeng/toolbar';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { CommandCommonService } from '../../../../entities/commande/command-common.service';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { BonEnCoursComponent } from '../../../../entities/commande/delevery/bon-en-cours/bon-en-cours.component';
import { ListBonsComponent } from '../../../../entities/commande/delevery/list-bons/list-bons.component';
import { FournisseurService } from '../../../../entities/fournisseur/fournisseur.service';
import { HttpResponse } from '@angular/common/http';
import { IFournisseur } from 'app/shared/model/fournisseur.model';
import { Select } from 'primeng/select';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';
import { RetourBonListComponent } from '../../../../entities/commande/retour_fournisseur/retour-bon-list.component';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { RetourBonStatut } from 'app/shared/model/enumerations/retour-bon-statut.model';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';
import { RepartitionStockComponent } from '../../../../entities/repartition-stock/repartition-stock.component';
import { CommandeDashboardComponent } from '../commande-dashboard/commande-dashboard.component';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { SuggestionHomeComponent } from '../suggestion/suggestion-home.component';
import { SemoisClasseConfigComponent } from '../semois-classe-config/semois-classe-config.component';

@Component({
  selector: 'app-commande-home',
  templateUrl: './commande-home.component.html',
  styleUrl: './commande-home.component.scss',
  imports: [
    CommonModule,
    NgbNav,
    NgbNavItem,
    NgbNavLink,
    NgbNavContent,
    NgbNavOutlet,
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
    SuggestionHomeComponent,
    ReactiveFormsModule,
    Select,
    RetourBonListComponent,
    DatePicker,
    FloatLabel,
    RepartitionStockComponent,
    CommandeDashboardComponent,
    SemoisClasseConfigComponent,
  ],
})
export class CommandeHomeComponent implements OnInit {
  protected fournisseurService = inject(FournisseurService);
  protected searchCommande = '';
  protected active = 'DASHBOARD';
  protected search = '';
  protected selectionLength = 0;
  protected readonly display = false;
  protected selectFournisseurId: number | null = null;
  protected fournisseurs: IFournisseur[] = [];
  protected selectedStatut: RetourBonStatut | null = null;
  protected dtStart: Date | null = new Date();
  protected dtEnd: Date | null = new Date();
  protected statutOptions = [
    { label: 'En attente de réponse', value: RetourBonStatut.VALIDATED },
    { label: 'Clôturé', value: RetourBonStatut.CLOSED },
  ];

  protected commandes: ICommande[] = [];
  protected selectedFilter = 'REQUESTED';
  protected loading!: boolean;
  protected readonly menuTileAndIcon = [
    { title: 'Tableau de bord', icon: 'pi pi-th-large', menuId: 'DASHBOARD' },
    { title: 'Commandes en cours', icon: 'pi pi-cart-plus', menuId: 'REQUESTED' },
    { title: 'Suggestions de commandes', icon: 'pi pi-lightbulb', menuId: 'SUGGESTIONS' },
    { title: 'Bons de livraison en cours', icon: 'pi pi-fw pi-truck', menuId: 'BONS_EN_COURS' },
    { title: 'Liste des bons de livraison', icon: 'pi pi-fw pi-list', menuId: 'LIST_BONS' },
    { title: 'Retours fournisseur', icon: 'pi pi-replay', menuId: 'RETOUR_FOURNISSEUR' },
    { title: 'Pilotage des stocks', icon: 'pi pi-sync', menuId: 'REPARTITION_STOCK' },
  ];

  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly modalService = inject(NgbModal);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly commandeEnCoursComponent = viewChild(CommandeEnCoursComponent);
  private readonly enCoursComponent = viewChild(BonEnCoursComponent);
  private readonly listBonsComponent = viewChild(ListBonsComponent);
  private readonly retourBonListComponent = viewChild(RetourBonListComponent);
  private readonly repartitionStockComponent = viewChild('repartitionStock', { read: RepartitionStockComponent });
  private readonly dashboardComponent = viewChild(CommandeDashboardComponent);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);

  ngOnInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    this.fournisseurService
      .query({ page: 0, size: 999 })
      .subscribe((res: HttpResponse<IFournisseur[]>) => {
        this.fournisseurs = res.body || [];
      });

    this.route.queryParams.subscribe(params => {
      if (params['tab']) {
        this.active = params['tab'];
        this.commandCommonService.updateCommandPreviousActiveNav(this.active);
      } else {
        this.active = this.commandCommonService.commandPreviousActiveNav();
      }
    });
  }

  protected onNavChange(evt: NgbNavChangeEvent): void {
    this.active = evt.nextId;
    this.commandCommonService.updateCommandPreviousActiveNav(this.active);
  }

  protected onFournisseurChange(event: any): void {
    this.selectFournisseurId = event.value;
    setTimeout(() => {
      if (this.active === 'LIST_BONS') {
        this.listBonsComponent().onSearch();
      }
    }, 50);
  }

  protected onSearch(): void {
    switch (this.active) {
      case 'DASHBOARD':
        this.dashboardComponent()?.refresh();
        break;
      case 'REQUESTED':
        this.commandeEnCoursComponent().onSearch();
        break;
      case 'BONS_EN_COURS':
        this.enCoursComponent().onSearch();
        break;
      case 'LIST_BONS':
        this.listBonsComponent().onSearch();
        break;
      case 'RETOUR_FOURNISSEUR':
        this.retourBonListComponent().onSearch();
        break;
      case 'REPARTITION_STOCK':
        this.repartitionStockComponent()?.onSearch();
        break;
    }
  }

  protected onCreatNewCommande(): void {
    this.commandCommonService.updateCommand(null);
    this.commandCommonService.updateCommandPreviousActiveNav(this.active);
    this.router.navigate(['/commande/new']);
  }

  protected onCreateRetourFournisseur(): void {
    this.router.navigate(['/commande/retour-fournisseur/new']);
  }

  protected fusionner(): void {
    if (this.active === 'REQUESTED') {
      this.commandeEnCoursComponent().fusionner();
    }
  }

  onShowNewCommandeDialog(): void {
    showCommonModal(
      this.modalService,
      ImportationNewCommandeComponent,
      { header: 'IMPORTATION DE NOUVELLE COMMANDE' },
      reason => {
        this.active = 'REQUESTED';
        this.onSearch();
        this.openImportResponseDialogComponent(reason);
      },
      'lg',
    );
  }

  confirmDeleteSelectedRows(): void {
    this.confirmDialog.onConfirm(
      () => {
        if (this.active === 'REQUESTED') {
          this.commandeEnCoursComponent().removeAll();
        }
      },
      'Suppression',
      'Êtes-vous sûr de vouloir supprimer ?',
    );
  }

  protected updateSelectionLength(lgth: number): void {
    this.selectionLength = lgth;
  }

  protected get title(): string {
    return this.menuTileAndIcon.find(m => m.menuId === this.active)?.title || '';
  }

  protected get icon(): string {
    return this.menuTileAndIcon.find(m => m.menuId === this.active)?.icon || '';
  }

  protected exportRepartitionStockToPdf(): void {
    this.repartitionStockComponent()?.exportToPdf();
  }

  private openImportResponseDialogComponent(responseCommande: ICommandeResponse): void {
    this.openCommandeResponseDialog(CommandeImportResponseDialogComponent, responseCommande);
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
}
