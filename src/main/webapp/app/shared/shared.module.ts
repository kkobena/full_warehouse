import {NgModule} from '@angular/core';

import {SharedLibsModule} from './shared-libs.module';
import {FindLanguageFromKeyPipe} from './language/find-language-from-key.pipe';
import {TranslateDirective} from './language/translate.directive';
import {AlertComponent} from './alert/alert.component';
import {AlertErrorComponent} from './alert/alert-error.component';
import {HasAnyAuthorityDirective} from './auth/has-any-authority.directive';
import {DurationPipe} from './date/duration.pipe';
import {FormatMediumDatetimePipe} from './date/format-medium-datetime.pipe';
import {FormatMediumDatePipe} from './date/format-medium-date.pipe';
import {SortByDirective} from './sort/sort-by.directive';
import {SortDirective} from './sort/sort.directive';
import {ItemCountComponent} from './pagination/item-count.component';
import {FilterComponent} from './filter/filter.component';
import {AlertInfoComponent} from "./alert/alert-info.component";
import {
  UninsuredCustomerFormComponent
} from "../entities/customer/uninsured-customer-form/uninsured-customer-form.component";
import {FormTiersPayantComponent} from "../entities/tiers-payant/form-tiers-payant/form-tiers-payant.component";
import {FormAssuredCustomerComponent} from "../entities/customer/form-assured-customer/form-assured-customer.component";
import {FormAyantDroitComponent} from "../entities/customer/form-ayant-droit/form-ayant-droit.component";
import {
  FormClientTierPayantComponent
} from "../entities/customer/form-client-tier-payant/form-client-tier-payant.component";
import {
  AyantDroitCustomerListComponent
} from "../entities/sales/ayant-droit-customer-list/ayant-droit-customer-list.component";
import {
  TiersPayantCustomerListComponent
} from "../entities/customer/tiers-payant-customer-list/tiers-payant-customer-list.component";

@NgModule({
  imports: [SharedLibsModule],
  declarations: [
    FindLanguageFromKeyPipe,
    TranslateDirective,
    AlertComponent,
    AlertErrorComponent,
    HasAnyAuthorityDirective,
    DurationPipe,
    FormatMediumDatetimePipe,
    FormatMediumDatePipe,
    SortByDirective,
    SortDirective,
    ItemCountComponent,
    FilterComponent,
    AlertInfoComponent,
    UninsuredCustomerFormComponent,
    FormTiersPayantComponent,
    FormAssuredCustomerComponent,
    FormAyantDroitComponent,
    FormClientTierPayantComponent,
    AyantDroitCustomerListComponent,
    TiersPayantCustomerListComponent,
    AlertInfoComponent,
  ],
  entryComponents: [

    AlertInfoComponent,
    UninsuredCustomerFormComponent,
    FormTiersPayantComponent,
    FormAssuredCustomerComponent,
    AyantDroitCustomerListComponent,
    TiersPayantCustomerListComponent,
  ],
  exports: [
    SharedLibsModule,
    FindLanguageFromKeyPipe,
    TranslateDirective,
    AlertComponent,
    AlertErrorComponent,
    HasAnyAuthorityDirective,
    DurationPipe,
    FormatMediumDatetimePipe,
    FormatMediumDatePipe,
    SortByDirective,
    SortDirective,
    ItemCountComponent,
    FilterComponent,
    UninsuredCustomerFormComponent,
    FormTiersPayantComponent,
    FormAssuredCustomerComponent,
    AyantDroitCustomerListComponent,
    TiersPayantCustomerListComponent,
    AlertInfoComponent,
  ],
})
export class SharedModule {
}
