import {Component} from '@angular/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';


import {IStoreInventory} from 'app/shared/model/store-inventory.model';
import {StoreInventoryService} from './store-inventory.service';

@Component({
  templateUrl: './store-inventory-delete-dialog.component.html',
})
export class StoreInventoryDeleteDialogComponent {
  storeInventory?: IStoreInventory;

  constructor(
    protected storeInventoryService: StoreInventoryService,
    public activeModal: NgbActiveModal
  ) {
  }

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.storeInventoryService.delete(id).subscribe(() => {
      this.activeModal.close();
    });
  }
}
