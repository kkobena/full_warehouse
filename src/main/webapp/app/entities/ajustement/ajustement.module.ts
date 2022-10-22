import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { AjustementComponent } from './ajustement.component';
import { AjustementDetailComponent } from './ajustement-detail.component';
import { ajustementRoute } from './ajustement.route';
import { NgSelectModule } from '@ng-select/ng-select';
import { AgGridModule } from 'ag-grid-angular';

import { AjustementBtnRemoveComponent } from './btn-remove/ajustement-btn-remove.component';
@NgModule({
  imports: [
    WarehouseSharedModule,
    NgSelectModule,
    AgGridModule.withComponents([AjustementBtnRemoveComponent]),
    RouterModule.forChild(ajustementRoute),
  ],
  declarations: [AjustementComponent, AjustementDetailComponent, AjustementBtnRemoveComponent],
})
export class WarehouseAjustementModule {}
