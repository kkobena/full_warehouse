import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';

import {SharedModule} from 'app/shared/shared.module';
import {SalesComponent} from './sales.component';
import {SalesDetailComponent} from './sales-detail.component';
import {SalesUpdateComponent} from './sales-update.component';
import {SalesDeleteDialogComponent} from './sales-delete-dialog.component';
import {salesRoute} from './sales.route';
import {AgGridModule} from 'ag-grid-angular';
import {NgSelectModule} from '@ng-select/ng-select';
import {PackDialogueComponent} from './pack-dialogue.component';
import {DetailDialogueComponent} from './detail-dialogue.component';
import {BtnRemoveComponent} from './btn-remove/btn-remove.component';
import {UninsuredCustomerListComponent} from './uninsured-customer-list/uninsured-customer-list.component';
import {PresaleComponent} from './presale/presale.component';
import {VenteEnCoursComponent} from './vente-en-cours/vente-en-cours.component';
import {AssuredCustomerListComponent} from './assured-customer-list/assured-customer-list.component';

@NgModule({
  imports: [SharedModule, NgSelectModule, AgGridModule, RouterModule.forChild(salesRoute)],
  declarations: [
    SalesComponent,
    SalesDetailComponent,
    SalesUpdateComponent,
    SalesDeleteDialogComponent,
    PackDialogueComponent,
    DetailDialogueComponent,
    BtnRemoveComponent,
    UninsuredCustomerListComponent,
    PresaleComponent,
    VenteEnCoursComponent,
    AssuredCustomerListComponent,
  ],
  entryComponents: [SalesDeleteDialogComponent, PackDialogueComponent, DetailDialogueComponent],
})
export class WarehouseSalesModule {
}
