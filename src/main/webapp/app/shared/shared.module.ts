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
@NgModule({
  imports: [WarehouseSharedLibsModule, NgSelectModule, AgGridModule.withComponents([])],
  declarations: [
    FindLanguageFromKeyPipe,
    AlertComponent,
    AlertInfoComponent,
    AlertErrorComponent,
    LoginModalComponent,
    HasAnyAuthorityDirective,
  ],
  entryComponents: [LoginModalComponent, AlertInfoComponent],
  exports: [
    WarehouseSharedLibsModule,
    FindLanguageFromKeyPipe,
    AlertComponent,
    AlertErrorComponent,
    LoginModalComponent,
    HasAnyAuthorityDirective,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA],
})
export class WarehouseSharedModule {}
