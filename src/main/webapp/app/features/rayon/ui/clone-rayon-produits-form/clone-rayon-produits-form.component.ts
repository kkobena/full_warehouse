import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonModule } from 'primeng/button';
import { Select } from 'primeng/select';
import { TooltipModule } from 'primeng/tooltip';
import { HttpResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { IRayon } from '../../models/rayon.model';
import { RayonApiService } from '../../data-access/services/rayon-api.service';
import { RayonProduitApiService } from '../../data-access/services/rayon-produit-api.service';
import { StorageService } from '../../../../entities/storage/storage.service';
import { Storage } from '../../../../entities/storage/storage.model';
import { MagasinService } from '../../../../entities/magasin/magasin.service';
import { NotificationService } from '../../../../shared/services/notification.service';
import { ErrorService } from '../../../../shared/error.service';
import { IResponseDto } from '../../../../shared/util/response-dto';

interface StorageRow {
  storage: Storage;
  rayons: IRayon[];
  selectedRayon: IRayon | null;
}

@Component({
  selector: 'app-clone-rayon-produits-form',
  templateUrl: './clone-rayon-produits-form.component.html',
  styleUrl: './clone-rayon-produits-form.component.scss',
  imports: [FormsModule, ButtonModule, Select, TooltipModule],
})
export class CloneRayonProduitsFormComponent implements OnInit {
  rayon!: IRayon;

  protected rows = signal<StorageRow[]>([]);
  protected loading = signal(false);
  protected saving = signal(false);

  protected readonly hasSelection = computed(() =>
    this.rows().some(r => r.selectedRayon != null),
  );

  private readonly modal = inject(NgbActiveModal);
  private readonly magasinService = inject(MagasinService);
  private readonly storageService = inject(StorageService);
  private readonly rayonApi = inject(RayonApiService);
  private readonly rayonProduitApi = inject(RayonProduitApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  // Filtre le stockage source — lisible au moment du rendu, après que le parent a positionné this.rayon.
  protected get availableRows(): StorageRow[] {
    return this.rows().filter(r => r.storage.id !== this.rayon?.storageId);
  }

  ngOnInit(): void {
    this.loading.set(true);
    this.magasinService.findCurrentUserMagasin().then(magasin => {
      this.storageService.fetchStorages({ magasinId: magasin.id }).subscribe({
        next: res => {
          const storages = (res.body ?? []).filter(s => s.id != null);
          if (!storages.length) { this.loading.set(false); return; }
          forkJoin<HttpResponse<IRayon[]>[]>(
            storages.map(s => this.rayonApi.query({ storageId: s.id!, size: 500 })),
          ).subscribe({
            next: (results: HttpResponse<IRayon[]>[]) => {
              this.rows.set(
                storages.map((s, i): StorageRow => ({
                  storage: s,
                  rayons: (results[i].body ?? []).filter(r => r.code !== 'SANS'),
                  selectedRayon: null,
                })),
              );
              this.loading.set(false);
            },
            error: () => this.loading.set(false),
          });
        },
        error: () => this.loading.set(false),
      });
    }).catch(() => this.loading.set(false));
  }

  protected onRayonSelect(storageId: number, rayon: IRayon | null): void {
    this.rows.update(rows =>
      rows.map(r => r.storage.id === storageId ? { ...r, selectedRayon: rayon } : r),
    );
  }

  protected confirm(): void {
    const targetRayonIds = this.rows()
      .filter(r => r.selectedRayon?.id != null)
      .map(r => r.selectedRayon!.id!);
    if (!targetRayonIds.length) return;
    this.saving.set(true);
    this.rayonProduitApi.cloneFromRayon(this.rayon.id!, targetRayonIds).subscribe({
      next: res => {
        this.saving.set(false);
        this.modal.close(res.body as IResponseDto);
      },
      error: err => {
        this.saving.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err));
      },
    });
  }

  protected dismiss(): void {
    this.modal.dismiss();
  }
}
