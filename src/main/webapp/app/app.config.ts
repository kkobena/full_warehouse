import {
  ApplicationConfig,
  importProvidersFrom,
  inject,
  LOCALE_ID,
  provideZoneChangeDetection
} from '@angular/core';
import { BrowserModule, Title } from '@angular/platform-browser';
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
import { ServiceWorkerModule } from '@angular/service-worker';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import './config/dayjs';
import { TranslationModule } from 'app/shared/language/translation.module';
import { environment } from 'environments/environment';
import { httpInterceptorProviders } from './core/interceptor';
import routes from './app.routes';

import { AppPageTitleStrategy } from './app-page-title-strategy';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideNgxWebstorage, withLocalStorage, withSessionStorage } from 'ngx-webstorage';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';

const routerFeatures: RouterFeatures[] = [
  withComponentInputBinding(),
  withNavigationErrorHandler((e: NavigationError) => {
    const router = inject(Router);
    if (e.error.status === 403) {
      router.navigate(['/accessdenied']);
    } else if (e.error.status === 404) {
      router.navigate(['/404']);
    } else if (e.error.status === 401) {
      router.navigate(['/login']);
    } else {
      router.navigate(['/error']);
    }
  })
];
if (environment.DEBUG_INFO_ENABLED) {
  routerFeatures.push(withDebugTracing());
}

export const appConfig: ApplicationConfig = {
  providers: [
    /* provideZoneChangeDetection({ eventCoalescing: true, runCoalescing: true }), */
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, ...routerFeatures),

    importProvidersFrom(BrowserModule),
    importProvidersFrom(BrowserAnimationsModule),
    importProvidersFrom(ServiceWorkerModule.register('ngsw-worker.js', { enabled: false })),
    importProvidersFrom(TranslationModule),
    provideHttpClient(withInterceptorsFromDi()),
    provideNgxWebstorage(withLocalStorage(), withSessionStorage()),
    Title,
    { provide: LOCALE_ID, useValue: 'fr' },
    httpInterceptorProviders,
    { provide: TitleStrategy, useClass: AppPageTitleStrategy },
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura
      }
      /*  translation: {}, */
    })
  ]
};
