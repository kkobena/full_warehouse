import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { InventoryTransactionComponent } from './inventory-transaction.component';
import { InventoryTransactionDetailComponent } from './inventory-transaction-detail.component';
import { inventoryTransactionRoute } from './inventory-transaction.route';
import { AgGridModule } from 'ag-grid-angular';
@NgModule({
  imports: [WarehouseSharedModule, AgGridModule.withComponents([]), RouterModule.forChild(inventoryTransactionRoute)],
  declarations: [InventoryTransactionComponent, InventoryTransactionDetailComponent],
})
export class WarehouseInventoryTransactionModule {}
