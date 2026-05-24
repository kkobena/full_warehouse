import { inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { Observable } from 'rxjs';

export const translationInitializer = (): Observable<any> => {
  const translateService = inject(TranslateService);
  const stateStorageService = inject(StateStorageService);
  translateService.setFallbackLang('fr');
  const langKey = stateStorageService.getLocale() ?? 'fr';
  return translateService.use(langKey);
};
