import {NgModule} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {MissingTranslationHandler, TranslateLoader, TranslateModule, TranslateService} from '@ngx-translate/core';
import {missingTranslationHandler, translatePartialLoader} from 'app/config/translation.config';
import {SessionStorageService} from 'ngx-webstorage';
import {PrimeNGConfig} from "primeng/api";

@NgModule({
  imports: [
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: translatePartialLoader,
        deps: [HttpClient],
      },
      missingTranslationHandler: {
        provide: MissingTranslationHandler,
        useFactory: missingTranslationHandler,
      },
    }),
  ],
})
export class TranslationModule {
  constructor(private translateService: TranslateService, sessionStorageService: SessionStorageService, private config: PrimeNGConfig) {
    translateService.setDefaultLang('fr');
    // if user have changed language and navigates away from the application and back to the application then use previously choosed language
    const langKey = sessionStorageService.retrieve('locale') ?? 'fr';
    translateService.use(langKey);
    this.translateService.use(langKey);
    this.translateService.get('primeng').subscribe(res => this.config.setTranslation(res));
  }
}
