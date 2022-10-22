import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { CommandeComponent } from './commande.component';
import { CommandeDetailComponent } from './commande-detail.component';
import { CommandeUpdateComponent } from './commande-update.component';
import { CommandeDeleteDialogComponent } from './commande-delete-dialog.component';
import { commandeRoute } from './commande.route';
import { NgSelectModule } from '@ng-select/ng-select';
import { CommandeEnCoursResponseDialogComponent } from './commande-en-cours-response-dialog.component';
import { CommandeImportResponseDialogComponent } from './commande-import-response-dialog.component';
import { ImportationNewCommandeComponent } from './importation-new-commande.component';
import { CommandeStockEntryComponent } from './commande-stock-entry.component';
import { AgGridModule } from 'ag-grid-angular';
import { CommandeBtnComponent } from './btn/commande-btn.component';

@NgModule({
  imports: [
    WarehouseSharedModule,
    NgSelectModule,
    AgGridModule.withComponents([CommandeBtnComponent]),
    RouterModule.forChild(commandeRoute),
  ],
  declarations: [
    CommandeComponent,
    CommandeDetailComponent,
    CommandeUpdateComponent,
    CommandeDeleteDialogComponent,
    CommandeEnCoursResponseDialogComponent,
    CommandeImportResponseDialogComponent,
    ImportationNewCommandeComponent,
    CommandeStockEntryComponent,
    CommandeBtnComponent,
  ],
  entryComponents: [
    CommandeDeleteDialogComponent,
    CommandeEnCoursResponseDialogComponent,
    CommandeImportResponseDialogComponent,
    ImportationNewCommandeComponent,
  ],
})
export class WarehouseCommandeModule {}
