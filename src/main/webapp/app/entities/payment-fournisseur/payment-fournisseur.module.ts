import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { WarehouseSharedModule } from 'app/shared/shared.module';
import { PaymentFournisseurComponent } from './payment-fournisseur.component';
import { PaymentFournisseurDetailComponent } from './payment-fournisseur-detail.component';
import { PaymentFournisseurUpdateComponent } from './payment-fournisseur-update.component';
import { PaymentFournisseurDeleteDialogComponent } from './payment-fournisseur-delete-dialog.component';
import { paymentFournisseurRoute } from './payment-fournisseur.route';

@NgModule({
  imports: [WarehouseSharedModule, RouterModule.forChild(paymentFournisseurRoute)],
  declarations: [
    PaymentFournisseurComponent,
    PaymentFournisseurDetailComponent,
    PaymentFournisseurUpdateComponent,
    PaymentFournisseurDeleteDialogComponent,
  ],
  entryComponents: [PaymentFournisseurDeleteDialogComponent],
})
export class WarehousePaymentFournisseurModule {}
