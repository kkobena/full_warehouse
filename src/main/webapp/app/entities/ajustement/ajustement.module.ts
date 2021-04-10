import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { AjustementComponent } from './ajustement.component';
import { AjustementDetailComponent } from './ajustement-detail.component';
import { ajustementRoute } from './ajustement.route';
import { NgSelectModule } from '@ng-select/ng-select';
import { AgGridModule } from 'ag-grid-angular';
@NgModule({
  imports: [WarehouseSharedModule, NgSelectModule, AgGridModule.withComponents([]), RouterModule.forChild(ajustementRoute)],
  declarations: [AjustementComponent, AjustementDetailComponent],
})
export class WarehouseAjustementModule {}
