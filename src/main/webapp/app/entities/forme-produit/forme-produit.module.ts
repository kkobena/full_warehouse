import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { FormeProduitComponent } from './forme-produit.component';
import { formeRoute } from './forme.route';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(formeRoute)],
  declarations: [FormeProduitComponent],
})
export class WarehouseFormeProduitModule {}
