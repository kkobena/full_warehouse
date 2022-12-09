import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from 'app/shared/shared.module';
import { InventoryTransactionComponent } from './inventory-transaction.component';
import { InventoryTransactionDetailComponent } from './inventory-transaction-detail.component';
import { inventoryTransactionRoute } from './inventory-transaction.route';
import { AgGridModule } from 'ag-grid-angular';
@NgModule({
  imports: [SharedModule, AgGridModule, RouterModule.forChild(inventoryTransactionRoute)],
  declarations: [InventoryTransactionComponent, InventoryTransactionDetailComponent],
})
export class WarehouseInventoryTransactionModule {}
