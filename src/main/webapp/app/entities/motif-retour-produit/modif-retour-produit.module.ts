import { NgModule } from '@angular/core';

import { WarehouseSharedModule } from '../../shared/shared.module';
import { RouterModule } from '@angular/router';
import { MotifRetourProduitComponent } from './motif-retour-produit.component';
import { motifRetourProduitRoute } from './motif-retour-produit.route';

@NgModule({
  declarations: [MotifRetourProduitComponent],

  imports: [WarehouseSharedModule, RouterModule.forChild(motifRetourProduitRoute)],
})
export class ModifRetourProduitModule {}
