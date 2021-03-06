import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { familleProduitRoute } from './famille-produit.route';
import { FormFamilleComponent } from './form-famille/form-famille.component';
import { FamilleProduitComponent } from './famille-produit.component';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(familleProduitRoute)],
  declarations: [FormFamilleComponent, FamilleProduitComponent],
  entryComponents: [FormFamilleComponent],
})
export class WarehouseFamilleProduitModule {}
