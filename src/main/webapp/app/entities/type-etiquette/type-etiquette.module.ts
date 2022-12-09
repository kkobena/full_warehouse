import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from 'app/shared/shared.module';

import { typeEtiquetteRoute } from './type-etiquette.route';
import { TypeEtiquetteComponent } from './type-etiquette.component';

@NgModule({
  imports: [SharedModule, RouterModule.forChild(typeEtiquetteRoute)],
  declarations: [TypeEtiquetteComponent],
})
export class WarehouseTypeEtiquetteModule {}
