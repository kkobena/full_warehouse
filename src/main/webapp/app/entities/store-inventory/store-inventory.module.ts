import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from 'app/shared/shared.module';
import { StoreInventoryComponent } from './store-inventory.component';
import { StoreInventoryDetailComponent } from './store-inventory-detail.component';
import { StoreInventoryUpdateComponent } from './store-inventory-update.component';
import { StoreInventoryDeleteDialogComponent } from './store-inventory-delete-dialog.component';
import { storeInventoryRoute } from './store-inventory.route';
import { AgGridModule } from 'ag-grid-angular';
import { EnCoursComponent } from './en-cours/en-cours.component';
import { CloturesComponent } from './clotures/clotures.component';
import { InventoryFormComponent } from './inventory-form/inventory-form.component';
import { TableEditorDirective } from './table-editor.directive';

@NgModule({
  imports: [SharedModule, AgGridModule, RouterModule.forChild(storeInventoryRoute)],
  declarations: [
    StoreInventoryComponent,
    StoreInventoryDetailComponent,
    StoreInventoryUpdateComponent,
    StoreInventoryDeleteDialogComponent,
    EnCoursComponent,
    CloturesComponent,
    InventoryFormComponent,
    TableEditorDirective,
  ],
})
export class WarehouseStoreInventoryModule {}
