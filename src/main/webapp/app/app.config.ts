import { ApplicationConfig, importProvidersFrom, inject, LOCALE_ID } from '@angular/core';
import { BrowserModule, Title } from '@angular/platform-browser';
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
import { ServiceWorkerModule } from '@angular/service-worker';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { NgbDateAdapter } from '@ng-bootstrap/ng-bootstrap';

import { DEBUG_INFO_ENABLED } from 'app/app.constants';
import './config/dayjs';
import { TranslationModule } from 'app/shared/language/translation.module';
import { httpInterceptorProviders } from './core/interceptor';
import routes from './app.routes';

import { NgbDateDayjsAdapter } from './config/datepicker-adapter';
import { AppPageTitleStrategy } from './app-page-title-strategy';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideNgxWebstorage, withLocalStorage, withSessionStorage } from 'ngx-webstorage';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';

const routerFeatures: Array<RouterFeatures> = [
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
  }),
];
if (DEBUG_INFO_ENABLED) {
  routerFeatures.push(withDebugTracing());
}
/* ModuleRegistry.registerModules([AllCommunityModule]);
provideGlobalGridOptions({ theme: 'legacy' }); */
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, ...routerFeatures),

    importProvidersFrom(BrowserModule),
    importProvidersFrom(BrowserAnimationsModule),
    // importProvidersFrom(ModuleRegistry.registerModules([ClientSideRowModelModule])),
    // Set this to true to enable service worker (PWA)
    importProvidersFrom(ServiceWorkerModule.register('ngsw-worker.js', { enabled: false })),
    importProvidersFrom(TranslationModule),
    provideHttpClient(withInterceptorsFromDi()),
    provideNgxWebstorage(withLocalStorage(), withSessionStorage()),
    Title,
    { provide: LOCALE_ID, useValue: 'fr' },
    { provide: NgbDateAdapter, useClass: NgbDateDayjsAdapter },
    httpInterceptorProviders,
    { provide: TitleStrategy, useClass: AppPageTitleStrategy },
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura,
      },
      /*  translation: {}, */
    }),
  ],
};
