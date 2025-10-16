import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { AuthExpiredInterceptor } from 'app/core/interceptor/auth-expired.interceptor';
import { ErrorHandlerInterceptor } from 'app/core/interceptor/error-handler.interceptor';
import { NotificationInterceptor } from 'app/core/interceptor/notification.interceptor';
import { authJwtInterceptor } from 'app/core/interceptor/auth-jwt.interceptor';

export const httpInterceptorProviders = [
  // JWT interceptor (functional) - adds Authorization header
  withInterceptors([authJwtInterceptor]),

  // Legacy class-based interceptors
  {
    provide: HTTP_INTERCEPTORS,
    useClass: AuthExpiredInterceptor,
    multi: true,
  },
  {
    provide: HTTP_INTERCEPTORS,
    useClass: ErrorHandlerInterceptor,
    multi: true,
  },
  {
    provide: HTTP_INTERCEPTORS,
    useClass: NotificationInterceptor,
    multi: true,
  },
];
