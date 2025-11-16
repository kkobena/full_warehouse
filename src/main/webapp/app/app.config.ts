import { ApplicationConfig, importProvidersFrom, inject, provideZoneChangeDetection, provideAppInitializer } from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import {
  NavigationError,
  provideRouter,
  Router,
  RouterFeatures,
  TitleStrategy,
  withComponentInputBinding,
  withDebugTracing,
  withNavigationErrorHandler,
} from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';
import { provideNgxWebstorage, withLocalStorage, withSessionStorage } from 'ngx-webstorage';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { environment } from '../environments/environment';
import routes from './app.routes';
import { AppPageTitleStrategy } from './app-page-title-strategy';
import { httpInterceptorProviders } from './core/interceptor';

import { provideTranslateService, MissingTranslationHandler } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { CustomMissingTranslationHandler } from './config/translation.config';
import { translationInitializer } from './shared/language/translation.initializer';

// --- Navigation error handler ---
function handleNavigationError(e: NavigationError) {
  const router = inject(Router);
  switch (e.error?.status) {
    case 401:
      router.navigate(['/login']);
      break;
    case 403:
      router.navigate(['/accessdenied']);
      break;
    case 404:
      router.navigate(['/404']);
      break;
    default:
      router.navigate(['/error']);
      break;
  }
}

const routerFeatures: RouterFeatures[] = [withComponentInputBinding(), withNavigationErrorHandler(handleNavigationError)];

if (environment.DEBUG_INFO_ENABLED) {
  routerFeatures.push(withDebugTracing());
}

// --- AppConfig standalone Angular 20 ---
export const appConfig: ApplicationConfig = {
  providers: [
    // --- Core Angular ---
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptorsFromDi()),

    // --- Routing ---
    provideRouter(routes, ...routerFeatures),
    { provide: TitleStrategy, useClass: AppPageTitleStrategy },

    // --- Service Worker ---
    provideServiceWorker('ngsw-worker.js', { enabled: environment.production }),

    // --- i18n / Locale ---
    provideTranslateService({
      loader: provideTranslateHttpLoader({
        prefix: 'i18n/',
        // @ts-ignore
        suffix: `.json?_=${I18N_HASH}`,
      }),
      missingTranslationHandler: {
        provide: MissingTranslationHandler,
        useClass: CustomMissingTranslationHandler,
      },
    }),
    provideAppInitializer(translationInitializer),

    // --- Storage ---
    provideNgxWebstorage(withLocalStorage(), withSessionStorage()),

    // --- UI / Animations ---
    provideNoopAnimations(),
    importProvidersFrom([]),
    providePrimeNG({ theme: { preset: Aura } }),

    // --- Interceptors ---
    httpInterceptorProviders,
  ],
};
