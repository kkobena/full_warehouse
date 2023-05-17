import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from 'app/shared/shared.module';
import { CommandeComponent } from './commande.component';
import { CommandeDetailComponent } from './commande-detail.component';
import { CommandeDeleteDialogComponent } from './commande-delete-dialog.component';
import { commandeRoute } from './commande.route';
import { CommandeEnCoursResponseDialogComponent } from './commande-en-cours-response-dialog.component';
import { CommandeImportResponseDialogComponent } from './commande-import-response-dialog.component';
import { ImportationNewCommandeComponent } from './importation-new-commande.component';
import { AgGridModule } from 'ag-grid-angular';
import { CommandeUpdateComponent } from './commande-update.component';
import { DeliveryModalComponent } from './delevery/form/delivery-modal.component';

@NgModule({
  imports: [SharedModule, AgGridModule, RouterModule.forChild(commandeRoute)],
  declarations: [
    CommandeComponent,
    CommandeDetailComponent,
    CommandeUpdateComponent,
    CommandeDeleteDialogComponent,
    CommandeEnCoursResponseDialogComponent,
    CommandeImportResponseDialogComponent,
    ImportationNewCommandeComponent,
    DeliveryModalComponent,
  ],
})
export class WarehouseCommandeModule {}
