import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { PaymentModeComponent } from './payment-mode.component';
import { PaymentModeDetailComponent } from './payment-mode-detail.component';
import { PaymentModeUpdateComponent } from './payment-mode-update.component';
import { PaymentModeDeleteDialogComponent } from './payment-mode-delete-dialog.component';
import { paymentModeRoute } from './payment-mode.route';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(paymentModeRoute)],
  declarations: [PaymentModeComponent, PaymentModeDetailComponent, PaymentModeUpdateComponent, PaymentModeDeleteDialogComponent],
  entryComponents: [PaymentModeDeleteDialogComponent],
})
export class WarehousePaymentModeModule {}
