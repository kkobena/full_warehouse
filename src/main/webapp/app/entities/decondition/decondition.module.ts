import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from 'app/shared/shared.module';
import { DeconditionComponent } from './decondition.component';
import { deconditionRoute } from './decondition.route';

@NgModule({
  imports: [SharedModule, RouterModule.forChild(deconditionRoute)],
  declarations: [DeconditionComponent],
})
export class WarehouseDeconditionModule {}
