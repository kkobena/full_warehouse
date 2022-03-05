import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { WarehouseSharedModule } from 'app/shared/shared.module';
import { tiersPayantRoute } from './tiers-payant.route';
import { TiersPayantComponent } from './tiers-payant.component';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(tiersPayantRoute)],
  declarations: [TiersPayantComponent],
  entryComponents: [],
})
export class WarehouseTiersPayantModule {}
