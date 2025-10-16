import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { JwtTokenService } from '../auth/jwt-token.service';

/**
 * HTTP Interceptor that adds JWT token to all outgoing requests.
 *
 * Automatically adds Authorization header with Bearer token for:
 * - All /api/** requests
 * - All /java-client/** requests
 *
 * Excludes:
 * - /api/auth/login (login endpoint doesn't need token)
 * - /api/auth/refresh (uses refresh token separately)
 * - Public endpoints
 */
export const authJwtInterceptor: HttpInterceptorFn = (req, next) => {
  const jwtTokenService = inject(JwtTokenService);

  // Skip adding token for login and refresh endpoints
  if (req.url.includes('/api/auth/login') || req.url.includes('/api/auth/refresh')) {
    return next(req);
  }

  // Skip adding token for public endpoints
  if (
    req.url.includes('/api/register') ||
    req.url.includes('/api/activate') ||
    req.url.includes('/api/account/reset-password') ||
    req.url.includes('/management/health') ||
    req.url.includes('/management/info')
  ) {
    return next(req);
  }

  // Get access token from storage
  const accessToken = jwtTokenService.getAccessToken();

  // Add Authorization header if token exists
  if (accessToken) {
    const clonedRequest = req.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
    return next(clonedRequest);
  }

  // No token, proceed without Authorization header
  return next(req);
};
