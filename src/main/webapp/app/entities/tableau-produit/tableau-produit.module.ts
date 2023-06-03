import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from 'app/shared/shared.module';
import { tableauProduitRoute } from './tableau-produit.route';
import { TableauProduitComponent } from './tableau-produit.component';
import { ProduitAssociesComponent } from './produits/produit-associes.component';

@NgModule({
  imports: [SharedModule, RouterModule.forChild(tableauProduitRoute)],
  declarations: [TableauProduitComponent, ProduitAssociesComponent],
})
export class WarehouseTableauProduitModule {}
