import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from 'app/shared/shared.module';

import { tvaRoute } from './tva.route';
import { TvaComponent } from './tva.component';

@NgModule({
  imports: [SharedModule, RouterModule.forChild(tvaRoute)],
  declarations: [TvaComponent],
})
export class WarehouseTvaModule {}
