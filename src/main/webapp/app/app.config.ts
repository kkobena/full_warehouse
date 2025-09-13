import {
  ApplicationConfig,
  importProvidersFrom,
  inject,
  provideZoneChangeDetection,
  provideAppInitializer
} from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import {
  NavigationError,
  provideRouter,
  Router,
  RouterFeatures,
  TitleStrategy,
  withComponentInputBinding,
  withDebugTracing,
  withNavigationErrorHandler
} from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';
import { provideNgxWebstorage, withLocalStorage, withSessionStorage } from 'ngx-webstorage';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';

import { environment } from '../environments/environment';
import routes from './app.routes';
import { AppPageTitleStrategy } from './app-page-title-strategy';
import { httpInterceptorProviders } from './core/interceptor';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideTranslations, translationInitializer } from './shared/language/translation.module';

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
    provideTranslations,
    provideAppInitializer(translationInitializer),

    // --- Storage ---
    provideNgxWebstorage(withLocalStorage(), withSessionStorage()),

    // --- UI / Animations ---
    provideAnimationsAsync(),
    importProvidersFrom([]), // ici tu peux importer tes modules animations si besoin
    providePrimeNG({ theme: { preset: Aura } }),

    // --- Interceptors ---
    httpInterceptorProviders,
  ],
};
