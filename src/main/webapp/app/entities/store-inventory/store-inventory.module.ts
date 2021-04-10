import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { StoreInventoryComponent } from './store-inventory.component';
import { StoreInventoryDetailComponent } from './store-inventory-detail.component';
import { StoreInventoryUpdateComponent } from './store-inventory-update.component';
import { StoreInventoryDeleteDialogComponent } from './store-inventory-delete-dialog.component';
import { storeInventoryRoute } from './store-inventory.route';
import { AgGridModule } from 'ag-grid-angular';

@NgModule({
  imports: [WarehouseSharedModule, AgGridModule.withComponents([]), RouterModule.forChild(storeInventoryRoute)],
  declarations: [
    StoreInventoryComponent,
    StoreInventoryDetailComponent,
    StoreInventoryUpdateComponent,
    StoreInventoryDeleteDialogComponent,
  ],
  entryComponents: [StoreInventoryDeleteDialogComponent],
})
export class WarehouseStoreInventoryModule {}
