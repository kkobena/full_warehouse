import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { IStoreInventory } from 'app/shared/model/store-inventory.model';
import { StoreInventoryService } from './store-inventory.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';

@Component({
  templateUrl: './store-inventory-delete-dialog.component.html',
  imports: [WarehouseCommonModule, FormsModule]
})
export class StoreInventoryDeleteDialogComponent {
  protected storeInventoryService = inject(StoreInventoryService);
  activeModal = inject(NgbActiveModal);

  storeInventory?: IStoreInventory;

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(): void {
    this.storeInventoryService.delete(this.storeInventory.id).subscribe(() => {
      this.activeModal.close();
    });
  }
}
