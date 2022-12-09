import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from 'app/shared/shared.module';

import { rayonRoute } from './rayon.route';
import { RayonComponent } from './rayon.component';
import { FormRayonComponent } from './form-rayon/form-rayon.component';

@NgModule({
  imports: [SharedModule, RouterModule.forChild(rayonRoute)],
  declarations: [RayonComponent, FormRayonComponent],
  entryComponents: [FormRayonComponent],
})
export class WarehouseRayonModule {}
