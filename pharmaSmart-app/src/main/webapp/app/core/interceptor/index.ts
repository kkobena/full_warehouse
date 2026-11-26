import { HTTP_INTERCEPTORS, HttpFeature, HttpFeatureKind, withInterceptors } from '@angular/common/http';
import { Provider } from '@angular/core';

import { AuthExpiredInterceptor } from 'app/core/interceptor/auth-expired.interceptor';
import { ErrorHandlerInterceptor } from 'app/core/interceptor/error-handler.interceptor';
import { NotificationInterceptor } from 'app/core/interceptor/notification.interceptor';
import { authJwtInterceptor } from 'app/core/interceptor/auth-jwt.interceptor';
import { apiBaseUrlInterceptor } from 'app/core/interceptor/api-base-url.interceptor';
import { tauriHeadersInterceptor } from 'app/core/interceptor/tauri-headers.interceptor';
import { licenseInterceptor } from 'app/core/interceptor/license.interceptor';

/**
 * Intercepteurs fonctionnels, à passer à `provideHttpClient(...)` : c'est une option du client HTTP,
 * pas un provider. Placée dans le tableau `providers`, elle n'était prise en compte que par
 * accident (Angular la confondait avec un `EnvironmentProviders`).
 *
 * - apiBaseUrlInterceptor en tête : l'URL doit être définitive avant l'ajout du jeton JWT ;
 * - tauriHeadersInterceptor ajoute l'en-tête X-Tauri-App du poste de bureau ;
 * - licenseInterceptor après authJwtInterceptor : il n'observe que les réponses, et n'a de sens
 *   que sur des requêtes déjà authentifiées (cf. PLAN-GESTION-LICENCE §5.4).
 */
export const httpInterceptorFeature: HttpFeature<HttpFeatureKind.Interceptors> = withInterceptors([
  apiBaseUrlInterceptor,
  tauriHeadersInterceptor,
  authJwtInterceptor,
  licenseInterceptor,
]);

/**
 * Intercepteurs de classe hérités, lus par `withInterceptorsFromDi()`.
 */
export const httpInterceptorProviders: Provider[] = [
  { provide: HTTP_INTERCEPTORS, useClass: AuthExpiredInterceptor, multi: true },
  { provide: HTTP_INTERCEPTORS, useClass: ErrorHandlerInterceptor, multi: true },
  { provide: HTTP_INTERCEPTORS, useClass: NotificationInterceptor, multi: true },
];
