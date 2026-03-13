import { Component, DestroyRef, effect, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Button } from 'primeng/button';
import { ProgressSpinner } from 'primeng/progressspinner';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InventoryApiService } from '../../data-access/services/inventory-api.service';
import { InventoryStore } from '../../data-access/store/inventory.store';
import { ImportResultRecord } from '../../models';

@Component({
  selector: 'app-inventory-import-modal',
  imports: [CommonModule, Button, ProgressSpinner],
  templateUrl: './inventory-import-modal.component.html',
  styleUrl: './inventory-import-modal.component.scss',
})
export class InventoryImportModalComponent implements OnInit {
  readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(InventoryApiService);
  private readonly store = inject(InventoryStore);
  private readonly destroyRef = inject(DestroyRef);

  inventoryId!: number;
  selectedFile = signal<File | null>(null);
  isUploading = signal(false);
  importResult = signal<ImportResultRecord | null>(null);
  errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.store.setLastImportResult(null);
    this.store.setError(null);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile.set(input.files[0]);
      this.importResult.set(null);
      this.errorMessage.set(null);
    }
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file) {
      return;
    }

    this.isUploading.set(true);
    this.errorMessage.set(null);
    this.importResult.set(null);

    this.api
      .importCsv(this.inventoryId, file)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: resp => {
          this.isUploading.set(false);
          const result = resp.body ?? null;
          this.importResult.set(result);
          this.store.setLastImportResult(result);
          this.store.emitEvent('IMPORT_COMPLETED', result);
          // Refresh progress after import
          this.api.getProgress(this.inventoryId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: progressResp => {
                this.store.setProgress(progressResp.body ?? null);
                this.store.emitEvent('PROGRESS_UPDATED', progressResp.body);
              },
              error: () => {},
            });
        },
        error: err => {
          this.isUploading.set(false);
          this.errorMessage.set(err?.error?.message ?? err?.message ?? "Erreur lors de l'import");
        },
      });
  }

  close(): void {
    this.activeModal.close(this.importResult());
  }

  cancel(): void {
    this.activeModal.dismiss();
  }
}
