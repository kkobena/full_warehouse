import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';

import {SharedModule} from 'app/shared/shared.module';

import {rayonRoute} from './rayon.route';
import {RayonComponent} from './rayon.component';
import {FormRayonComponent} from './form-rayon/form-rayon.component';

@NgModule({
  imports: [SharedModule, RouterModule.forChild(rayonRoute)],
  declarations: [RayonComponent, FormRayonComponent],
  entryComponents: [FormRayonComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class WarehouseRayonModule {
}
