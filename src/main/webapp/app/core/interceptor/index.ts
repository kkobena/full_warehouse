import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { AuthExpiredInterceptor } from 'app/core/interceptor/auth-expired.interceptor';
import { ErrorHandlerInterceptor } from 'app/core/interceptor/error-handler.interceptor';
import { NotificationInterceptor } from 'app/core/interceptor/notification.interceptor';
import { authJwtInterceptor } from 'app/core/interceptor/auth-jwt.interceptor';
import { apiBaseUrlInterceptor } from 'app/core/interceptor/api-base-url.interceptor';
import { tauriHeadersInterceptor } from 'app/core/interceptor/tauri-headers.interceptor';

export const httpInterceptorProviders = [
  // API base URL interceptor (functional) - prepends server URL for Electron
  // Tauri headers interceptor - adds X-Tauri-App header for desktop app identification
  // Must be FIRST to ensure correct URL before JWT interceptor
  withInterceptors([apiBaseUrlInterceptor, tauriHeadersInterceptor, authJwtInterceptor]),

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
