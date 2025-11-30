package com.kobe.warehouse.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";
    public static final String ROLE_RESPONSABLE_COMMANDE = "ROLE_RESPONSABLE_COMMANDE";
    public static final String ROLE_CAISSIER = "ROLE_CAISSIER";
    public static final String ROLE_VENDEUR = "ROLE_VENDEUR";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    private AuthoritiesConstants() {}
}
