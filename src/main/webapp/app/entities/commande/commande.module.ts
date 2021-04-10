import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { CommandeComponent } from './commande.component';
import { CommandeDetailComponent } from './commande-detail.component';
import { CommandeUpdateComponent } from './commande-update.component';
import { CommandeDeleteDialogComponent } from './commande-delete-dialog.component';
import { commandeRoute } from './commande.route';
import { AgGridModule } from 'ag-grid-angular';
@NgModule({
  imports: [WarehouseSharedModule, AgGridModule.withComponents([]), RouterModule.forChild(commandeRoute)],
  declarations: [CommandeComponent, CommandeDetailComponent, CommandeUpdateComponent, CommandeDeleteDialogComponent],
  entryComponents: [CommandeDeleteDialogComponent],
})
export class WarehouseCommandeModule {}
