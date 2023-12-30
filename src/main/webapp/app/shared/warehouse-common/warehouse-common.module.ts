import { CUSTOM_ELEMENTS_SCHEMA, LOCALE_ID, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertComponent } from '../alert/alert.component';
import { AlertErrorComponent } from '../alert/alert-error.component';
import { FindLanguageFromKeyPipe } from '../language/find-language-from-key.pipe';
import { TranslateDirective } from '../language/translate.directive';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslateModule } from '@ngx-translate/core';
import { AlertInfoComponent } from '../alert/alert-info.component';

@NgModule({
  declarations: [],
  imports: [AlertComponent, AlertInfoComponent, AlertErrorComponent, FindLanguageFromKeyPipe, TranslateDirective],
  exports: [
    CommonModule,
    NgbModule,
    FontAwesomeModule,
    AlertComponent,
    AlertErrorComponent,
    TranslateModule,
    FindLanguageFromKeyPipe,
    TranslateDirective,
    AlertInfoComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  providers: [{ provide: LOCALE_ID, useValue: 'fr' }],
})
export class WarehouseCommonModule {}
