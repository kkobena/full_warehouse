import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';

import {SharedModule} from 'app/shared/shared.module';
import {ProduitComponent} from './produit.component';
import {ProduitDetailComponent} from './produit-detail.component';
import {ProduitDeleteDialogComponent} from './produit-delete-dialog.component';
import {produitRoute} from './produit.route';
import {AgGridModule} from 'ag-grid-angular';
import {DetailFormDialogComponent} from './detail-form-dialog.component';
import {DeconditionDialogComponent} from './decondition.dialog.component';
import {DetailProduitFormComponent} from './detail-produit-form/detail-produit-form.component';
import {FormProduitFournisseurComponent} from './form-produit-fournisseur/form-produit-fournisseur.component';
import {FormRayonProduitComponent} from './form-rayon-produit/form-rayon-produit.component';

@NgModule({
  imports: [SharedModule, AgGridModule, RouterModule.forChild(produitRoute)],
  declarations: [
    ProduitComponent,
    ProduitDetailComponent,
    ProduitDeleteDialogComponent,
    DetailFormDialogComponent,
    DeconditionDialogComponent,
    DetailProduitFormComponent,
    FormProduitFournisseurComponent,
    FormRayonProduitComponent,
  ],
  entryComponents: [
    ProduitDeleteDialogComponent,
    DetailFormDialogComponent,
    DeconditionDialogComponent,
    FormProduitFournisseurComponent,
    FormRayonProduitComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class WarehouseProduitModule {
}
