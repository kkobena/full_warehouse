import {Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ButtonComponent, CardComponent, SelectComponent} from '../../../../shared/ui';
import {InventoryApiService} from '../../data-access/services/inventory-api.service';
import {StorageService} from '../../../../entities/storage/storage.service';
import {RayonService} from '../../../../entities/rayon/rayon.service';
import {IStorage} from '../../../../shared/model/magasin.model';
import {IRayon} from '../../../../shared/model/rayon.model';
import {TauriPrinterService} from '../../../../shared/services/tauri-printer.service';
import {handleBlobForTauri} from '../../../../shared/util/tauri-util';
import {LINE_FILTERS} from '../../models';
import {ConfigurationService} from '../../../../shared/configuration.service';

const GROUP_BY_OPTIONS = [
  {value: 'RAYON', label: 'Grouper par rayon'},
//  {value: 'STORAGE', label: 'Grouper par emplacement'},
  {value: 'FAMILLY', label: 'Grouper par famille'},
  {value: 'NONE', label: 'Sans regroupement'},
];

@Component({
  selector: 'app-inventory-export-modal',
  imports: [CommonModule, FormsModule, ButtonComponent, SelectComponent, CardComponent],
  templateUrl: './inventory-export-modal.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './inventory-export-modal.component.scss',
})
export class InventoryExportModalComponent implements OnInit {
  readonly activeModal = inject(NgbActiveModal);
  /** Set by opener */
  inventoryId!: number;
  inventoryDescription?: string;
  gestionLot = false;
  exportGroupBy = 'RAYON';
  selectedFilter = 'NONE';
  selectedStorageId: number | null = null;
  selectedRayonId: number | null = null;
  search = '';
  storages = signal<IStorage[]>([]);
  rayons = signal<IRayon[]>([]);
  exporting = signal(false);
  errorMessage = signal<string | null>(null);
  readonly groupByOptions = GROUP_BY_OPTIONS;
  readonly lineFilterOptions = LINE_FILTERS;
  private readonly api = inject(InventoryApiService);
  private readonly storageService = inject(StorageService);
  private readonly rayonService = inject(RayonService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly configService = inject(ConfigurationService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.storageService
      .fetchUserStorages()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({next: resp => this.storages.set(resp.body ?? [])});
    this.configService.find('APP_GESTION_LOT_INVENTAIRE')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({next: resp => this.gestionLot = resp.body?.value === '1'});
  }

  onStorageChange(storageId: number | null): void {
    this.selectedRayonId = null;
    this.rayons.set([]);
    if (storageId) {
      this.rayonService
        .query({storageId, size: 9999})
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({next: resp => this.rayons.set(resp.body ?? [])});
    }
  }

  export(): void {
    this.exporting.set(true);
    this.errorMessage.set(null);
    const filterParams: Record<string, any> = {};
    if (this.search) {
      filterParams['search'] = this.search;
    }
    if (this.selectedStorageId) {
      filterParams['storageId'] = this.selectedStorageId;
    }
    if (this.selectedRayonId) {
      filterParams['rayonId'] = this.selectedRayonId;
    }
    if (this.selectedFilter !== 'NONE') {
      filterParams['selectedFilter'] = this.selectedFilter;
    }
    this.api
      .exportToPdf(this.inventoryId, this.exportGroupBy as any, filterParams, this.gestionLot)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          if (this.tauriPrinterService.isRunningInTauri()) {
            handleBlobForTauri(blob, `inventaire-${this.inventoryId}`);
          } else {
            window.open(URL.createObjectURL(blob));
          }
          this.exporting.set(false);
          this.activeModal.close();
        },
        error: () => {
          this.exporting.set(false);
          this.errorMessage.set("Échec de l'export PDF");
        },
      });
  }

  skip(): void {
    this.activeModal.close();
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}
