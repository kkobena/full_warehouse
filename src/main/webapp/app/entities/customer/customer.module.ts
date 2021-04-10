import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { CustomerComponent } from './customer.component';
import { CustomerDetailComponent } from './customer-detail.component';
import { CustomerUpdateComponent } from './customer-update.component';
import { CustomerDeleteDialogComponent } from './customer-delete-dialog.component';
import { customerRoute } from './customer.route';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(customerRoute)],
  declarations: [CustomerComponent, CustomerDetailComponent, CustomerUpdateComponent, CustomerDeleteDialogComponent],
  entryComponents: [CustomerDeleteDialogComponent],
})
export class WarehouseCustomerModule {}
