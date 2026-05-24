import {Component, DestroyRef, effect, inject, OnInit, signal, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router} from '@angular/router';
import {
  NgbModal,
  NgbNav,
  NgbNavChangeEvent,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavOutlet
} from '@ng-bootstrap/ng-bootstrap';
import {Button} from 'primeng/button';
import {Toolbar} from 'primeng/toolbar';
import {Toast} from 'primeng/toast';
import {MessageService} from 'primeng/api';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {filter} from 'rxjs';
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
import {Tooltip} from "primeng/tooltip";
import {ButtonGroup} from "primeng/buttongroup";
import {TableModule} from 'primeng/table';
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

@Component({
  selector: 'app-inventory-home',
  imports: [
    CommonModule,
    Button,
    Toolbar,
    Toast,
    NgbNav,
    NgbNavItem,
    NgbNavLink,
    NgbNavContent,
    NgbNavOutlet,
    Tooltip,
    ButtonGroup,
    TableModule,
    PlanningTournantListComponent,
    GapSummaryComponent,
    InventoryValuationComponent,
  ],
  providers: [MessageService],
  templateUrl: './inventory-home.component.html',
  styleUrl: './inventory-home.component.scss',
})
export class InventoryHomeComponent implements OnInit {
  @ViewChild(PlanningTournantListComponent) planningList?: PlanningTournantListComponent;

  readonly listFacade = inject(InventoryListFacade);
  readonly store = inject(InventoryStore);
  activeTab = signal<string>('en-cours');

  private readonly ability = inject(AbilityService);

  protected readonly showEnCours  = this.ability.canSignal('display', 'inventaire.en-cours');
  protected readonly showTournant = this.ability.canSignal('display', 'inventaire.tournant');
  protected readonly showClotures = this.ability.canSignal('display', 'inventaire.clotures');
  page = signal(0);
  size = signal(20);
  private readonly router = inject(Router);
  private readonly modal = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);
  private lastEvent$ = toObservable(this.store.lastEvent).pipe(
    filter((e): e is InventoryEvent => e !== null),
  );

  constructor() {
    effect(() => {
      const err = this.store.error();
      if (err) {
        this.messageService.add({severity: 'error', summary: 'Erreur', detail: err, life: 5000});
      }
    });
  }

  ngOnInit(): void {
    this.subscribeToEvents();
    this.loadList();
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
          this.openExportModalFor(inventory, () =>
            this.router.navigate(['/inventaire', inventory.id, 'edit'])
          );
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
    this.router.navigate(['/inventaire', inventory.id, 'edit']);
  }

  protected openReadOnly(inventory: IStoreInventory): void {
    this.router.navigate(['/inventaire', inventory.id, 'edit']);
  }

  protected openGapAnalysisFor(inventory: IStoreInventory): void {
    import('../../ui/gap-analysis-modal/gap-analysis-modal.component').then(m => {
      const ref = this.modal.open(m.GapAnalysisModalComponent, {size: 'xl', backdrop: 'static'});
      ref.componentInstance.inventoryId = inventory.id;
    });
  }

  protected exportPdf(inventory: IStoreInventory): void {
    this.openExportModalFor(inventory);
  }

  protected onRowExpand(_event: any): void {
    // expansion handled by p-table dataKey
  }

  protected deleteInventory(inventory: IStoreInventory): void {
    this.confirmDialog.onConfirm(
      () => this.listFacade.deleteInventory(inventory.id!),
      'Suppression inventaire',
      `Supprimer l'inventaire "${inventory.description ?? inventory.id}" ?`,
      'pi pi-trash',
    );
  }

  protected closeInventory(inventory: IStoreInventory): void {
    this.confirmDialog.onConfirm(
      () => this.listFacade.closeInventory(inventory.id!),
      'Clôture inventaire',
      `Clôturer l'inventaire "${inventory.description ?? inventory.id}" ? Cette action est irréversible.`,
      'pi pi-lock',
    );
  }

  protected getStatutBadgeClass(statut?: string): string {
    switch (statut) {
      case 'CREATE':
        return 'badge-statut-create';
      case 'PROCESSING':
        return 'badge-statut-processing';
      case 'CLOSED':
        return 'badge-statut-closed';
      default:
        return 'bg-secondary';
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
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Inventaire supprimé'
            });
            this.loadList();
            break;
          case 'INVENTORY_CLOSED':
            this.messageService.add({
              severity: 'success',
              summary: 'Succès',
              detail: 'Inventaire clôturé avec succès'
            });
            this.loadList();
            break;
        }
      });
  }
}
