import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from 'app/shared/shared.module';
import { PaymentComponent } from './payment.component';
import { PaymentDetailComponent } from './payment-detail.component';

import { PaymentDeleteDialogComponent } from './payment-delete-dialog.component';
import { paymentRoute } from './payment.route';

@NgModule({
  imports: [SharedModule, RouterModule.forChild(paymentRoute)],
  declarations: [PaymentComponent, PaymentDetailComponent, PaymentDeleteDialogComponent],
  entryComponents: [PaymentDeleteDialogComponent],
})
export class WarehousePaymentModule {}
