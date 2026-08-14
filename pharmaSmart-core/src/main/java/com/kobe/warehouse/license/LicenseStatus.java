package com.kobe.warehouse.license;

/**
 * Statuts de licence (cf. PLAN-GESTION-LICENCE §3.2).
 *
 * <p>Seul {@link #readOnly} fait autorité pour l'exigence B4 : à l'expiration, toute modification
 * de données est refusée, mais la lecture, l'impression et les exports restent possibles
 * (obligation légale de traçabilité pharmaceutique).
 */
public enum LicenseStatus {
    /** {@code now < expiresAt - seuil d'alerte} — aucun effet. */
    VALID(false),
    /** Toast à la connexion (B2). */
    EXPIRING_SOON(false),
    /** Toast (B2) + bannière permanente non masquable (B3). */
    EXPIRING_CRITICAL(false),
    /** Échéance dépassée mais période de grâce en cours : écriture encore autorisée. */
    GRACE(false),
    /** Échéance + grâce dépassées : lecture seule (B4). */
    EXPIRED(true),
    /** Aucun fichier de licence trouvé, ni sur disque ni en base. */
    MISSING(true),
    /** Signature KO, JSON corrompu, officine ou empreinte divergente, {@code validFrom} futur. */
    INVALID(true),
    /** Horloge système reculée au-delà de la tolérance (cf. §3.3). */
    CLOCK_TAMPERED(true),
    /**
     * Quota de la version de démonstration atteint (§3.5).
     *
     * <p>Statut distinct de {@link #EXPIRED} : la licence est parfaitement valide, c'est son volume
     * d'usage qui est épuisé. Les confondre afficherait « abonnement expiré » à un prospect dont la
     * démo est encore en cours de validité, ce qui n'aurait aucun sens pour lui.
     */
    DEMO_QUOTA_REACHED(true),
    /**
     * Statut indéterminable : l'évaluation elle-même a échoué pour une raison technique — base
     * injoignable au démarrage, disque en erreur (§4.3).
     *
     * <p>Volontairement <strong>non</strong> bloquant. Une panne d'infrastructure n'est pas une
     * fraude : basculer l'officine en lecture seule parce que PostgreSQL n'avait pas fini de
     * démarrer transformerait un incident technique passager en incident commercial. Et le risque de
     * contournement est nul : sans base, aucune écriture n'aboutit de toute façon.
     */
    UNKNOWN(false);

    private final boolean readOnly;

    LicenseStatus(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /** {@code true} si toute écriture doit être refusée. */
    public boolean isReadOnly() {
        return readOnly;
    }

    /** Statuts déclenchant la bannière permanente (B3). */
    public boolean requiresBanner() {
        return this != VALID && this != EXPIRING_SOON && this != UNKNOWN;
    }
}
