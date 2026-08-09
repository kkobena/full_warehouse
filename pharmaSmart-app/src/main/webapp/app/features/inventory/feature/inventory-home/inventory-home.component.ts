import {Component, computed, DestroyRef, effect, inject, OnInit, signal, ViewChild, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {
  NgbModal,
  NgbNav,
  NgbNavChangeEvent,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavOutlet,
  NgbTooltip
} from '@ng-bootstrap/ng-bootstrap';
import {NotificationService} from '../../../../shared/services/notification.service';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {catchError, filter, finalize, map, of, switchMap} from 'rxjs';
import {InventoryListFacade} from '../../data-access/facades/inventory-list.facade';
import {InventoryStore} from '../../data-access/store/inventory.store';
import {
  InventoryCreateModalComponent
} from '../../ui/inventory-create-modal/inventory-create-modal.component';
import {IStoreInventory} from '../../../../shared/model';
import {InventoryEvent} from '../../models';
import {
  NgbConfirmDialogService
} from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {
  PlanningTournantListComponent
} from '../../ui/planning-tournant-list/planning-tournant-list.component';
import {
  PlanningTournantModalComponent
} from '../../ui/planning-tournant-modal/planning-tournant-modal.component';
import {GapSummaryComponent} from '../../ui/gap-summary/gap-summary.component';
import {
  InventoryValuationComponent
} from '../../ui/inventory-valuation/inventory-valuation.component';
import {
  InventoryExportModalComponent
} from '../../ui/inventory-export-modal/inventory-export-modal.component';
import { AbilityService } from 'app/core/auth/ability.service';
import {ButtonComponent, DataTableComponent, RowTogglerDirective, ToolbarComponent} from '../../../../shared/ui';
import {InventoryApiService} from '../../data-access/services/inventory-api.service';
import {ConfigurationService} from '../../../../shared/configuration.service';
import {BlobDownloadService} from '../../../../shared/services/blob-download.service';

@Component({
  selector: 'app-inventory-home',
  imports: [
    CommonModule,
    ButtonComponent,
    ToolbarComponent,
    NgbNav,
    NgbNavItem,
    NgbNavLink,
    NgbNavContent,
    NgbNavOutlet,
    NgbTooltip,
    DataTableComponent,
    RowTogglerDirective,
    PlanningTournantListComponent,
    GapSummaryComponent,
    InventoryValuationComponent,
  ],
  templateUrl: './inventory-home.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './inventory-home.component.scss',
})
export class InventoryHomeComponent implements OnInit {
  @ViewChild(PlanningTournantListComponent) planningList?: PlanningTournantListComponent;

  readonly listFacade = inject(InventoryListFacade);
  readonly store = inject(InventoryStore);
  activeTab = signal<string>('en-cours');

  protected readonly toolbarTitle = computed(() => {
    switch (this.activeTab()) {
      case 'tournant':
        return 'Inventaire tournant';
      case 'clotures':
        return 'Inventaires clôturés';
      default:
        return 'Inventaires en cours';
    }
  });

  protected readonly toolbarIcon = computed(() => {
    switch (this.activeTab()) {
      case 'tournant':
        return 'pi pi-sync';
      case 'clotures':
        return 'pi pi-lock';
      default:
        return 'pi pi-refresh';
    }
  });

  private readonly ability = inject(AbilityService);

  protected readonly showEnCours  = this.ability.canSignal('display', 'inventaire.en-cours');
  protected readonly showTournant = this.ability.canSignal('display', 'inventaire.tournant');
  protected readonly showClotures = this.ability.canSignal('display', 'inventaire.clotures');
  page = signal(0);
  size = signal(20);
  /** Inventaire dont l'export est en cours — pilote le spinner du bouton de sa ligne. */
  protected readonly exportingId = signal<number | null>(null);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly modal = inject(NgbModal);
  private readonly inventoryApi = inject(InventoryApiService);
  private readonly configurationService = inject(ConfigurationService);
  private readonly blobDownloadService = inject(BlobDownloadService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private lastEvent$ = toObservable(this.store.lastEvent).pipe(
    filter((e): e is InventoryEvent => e !== null),
  );

  constructor() {
    effect(() => {
      const err = this.store.error();
      if (!err) {
        return;
      }
      this.notificationService.error(err, 'Erreur');
      // Consommée : un signal dont la valeur ne change pas ne redéclenche pas l'effect,
      // deux erreurs identiques successives ne produiraient qu'une seule notification.
      this.store.setError(null);
    });
  }

  ngOnInit(): void {
    // Avant loadList() : c'est l'onglet actif qui détermine les statuts chargés.
    this.restoreTabFromRoute();
    this.subscribeToEvents();
    this.loadList();
  }

  /**
   * Restaure l'onglet transmis par l'éditeur (`?tab=`). Sans cela, tout retour depuis
   * l'éditeur retombait sur « En cours », y compris en venant de « Clôturés » ou
   * « Tournant ».
   *
   * L'onglet n'est repris que s'il est effectivement affiché : un `tab` obsolète ou
   * interdit par les droits sélectionnerait un onglet inexistant, et la vue resterait vide.
   */
  private restoreTabFromRoute(): void {
    const tab = this.route.snapshot.queryParamMap.get('tab');
    if (tab && this.isTabAvailable(tab)) {
      this.activeTab.set(tab);
    }
  }

  private isTabAvailable(tab: string): boolean {
    switch (tab) {
      case 'en-cours':
        return this.showEnCours();
      case 'tournant':
        return this.showTournant();
      case 'clotures':
        return this.showClotures();
      default:
        return false;
    }
  }

  protected onNavChange(evt: NgbNavChangeEvent): void {
    this.activeTab.set(evt.nextId);
    this.page.set(0);
    this.loadList();
  }

  protected loadList(): void {
    if (this.activeTab() === 'tournant') {
      return;
    }
    const statuts = this.activeTab() === 'en-cours' ? ['CREATE', 'PROCESSING'] : ['CLOSED'];
    this.listFacade.loadList({page: this.page(), size: this.size(), statuts});
  }

  protected isTournantTab(): boolean {
    return this.activeTab() === 'tournant';
  }

  protected openCreateModal(): void {
    const ref = this.modal.open(InventoryCreateModalComponent, {
      size: 'lg',
      backdrop: 'static',
      keyboard: false,
    });
    ref.result.then(
      (inventory: IStoreInventory) => {
        if (inventory?.id) {
          this.openExportModalFor(inventory, () => this.navigateToEditor(inventory));
        }
      },
      () => {},
    );
  }

  protected openCreatePlanningModal(): void {
    const ref = this.modal.open(PlanningTournantModalComponent, {size: 'lg', backdrop: 'static'});
    ref.closed.subscribe(() => {
      this.planningList?.loadAll();
      this.planningList?.loadDashboard();
    });
  }

  protected openEditor(inventory: IStoreInventory): void {
    this.navigateToEditor(inventory);
  }

  protected openReadOnly(inventory: IStoreInventory): void {
    this.navigateToEditor(inventory);
  }

  /**
   * Ouvre l'éditeur en lui transmettant l'onglet d'origine : son bouton « Retour » nous
   * ramène ainsi ici, et pas systématiquement sur « En cours ».
   */
  private navigateToEditor(inventory: IStoreInventory): void {
    this.router.navigate(['/inventaire', inventory.id, 'edit'],
      {queryParams: {tab: this.activeTab()}});
  }

  protected openGapAnalysisFor(inventory: IStoreInventory): void {
    import('../../ui/gap-analysis-modal/gap-analysis-modal.component').then(m => {
      const ref = this.modal.open(m.GapAnalysisModalComponent, {size: 'xl', backdrop: 'static',centered:true});
      ref.componentInstance.inventoryId = inventory.id;
    });
  }

  protected exportPdf(inventory: IStoreInventory): void {
    this.openExportModalFor(inventory);
  }


  protected exportEnCoursPdf(inventory: IStoreInventory): void {
    const id = inventory.id!;
    this.exportingId.set(id);

    this.configurationService.find('APP_GESTION_LOT_INVENTAIRE')
      .pipe(
        map(res => res.body?.value === '1'),
        catchError(() => of(false)),
        switchMap(gestionLot => this.inventoryApi.exportToPdf(id, 'RAYON', {}, gestionLot)),
        finalize(() => this.exportingId.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: blob => this.blobDownloadService.downloadPdf(blob, `inventaire-${id}`),
        error: () => this.notificationService.error("Échec de l'export PDF", 'Erreur'),
      });
  }

  protected isExporting(inventory: IStoreInventory): boolean {
    return this.exportingId() === inventory.id;
  }

  protected deleteInventory(inventory: IStoreInventory): void {
    this.confirmDialog.onConfirm(
      () => this.listFacade.deleteInventory(inventory.id!),
      'Suppression inventaire',
      `Supprimer l'inventaire "${inventory.description ?? inventory.id}" ?`,
      'pi pi-trash',
    );
  }

  /**
   * Clôture précédée du récapitulatif (lignes restantes, écarts valorisés), comme dans
   * l'éditeur : l'action est irréversible, elle ne doit pas se confirmer à l'aveugle.
   */
  protected closeInventory(inventory: IStoreInventory): void {
    import('../../ui/inventory-close-modal/inventory-close-modal.component').then(m => {
      const ref = this.modal.open(m.InventoryCloseModalComponent, {
        size: 'xl',
        backdrop: 'static',
        centered:true
      });
      ref.componentInstance.inventoryId = inventory.id;
      ref.result.then(
        confirmed => {
          if (confirmed) {
            this.listFacade.closeInventory(inventory.id!);
          }
        },
        () => {
          // fermeture / annulation
        },
      );
    });
  }

  protected getStatutBadgeClass(statut?: string): string {
    switch (statut) {
      case 'CREATE':
        return 'pharma-badge-primary';
      case 'PROCESSING':
        return 'pharma-badge-warning';
      case 'CLOSED':
        return 'pharma-badge-success';
      default:
        return 'pharma-badge-secondary';
    }
  }

  protected getStatutLabel(statut?: string): string {
    switch (statut) {
      case 'CREATE':
        return 'Créé';
      case 'PROCESSING':
        return 'En cours';
      case 'CLOSED':
        return 'Clôturé';
      default:
        return statut ?? '-';
    }
  }

  protected getCategoryLabel(cat?: string): string {
    const labels: Record<string, string> = {
      MAGASIN: 'Global (magasin)',
      STORAGE: 'Emplacement',
      RAYON: 'Rayon',
      FAMILLY: 'Famille',
      PERIME: 'Périmés',
      ALERTE_PEREMPTION: 'Alerte péremption',
      VENDU: 'Vendus (période)',
      INVENDU: 'Invendus (période)',
      SOUS_SEUIL: 'Sous seuil',
      EN_RUPTURE: 'Rupture',
    };
    return labels[cat ?? ''] ?? cat ?? '-';
  }

  private openExportModalFor(inventory: IStoreInventory, onClose?: () => void): void {
    const ref = this.modal.open(InventoryExportModalComponent, {
      size: 'md',
      backdrop: 'static',
    });
    ref.componentInstance.inventoryId = inventory.id;
    ref.componentInstance.inventoryDescription = inventory.description ?? `#${inventory.id}`;
    // always resolve (export or skip), then run optional callback
    ref.result.then(
      () => onClose?.(),
      () => onClose?.(),
    );
  }

  private subscribeToEvents(): void {
    this.lastEvent$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event: InventoryEvent) => {
        switch (event.type) {
          case 'INVENTORY_CREATED':
            // Navigation handled in openCreateModal after export modal
            break;
          case 'INVENTORY_DELETED':
            this.notificationService.success('Inventaire supprimé', 'Succès');
            this.loadList();
            break;
          case 'INVENTORY_CLOSED':
            this.notificationService.success('Inventaire clôturé avec succès', 'Succès');
            this.loadList();
            break;
        }
      });
  }
}
