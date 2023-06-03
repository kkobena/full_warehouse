import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from 'app/shared/shared.module';
import { tiersPayantRoute } from './tiers-payant.route';
import { TiersPayantComponent } from './tiers-payant.component';

@NgModule({
  imports: [SharedModule, RouterModule.forChild(tiersPayantRoute)],
  declarations: [TiersPayantComponent],
})
export class WarehouseTiersPayantModule {}
