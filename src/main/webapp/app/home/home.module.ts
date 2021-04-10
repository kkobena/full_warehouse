import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { HOME_ROUTE } from './home.route';
import { HomeComponent } from './home.component';
import { AgGridModule } from 'ag-grid-angular';
@NgModule({
  imports: [WarehouseSharedModule, AgGridModule.withComponents([]), RouterModule.forChild([HOME_ROUTE])],
  declarations: [HomeComponent],
})
export class WarehouseHomeModule {}
