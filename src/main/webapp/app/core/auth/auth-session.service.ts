import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { Login } from 'app/login/login.model';
import { ApplicationConfigService } from '../config/application-config.service';
import { JwtTokenService } from './jwt-token.service';
import { JwtTokenResponse } from './jwt-token.model';

/**
 * Authentication Service Provider using JWT tokens.
 *
 * Handles:
 * - Login via /api/auth/login (returns JWT tokens)
 * - Logout (clears JWT tokens)
 * - Token storage via JwtTokenService
 */
@Injectable({ providedIn: 'root' })
export class AuthServerProvider {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly jwtTokenService = inject(JwtTokenService);

  /**
   * Login user with username/password and store JWT tokens.
   *
   * POST /api/auth/login
   * Request body: { username, password }
   * Response: { accessToken, refreshToken, tokenType, expiresIn }
   *
   * @param credentials User login credentials
   * @returns Observable that completes when login succeeds
   */
  login(credentials: Login): Observable<JwtTokenResponse> {
    const loginRequest = {
      username: credentials.username,
      password: credentials.password,
    };

    return this.http
      .post<JwtTokenResponse>(this.applicationConfigService.getEndpointFor('api/auth/login'), loginRequest)
      .pipe(
        tap(response => {
          // Store JWT tokens in localStorage
          this.jwtTokenService.storeTokens(response);
        }),
      );
  }

  /**
   * Logout user by clearing stored JWT tokens.
   *
   * Note: JWT is stateless, so server-side logout is not needed.
   * We just clear tokens from browser storage.
   *
   * @returns Observable that completes immediately
   */
  logout(): Observable<void> {
    return new Observable(observer => {
      // Clear JWT tokens from storage
      this.jwtTokenService.clearTokens();
      observer.next();
      observer.complete();
    });
  }

  /**
   * Refresh access token using refresh token.
   *
   * POST /api/auth/refresh
   * Request body: { refreshToken }
   * Response: { accessToken, refreshToken, tokenType, expiresIn }
   *
   * @returns Observable with new token response
   */
  refreshToken(): Observable<JwtTokenResponse> {
    const refreshToken = this.jwtTokenService.getRefreshToken();

    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    return this.http.post<JwtTokenResponse>(this.applicationConfigService.getEndpointFor('api/auth/refresh'), refreshToken).pipe(
      tap(response => {
        // Update stored tokens with new access token
        this.jwtTokenService.updateAccessToken(response);
      }),
    );
  }
}
