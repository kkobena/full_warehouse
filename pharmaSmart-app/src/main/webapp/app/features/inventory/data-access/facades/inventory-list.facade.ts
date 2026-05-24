import { inject, Injectable } from '@angular/core';
import { InventoryApiService } from '../services/inventory-api.service';
import { InventoryStore } from '../store/inventory.store';
import { StoreInventoryCreateRecord } from '../../models';

@Injectable({ providedIn: 'root' })
export class InventoryListFacade {
  private readonly api = inject(InventoryApiService);
  readonly store = inject(InventoryStore);

  // Expose store state
  readonly inventories = this.store.inventories;
  readonly totalInventories = this.store.totalInventories;
  readonly loadingList = this.store.loadingList;
  readonly currentInventory = this.store.currentInventory;
  readonly lastEvent = this.store.lastEvent;

  loadList(params: any): void {
    this.store.setLoadingList(true);
    this.store.setError(null);
    this.api.list(params).subscribe({
      next: resp => {
        const total = parseInt(resp.headers.get('X-Total-Count') ?? '0', 10);
        this.store.setInventories(resp.body ?? [], total);
        this.store.setLoadingList(false);
      },
      error: err => {
        this.store.setError(err?.message ?? 'Erreur lors du chargement des inventaires');
        this.store.setLoadingList(false);
      },
    });
  }

  createInventory(record: StoreInventoryCreateRecord): void {
    this.store.setLoadingList(true);
    this.api.create(record).subscribe({
      next: resp => {
        this.store.setLoadingList(false);
        this.store.emitEvent('INVENTORY_CREATED', resp.body);
      },
      error: err => {
        this.store.setError(err?.message ?? "Erreur lors de la création de l'inventaire");
        this.store.setLoadingList(false);
      },
    });
  }

  deleteInventory(id: number): void {
    this.store.setLoadingList(true);
    this.api.delete(id).subscribe({
      next: () => {
        this.store.setLoadingList(false);
        this.store.emitEvent('INVENTORY_DELETED', { id });
      },
      error: err => {
        this.store.setError(err?.message ?? "Erreur lors de la suppression de l'inventaire");
        this.store.setLoadingList(false);
      },
    });
  }

  closeInventory(id: number): void {
    this.store.setLoadingList(true);
    this.api.close(id).subscribe({
      next: resp => {
        this.store.setLoadingList(false);
        this.store.emitEvent('INVENTORY_CLOSED', { id, count: resp.body?.count });
      },
      error: err => {
        this.store.setError(err?.message ?? "Erreur lors de la clôture de l'inventaire");
        this.store.setLoadingList(false);
      },
    });
  }

  loadInventory(id: number): void {
    this.store.setLoadingList(true);
    this.api.get(id).subscribe({
      next: resp => {
        this.store.setCurrentInventory(resp.body ?? null);
        this.store.setLoadingList(false);
      },
      error: err => {
        this.store.setError(err?.message ?? "Erreur lors du chargement de l'inventaire");
        this.store.setLoadingList(false);
      },
    });
  }
}
