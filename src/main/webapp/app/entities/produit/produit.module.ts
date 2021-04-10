import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { ProduitComponent } from './produit.component';
import { ProduitDetailComponent } from './produit-detail.component';
import { ProduitUpdateComponent } from './produit-update.component';
import { ProduitDeleteDialogComponent } from './produit-delete-dialog.component';
import { produitRoute } from './produit.route';
import { ProduitImageDialogueComponent } from './produit-image-dialogue.component';
import { AgGridModule } from 'ag-grid-angular';
import { DetailFormDialogComponent } from './detail-form-dialog.component';
import { DeconditionDialogComponent } from './decondition.dialog.component';
@NgModule({
  imports: [WarehouseSharedModule, AgGridModule.withComponents([]), RouterModule.forChild(produitRoute)],
  declarations: [
    ProduitComponent,
    ProduitDetailComponent,
    ProduitUpdateComponent,
    ProduitDeleteDialogComponent,
    ProduitImageDialogueComponent,
    DetailFormDialogComponent,
    DeconditionDialogComponent,
  ],
  entryComponents: [ProduitDeleteDialogComponent, ProduitImageDialogueComponent, DetailFormDialogComponent, DeconditionDialogComponent],
})
export class WarehouseProduitModule {}
