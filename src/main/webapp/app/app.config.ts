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
import { HttpClientModule } from '@angular/common/http';

import { NgbDateAdapter } from '@ng-bootstrap/ng-bootstrap';
import './config/dayjs';
import { TranslationModule } from 'app/shared/language/translation.module';
import { httpInterceptorProviders } from './core/interceptor';
import routes from './app.routes';

import { NgbDateDayjsAdapter } from './config/datepicker-adapter';
import { AppPageTitleStrategy } from './app-page-title-strategy';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgxWebstorageModule } from 'ngx-webstorage';

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
/*if (DEBUG_INFO_ENABLED) {
  routerFeatures.push(withDebugTracing());
}*/
routerFeatures.push(withDebugTracing());
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, ...routerFeatures),
    importProvidersFrom(BrowserModule),
    importProvidersFrom(BrowserAnimationsModule),
    // Set this to true to enable service worker (PWA)
    importProvidersFrom(ServiceWorkerModule.register('ngsw-worker.js', { enabled: false })),
    importProvidersFrom(TranslationModule),
    importProvidersFrom(HttpClientModule),
    importProvidersFrom(NgxWebstorageModule.forRoot()),

    Title,
    { provide: LOCALE_ID, useValue: 'fr' },
    { provide: NgbDateAdapter, useClass: NgbDateDayjsAdapter },
    httpInterceptorProviders,
    { provide: TitleStrategy, useClass: AppPageTitleStrategy },
  ],
};
