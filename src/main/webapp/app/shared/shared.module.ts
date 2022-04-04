import { CUSTOM_ELEMENTS_SCHEMA, NgModule, NO_ERRORS_SCHEMA } from '@angular/core';
import { WarehouseSharedLibsModule } from './shared-libs.module';
import { FindLanguageFromKeyPipe } from './language/find-language-from-key.pipe';
import { AlertComponent } from './alert/alert.component';
import { AlertErrorComponent } from './alert/alert-error.component';
import { LoginModalComponent } from './login/login.component';
import { HasAnyAuthorityDirective } from './auth/has-any-authority.directive';
import { AgGridModule } from 'ag-grid-angular';
import { AlertInfoComponent } from './alert/alert-info.component';
import { NgSelectModule } from '@ng-select/ng-select';
import { UninsuredCustomerFormComponent } from 'app/entities/customer/uninsured-customer-form/uninsured-customer-form.component';
import { FormTiersPayantComponent } from 'app/entities/tiers-payant/form-tiers-payant/form-tiers-payant.component';
import { FormAssuredCustomerComponent } from 'app/entities/customer/form-assured-customer/form-assured-customer.component';
import { FormAyantDroitComponent } from 'app/entities/customer/form-ayant-droit/form-ayant-droit.component';
import { FormClientTierPayantComponent } from 'app/entities/customer/form-client-tier-payant/form-client-tier-payant.component';
import { AyantDroitCustomerListComponent } from 'app/entities/sales/ayant-droit-customer-list/ayant-droit-customer-list.component';

@NgModule({
  imports: [WarehouseSharedLibsModule, NgSelectModule, AgGridModule.withComponents([])],
  declarations: [
    FindLanguageFromKeyPipe,
    AlertComponent,
    AlertInfoComponent,
    AlertErrorComponent,
    LoginModalComponent,
    HasAnyAuthorityDirective,
    UninsuredCustomerFormComponent,
    FormTiersPayantComponent,
    FormAssuredCustomerComponent,
    FormAyantDroitComponent,
    FormClientTierPayantComponent,
    AyantDroitCustomerListComponent,
  ],
  entryComponents: [
    LoginModalComponent,
    AlertInfoComponent,
    UninsuredCustomerFormComponent,
    FormTiersPayantComponent,
    FormAssuredCustomerComponent,
    AyantDroitCustomerListComponent,
  ],
  exports: [
    WarehouseSharedLibsModule,
    FindLanguageFromKeyPipe,
    AlertComponent,
    UninsuredCustomerFormComponent,
    AlertErrorComponent,
    LoginModalComponent,
    HasAnyAuthorityDirective,
    FormTiersPayantComponent,
    FormAssuredCustomerComponent,
    FormAyantDroitComponent,
    FormClientTierPayantComponent,
    AyantDroitCustomerListComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA],
})
export class WarehouseSharedModule {}
