import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from 'app/shared/shared.module';
import { FormeProduitComponent } from './forme-produit.component';
import { formeRoute } from './forme.route';

@NgModule({
  imports: [SharedModule, RouterModule.forChild(formeRoute)],
  declarations: [FormeProduitComponent],
})
export class WarehouseFormeProduitModule {}
