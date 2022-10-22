import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IStoreInventory } from 'app/shared/model/store-inventory.model';
import { StoreInventoryService } from './store-inventory.service';

@Component({
  templateUrl: './store-inventory-delete-dialog.component.html',
})
export class StoreInventoryDeleteDialogComponent {
  storeInventory?: IStoreInventory;

  constructor(
    protected storeInventoryService: StoreInventoryService,
    public activeModal: NgbActiveModal,
    protected eventManager: JhiEventManager
  ) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.storeInventoryService.delete(id).subscribe(() => {
      this.eventManager.broadcast('storeInventoryListModification');
      this.activeModal.close();
    });
  }
}
