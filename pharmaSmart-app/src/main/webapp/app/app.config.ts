import {
  ApplicationConfig,
  inject,
  LOCALE_ID,
  provideAppInitializer,
  provideZoneChangeDetection
} from '@angular/core';
import {TauriDeviceDetectionService} from './shared/services/tauri-device-detection.service';
import {provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {
  NavigationError,
  provideRouter,
  RedirectCommand,
  Router,
  RouterFeatures,
  TitleStrategy,
  withComponentInputBinding,
  withDebugTracing,
  withNavigationErrorHandler,
} from '@angular/router';
import {provideServiceWorker} from '@angular/service-worker';
import {provideNgxWebstorage, withLocalStorage, withSessionStorage} from 'ngx-webstorage';

import {environment} from '../environments/environment';
import routes from './app.routes';
import {AppPageTitleStrategy} from './app-page-title-strategy';
import {httpInterceptorFeature, httpInterceptorProviders} from './core/interceptor';

import {
  MissingTranslationHandler,
  provideTranslateService
} from '@ngx-translate/core';
import {provideTranslateHttpLoader} from '@ngx-translate/http-loader';
import {CustomMissingTranslationHandler} from './config/translation.config';
import {translationInitializer} from './shared/language/translation.initializer';

import { deviseInitializer } from 'app/shared/utils/devise.initializer';
// --- Navigation error handler ---
// Retourner une RedirectCommand laisse le routeur conduire la redirection lui-même, au lieu de
// lancer une seconde navigation pendant que la première échoue.
function handleNavigationError(e: NavigationError): RedirectCommand {
  const router = inject(Router);
  switch (e.error?.status) {
    case 401:
      return new RedirectCommand(router.parseUrl('/login'));
    case 403:
      return new RedirectCommand(router.parseUrl('/accessdenied'));
    case 404:
      return new RedirectCommand(router.parseUrl('/404'));
    default:
      return new RedirectCommand(router.parseUrl('/error'));
  }
}

const routerFeatures: RouterFeatures[] = [withComponentInputBinding(), withNavigationErrorHandler(handleNavigationError)];

if (environment.DEBUG_INFO_ENABLED) {
  routerFeatures.push(withDebugTracing());
}

// --- AppConfig standalone ---
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({eventCoalescing: true}),
    // Ordre de la chaîne : intercepteurs de classe hérités, puis intercepteurs fonctionnels.
    provideHttpClient(withInterceptorsFromDi(), httpInterceptorFeature),

    // --- Routing ---
    provideRouter(routes, ...routerFeatures),
    {provide: TitleStrategy, useClass: AppPageTitleStrategy},

    // --- Service Worker ---
    provideServiceWorker('ngsw-worker.js', {enabled: environment.production}),

    // --- i18n / Locale ---
    {provide: LOCALE_ID, useValue: 'fr'},
    provideTranslateService({
      loader: provideTranslateHttpLoader({
        prefix: 'i18n/',
        suffix: `.json?_=${I18N_HASH}`,
      }),
      missingTranslationHandler: {
        provide: MissingTranslationHandler,
        useClass: CustomMissingTranslationHandler,
      },
    }),
    provideAppInitializer(translationInitializer),

    provideAppInitializer(deviseInitializer),
    provideAppInitializer(() => {
      const tauriDeviceService = inject(TauriDeviceDetectionService);
      if (tauriDeviceService.isTauriAvailable()) {
        return tauriDeviceService.loadSystemInfo();
      }
      return Promise.resolve();
    }),

    // --- Storage ---
    provideNgxWebstorage(withLocalStorage(), withSessionStorage()),

    // --- Interceptors (classes héritées, lues par withInterceptorsFromDi) ---
    httpInterceptorProviders,
  ],
};
