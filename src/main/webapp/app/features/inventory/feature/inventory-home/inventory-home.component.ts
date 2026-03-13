import {Component, DestroyRef, effect, inject, OnInit, signal} from '@angular/core';
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
  ],
  providers: [MessageService],
  templateUrl: './inventory-home.component.html',
  styleUrl: './inventory-home.component.scss',
})
export class InventoryHomeComponent implements OnInit {
  readonly listFacade = inject(InventoryListFacade);
  readonly store = inject(InventoryStore);
  activeTab = signal<string>('en-cours');
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
    const statuts = this.activeTab() === 'en-cours' ? ['CREATE', 'PROCESSING'] : ['CLOSED'];
    this.listFacade.loadList({page: this.page(), size: this.size(), statuts});
  }

  protected openCreateModal(): void {
    this.modal.open(InventoryCreateModalComponent, {
      size: 'lg',
      backdrop: 'static',
      keyboard: false,
    });
  }

  protected openEditor(inventory: IStoreInventory): void {
    this.router.navigate(['/inventaire', inventory.id, 'edit']);
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

  private subscribeToEvents(): void {
    this.lastEvent$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event: InventoryEvent) => {
        switch (event.type) {
          case 'INVENTORY_CREATED':
            if (event.payload?.id) {
              this.router.navigate(['/inventaire', event.payload.id, 'edit']);
            } else {
              this.loadList();
            }
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
