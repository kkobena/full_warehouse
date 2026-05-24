import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { TooltipModule } from 'primeng/tooltip';
import { Select } from 'primeng/select';
import { RayonApiService } from '../../data-access/services/rayon-api.service';
import { IRayon } from '../../models/rayon.model';
import { IProduit } from '../../../../shared/model';
import { StorageService } from '../../../../entities/storage/storage.service';
import { MagasinService } from '../../../../entities/magasin/magasin.service';

interface StorageOption {
  storageId: number;
  storageLibelle: string;
  storageType: string;
}

export interface RayonAssignResult {
  produitId: number;
  rayonId: number;
}

@Component({
  selector: 'app-rayon-assign-form',
  templateUrl: './rayon-assign-form.component.html',
  styleUrl: './rayon-assign-form.component.scss',
  imports: [FormsModule, ButtonModule, InputTextModule, IconField, InputIcon, TooltipModule, Select],
})
export class RayonAssignFormComponent implements OnInit {
  produit!: IProduit;
  mode: 'move' | 'add-storage' = 'move';
  title = 'Choisir un emplacement';
  currentStorageId?: number;
  currentRayonId?: number;
  currentRayonIsSans = false;
  occupiedRealStorageIds: number[] = [];

  protected allRayons = signal<IRayon[]>([]);
  protected allStorages = signal<StorageOption[]>([]);
  protected loading = signal(false);
  protected searchText = signal('');
  protected selectedRayon = signal<IRayon | null>(null);
  protected selectedStorage = signal<StorageOption | null>(null);

  private readonly modal = inject(NgbActiveModal);
  private readonly rayonApi = inject(RayonApiService);
  private readonly storageService = inject(StorageService);
  private readonly magasinService = inject(MagasinService);

  /** Stockages disponibles : tous les stockages sauf ceux déjà occupés en mode add-storage. */
  protected readonly availableStorages = computed<StorageOption[]>(() =>
    this.allStorages().filter(s =>
      this.mode !== 'add-storage' || !this.occupiedRealStorageIds.includes(s.storageId),
    ),
  );

  /** Rayons du stockage sélectionné (déjà filtrés par storageId côté API), filtrés par texte. */
  protected readonly filteredRayons = computed<IRayon[]>(() => {
    if (!this.selectedStorage()) return [];
    const search = this.searchText().toLowerCase();
    return this.allRayons().filter(r => {
      if (r.code === 'SANS') return false;
      if (r.id === this.currentRayonId) return false;
      return !(search && !(
        r.code?.toLowerCase().includes(search) ||
        r.libelle?.toLowerCase().includes(search)
      ));
    });
  });

  protected readonly hasNoRayons = computed(
    () => !this.loading() && !!this.selectedStorage() && this.filteredRayons().length === 0,
  );

  ngOnInit(): void {
    // Charge uniquement les stockages — les rayons sont chargés à la sélection du stockage.
    this.loading.set(true);
    this.magasinService.findCurrentUserMagasin().then(magasin => {
      this.storageService.fetchStorages({ magasinId: magasin.id }).subscribe({
        next: res => {
          this.allStorages.set(
            (res.body ?? [])
              .filter(s => s.id != null)
              .map(s => ({
                storageId: s.id!,
                storageLibelle: s.name ?? `Stockage ${s.id}`,
                storageType: s.storageType ?? s.type ?? '',
              })),
          );
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
    }).catch(() => this.loading.set(false));
  }

  protected onStorageChange(storage: StorageOption | null): void {
    this.selectedStorage.set(storage);
    this.selectedRayon.set(null);
    this.searchText.set('');
    this.allRayons.set([]);
    if (!storage) return;
    this.loading.set(true);
    this.rayonApi.query({ storageId: storage.storageId, size: 500 }).subscribe({
      next: res => { this.allRayons.set(res.body ?? []); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  protected onSearchChange(value: string): void {
    this.searchText.set(value);
  }

  protected selectRayon(rayon: IRayon): void {
    this.selectedRayon.set(rayon);
  }

  protected onSansEmplacement(): void {
    // currentStorageId est disponible ici (positionné par le parent après open()).
    this.rayonApi.query({ storageId: this.currentStorageId, size: 500 }).subscribe({
      next: res => {
        const sansRayon = (res.body ?? []).find(r => r.code === 'SANS');
        if (sansRayon?.id) {
          this.modal.close({ produitId: this.produit.id!, rayonId: sansRayon.id } satisfies RayonAssignResult);
        }
      },
    });
  }

  protected confirm(): void {
    const rayon = this.selectedRayon();
    if (!rayon?.id) return;
    this.modal.close({ produitId: this.produit.id!, rayonId: rayon.id } satisfies RayonAssignResult);
  }

  protected dismiss(): void {
    this.modal.dismiss();
  }
}
