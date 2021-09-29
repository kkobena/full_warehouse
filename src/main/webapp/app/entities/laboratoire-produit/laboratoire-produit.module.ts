import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { WarehouseSharedModule } from 'app/shared/shared.module';
import { laboratoireProduitRoute } from './laboratoire-produit.route';
import { LaboratoireProduitComponent } from './laboratoire-produit.component';
import { FormLaboratoireComponent } from './form-laboratoire/form-laboratoire.component';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(laboratoireProduitRoute)],
  declarations: [LaboratoireProduitComponent, FormLaboratoireComponent],
  entryComponents: [FormLaboratoireComponent],
})
export class WarehouseLaboratoireProduitModule {}
