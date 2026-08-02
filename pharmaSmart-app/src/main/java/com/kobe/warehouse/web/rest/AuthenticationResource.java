package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.JwtService;
import com.kobe.warehouse.service.dto.JwtTokenDTO;
import com.kobe.warehouse.service.dto.LoginRequestDTO;
import com.kobe.warehouse.service.dto.RefreshTokenRequestDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication using JWT tokens.
 *
 * Flow:
 * 1. Client sends POST to /api/auth/login with username/password
 * 2. Backend validates credentials against database
 * 3. If valid, generate JWT access token and refresh token
 * 4. Return tokens to client
 * 5. Client stores tokens and includes access token in Authorization header for subsequent requests
 */
@RestController
@RequestMapping("/api/auth")
public class AuthenticationResource {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationResource.class);

    /** Durée de validité de l'access token, en heures. */
    private static final long ACCESS_TOKEN_HOURS = 8;
    /**
     * Durée de validité du refresh token, en heures.
     *
     * <p>48 h : couvre l'amplitude d'ouverture de l'officine sur deux jours — un
     * opérateur qui reprend son poste le lendemain n'a pas à se reconnecter — tout en
     * limitant l'exposition d'un jeton intercepté, le trafic circulant aujourd'hui en
     * clair sur le réseau local (cf. docs/PLAN-HTTPS-ET-CERTIFICATS.md). La rotation à
     * chaque renouvellement fait glisser la fenêtre tant que l'utilisateur reste actif.
     */
    private static final long REFRESH_TOKEN_HOURS = 48;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtDecoder jwtDecoder;
    private final UserDetailsService userDetailsService;

    public AuthenticationResource(
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        JwtDecoder jwtDecoder,
        UserDetailsService userDetailsService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtDecoder = jwtDecoder;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Authenticate user with username/password and return JWT tokens.
     *
     * POST /api/auth/login
     *
     * @param loginRequest Login credentials
     * @return JWT access token and refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<JwtTokenDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            // Authenticate user credentials against database
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
            );

            // Generate JWT tokens
            String accessToken = jwtService.generateAccessToken(authentication, ACCESS_TOKEN_HOURS);
            String refreshToken = jwtService.generateRefreshToken(authentication, REFRESH_TOKEN_HOURS);

            JwtTokenDTO tokenResponse = new JwtTokenDTO(
                accessToken,
                refreshToken,
                ACCESS_TOKEN_HOURS * 3600 // expiration in seconds
            );

            log.debug("User {} authenticated successfully", loginRequest.username());
            return ResponseEntity.ok(tokenResponse);
        } catch (BadCredentialsException e) {
            log.debug("Authentication failed for user: {}", loginRequest.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Renouvelle l'access token à partir d'un refresh token.
     *
     * <p>POST /api/auth/refresh
     *
     * <p>L'utilisateur est <b>rechargé depuis la base</b> plutôt que de réutiliser les
     * droits figés dans le refresh token : un compte désactivé ou dont les rôles ont
     * changé depuis la connexion ne doit pas conserver ses anciens privilèges pendant
     * la durée de validité du refresh token.
     *
     * @param request refresh token obtenu à la connexion
     * @return nouveaux tokens, ou 401 si le refresh token est invalide, expiré, ou si le
     * compte n'est plus utilisable
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtTokenDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        try {
            Jwt refreshToken = jwtDecoder.decode(request.refreshToken());

            // Un access token ne doit pas pouvoir servir de refresh token
            if (!"refresh".equals(refreshToken.getClaimAsString("token_type"))) {
                log.debug("Refresh refusé : claim token_type absent ou incorrect");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String login = refreshToken.getSubject();
            UserDetails userDetails = userDetailsService.loadUserByUsername(login);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );

            String accessToken = jwtService.generateAccessToken(authentication, ACCESS_TOKEN_HOURS);
            // Rotation : le refresh token consommé est remplacé, la session glisse tant
            // que l'utilisateur reste actif
            String newRefreshToken = jwtService.generateRefreshToken(authentication, REFRESH_TOKEN_HOURS);

            log.debug("Access token renouvelé pour l'utilisateur {}", login);
            return ResponseEntity.ok(
                new JwtTokenDTO(accessToken, newRefreshToken, ACCESS_TOKEN_HOURS * 3600)
            );
        } catch (JwtException e) {
            log.debug("Refresh token invalide ou expiré : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (AuthenticationException e) {
            log.debug("Refresh refusé, compte inutilisable : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
