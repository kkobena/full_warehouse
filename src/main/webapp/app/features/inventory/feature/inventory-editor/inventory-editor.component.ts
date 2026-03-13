import {Component, computed, DestroyRef, effect, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {Button} from 'primeng/button';
import {Toolbar} from 'primeng/toolbar';
import {Toast} from 'primeng/toast';
import {MessageService} from 'primeng/api';
import {Tooltip} from 'primeng/tooltip';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {filter} from 'rxjs';
import {InventoryEditorFacade} from '../../data-access/facades/inventory-editor.facade';
import {InventoryListFacade} from '../../data-access/facades/inventory-list.facade';
import {InventoryStore} from '../../data-access/store/inventory.store';
import {
  InventoryProgressBarComponent
} from '../../ui/inventory-progress-bar/inventory-progress-bar.component';
import {
  InventoryLinesGridComponent
} from '../../ui/inventory-lines-grid/inventory-lines-grid.component';
import {
  InventoryImportModalComponent
} from '../../ui/inventory-import-modal/inventory-import-modal.component';
import {FormsModule} from '@angular/forms';
import {Select} from 'primeng/select';
import {INVENTORY_CATEGORIES, InventoryEvent, InventoryLineFilter,} from '../../models';
import {StorageService} from '../../../../entities/storage/storage.service';
import {RayonService} from '../../../../entities/rayon/rayon.service';
import {IStorage} from '../../../../shared/model/magasin.model';
import {IRayon} from '../../../../shared/model/rayon.model';
import {
  NgbConfirmDialogService
} from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {HasAuthorityService} from '../../../../entities/sales/service/has-authority.service';
import {Authority} from '../../../../shared/constants/authority.constants';

@Component({
  selector: 'app-inventory-editor',
  imports: [
    CommonModule,
    FormsModule,
    Button,
    Toolbar,
    Toast,
    Tooltip,
    Select,
    InventoryProgressBarComponent,
    InventoryLinesGridComponent,
  ],
  providers: [MessageService],
  templateUrl: './inventory-editor.component.html',
  styleUrl: './inventory-editor.component.scss',
})
export class InventoryEditorComponent implements OnInit {
  readonly editorFacade = inject(InventoryEditorFacade);
  readonly listFacade = inject(InventoryListFacade);
  readonly store = inject(InventoryStore);

  inventoryId = signal<number>(0);
  storages = signal<IStorage[]>([]);
  rayons = signal<IRayon[]>([]);
  page = signal(0);
  size = signal(20);
  readonly pageSizeOptions = [10, 20, 50, 100];
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.store.totalLines() / this.size())));
  readonly isFirstPage = computed(() => this.page() === 0);
  readonly isLastPage = computed(() => this.page() >= this.totalPages() - 1);
  private selectedLineFilter: InventoryLineFilter = 'NONE';
  private selectedStorageId: number | null = null;
  private selectedRayonId: number | null = null;
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly modal = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly messageService = inject(MessageService);
  private readonly storageService = inject(StorageService);
  private readonly rayonService = inject(RayonService);
  private readonly hasAuthority = inject(HasAuthorityService);
  /** Blind mode driven by privilege — ADMIN et STORE_INVENTORY voient le stock */
  readonly blindMode = computed(() =>
    !this.hasAuthority.hasAuthorities([Authority.PR_VOIR_STOCK_INVENTAIRE])
  );
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

  protected get isInventoryClosed(): boolean {
    return this.store.currentInventory()?.statut === 'CLOSED';
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/inventaire']);
      return;
    }
    this.inventoryId.set(id);
    this.store.resetEditor();
    this.listFacade.loadInventory(id);
    this.loadLines();
    this.editorFacade.refreshProgress(id);
    this.loadStorages();
    this.subscribeToEvents();
  }

  protected loadLines(): void {
    const params: any = {
      page: this.page(),
      size: this.size(),
      storeInventoryId: this.inventoryId(),
    };
    if (this.selectedLineFilter !== 'NONE') {
      params.selectedFilter = this.selectedLineFilter;
    }
    if (this.selectedStorageId) {
      params.storageId = this.selectedStorageId;
    }
    if (this.selectedRayonId) {
      params.rayonId = this.selectedRayonId;
    }
    this.editorFacade.loadLines(this.inventoryId(), params);
  }

  protected onGridFilterChange(event: {
    lineFilter: InventoryLineFilter;
    storageId: number | null;
    rayonId: number | null
  }): void {
    this.selectedLineFilter = event.lineFilter;
    this.selectedStorageId = event.storageId;
    this.selectedRayonId = event.rayonId;
    this.page.set(0);
    this.loadLines();
  }

  protected onPageChange(delta: number): void {
    const next = this.page() + delta;
    if (next >= 0 && next < this.totalPages()) {
      this.page.set(next);
      this.loadLines();
    }
  }

  protected onNextPage(): void {
    const next = this.page() + 1;
    if (next < this.totalPages()) {
      this.page.set(next);
      this.loadLines();
    }
  }

  protected onPageSizeChange(newSize: number): void {
    this.size.set(newSize);
    this.page.set(0);
    this.loadLines();
  }

  protected onGridStorageChange(storageId: number | null): void {
    this.rayons.set([]);
    if (storageId) {
      this.rayonService
        .query({storageId, size: 9999})
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({next: resp => this.rayons.set(resp.body ?? [])});
    }
  }

  protected openImportModal(): void {
    const ref = this.modal.open(InventoryImportModalComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    ref.componentInstance.inventoryId = this.inventoryId();
    ref.result.then(() => this.loadLines(), () => {
    });
  }

  protected closeInventory(): void {
    this.confirmDialog.onConfirm(
      () => this.listFacade.closeInventory(this.inventoryId()),
      'Clôture inventaire',
      'Clôturer cet inventaire ? Cette action est irréversible.',
      'pi pi-lock',
    );
  }

  protected goBack(): void {
    this.router.navigate(['/inventaire']);
  }

  protected getCategoryLabel(cat: any): string {
    const name = typeof cat === 'string' ? cat : cat?.name;
    return INVENTORY_CATEGORIES.find(c => c.value === name)?.label ?? name ?? '-';
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

  private loadStorages(): void {
    this.storageService
      .fetchUserStorages()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({next: resp => this.storages.set(resp.body ?? [])});
  }

  private subscribeToEvents(): void {
    this.lastEvent$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event: InventoryEvent) => {
        switch (event.type) {
          case 'LINE_SAVED':
            break;
          case 'LINE_SAVE_ERROR':
            this.messageService.add({
              severity: 'error',
              summary: 'Erreur',
              detail: 'Erreur lors de la sauvegarde de la ligne'
            });
            break;
          case 'IMPORT_COMPLETED':
            this.messageService.add({
              severity: 'success',
              summary: 'Import',
              detail: 'Import CSV terminé'
            });
            this.loadLines();
            break;
          case 'INVENTORY_CLOSED':
            this.messageService.add({
              severity: 'success',
              summary: 'Clôture',
              detail: 'Inventaire clôturé avec succès'
            });
            this.listFacade.loadInventory(this.inventoryId());
            break;
        }
      });
  }
}
