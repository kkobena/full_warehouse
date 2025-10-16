import { Injectable } from '@angular/core';
import { JwtToken, JwtTokenResponse } from './jwt-token.model';

/**
 * Service for managing JWT tokens in browser storage.
 *
 * Handles:
 * - Storing access token and refresh token
 * - Retrieving tokens
 * - Checking token expiration
 * - Clearing tokens on logout
 */
@Injectable({ providedIn: 'root' })
export class JwtTokenService {
  private readonly ACCESS_TOKEN_KEY = 'pharma_smart_access_token';
  private readonly REFRESH_TOKEN_KEY = 'pharma_smart_refresh_token';
  private readonly TOKEN_EXPIRY_KEY = 'pharma_smart_token_expiry';

  /**
   * Store JWT tokens in localStorage after successful login.
   *
   * @param tokenResponse Response from /api/auth/login
   */
  storeTokens(tokenResponse: JwtTokenResponse): void {
    const expiresAt = Date.now() + tokenResponse.expiresIn * 1000;

    localStorage.setItem(this.ACCESS_TOKEN_KEY, tokenResponse.accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, tokenResponse.refreshToken);
    localStorage.setItem(this.TOKEN_EXPIRY_KEY, expiresAt.toString());
  }

  /**
   * Get current access token.
   *
   * @returns Access token or null if not found
   */
  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  /**
   * Get current refresh token.
   *
   * @returns Refresh token or null if not found
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Get full JWT token information.
   *
   * @returns JWT token object or null if no token stored
   */
  getToken(): JwtToken | null {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();
    const expiresAt = localStorage.getItem(this.TOKEN_EXPIRY_KEY);

    if (!accessToken || !refreshToken || !expiresAt) {
      return null;
    }

    return {
      accessToken,
      refreshToken,
      expiresAt: parseInt(expiresAt, 10),
    };
  }

  /**
   * Check if current access token is expired.
   *
   * @returns true if token is expired or doesn't exist
   */
  isTokenExpired(): boolean {
    const expiresAt = localStorage.getItem(this.TOKEN_EXPIRY_KEY);
    if (!expiresAt) {
      return true;
    }

    // Add 60 second buffer to refresh before actual expiration
    return Date.now() >= parseInt(expiresAt, 10) - 60000;
  }

  /**
   * Check if user has a valid (non-expired) token.
   *
   * @returns true if valid token exists
   */
  hasValidToken(): boolean {
    return this.getAccessToken() !== null && !this.isTokenExpired();
  }

  /**
   * Clear all stored tokens (logout).
   */
  clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.TOKEN_EXPIRY_KEY);
  }

  /**
   * Update access token after refresh (keeps same refresh token).
   *
   * @param tokenResponse New token response from refresh endpoint
   */
  updateAccessToken(tokenResponse: JwtTokenResponse): void {
    const expiresAt = Date.now() + tokenResponse.expiresIn * 1000;

    localStorage.setItem(this.ACCESS_TOKEN_KEY, tokenResponse.accessToken);
    localStorage.setItem(this.TOKEN_EXPIRY_KEY, expiresAt.toString());

    // Only update refresh token if provided
    if (tokenResponse.refreshToken) {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, tokenResponse.refreshToken);
    }
  }
}
