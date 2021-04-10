import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { SalesComponent } from './sales.component';
import { SalesDetailComponent } from './sales-detail.component';
import { SalesUpdateComponent } from './sales-update.component';
import { SalesDeleteDialogComponent } from './sales-delete-dialog.component';
import { salesRoute } from './sales.route';
import { AgGridModule } from 'ag-grid-angular';
import { NgSelectModule } from '@ng-select/ng-select';
import { PackDialogueComponent } from './pack-dialogue.component';
import { DetailDialogueComponent } from './detail-dialogue.component';
import { BtnRemoveComponent } from './btn-remove/btn-remove.component';
@NgModule({
  imports: [WarehouseSharedModule, NgSelectModule, AgGridModule.withComponents([BtnRemoveComponent]), RouterModule.forChild(salesRoute)],
  declarations: [
    SalesComponent,
    SalesDetailComponent,
    SalesUpdateComponent,
    SalesDeleteDialogComponent,
    PackDialogueComponent,
    DetailDialogueComponent,
    BtnRemoveComponent,
  ],
  entryComponents: [SalesDeleteDialogComponent, PackDialogueComponent, DetailDialogueComponent],
})
export class WarehouseSalesModule {}
