import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';

import { tvaRoute } from './tva.route';
import { TvaComponent } from './tva.component';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(tvaRoute)],
  declarations: [TvaComponent],
})
export class WarehouseTvaModule {}
