import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { FloatLabelModule } from 'primeng/floatlabel';
import { SelectModule } from 'primeng/select';
import { Toolbar } from 'primeng/toolbar';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TableLazyLoadEvent } from 'primeng/table';
import { Toast } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs/operators';
import { MagasinService } from '../../../../entities/magasin/magasin.service';
import { StorageService } from '../../../../entities/storage/storage.service';
import { BlobDownloadService } from '../../../../shared/services/blob-download.service';
import { Storage } from '../../../../entities/storage/storage.model';
import { NgbConfirmDialogService } from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { FileUploadDialogComponent } from '../../../../entities/groupe-tiers-payant/file-upload-dialog/file-upload-dialog.component';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';
import { ITEMS_PER_PAGE } from '../../../../shared/constants/pagination.constants';
import { RayonApiService } from '../../data-access/services/rayon-api.service';
import { RayonProduitApiService } from '../../data-access/services/rayon-produit-api.service';
import { IRayon, TYPE_ZONE_OPTIONS, TypeZone } from '../../models/rayon.model';
import { RayonListComponent } from '../../ui/rayon-list/rayon-list.component';
import { RayonDetailPanelComponent } from '../../ui/rayon-detail-panel/rayon-detail-panel.component';
import { RayonFormComponent } from '../../ui/rayon-form/rayon-form.component';
import { CloneRayonFormComponent } from '../../ui/clone-rayon-form/clone-rayon-form.component';
import { InventoryCreateModalComponent } from '../../../inventory/ui/inventory-create-modal/inventory-create-modal.component';

@Component({
  selector: 'app-rayon-home',
  templateUrl: './rayon-home.component.html',
  styleUrl: './rayon-home.component.scss',
  imports: [
    FormsModule,
    Toolbar,
    ButtonModule,
    FloatLabelModule,
    SelectModule,
    IconField,
    InputIcon,
    InputTextModule,
    RayonListComponent,
    RayonDetailPanelComponent,
    Toast,
    TooltipModule,
  ],
})
export class RayonHomeComponent implements OnInit {
  protected rayons = signal<IRayon[]>([]);
  protected totalItems = signal(0);
  protected loading = signal(false);
  protected selectedRayon = signal<IRayon | null>(null);
  protected panelOpen = computed(() => this.selectedRayon() !== null);

  protected page = 0;
  protected rows = ITEMS_PER_PAGE;

  protected storages: Storage[] = [];
  protected selectedStorage: Storage | null = null;
  protected selectedTypeZone: TypeZone | null = null;
  protected search = '';
  protected readonly typeZoneOptions = TYPE_ZONE_OPTIONS;


  private readonly router = inject(Router);
  private readonly rayonApi = inject(RayonApiService);
  private readonly rayonProduitApi = inject(RayonProduitApiService);
  private readonly magasinService = inject(MagasinService);
  private readonly storageService = inject(StorageService);
  private readonly modalService = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly downloadService = inject(BlobDownloadService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.loadStorages();
  }

  protected onStorageChange(): void {
    this.page = 0;
    this.selectedRayon.set(null);
    this.loadPage();
  }

  protected onSearch(): void {
    this.page = 0;
    this.loadPage();
  }

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.page = Math.floor((event.first ?? 0) / (event.rows ?? this.rows));
    this.rows = event.rows ?? this.rows;
    this.loadPage();
  }

  protected onRayonSelected(rayon: IRayon): void {
    this.selectedRayon.set(rayon);
  }

  protected onClosePanel(): void {
    this.selectedRayon.set(null);
  }

  protected onEditRequested(rayon: IRayon): void {
    this.openForm(rayon);
  }

  protected onRayonEdited(saved: IRayon): void {
    this.rayons.update(list => list.map(r => r.id === saved.id ? { ...r, ...saved } : r));
    this.selectedRayon.update(r => r?.id === saved.id ? { ...r, ...saved } : r);
  }

  protected onDeleteRequested(rayon: IRayon): void {
    this.confirmDialog.onConfirm(
      () => this.deleteRayon(rayon),
      'Suppression',
      `Supprimer le rayon "${rayon.libelle}" ?`
    );
  }

  protected onInventaireRequested(rayon: IRayon): void {
    const ref = this.modalService.open(InventoryCreateModalComponent, {
      size: 'lg',
      centered: true,
      backdrop: 'static',
    });
    const inst = ref.componentInstance as InventoryCreateModalComponent;
    inst.prefill = { inventoryCategory: 'RAYON', storageId: rayon.storageId, rayonId: rayon.id };
    ref.closed.subscribe((inventory: { id?: number }) => {
      if (inventory?.id) {
        this.router.navigate(['/inventaire', inventory.id, 'edit']);
      }
    });
  }

  protected importRayonProduits(): void {
    if (!this.selectedStorage?.id) return;
    showCommonModal(
      this.modalService,
      FileUploadDialogComponent,
      {},
      result => {
        const fd = new FormData();
        fd.append('importcsv', result);
        this.rayonProduitApi
          .importCsv(fd, this.selectedStorage!.id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: res => {
              const dto = res.body;
              if (dto) {
                this.notificationService.success(dto.message ?? 'Import terminé');
              }
            },
            error: err => this.notificationService.error(this.errorService.getErrorMessage(err)),
          });
      },
      'lg'
    );
  }

  protected addNew(): void {
    this.openForm(null);
  }

  protected showFileDialog(): void {
    showCommonModal(
      this.modalService,
      FileUploadDialogComponent,
      {},
      result => {
        const fd = new FormData();
        fd.append('importcsv', result);
        this.rayonApi
          .uploadFile(fd)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.notificationService.success('Import réussi');
              this.loadPage();
            },
            error: err => this.notificationService.error(this.errorService.getErrorMessage(err)),
          });
      },
      'lg'
    );
  }

  protected cloneRayons(): void {
    const ref = this.modalService.open(CloneRayonFormComponent, {
      size: 'lg',
      centered: true,
      backdrop: 'static',
    });
    const inst = ref.componentInstance as CloneRayonFormComponent;
    inst.rayons = this.rayons();
    ref.closed.subscribe(() => this.notificationService.success('Rayons clonés avec succès'));
  }

  protected exportCsv(): void {
    this.downloadService.downloadFromObservable(
      this.rayonApi.exportCsv(this.selectedStorage?.id),
      `rayons_${this.selectedStorage?.id ?? 'export'}`,
      'csv',
      undefined,
      undefined,
      () => this.notificationService.error("Erreur lors de l'export CSV"),
    );
  }

  private openForm(rayon: IRayon | null): void {
    const ref = this.modalService.open(RayonFormComponent, {
      size: 'lg',
      centered: true,
      backdrop: 'static',
    });
    const inst = ref.componentInstance as RayonFormComponent;
    inst.entity = rayon ? { ...rayon } : null;
    inst.header = rayon ? 'Modifier ' + rayon.libelle : 'Ajouter un nouveau rayon';
    ref.closed.subscribe((saved: IRayon) => {
      if (!saved) return;
      if (rayon) {
        this.onRayonEdited(saved);
      } else {
        this.page = 0;
        this.loadPage();
      }
    });
  }

  private deleteRayon(rayon: IRayon): void {
    this.rayonApi
      .delete(rayon.id!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.rayons.update(list => list.filter(r => r.id !== rayon.id));
          this.totalItems.update(n => n - 1);
          if (this.selectedRayon()?.id === rayon.id) {
            this.selectedRayon.set(null);
          }
          this.notificationService.success(`Rayon "${rayon.libelle}" supprimé`);
        },
        error: err => this.notificationService.error(this.errorService.getErrorMessage(err)),
      });
  }

  private loadStorages(): void {
    this.magasinService.findCurrentUserMagasin().then(magasin => {
      this.storageService
        .fetchStorages({ magasinId: magasin.id })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((res: HttpResponse<Storage[]>) => {
          this.storages = res.body ?? [];
          this.selectedStorage = this.storages.find(s => s.type === 'PRINCIPAL') ?? null;
          this.loadPage();
        });
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    this.rayonApi
      .query({
        page: this.page,
        size: this.rows,
        search: this.search || undefined,
        storageId: this.selectedStorage?.id ?? undefined,
        typeZone: this.selectedTypeZone ?? undefined,
      })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.loading.set(false)))
      .subscribe({
        next: (res: HttpResponse<IRayon[]>) => this.onSuccess(res.body ?? [], res.headers),
        error: () => {},
      });
  }

  private onSuccess(data: IRayon[], headers: HttpHeaders): void {
    this.rayons.set(data);
    this.totalItems.set(Number(headers.get('X-Total-Count') ?? 0));
  }
}
