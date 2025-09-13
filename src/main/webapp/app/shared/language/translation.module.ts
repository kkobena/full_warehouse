import { importProvidersFrom, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MissingTranslationHandler, TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { missingTranslationHandler, translatePartialLoader } from 'app/config/translation.config';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { Observable } from 'rxjs';

export const provideTranslations = importProvidersFrom(
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
);

export const translationInitializer = (): Observable<any> => {
  const translateService = inject(TranslateService);
  const stateStorageService = inject(StateStorageService);
  translateService.setDefaultLang('fr');
  const langKey = stateStorageService.getLocale() ?? 'fr';
  return translateService.use(langKey);
};
