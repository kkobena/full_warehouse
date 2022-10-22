import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { WarehouseSharedModule } from 'app/shared/shared.module';
import { gammeProduitRoute } from './gamme-produit.route';
import { GammeProduitComponent } from './gamme-produit.component';
import { FormGammeComponent } from './form-gamme/form-gamme.component';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(gammeProduitRoute)],
  declarations: [GammeProduitComponent, FormGammeComponent],
  entryComponents: [FormGammeComponent],
})
export class WarehouseGammeProduitModule {}
