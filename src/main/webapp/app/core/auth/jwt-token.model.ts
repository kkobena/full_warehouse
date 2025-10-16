/**
 * JWT Token Response from /api/auth/login endpoint
 */
export interface JwtTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number; // seconds
}

/**
 * Stored JWT Token information
 */
export interface JwtToken {
  accessToken: string;
  refreshToken: string;
  expiresAt: number; // timestamp in milliseconds
}
