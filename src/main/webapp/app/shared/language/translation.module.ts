import { NgModule } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MissingTranslationHandler, TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { missingTranslationHandler, translatePartialLoader } from 'app/config/translation.config';
import { StateStorageService } from '../../core/auth/state-storage.service';

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
  constructor(
    private translateService: TranslateService,
    private stateStorageService: StateStorageService,
    //  private config: PrimeNGConfig,
  ) {
    translateService.setDefaultLang('fr');

    const langKey = this.stateStorageService.getLocale() ?? 'fr';
    this.translateService.use(langKey);
    // this.translateService.get('primeng').subscribe(res => this.config.setTranslation(res));
  }
}
