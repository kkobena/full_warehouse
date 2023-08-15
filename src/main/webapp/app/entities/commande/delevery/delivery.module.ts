import { NgModule } from '@angular/core';

import { SharedModule } from '../../../shared/shared.module';
import { AgGridModule } from 'ag-grid-angular';
import { RouterModule } from '@angular/router';
import { deliveryRoute } from './delivery.route';
import { DeliveryComponent } from './delivery.component';
import { CommandeStockEntryComponent } from '../commande-stock-entry.component';
import { CommandeBtnComponent } from '../btn/commande-btn.component';
import { FormLotComponent } from '../lot/form-lot.component';
import { ListLotComponent } from '../lot/list/list-lot.component';
import { EtiquetteComponent } from './etiquette/etiquette.component';
import { ImportDeliveryFormComponent } from './form/import/import-delivery-form.component';
import { ReceiptStatusComponent } from '../status/receipt-status.component';
import { EditProduitComponent } from './form/edit-produit/edit-produit.component';

@NgModule({
  declarations: [
    DeliveryComponent,
    CommandeStockEntryComponent,
    CommandeBtnComponent,
    FormLotComponent,
    ListLotComponent,

    EtiquetteComponent,
    ImportDeliveryFormComponent,
    ReceiptStatusComponent,
    EditProduitComponent,
  ],
  imports: [SharedModule, AgGridModule, RouterModule.forChild(deliveryRoute)],
})
export class DeliveryModule {}
