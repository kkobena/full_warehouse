import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AppSettingsService } from '../config/app-settings.service';
import { environment } from '../../../environments/environment';

/**
 * Interceptor that prepends the API server URL to all API requests.
 *
 * This is necessary for:
 * 1. Electron apps where the app is loaded from file:// protocol
 * 2. Web apps accessing a backend on a different host/port
 * 3. LAN deployments where backend is on another machine
 *
 * The API server URL can be configured at runtime via AppSettingsService,
 * allowing users to connect to any backend on their network.
 */
export const apiBaseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  // Only modify requests that start with 'api/' or '/api/'
  const url = req.url;
  if (url.startsWith('api/') || url.startsWith('/api/')) {
    const appSettingsService = inject(AppSettingsService);

    // Priority: 1. User settings, 2. Environment config, 3. Relative URLs
    const apiServerUrl = appSettingsService.getApiServerUrl() || environment.apiServerUrl;

    // If apiServerUrl is configured (Electron/production), prepend it
    if (apiServerUrl) {
      const absoluteUrl = url.startsWith('/') ? `${apiServerUrl}${url}` : `${apiServerUrl}/${url}`;

      const clonedRequest = req.clone({
        url: absoluteUrl,
      });

      return next(clonedRequest);
    }
  }

  // Otherwise, pass through unchanged (relative URLs for dev server)
  return next(req);
};
