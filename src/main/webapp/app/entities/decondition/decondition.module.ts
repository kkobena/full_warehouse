import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { DeconditionComponent } from './decondition.component';
import { deconditionRoute } from './decondition.route';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(deconditionRoute)],
  declarations: [DeconditionComponent],
})
export class WarehouseDeconditionModule {}
