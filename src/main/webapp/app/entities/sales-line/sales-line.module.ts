import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { SalesLineComponent } from './sales-line.component';
import { SalesLineDetailComponent } from './sales-line-detail.component';
import { SalesLineUpdateComponent } from './sales-line-update.component';
import { SalesLineDeleteDialogComponent } from './sales-line-delete-dialog.component';
import { salesLineRoute } from './sales-line.route';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(salesLineRoute)],
  declarations: [SalesLineComponent, SalesLineDetailComponent, SalesLineUpdateComponent, SalesLineDeleteDialogComponent],
  entryComponents: [SalesLineDeleteDialogComponent],
})
export class WarehouseSalesLineModule {}
