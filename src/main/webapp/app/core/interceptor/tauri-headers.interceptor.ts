import { HttpInterceptorFn } from '@angular/common/http';

export const tauriHeadersInterceptor: HttpInterceptorFn = (req, next) => {
  // Only add headers if running in Tauri environment
  if (!isRunningInTauri()) {
    return next(req);
  }

  // Clone request and add Tauri identification headers
  const modifiedRequest = req.clone({
    setHeaders: {
      'X-Tauri-App': 'true',
    },
  });

  return next(modifiedRequest);
};

/**
 * Check if application is running in Tauri environment
 */
function isRunningInTauri(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }

  // Check for Tauri runtime internals (only exists in actual Tauri app, not browser)
  // @ts-ignore - __TAURI_INTERNALS__ is injected by Tauri at runtime
  return !!window.__TAURI_INTERNALS__;
}
